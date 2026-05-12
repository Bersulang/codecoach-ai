package com.codecoach.module.agent.service.impl;

import com.codecoach.common.exception.BusinessException;
import com.codecoach.module.agent.model.AgentReviewContext;
import com.codecoach.module.agent.model.AgentReviewResult;
import com.codecoach.module.agent.service.AiAgentReviewService;
import com.codecoach.module.ai.config.AiProperties;
import com.codecoach.module.ai.entity.AiCallLog;
import com.codecoach.module.ai.enums.AiRequestType;
import com.codecoach.module.ai.service.AiCallLogService;
import com.codecoach.module.ai.service.PromptTemplateService;
import com.codecoach.module.ai.support.AiJsonParser;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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
public class OpenAiCompatibleAgentReviewServiceImpl implements AiAgentReviewService {

    private static final int AI_CALL_FAILED_CODE = 3003;
    private static final String AI_CALL_FAILED_MESSAGE = "AI 调用失败，请稍后重试";
    private static final String TEMPLATE = "agent_review.md";
    private static final String PROMPT_VERSION = "v1";
    private static final String SYSTEM_PROMPT = "你是 CodeCoach AI 的面试复盘 Agent，只做基于用户训练数据的结构化复盘。";

    private final AiProperties aiProperties;
    private final PromptTemplateService promptTemplateService;
    private final AiJsonParser aiJsonParser;
    private final AiCallLogService aiCallLogService;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public OpenAiCompatibleAgentReviewServiceImpl(
            AiProperties aiProperties,
            PromptTemplateService promptTemplateService,
            AiJsonParser aiJsonParser,
            AiCallLogService aiCallLogService,
            ObjectMapper objectMapper
    ) {
        this.aiProperties = aiProperties;
        this.promptTemplateService = promptTemplateService;
        this.aiJsonParser = aiJsonParser;
        this.aiCallLogService = aiCallLogService;
        this.objectMapper = objectMapper;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        Duration timeout = Duration.ofSeconds(resolveTimeoutSeconds(aiProperties));
        requestFactory.setConnectTimeout(timeout);
        requestFactory.setReadTimeout(timeout);
        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
    }

    @Override
    public AgentReviewResult generateReview(AgentReviewContext context) {
        long start = System.currentTimeMillis();
        AiCallLog log = buildCallLog(context);
        try {
            String prompt = promptTemplateService.render(TEMPLATE, Map.of(
                    "scopeType", safe(context.getScopeType()),
                    "sourceSnapshot", safe(context.getSourceSnapshotJson()),
                    "projectReports", safe(context.getProjectReportsJson()),
                    "questionReports", safe(context.getQuestionReportsJson()),
                    "abilitySnapshots", safe(context.getAbilitySnapshotsJson()),
                    "resumeRisks", safe(context.getResumeRisksJson()),
                    "ragArticles", safe(context.getRagArticlesJson())
            ));
            ChatResult chatResult = chat(List.of(
                    new ChatMessage("system", SYSTEM_PROMPT),
                    new ChatMessage("user", prompt)
            ));
            AgentReviewResult result = aiJsonParser.parseObject(chatResult.content(), AgentReviewResult.class);
            if (result == null || !StringUtils.hasText(result.getSummary())) {
                throw new BusinessException(AI_CALL_FAILED_CODE, AI_CALL_FAILED_MESSAGE);
            }
            record(log, chatResult, true, null, start);
            return result;
        } catch (RestClientResponseException exception) {
            record(log, new ChatResult(null, exception.getResponseBodyAsString(), exception.getStatusCode().value(), null), false, exception.getMessage(), start);
            throw new BusinessException(AI_CALL_FAILED_CODE, AI_CALL_FAILED_MESSAGE);
        } catch (RestClientException | BusinessException exception) {
            record(log, null, false, exception.getMessage(), start);
            if (exception instanceof BusinessException businessException) {
                throw businessException;
            }
            throw new BusinessException(AI_CALL_FAILED_CODE, AI_CALL_FAILED_MESSAGE);
        } catch (Exception exception) {
            record(log, null, false, exception.getMessage(), start);
            throw new BusinessException(AI_CALL_FAILED_CODE, AI_CALL_FAILED_MESSAGE);
        }
    }

    private ChatResult chat(List<ChatMessage> messages) {
        AiProperties.OpenAiCompatible config = aiProperties.getOpenAiCompatible();
        String endpoint = resolveChatCompletionsEndpoint(config.getBaseUrl());
        ResponseEntity<String> response = restClient.post()
                .uri(endpoint)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + config.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ChatRequest(config.getModel(), messages, 0.4))
                .retrieve()
                .toEntity(String.class);
        try {
            ChatResponse chatResponse = objectMapper.readValue(response.getBody(), ChatResponse.class);
            String content = chatResponse.choices() == null || chatResponse.choices().isEmpty()
                    ? null
                    : chatResponse.choices().get(0).message().content();
            return new ChatResult(content, response.getBody(), response.getStatusCode().value(), chatResponse.usage());
        } catch (Exception exception) {
            throw new BusinessException(AI_CALL_FAILED_CODE, AI_CALL_FAILED_MESSAGE);
        }
    }

    private AiCallLog buildCallLog(AgentReviewContext context) {
        AiProperties.OpenAiCompatible config = aiProperties.getOpenAiCompatible();
        AiCallLog log = new AiCallLog();
        log.setUserId(context == null ? null : context.getUserId());
        log.setProvider(aiProperties.getProvider());
        log.setModelName(config == null ? null : config.getModel());
        log.setRequestType(AiRequestType.AGENT_REVIEW.name());
        log.setPromptVersion(PROMPT_VERSION);
        log.setCreatedAt(LocalDateTime.now());
        return log;
    }

    private void record(AiCallLog log, ChatResult result, boolean success, String error, long start) {
        try {
            log.setSuccess(success ? 1 : 0);
            log.setLatencyMs(System.currentTimeMillis() - start);
            if (result != null) {
                log.setStatusCode(result.statusCode());
                log.setRawResponse(result.rawResponse());
                Usage usage = result.usage();
                if (usage != null) {
                    log.setPromptTokens(usage.promptTokens());
                    log.setCompletionTokens(usage.completionTokens());
                    log.setTotalTokens(usage.totalTokens());
                }
            }
            if (!success) {
                log.setErrorCode("AGENT_REVIEW_FAILED");
                log.setErrorMessage(abbreviate(error, 1000));
            }
            aiCallLogService.record(log);
        } catch (Exception ignored) {
            // AI call logging must not break user-facing review generation.
        }
    }

    private int resolveTimeoutSeconds(AiProperties properties) {
        Integer timeout = properties == null || properties.getOpenAiCompatible() == null
                ? null
                : properties.getOpenAiCompatible().getTimeoutSeconds();
        return timeout == null || timeout < 1 ? 120 : timeout;
    }

    private String resolveChatCompletionsEndpoint(String baseUrl) {
        String normalized = StringUtils.hasText(baseUrl) ? baseUrl.trim() : "";
        if (normalized.endsWith("/chat/completions")) {
            return normalized;
        }
        if (normalized.endsWith("/")) {
            return normalized + "chat/completions";
        }
        return normalized + "/chat/completions";
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String abbreviate(String value, int limit) {
        if (!StringUtils.hasText(value) || value.length() <= limit) {
            return value;
        }
        return value.substring(0, limit);
    }

    private record ChatMessage(String role, String content) {
    }

    private record ChatRequest(String model, List<ChatMessage> messages, double temperature) {
    }

    private record ChatResult(String content, String rawResponse, int statusCode, Usage usage) {
    }

    private record ChatResponse(List<Choice> choices, Usage usage) {
    }

    private record Choice(ChatMessageContent message) {
    }

    private record ChatMessageContent(String content) {
    }

    private record Usage(
            @JsonProperty("prompt_tokens") Integer promptTokens,
            @JsonProperty("completion_tokens") Integer completionTokens,
            @JsonProperty("total_tokens") Integer totalTokens
    ) {
    }
}
