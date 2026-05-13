package com.codecoach.module.ai.gateway.impl;

import com.codecoach.common.exception.BusinessException;
import com.codecoach.module.ai.config.AiProperties;
import com.codecoach.module.ai.entity.AiCallLog;
import com.codecoach.module.ai.gateway.AiModelGateway;
import com.codecoach.module.ai.gateway.model.AiChatMessage;
import com.codecoach.module.ai.gateway.model.AiChatRequest;
import com.codecoach.module.ai.gateway.model.AiChatResponse;
import com.codecoach.module.ai.gateway.model.AiStructuredChatRequest;
import com.codecoach.module.ai.gateway.model.ParsedToolIntent;
import com.codecoach.module.ai.service.AiCallLogService;
import com.codecoach.module.ai.service.AiTokenStreamHandler;
import com.codecoach.module.ai.support.AiJsonParser;
import com.codecoach.module.observability.trace.TraceContextHolder;
import com.codecoach.security.UserContext;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Service
public class OpenAiCompatibleModelGateway implements AiModelGateway {

    private static final int AI_CALL_FAILED_CODE = 3003;
    private static final String AI_CALL_FAILED_MESSAGE = "AI 调用失败，请稍后重试";

    private final AiProperties aiProperties;
    private final AiCallLogService aiCallLogService;
    private final AiJsonParser aiJsonParser;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public OpenAiCompatibleModelGateway(
            AiProperties aiProperties,
            AiCallLogService aiCallLogService,
            AiJsonParser aiJsonParser,
            ObjectMapper objectMapper
    ) {
        this.aiProperties = aiProperties;
        this.aiCallLogService = aiCallLogService;
        this.aiJsonParser = aiJsonParser;
        this.objectMapper = objectMapper;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        Duration timeout = Duration.ofSeconds(resolveTimeoutSeconds());
        requestFactory.setConnectTimeout(timeout);
        requestFactory.setReadTimeout(timeout);
        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
    }

    @Override
    public String provider() {
        return "openai-compatible";
    }

    @Override
    public AiChatResponse chat(AiChatRequest request) {
        long start = System.currentTimeMillis();
        String traceId = TraceContextHolder.getOrCreateTraceId();
        AiCallLog callLog = buildCallLog(request, traceId);
        try {
            AiProperties.OpenAiCompatible config = config();
            ChatRequest chatRequest = new ChatRequest(
                    config.getModel(),
                    messages(request),
                    request == null || request.getTemperature() == null ? 0.7 : request.getTemperature(),
                    request == null ? null : request.getToolSchemas(),
                    false
            );
            ResponseEntity<String> response = restClient.post()
                    .uri(resolveChatCompletionsEndpoint(config.getBaseUrl()))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + config.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(chatRequest)
                    .retrieve()
                    .toEntity(String.class);
            ChatResponse chatResponse = objectMapper.readValue(response.getBody(), ChatResponse.class);
            AiChatResponse result = toAiResponse(chatResponse, response.getBody(), response.getStatusCode().value(), traceId, start);
            record(callLog, result, true, null, response.getStatusCode().value(), start);
            return result;
        } catch (Exception exception) {
            record(callLog, null, false, abbreviate(exception.getMessage(), 500), null, start);
            throw new BusinessException(AI_CALL_FAILED_CODE, AI_CALL_FAILED_MESSAGE);
        }
    }

    @Override
    public AiChatResponse streamChat(AiChatRequest request, AiTokenStreamHandler streamHandler) {
        AiChatResponse response = chat(request);
        if (streamHandler != null && StringUtils.hasText(response.getContent())) {
            streamHandler.onDelta(response.getContent());
        }
        return response;
    }

    @Override
    public <T> T structuredChat(AiStructuredChatRequest request, Class<T> responseType) {
        AiChatResponse response = chat(request);
        return aiJsonParser.parseObject(response.getContent(), responseType);
    }

    @Override
    public List<ParsedToolIntent> parseToolIntent(String modelResponse) {
        if (!StringUtils.hasText(modelResponse)) {
            return List.of();
        }
        try {
            JsonNode root = objectMapper.readTree(modelResponse);
            List<ParsedToolIntent> intents = new ArrayList<>();
            if (root.has("toolIntents") && root.get("toolIntents").isArray()) {
                for (JsonNode item : root.get("toolIntents")) {
                    String toolName = text(item, "toolName");
                    if (!StringUtils.hasText(toolName)) {
                        continue;
                    }
                    Map<String, Object> params = item.has("params")
                            ? objectMapper.convertValue(item.get("params"), new TypeReference<>() {
                            })
                            : Map.of();
                    intents.add(new ParsedToolIntent(toolName, text(item, "reason"), params));
                }
            }
            return intents;
        } catch (Exception exception) {
            return List.of();
        }
    }

    private AiCallLog buildCallLog(AiChatRequest request, String traceId) {
        AiCallLog callLog = new AiCallLog();
        callLog.setUserId(currentUserIdOrNull());
        callLog.setProvider(provider());
        callLog.setModelName(config().getModel());
        callLog.setRequestType(StringUtils.hasText(request == null ? null : request.getRequestType())
                ? request.getRequestType()
                : "AI_GATEWAY_CHAT");
        callLog.setPromptVersion(request == null ? null : request.getPromptVersion());
        callLog.setTraceId(traceId);
        callLog.setCreatedAt(LocalDateTime.now());
        return callLog;
    }

    private void record(AiCallLog callLog, AiChatResponse response, boolean success, String error, Integer statusCode, long start) {
        try {
            callLog.setSuccess(success ? 1 : 0);
            callLog.setLatencyMs(System.currentTimeMillis() - start);
            callLog.setStatusCode(statusCode);
            if (response != null) {
                callLog.setRequestId(response.getRequestId());
                callLog.setPromptTokens(response.getPromptTokens());
                callLog.setCompletionTokens(response.getCompletionTokens());
                callLog.setTotalTokens(response.getTotalTokens());
            }
            if (!success) {
                callLog.setErrorCode("AI_GATEWAY_FAILED");
                callLog.setErrorMessage(error);
            }
            aiCallLogService.record(callLog);
        } catch (RuntimeException ignored) {
            // AI logging must never break the model call path.
        }
    }

    private List<AiChatMessage> messages(AiChatRequest request) {
        if (request != null && request.getMessages() != null && !request.getMessages().isEmpty()) {
            return request.getMessages();
        }
        List<AiChatMessage> messages = new ArrayList<>();
        if (StringUtils.hasText(request == null ? null : request.getSystemPrompt())) {
            messages.add(new AiChatMessage("system", request.getSystemPrompt()));
        }
        messages.add(new AiChatMessage("user", request == null ? "" : safe(request.getUserMessage())));
        return messages;
    }

    private AiChatResponse toAiResponse(ChatResponse response, String raw, Integer statusCode, String traceId, long start) {
        Choice choice = response == null || response.choices() == null || response.choices().isEmpty()
                ? null
                : response.choices().get(0);
        Message message = choice == null ? null : choice.message();
        Usage usage = response == null ? null : response.usage();
        AiChatResponse result = new AiChatResponse();
        result.setContent(message == null ? null : message.content());
        result.setProvider(provider());
        result.setModel(config().getModel());
        result.setTraceId(traceId);
        result.setLatencyMs(System.currentTimeMillis() - start);
        result.setToolCalls(message == null ? List.of() : message.toolCalls());
        if (usage != null) {
            result.setPromptTokens(usage.promptTokens());
            result.setCompletionTokens(usage.completionTokens());
            result.setTotalTokens(usage.totalTokens());
        }
        return result;
    }

    private AiProperties.OpenAiCompatible config() {
        return aiProperties.getOpenAiCompatible();
    }

    private int resolveTimeoutSeconds() {
        Integer timeout = config() == null ? null : config().getTimeoutSeconds();
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

    private Long currentUserIdOrNull() {
        try {
            return UserContext.getCurrentUserId();
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private String text(JsonNode node, String field) {
        return node == null || !node.has(field) || node.get(field).isNull() ? null : node.get(field).asText();
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

    private record ChatRequest(
            String model,
            List<AiChatMessage> messages,
            double temperature,
            List<Map<String, Object>> tools,
            Boolean stream
    ) {
    }

    private record ChatResponse(List<Choice> choices, Usage usage) {
    }

    private record Choice(Message message) {
    }

    private record Message(String content, @JsonProperty("tool_calls") List<Map<String, Object>> toolCalls) {
    }

    private record Usage(
            @JsonProperty("prompt_tokens") Integer promptTokens,
            @JsonProperty("completion_tokens") Integer completionTokens,
            @JsonProperty("total_tokens") Integer totalTokens
    ) {
    }
}
