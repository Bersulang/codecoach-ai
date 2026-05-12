package com.codecoach.module.ai.service.impl;

import com.codecoach.common.exception.BusinessException;
import com.codecoach.module.ai.config.AiProperties;
import com.codecoach.module.ai.entity.AiCallLog;
import com.codecoach.module.ai.enums.AiRequestType;
import com.codecoach.module.ai.model.QuestionFeedbackResult;
import com.codecoach.module.ai.model.QuestionPracticeContext;
import com.codecoach.module.ai.model.QuestionReportGenerateResult;
import com.codecoach.module.ai.service.AiCallLogService;
import com.codecoach.module.ai.service.AiQuestionPracticeService;
import com.codecoach.module.ai.service.AiTokenStreamHandler;
import com.codecoach.module.ai.service.PromptTemplateService;
import com.codecoach.module.ai.support.AiJsonParser;
import com.codecoach.module.ai.support.AiResponseValidator;
import com.codecoach.module.ai.support.AiTextSanitizer;
import com.codecoach.module.ai.support.InterviewDifficultyStrategy;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Service
@ConditionalOnProperty(prefix = "ai", name = "provider", havingValue = "openai-compatible")
public class OpenAiCompatibleQuestionPracticeServiceImpl implements AiQuestionPracticeService {

    private static final Logger log = LoggerFactory.getLogger(OpenAiCompatibleQuestionPracticeServiceImpl.class);

    private static final Integer AI_CALL_FAILED_CODE = 3003;

    private static final String AI_CALL_FAILED_MESSAGE = "AI 调用失败，请稍后重试";

    private static final String PROMPT_VERSION = "v1";

    private static final int SUCCESS = 1;

    private static final int FAILURE = 0;

    private static final String FIRST_QUESTION_TEMPLATE = "question_first_question.md";

    private static final String FEEDBACK_NEXT_QUESTION_TEMPLATE = "question_feedback_next_question.md";

    private static final String REPORT_TEMPLATE = "question_report.md";

    private static final String SYSTEM_PROMPT = "你是一个专业的 Java 后端八股问答面试官，"
            + "需要围绕指定知识点进行准确、具体、循序渐进的追问训练。";

    private static final int DEFAULT_TIMEOUT_SECONDS = 120;

    private final AiProperties aiProperties;

    private final PromptTemplateService promptTemplateService;

    private final AiJsonParser aiJsonParser;

    private final AiResponseValidator aiResponseValidator;

    private final AiCallLogService aiCallLogService;

    private final ObjectMapper objectMapper;

    private final RestClient restClient;

    private final HttpClient httpClient;

    public OpenAiCompatibleQuestionPracticeServiceImpl(
            AiProperties aiProperties,
            PromptTemplateService promptTemplateService,
            AiJsonParser aiJsonParser,
            AiResponseValidator aiResponseValidator,
            AiCallLogService aiCallLogService,
            ObjectMapper objectMapper
    ) {
        this.aiProperties = aiProperties;
        this.promptTemplateService = promptTemplateService;
        this.aiJsonParser = aiJsonParser;
        this.aiResponseValidator = aiResponseValidator;
        this.aiCallLogService = aiCallLogService;
        this.objectMapper = objectMapper;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        Duration timeout = Duration.ofSeconds(resolveTimeoutSeconds(aiProperties));
        requestFactory.setConnectTimeout(timeout);
        requestFactory.setReadTimeout(timeout);
        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(timeout)
                .build();
    }

    @Override
    public String generateFirstQuestion(QuestionPracticeContext context) {
        long startTime = System.currentTimeMillis();
        AiCallLog callLog = buildCallLog(AiRequestType.QUESTION_FIRST_QUESTION, context);

        try {
            String prompt = promptTemplateService.render(FIRST_QUESTION_TEMPLATE, buildQuestionVariables(context));
            ChatResult chatResult = chat(List.of(systemMessage(), userMessage(prompt)), 0.7);
            String content = chatResult.getContent();
            if (!StringUtils.hasText(content)) {
                throw new AiCallException(
                        "EMPTY_CONTENT",
                        "AI returned empty question practice first question",
                        chatResult.getStatusCode(),
                        chatResult.getRawResponse(),
                        chatResult.getRequestId(),
                        null
                );
            }
            recordSuccess(callLog, chatResult, startTime);
            return content.trim();
        } catch (AiCallException exception) {
            recordFailure(callLog, exception, startTime);
            throw aiCallFailed(exception.getMessage(), exception);
        } catch (BusinessException exception) {
            recordFailure(callLog, "PROMPT_RENDER_FAILED", "PROMPT_RENDER_FAILED", null, startTime);
            throw exception;
        }
    }

    @Override
    public QuestionFeedbackResult generateFeedbackAndNextQuestion(
            QuestionPracticeContext context,
            boolean needNextQuestion
    ) {
        long startTime = System.currentTimeMillis();
        AiCallLog callLog = buildCallLog(AiRequestType.QUESTION_FEEDBACK_NEXT_QUESTION, context);

        try {
            String prompt = promptTemplateService.render(FEEDBACK_NEXT_QUESTION_TEMPLATE, buildQuestionVariables(context));
            ChatResult chatResult = chat(List.of(systemMessage(), userMessage(prompt)), 0.7);
            QuestionFeedbackResult result;
            try {
                result = aiJsonParser.parseObject(chatResult.getContent(), QuestionFeedbackResult.class);
                if (needNextQuestion) {
                    aiResponseValidator.validateQuestionFeedbackAndNextQuestion(result);
                } else {
                    aiResponseValidator.validateQuestionFeedbackOnly(result);
                }
            } catch (BusinessException exception) {
                throw new AiCallException(
                        "JSON_PARSE_FAILED",
                        "JSON_PARSE_FAILED",
                        chatResult.getStatusCode(),
                        chatResult.getRawResponse(),
                        chatResult.getRequestId(),
                        exception
                );
            }
            recordSuccess(callLog, chatResult, startTime);
            return result;
        } catch (AiCallException exception) {
            recordFailure(callLog, exception, startTime);
            throw aiCallFailed(exception.getMessage(), exception);
        } catch (BusinessException exception) {
            recordFailure(callLog, "PROMPT_RENDER_FAILED", "PROMPT_RENDER_FAILED", null, startTime);
            throw exception;
        }
    }

    @Override
    public QuestionFeedbackResult generateFeedbackAndNextQuestionStream(
            QuestionPracticeContext context,
            boolean needNextQuestion,
            AiTokenStreamHandler streamHandler
    ) {
        long startTime = System.currentTimeMillis();
        AiCallLog callLog = buildCallLog(AiRequestType.QUESTION_FEEDBACK_NEXT_QUESTION, context);

        try {
            String prompt = promptTemplateService.render(FEEDBACK_NEXT_QUESTION_TEMPLATE, buildQuestionVariables(context));
            ChatResult chatResult = chatStream(
                    List.of(systemMessage(), userMessage(prompt)),
                    0.7,
                    visibleQuestionFeedbackStream(streamHandler)
            );
            QuestionFeedbackResult result;
            try {
                result = aiJsonParser.parseObject(chatResult.getContent(), QuestionFeedbackResult.class);
                if (needNextQuestion) {
                    aiResponseValidator.validateQuestionFeedbackAndNextQuestion(result);
                } else {
                    aiResponseValidator.validateQuestionFeedbackOnly(result);
                }
            } catch (BusinessException exception) {
                throw new AiCallException(
                        "JSON_PARSE_FAILED",
                        "JSON_PARSE_FAILED",
                        chatResult.getStatusCode(),
                        chatResult.getRawResponse(),
                        chatResult.getRequestId(),
                        exception
                );
            }
            recordSuccess(callLog, chatResult, startTime);
            return result;
        } catch (AiCallException exception) {
            recordFailure(callLog, exception, startTime);
            throw aiCallFailed(exception.getMessage(), exception);
        } catch (BusinessException exception) {
            recordFailure(callLog, "PROMPT_RENDER_FAILED", "PROMPT_RENDER_FAILED", null, startTime);
            throw exception;
        }
    }

    @Override
    public QuestionReportGenerateResult generateReport(QuestionPracticeContext context) {
        long startTime = System.currentTimeMillis();
        AiCallLog callLog = buildCallLog(AiRequestType.QUESTION_REPORT, context);

        try {
            String prompt = promptTemplateService.render(REPORT_TEMPLATE, buildQuestionVariables(context));
            ChatResult chatResult = chat(List.of(systemMessage(), userMessage(prompt)), 0.7);
            QuestionReportGenerateResult result;
            try {
                result = aiJsonParser.parseObject(chatResult.getContent(), QuestionReportGenerateResult.class);
                aiResponseValidator.validateQuestionReport(result);
            } catch (BusinessException exception) {
                throw new AiCallException(
                        "JSON_PARSE_FAILED",
                        "JSON_PARSE_FAILED",
                        chatResult.getStatusCode(),
                        chatResult.getRawResponse(),
                        chatResult.getRequestId(),
                        exception
                );
            }
            recordSuccess(callLog, chatResult, startTime);
            return result;
        } catch (AiCallException exception) {
            recordFailure(callLog, exception, startTime);
            throw aiCallFailed(exception.getMessage(), exception);
        } catch (BusinessException exception) {
            recordFailure(callLog, "PROMPT_RENDER_FAILED", "PROMPT_RENDER_FAILED", null, startTime);
            throw exception;
        }
    }

    private ChatResult chat(List<ChatMessage> messages, double temperature) {
        AiProperties.OpenAiCompatible config = getConfig();
        String endpoint = resolveChatCompletionsEndpoint(config.getBaseUrl());
        OpenAiChatRequest request = new OpenAiChatRequest(config.getModel(), messages, temperature);

        try {
            ResponseEntity<String> responseEntity = restClient.post()
                    .uri(endpoint)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + config.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .toEntity(String.class);
            OpenAiChatResponse response = readChatResponse(
                    responseEntity.getBody(),
                    responseEntity.getStatusCode().value(),
                    getRequestId(responseEntity.getHeaders(), null)
            );
            String requestId = getRequestId(responseEntity.getHeaders(), response);
            String content = extractContent(
                    response,
                    responseEntity.getBody(),
                    responseEntity.getStatusCode().value(),
                    requestId
            );
            return new ChatResult(
                    content,
                    responseEntity.getBody(),
                    responseEntity.getStatusCode().value(),
                    requestId,
                    response.getUsage()
            );
        } catch (RestClientResponseException ex) {
            ErrorInfo errorInfo = parseErrorInfo(ex.getResponseBodyAsString());
            throw new AiCallException(
                    errorInfo.getErrorCode(),
                    errorInfo.getErrorMessage(),
                    ex.getStatusCode().value(),
                    ex.getResponseBodyAsString(),
                    getRequestId(ex.getResponseHeaders(), null),
                    ex
            );
        } catch (ResourceAccessException ex) {
            if (isTimeoutException(ex)) {
                throw new AiCallException("TIMEOUT", "TIMEOUT", ex);
            }
            throw new AiCallException("NETWORK_ERROR", "NETWORK_ERROR", ex);
        } catch (RestClientException ex) {
            if (isTimeoutException(ex)) {
                throw new AiCallException("TIMEOUT", "TIMEOUT", ex);
            }
            throw new AiCallException("HTTP_REQUEST_FAILED", "HTTP_REQUEST_FAILED", ex);
        }
    }

    private ChatResult chatStream(
            List<ChatMessage> messages,
            double temperature,
            AiTokenStreamHandler streamHandler
    ) {
        AiProperties.OpenAiCompatible config = getConfig();
        String endpoint = resolveChatCompletionsEndpoint(config.getBaseUrl());

        try {
            String requestBody = objectMapper.writeValueAsString(
                    new OpenAiChatRequest(config.getModel(), messages, temperature, true)
            );
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(resolveTimeoutSeconds(aiProperties)))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + config.getApiKey())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .header(HttpHeaders.ACCEPT, MediaType.TEXT_EVENT_STREAM_VALUE)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();
            HttpResponse<Stream<String>> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofLines()
            );
            String requestId = response.headers().firstValue("x-request-id")
                    .or(() -> response.headers().firstValue("x-correlation-id"))
                    .orElse(null);
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                String rawError = collectBody(response.body());
                ErrorInfo errorInfo = parseErrorInfo(rawError);
                throw new AiCallException(
                        errorInfo.getErrorCode(),
                        errorInfo.getErrorMessage(),
                        response.statusCode(),
                        rawError,
                        requestId,
                        null
                );
            }
            return readStreamResponse(response.body(), response.statusCode(), requestId, streamHandler);
        } catch (JsonProcessingException exception) {
            throw new AiCallException("REQUEST_BUILD_FAILED", "REQUEST_BUILD_FAILED", exception);
        } catch (IOException exception) {
            if (isTimeoutException(exception)) {
                throw new AiCallException("TIMEOUT", "TIMEOUT", exception);
            }
            throw new AiCallException("NETWORK_ERROR", "NETWORK_ERROR", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AiCallException("INTERRUPTED", "INTERRUPTED", exception);
        }
    }

    private ChatResult readStreamResponse(
            Stream<String> lines,
            Integer statusCode,
            String requestId,
            AiTokenStreamHandler streamHandler
    ) {
        StringBuilder contentBuilder = new StringBuilder();
        StringBuilder rawBuilder = new StringBuilder();
        try (lines) {
            lines.forEach(line -> handleStreamLine(line, contentBuilder, rawBuilder, streamHandler));
        } catch (RuntimeException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof AiCallException aiCallException) {
                throw aiCallException;
            }
            throw exception;
        }
        String content = contentBuilder.toString();
        if (!StringUtils.hasText(content)) {
            throw new AiCallException("EMPTY_CONTENT", "EMPTY_CONTENT", statusCode, rawBuilder.toString(), requestId, null);
        }
        return new ChatResult(content, rawBuilder.toString(), statusCode, requestId, null);
    }

    private void handleStreamLine(
            String line,
            StringBuilder contentBuilder,
            StringBuilder rawBuilder,
            AiTokenStreamHandler streamHandler
    ) {
        if (!StringUtils.hasText(line) || !line.startsWith("data:")) {
            return;
        }
        String data = line.substring("data:".length()).trim();
        if ("[DONE]".equals(data)) {
            return;
        }
        rawBuilder.append(data).append('\n');
        try {
            JsonNode root = objectMapper.readTree(data);
            JsonNode choice = root.path("choices").isArray() && !root.path("choices").isEmpty()
                    ? root.path("choices").get(0)
                    : null;
            if (choice == null) {
                return;
            }
            JsonNode deltaNode = choice.path("delta");
            String delta = deltaNode.path("content").asText("");
            if (delta.isEmpty()) {
                return;
            }
            contentBuilder.append(delta);
            if (streamHandler != null) {
                streamHandler.onDelta(delta);
            }
        } catch (JsonProcessingException exception) {
            throw new RuntimeException(new AiCallException(
                    "STREAM_PARSE_FAILED",
                    "STREAM_PARSE_FAILED",
                    null,
                    data,
                    null,
                    exception
            ));
        }
    }

    private String collectBody(Stream<String> lines) {
        if (lines == null) {
            return "";
        }
        try (lines) {
            return String.join("\n", lines.toList());
        }
    }

    private AiTokenStreamHandler visibleQuestionFeedbackStream(AiTokenStreamHandler delegate) {
        if (delegate == null) {
            return null;
        }
        JsonFieldStreamProjector projector = new JsonFieldStreamProjector(List.of(
                new StreamField("feedback", "本轮回答反馈"),
                new StreamField("referenceAnswer", "结构化参考答案"),
                new StreamField("nextQuestion", "下一轮追问")
        ));
        return rawDelta -> {
            String visibleDelta = projector.append(rawDelta);
            if (visibleDelta != null && !visibleDelta.isEmpty()) {
                delegate.onDelta(visibleDelta);
            }
        };
    }

    private AiProperties.OpenAiCompatible getConfig() {
        AiProperties.OpenAiCompatible config = aiProperties.getOpenAiCompatible();
        if (config == null) {
            throw missingConfig("openai-compatible");
        }
        if (!StringUtils.hasText(aiProperties.getProvider())) {
            throw missingConfig("provider");
        }
        if (!StringUtils.hasText(config.getBaseUrl())) {
            throw missingConfig("baseUrl");
        }
        if (!StringUtils.hasText(config.getApiKey())) {
            throw missingConfig("apiKey");
        }
        if (!StringUtils.hasText(config.getModel())) {
            throw missingConfig("model");
        }
        if (config.getTimeoutSeconds() == null || config.getTimeoutSeconds() <= 0) {
            throw missingConfig("timeoutSeconds");
        }
        return config;
    }

    private AiCallException missingConfig(String fieldName) {
        log.warn("AI config missing: {}", fieldName);
        return new AiCallException("CONFIG_MISSING", "CONFIG_MISSING: " + fieldName);
    }

    private String extractContent(OpenAiChatResponse response, String rawResponse, Integer statusCode, String requestId) {
        if (response == null || response.getChoices() == null || response.getChoices().isEmpty()) {
            throw new AiCallException("EMPTY_CHOICES", "EMPTY_CHOICES", statusCode, rawResponse, requestId, null);
        }
        OpenAiChoice choice = response.getChoices().get(0);
        if (choice == null || choice.getMessage() == null || !StringUtils.hasText(choice.getMessage().getContent())) {
            throw new AiCallException("EMPTY_CONTENT", "EMPTY_CONTENT", statusCode, rawResponse, requestId, null);
        }
        return choice.getMessage().getContent().trim();
    }

    private OpenAiChatResponse readChatResponse(String rawResponse, Integer statusCode, String requestId) {
        if (!StringUtils.hasText(rawResponse)) {
            throw new AiCallException("EMPTY_RESPONSE", "EMPTY_RESPONSE", statusCode, null, requestId, null);
        }
        try {
            return objectMapper.readValue(rawResponse, OpenAiChatResponse.class);
        } catch (JsonProcessingException exception) {
            throw new AiCallException(
                    "RESPONSE_PARSE_FAILED",
                    "RESPONSE_PARSE_FAILED",
                    statusCode,
                    rawResponse,
                    requestId,
                    exception
            );
        }
    }

    private ErrorInfo parseErrorInfo(String rawResponse) {
        if (!StringUtils.hasText(rawResponse)) {
            return new ErrorInfo("HTTP_ERROR", "OpenAI-compatible question practice request failed");
        }
        try {
            JsonNode errorNode = objectMapper.readTree(rawResponse).path("error");
            String errorCode = textOrDefault(errorNode.path("code"), "HTTP_ERROR");
            String errorMessage = textOrDefault(errorNode.path("message"), "OpenAI-compatible question practice request failed");
            return new ErrorInfo(errorCode, errorMessage);
        } catch (JsonProcessingException exception) {
            return new ErrorInfo("HTTP_ERROR", abbreviate(rawResponse));
        }
    }

    private String getRequestId(HttpHeaders headers, OpenAiChatResponse response) {
        if (headers == null) {
            return response == null ? null : response.getId();
        }
        String requestId = headers.getFirst("x-request-id");
        if (StringUtils.hasText(requestId)) {
            return requestId;
        }
        requestId = headers.getFirst("x-correlation-id");
        if (StringUtils.hasText(requestId)) {
            return requestId;
        }
        if (response != null && StringUtils.hasText(response.getId())) {
            return response.getId();
        }
        return null;
    }

    private AiCallLog buildCallLog(AiRequestType requestType, QuestionPracticeContext context) {
        AiCallLog callLog = new AiCallLog();
        callLog.setProvider(textValue(aiProperties.getProvider()));
        callLog.setModelName(getModelName());
        callLog.setRequestType(requestType.name());
        callLog.setPromptVersion(PROMPT_VERSION);
        callLog.setCreatedAt(LocalDateTime.now());
        if (context != null) {
            callLog.setUserId(context.getUserId());
            callLog.setSessionId(context.getSessionId());
        }
        return callLog;
    }

    private void recordSuccess(AiCallLog callLog, ChatResult chatResult, long startTime) {
        callLog.setLatencyMs(System.currentTimeMillis() - startTime);
        callLog.setSuccess(SUCCESS);
        callLog.setStatusCode(chatResult.getStatusCode());
        callLog.setRequestId(chatResult.getRequestId());
        setUsage(callLog, chatResult.getUsage());
        callLog.setRawResponse(shouldSaveRawResponse() ? chatResult.getRawResponse() : null);
        aiCallLogService.record(callLog);
    }

    private void recordFailure(AiCallLog callLog, AiCallException exception, long startTime) {
        callLog.setLatencyMs(System.currentTimeMillis() - startTime);
        callLog.setSuccess(FAILURE);
        callLog.setStatusCode(exception.getStatusCode());
        callLog.setErrorCode(exception.getErrorCode());
        callLog.setErrorMessage(abbreviate(exception.getErrorMessage()));
        callLog.setRequestId(exception.getRequestId());
        callLog.setRawResponse(shouldSaveRawResponse() ? exception.getRawResponse() : null);
        aiCallLogService.record(callLog);
    }

    private void recordFailure(
            AiCallLog callLog,
            String errorCode,
            String errorMessage,
            String rawResponse,
            long startTime
    ) {
        callLog.setLatencyMs(System.currentTimeMillis() - startTime);
        callLog.setSuccess(FAILURE);
        callLog.setErrorCode(errorCode);
        callLog.setErrorMessage(abbreviate(errorMessage));
        callLog.setRawResponse(shouldSaveRawResponse() ? rawResponse : null);
        aiCallLogService.record(callLog);
    }

    private void setUsage(AiCallLog callLog, OpenAiUsage usage) {
        if (usage == null) {
            return;
        }
        callLog.setPromptTokens(usage.getPromptTokens());
        callLog.setCompletionTokens(usage.getCompletionTokens());
        callLog.setTotalTokens(usage.getTotalTokens());
    }

    private Map<String, Object> buildQuestionVariables(QuestionPracticeContext context) {
        Map<String, Object> variables = new HashMap<>();
        String difficulty = context == null ? null : context.getDifficulty();
        InterviewDifficultyStrategy strategy = InterviewDifficultyStrategy.from(difficulty);
        variables.put("category", textValue(context == null ? null : context.getCategory()));
        variables.put("topicName", textValue(context == null ? null : context.getTopicName()));
        variables.put("topicDescription", textValue(context == null ? null : context.getTopicDescription()));
        variables.put("interviewFocus", textValue(context == null ? null : context.getInterviewFocus()));
        variables.put("tags", textValue(context == null ? null : context.getTags()));
        variables.put("targetRole", textValue(context == null ? null : context.getTargetRole()));
        variables.put("difficulty", textValue(difficulty));
        variables.put("difficultyName", strategy.getDifficultyName());
        variables.put("interviewerStyle", strategy.getInterviewerStyle());
        variables.put("questionDepth", strategy.getQuestionDepth());
        variables.put("focusAreas", strategy.getFocusAreas());
        variables.put("feedbackStyle", strategy.getFeedbackStyle());
        variables.put("scoringPolicy", strategy.getScoringPolicy());
        variables.put("currentRound", textValue(context == null ? null : context.getCurrentRound()));
        variables.put("maxRound", textValue(context == null ? null : context.getMaxRound()));
        variables.put("historyMessages", textValue(context == null ? null : context.getHistoryMessages()));
        variables.put("currentQuestion", textValue(context == null ? null : context.getCurrentQuestion()));
        variables.put("userAnswer", textValue(context == null ? null : context.getUserAnswer()));
        variables.put("ragContext", textValue(context == null ? null : context.getRagContext()));
        return variables;
    }

    private boolean shouldSaveRawResponse() {
        return aiProperties.getLog() != null && Boolean.TRUE.equals(aiProperties.getLog().getSaveRawResponse());
    }

    private String getModelName() {
        AiProperties.OpenAiCompatible config = aiProperties.getOpenAiCompatible();
        if (config == null) {
            return "";
        }
        return textValue(config.getModel());
    }

    private String textOrDefault(JsonNode node, String defaultValue) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return defaultValue;
        }
        String value = node.asText();
        return StringUtils.hasText(value) ? value : defaultValue;
    }

    private boolean isTimeoutException(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof SocketTimeoutException || current instanceof HttpTimeoutException) {
                return true;
            }
            String message = current.getMessage();
            if (StringUtils.hasText(message) && message.toLowerCase().contains("timed out")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private String abbreviate(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= 500) {
            return normalized;
        }
        return normalized.substring(0, 500);
    }

    private BusinessException aiCallFailed(String reason, Exception ex) {
        if (ex == null) {
            log.warn(reason);
        } else {
            log.warn(reason, ex);
        }
        return new BusinessException(AI_CALL_FAILED_CODE, AI_CALL_FAILED_MESSAGE);
    }

    private ChatMessage systemMessage() {
        return new ChatMessage("system", SYSTEM_PROMPT);
    }

    private ChatMessage userMessage(String content) {
        return new ChatMessage("user", content);
    }

    private String textValue(Object value) {
        if (value == null) {
            return "";
        }
        return String.valueOf(value);
    }

    private String trimTrailingSlash(String value) {
        String result = value.trim();
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private String resolveChatCompletionsEndpoint(String baseUrl) {
        String normalized = trimTrailingSlash(baseUrl);
        if (normalized.endsWith("/v1/chat/completions")) {
            return normalized;
        }
        if (normalized.endsWith("/v1")) {
            return normalized + "/chat/completions";
        }
        return normalized + "/v1/chat/completions";
    }

    private int resolveTimeoutSeconds(AiProperties properties) {
        if (properties == null
                || properties.getOpenAiCompatible() == null
                || properties.getOpenAiCompatible().getTimeoutSeconds() == null
                || properties.getOpenAiCompatible().getTimeoutSeconds() <= 0) {
            return DEFAULT_TIMEOUT_SECONDS;
        }
        return Math.max(properties.getOpenAiCompatible().getTimeoutSeconds(), DEFAULT_TIMEOUT_SECONDS);
    }

    private static class ChatResult {

        private final String content;

        private final String rawResponse;

        private final Integer statusCode;

        private final String requestId;

        private final OpenAiUsage usage;

        private ChatResult(
                String content,
                String rawResponse,
                Integer statusCode,
                String requestId,
                OpenAiUsage usage
        ) {
            this.content = content;
            this.rawResponse = rawResponse;
            this.statusCode = statusCode;
            this.requestId = requestId;
            this.usage = usage;
        }

        public String getContent() {
            return content;
        }

        public String getRawResponse() {
            return rawResponse;
        }

        public Integer getStatusCode() {
            return statusCode;
        }

        public String getRequestId() {
            return requestId;
        }

        public OpenAiUsage getUsage() {
            return usage;
        }
    }

    private static class ErrorInfo {

        private final String errorCode;

        private final String errorMessage;

        private ErrorInfo(String errorCode, String errorMessage) {
            this.errorCode = errorCode;
            this.errorMessage = errorMessage;
        }

        public String getErrorCode() {
            return errorCode;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }

    private static class AiCallException extends RuntimeException {

        private final String errorCode;

        private final String errorMessage;

        private final Integer statusCode;

        private final String rawResponse;

        private final String requestId;

        private AiCallException(String errorCode, String errorMessage) {
            this(errorCode, errorMessage, null, null, null, null);
        }

        private AiCallException(String errorCode, String errorMessage, Throwable cause) {
            this(errorCode, errorMessage, null, null, null, cause);
        }

        private AiCallException(
                String errorCode,
                String errorMessage,
                Integer statusCode,
                String rawResponse,
                String requestId,
                Throwable cause
        ) {
            super(errorMessage, cause);
            this.errorCode = errorCode;
            this.errorMessage = errorMessage;
            this.statusCode = statusCode;
            this.rawResponse = rawResponse;
            this.requestId = requestId;
        }

        public String getErrorCode() {
            return errorCode;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public Integer getStatusCode() {
            return statusCode;
        }

        public String getRawResponse() {
            return rawResponse;
        }

        public String getRequestId() {
            return requestId;
        }
    }

    private static class JsonFieldStreamProjector {

        private final List<StreamField> fields;

        private final StringBuilder rawContent = new StringBuilder();

        private String previousVisibleContent = "";

        private JsonFieldStreamProjector(List<StreamField> fields) {
            this.fields = fields;
        }

        private String append(String delta) {
            rawContent.append(delta);
            String visibleContent = AiTextSanitizer.toPlainText(buildVisibleContent(rawContent.toString()));
            if (visibleContent.length() <= previousVisibleContent.length()
                    || !visibleContent.startsWith(previousVisibleContent)) {
                previousVisibleContent = visibleContent;
                return "";
            }
            String visibleDelta = visibleContent.substring(previousVisibleContent.length());
            previousVisibleContent = visibleContent;
            return visibleDelta;
        }

        private String buildVisibleContent(String jsonLikeContent) {
            List<String> sections = new java.util.ArrayList<>();
            for (StreamField field : fields) {
                String value = extractJsonStringValue(jsonLikeContent, field.name());
                if (value != null && !value.isEmpty()) {
                    sections.add(field.label() + "\n" + value);
                }
            }
            return String.join("\n\n", sections);
        }

        private String extractJsonStringValue(String content, String fieldName) {
            String key = "\"" + fieldName + "\"";
            int keyIndex = content.indexOf(key);
            if (keyIndex < 0) {
                return null;
            }
            int colonIndex = content.indexOf(':', keyIndex + key.length());
            if (colonIndex < 0) {
                return null;
            }
            int quoteIndex = content.indexOf('"', colonIndex + 1);
            if (quoteIndex < 0) {
                return null;
            }
            StringBuilder value = new StringBuilder();
            boolean escaping = false;
            for (int index = quoteIndex + 1; index < content.length(); index++) {
                char current = content.charAt(index);
                if (escaping) {
                    appendEscaped(value, current);
                    escaping = false;
                    continue;
                }
                if (current == '\\') {
                    escaping = true;
                    continue;
                }
                if (current == '"') {
                    break;
                }
                value.append(current);
            }
            return value.toString();
        }

        private void appendEscaped(StringBuilder value, char escaped) {
            switch (escaped) {
                case 'n' -> value.append('\n');
                case 'r' -> value.append('\r');
                case 't' -> value.append('\t');
                case '"' -> value.append('"');
                case '\\' -> value.append('\\');
                default -> value.append(escaped);
            }
        }
    }

    private record StreamField(String name, String label) {
    }

    public static class OpenAiChatRequest {

        private String model;

        private List<ChatMessage> messages;

        private Double temperature;

        private Boolean stream;

        public OpenAiChatRequest(String model, List<ChatMessage> messages, Double temperature) {
            this(model, messages, temperature, false);
        }

        public OpenAiChatRequest(String model, List<ChatMessage> messages, Double temperature, Boolean stream) {
            this.model = model;
            this.messages = messages;
            this.temperature = temperature;
            this.stream = stream;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public List<ChatMessage> getMessages() {
            return messages;
        }

        public void setMessages(List<ChatMessage> messages) {
            this.messages = messages;
        }

        public Double getTemperature() {
            return temperature;
        }

        public void setTemperature(Double temperature) {
            this.temperature = temperature;
        }

        public Boolean getStream() {
            return stream;
        }

        public void setStream(Boolean stream) {
            this.stream = stream;
        }
    }

    public static class ChatMessage {

        private String role;

        private String content;

        public ChatMessage() {
        }

        public ChatMessage(String role, String content) {
            this.role = role;
            this.content = content;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }

    public static class OpenAiChatResponse {

        private String id;

        private List<OpenAiChoice> choices;

        private OpenAiUsage usage;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public List<OpenAiChoice> getChoices() {
            return choices;
        }

        public void setChoices(List<OpenAiChoice> choices) {
            this.choices = choices;
        }

        public OpenAiUsage getUsage() {
            return usage;
        }

        public void setUsage(OpenAiUsage usage) {
            this.usage = usage;
        }
    }

    public static class OpenAiChoice {

        private ChatMessage message;

        public ChatMessage getMessage() {
            return message;
        }

        public void setMessage(ChatMessage message) {
            this.message = message;
        }
    }

    public static class OpenAiUsage {

        @JsonProperty("prompt_tokens")
        private Integer promptTokens;

        @JsonProperty("completion_tokens")
        private Integer completionTokens;

        @JsonProperty("total_tokens")
        private Integer totalTokens;

        public Integer getPromptTokens() {
            return promptTokens;
        }

        public void setPromptTokens(Integer promptTokens) {
            this.promptTokens = promptTokens;
        }

        public Integer getCompletionTokens() {
            return completionTokens;
        }

        public void setCompletionTokens(Integer completionTokens) {
            this.completionTokens = completionTokens;
        }

        public Integer getTotalTokens() {
            return totalTokens;
        }

        public void setTotalTokens(Integer totalTokens) {
            this.totalTokens = totalTokens;
        }
    }
}
