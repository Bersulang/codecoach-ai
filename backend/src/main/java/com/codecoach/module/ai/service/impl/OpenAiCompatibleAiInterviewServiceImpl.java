package com.codecoach.module.ai.service.impl;

import com.codecoach.common.exception.BusinessException;
import com.codecoach.module.ai.config.AiProperties;
import com.codecoach.module.ai.dto.InterviewContext;
import com.codecoach.module.ai.entity.AiCallLog;
import com.codecoach.module.ai.enums.AiRequestType;
import com.codecoach.module.ai.service.AiCallLogService;
import com.codecoach.module.ai.service.AiInterviewService;
import com.codecoach.module.ai.service.PromptTemplateService;
import com.codecoach.module.ai.support.AiJsonParser;
import com.codecoach.module.ai.support.AiResponseValidator;
import com.codecoach.module.ai.vo.FeedbackAndQuestionResult;
import com.codecoach.module.ai.vo.ReportGenerateResult;
import com.codecoach.module.project.entity.Project;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Service
@ConditionalOnProperty(prefix = "ai", name = "provider", havingValue = "openai-compatible")
public class OpenAiCompatibleAiInterviewServiceImpl implements AiInterviewService {

    private static final Logger log = LoggerFactory.getLogger(OpenAiCompatibleAiInterviewServiceImpl.class);

    private static final Integer AI_CALL_FAILED_CODE = 3003;

    private static final String AI_CALL_FAILED_MESSAGE = "AI 调用失败，请稍后重试";

    private static final String PROMPT_VERSION = "v1";

    private static final int SUCCESS = 1;

    private static final int FAILURE = 0;

    private static final String SYSTEM_PROMPT = "你是一个严厉但专业的 Java 后端面试官，"
            + "需要围绕候选人的项目经历进行追问、反馈和总结。回答必须准确、具体、直接。";

    private static final String FIRST_QUESTION_TEMPLATE = "interview_first_question.md";

    private static final String FEEDBACK_NEXT_QUESTION_TEMPLATE = "interview_feedback_next_question.md";

    private static final String REPORT_TEMPLATE = "interview_report.md";

    private final AiProperties aiProperties;

    private final PromptTemplateService promptTemplateService;

    private final AiJsonParser aiJsonParser;

    private final AiResponseValidator aiResponseValidator;

    private final AiCallLogService aiCallLogService;

    private final ObjectMapper objectMapper;

    private final RestClient restClient;

    public OpenAiCompatibleAiInterviewServiceImpl(
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
    }

    @Override
    public String generateFirstQuestion(Project project, String targetRole, String difficulty) {
        long startTime = System.currentTimeMillis();
        AiCallLog callLog = buildCallLog(AiRequestType.INTERVIEW_FIRST_QUESTION, project, null);
        String prompt = promptTemplateService.render(
                FIRST_QUESTION_TEMPLATE,
                buildProjectVariables(project, targetRole, difficulty)
        );

        try {
            ChatResult chatResult = chat(List.of(systemMessage(), userMessage(prompt)), 0.7);
            String content = chatResult.getContent();
            if (!StringUtils.hasText(content)) {
                throw new AiCallException(
                        "EMPTY_CONTENT",
                        "AI returned empty first question",
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
            recordFailure(callLog, "AI_CALL_FAILED", exception.getMessage(), null, startTime);
            throw exception;
        }
    }

    @Override
    public FeedbackAndQuestionResult generateFeedbackAndNextQuestion(InterviewContext context) {
        long startTime = System.currentTimeMillis();
        AiCallLog callLog = buildCallLog(AiRequestType.INTERVIEW_FEEDBACK_NEXT_QUESTION, null, context);
        String prompt = promptTemplateService.render(
                FEEDBACK_NEXT_QUESTION_TEMPLATE,
                buildContextVariables(context)
        );

        try {
            ChatResult chatResult = chat(List.of(systemMessage(), userMessage(prompt)), 0.7);
            FeedbackAndQuestionResult result;
            try {
                result = aiJsonParser.parseObject(chatResult.getContent(), FeedbackAndQuestionResult.class);
                aiResponseValidator.validateFeedbackAndNextQuestion(result);
            } catch (BusinessException exception) {
                throw new AiCallException(
                        "JSON_PARSE_OR_VALIDATE_FAILED",
                        exception.getMessage(),
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
            recordFailure(callLog, "JSON_PARSE_OR_VALIDATE_FAILED", exception.getMessage(), null, startTime);
            throw exception;
        }
    }

    @Override
    public ReportGenerateResult generateReport(InterviewContext context) {
        long startTime = System.currentTimeMillis();
        AiCallLog callLog = buildCallLog(AiRequestType.INTERVIEW_REPORT, null, context);
        String prompt = promptTemplateService.render(REPORT_TEMPLATE, buildContextVariables(context));

        try {
            ChatResult chatResult = chat(List.of(systemMessage(), userMessage(prompt)), 0.7);
            ReportGenerateResult result;
            try {
                result = aiJsonParser.parseObject(chatResult.getContent(), ReportGenerateResult.class);
                aiResponseValidator.validateReport(result);
            } catch (BusinessException exception) {
                throw new AiCallException(
                        "JSON_PARSE_OR_VALIDATE_FAILED",
                        exception.getMessage(),
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
            recordFailure(callLog, "JSON_PARSE_OR_VALIDATE_FAILED", exception.getMessage(), null, startTime);
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
            OpenAiChatResponse response = readChatResponse(responseEntity.getBody());
            String content = extractContent(response);
            return new ChatResult(
                    content,
                    responseEntity.getBody(),
                    responseEntity.getStatusCode().value(),
                    getRequestId(responseEntity.getHeaders(), response),
                    response.getUsage()
            );
        } catch (RestClientResponseException ex) {
            ErrorInfo errorInfo = parseErrorInfo(ex.getResponseBodyAsString());
            throw new AiCallException(
                    errorInfo.getErrorCode(),
                    errorInfo.getErrorMessage(),
                    ex.getStatusCode().value(),
                    ex.getResponseBodyAsString(),
                    null,
                    ex
            );
        } catch (RestClientException ex) {
            throw new AiCallException("HTTP_REQUEST_FAILED", "OpenAI-compatible chat completions request failed", ex);
        }
    }

    private AiProperties.OpenAiCompatible getConfig() {
        AiProperties.OpenAiCompatible config = aiProperties.getOpenAiCompatible();
        if (config == null
                || !StringUtils.hasText(config.getBaseUrl())
                || !StringUtils.hasText(config.getApiKey())
                || !StringUtils.hasText(config.getModel())) {
            throw aiCallFailed("OpenAI-compatible AI configuration is incomplete", null);
        }
        return config;
    }

    private String extractContent(OpenAiChatResponse response) {
        if (response == null || response.getChoices() == null || response.getChoices().isEmpty()) {
            throw new AiCallException("EMPTY_CHOICES", "AI response has no choices");
        }
        OpenAiChoice choice = response.getChoices().get(0);
        if (choice == null || choice.getMessage() == null || !StringUtils.hasText(choice.getMessage().getContent())) {
            throw new AiCallException("EMPTY_CONTENT", "AI response content is empty");
        }
        return choice.getMessage().getContent().trim();
    }

    private OpenAiChatResponse readChatResponse(String rawResponse) {
        if (!StringUtils.hasText(rawResponse)) {
            throw new AiCallException("EMPTY_RESPONSE", "AI raw response is empty");
        }
        try {
            return objectMapper.readValue(rawResponse, OpenAiChatResponse.class);
        } catch (JsonProcessingException exception) {
            throw new AiCallException("RESPONSE_PARSE_FAILED", "OpenAI-compatible response parse failed", exception);
        }
    }

    private ErrorInfo parseErrorInfo(String rawResponse) {
        if (!StringUtils.hasText(rawResponse)) {
            return new ErrorInfo("HTTP_ERROR", "OpenAI-compatible chat completions request failed");
        }
        try {
            JsonNode errorNode = objectMapper.readTree(rawResponse).path("error");
            String errorCode = textOrDefault(errorNode.path("code"), "HTTP_ERROR");
            String errorMessage = textOrDefault(errorNode.path("message"), "OpenAI-compatible chat completions request failed");
            return new ErrorInfo(errorCode, errorMessage);
        } catch (JsonProcessingException exception) {
            return new ErrorInfo("HTTP_ERROR", abbreviate(rawResponse));
        }
    }

    private String getRequestId(HttpHeaders headers, OpenAiChatResponse response) {
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

    private AiCallLog buildCallLog(AiRequestType requestType, Project project, InterviewContext context) {
        AiCallLog callLog = new AiCallLog();
        callLog.setProvider(textValue(aiProperties.getProvider()));
        callLog.setModelName(getModelName());
        callLog.setRequestType(requestType.name());
        callLog.setPromptVersion(PROMPT_VERSION);
        callLog.setCreatedAt(LocalDateTime.now());
        if (context != null) {
            callLog.setUserId(context.getUserId());
            callLog.setProjectId(context.getProjectId());
            callLog.setSessionId(context.getSessionId());
        } else if (project != null) {
            callLog.setUserId(project.getUserId());
            callLog.setProjectId(project.getId());
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

    private Map<String, Object> buildContextVariables(InterviewContext context) {
        Map<String, Object> variables = buildProjectVariables(
                context == null ? null : context.getProject(),
                context == null ? null : context.getTargetRole(),
                context == null ? null : context.getDifficulty()
        );
        variables.put("currentRound", context == null ? "" : textValue(context.getRoundNo()));
        variables.put("maxRound", context == null ? "" : textValue(context.getMaxRound()));
        variables.put("currentQuestion", context == null ? "" : textValue(context.getCurrentQuestion()));
        variables.put("historyMessages", buildHistoryMessages(context));
        variables.put("userAnswer", context == null ? "" : textValue(context.getUserAnswer()));
        return variables;
    }

    private Map<String, Object> buildProjectVariables(Project project, String targetRole, String difficulty) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("projectName", projectValue(project == null ? null : project.getName()));
        variables.put("projectDescription", projectValue(project == null ? null : project.getDescription()));
        variables.put("techStack", projectValue(project == null ? null : project.getTechStack()));
        variables.put("role", projectValue(project == null ? null : project.getRole()));
        variables.put("highlights", projectValue(project == null ? null : project.getHighlights()));
        variables.put("difficulties", projectValue(project == null ? null : project.getDifficulties()));
        variables.put("targetRole", textValue(targetRole));
        variables.put("difficulty", textValue(difficulty));
        variables.put("currentRound", "");
        variables.put("maxRound", "");
        variables.put("historyMessages", "");
        variables.put("userAnswer", "");
        variables.put("currentQuestion", "");
        return variables;
    }

    private String buildHistoryMessages(InterviewContext context) {
        if (context == null) {
            return "暂无历史消息";
        }

        String qaRecordsText = buildQaRecordsText(context.getQaRecords());
        if (!StringUtils.hasText(context.getUserAnswer())) {
            return qaRecordsText;
        }

        return qaRecordsText + "\n\n当前轮次：\n问题："
                + textValue(context.getCurrentQuestion())
                + "\n回答："
                + textValue(context.getUserAnswer());
    }

    private String buildQaRecordsText(List<InterviewContext.QaRecord> qaRecords) {
        if (qaRecords == null || qaRecords.isEmpty()) {
            return "暂无历史消息";
        }

        List<String> items = new ArrayList<>();
        for (int i = 0; i < qaRecords.size(); i++) {
            InterviewContext.QaRecord record = qaRecords.get(i);
            items.add("""
                    第 %d 组：
                    问题：%s
                    回答：%s
                    反馈：%s
                    """.formatted(
                    i + 1,
                    textValue(record == null ? null : record.getQuestion()),
                    textValue(record == null ? null : record.getAnswer()),
                    textValue(record == null ? null : record.getFeedback())
            ));
        }
        return String.join("\n", items);
    }

    private String projectValue(String value) {
        if (!StringUtils.hasText(value)) {
            return "未填写";
        }
        return value;
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
            return 30;
        }
        return properties.getOpenAiCompatible().getTimeoutSeconds();
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

    public static class OpenAiChatRequest {

        private String model;

        private List<ChatMessage> messages;

        private Double temperature;

        public OpenAiChatRequest(String model, List<ChatMessage> messages, Double temperature) {
            this.model = model;
            this.messages = messages;
            this.temperature = temperature;
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
