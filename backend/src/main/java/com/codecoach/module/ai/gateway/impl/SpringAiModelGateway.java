package com.codecoach.module.ai.gateway.impl;

import com.codecoach.common.exception.BusinessException;
import com.codecoach.module.ai.config.AiProperties;
import com.codecoach.module.ai.entity.AiCallLog;
import com.codecoach.module.ai.gateway.AiModelGateway;
import com.codecoach.module.ai.gateway.model.AiChatRequest;
import com.codecoach.module.ai.gateway.model.AiChatResponse;
import com.codecoach.module.ai.gateway.model.AiStructuredChatRequest;
import com.codecoach.module.ai.gateway.model.ParsedToolIntent;
import com.codecoach.module.ai.service.AiCallLogService;
import com.codecoach.module.ai.service.AiTokenStreamHandler;
import com.codecoach.module.ai.support.AiJsonParser;
import com.codecoach.module.observability.trace.TraceContextHolder;
import com.codecoach.security.UserContext;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@ConditionalOnProperty(prefix = "ai.spring-ai", name = "enabled", havingValue = "true")
public class SpringAiModelGateway implements AiModelGateway {

    private static final int AI_CALL_FAILED_CODE = 3003;

    private final ApplicationContext applicationContext;
    private final AiProperties aiProperties;
    private final AiJsonParser aiJsonParser;
    private final AiCallLogService aiCallLogService;

    public SpringAiModelGateway(
            ApplicationContext applicationContext,
            AiProperties aiProperties,
            AiJsonParser aiJsonParser,
            AiCallLogService aiCallLogService
    ) {
        this.applicationContext = applicationContext;
        this.aiProperties = aiProperties;
        this.aiJsonParser = aiJsonParser;
        this.aiCallLogService = aiCallLogService;
    }

    @Override
    public String provider() {
        String provider = aiProperties.getSpringAi() == null ? null : aiProperties.getSpringAi().getProvider();
        return StringUtils.hasText(provider) ? provider : "spring-ai";
    }

    @Override
    public AiChatResponse chat(AiChatRequest request) {
        long start = System.currentTimeMillis();
        String traceId = TraceContextHolder.getOrCreateTraceId();
        AiCallLog callLog = buildCallLog(request, traceId);
        try {
            Object chatClient = resolveChatClient();
            Method prompt = chatClient.getClass().getMethod("prompt", String.class);
            Object promptSpec = prompt.invoke(chatClient, promptText(request));
            Object callSpec = promptSpec.getClass().getMethod("call").invoke(promptSpec);
            Object content = callSpec.getClass().getMethod("content").invoke(callSpec);
            AiChatResponse response = new AiChatResponse();
            response.setContent(content == null ? null : String.valueOf(content));
            response.setProvider(provider());
            response.setModel(aiProperties.getSpringAi().getModel());
            response.setTraceId(traceId);
            response.setLatencyMs(System.currentTimeMillis() - start);
            record(callLog, response, true, null, start);
            return response;
        } catch (Exception exception) {
            record(callLog, null, false, exception.getMessage(), start);
            throw new BusinessException(AI_CALL_FAILED_CODE, "Spring AI POC 未启用或 ChatClient 不可用");
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
        return aiJsonParser.parseObject(chat(request).getContent(), responseType);
    }

    @Override
    public List<ParsedToolIntent> parseToolIntent(String modelResponse) {
        return List.of();
    }

    private Object resolveChatClient() {
        try {
            Class<?> chatClientClass = Class.forName("org.springframework.ai.chat.client.ChatClient");
            Object existing = applicationContext.getBeanProvider(chatClientClass).getIfAvailable();
            if (existing != null) {
                return existing;
            }
            Class<?> builderClass = Class.forName("org.springframework.ai.chat.client.ChatClient$Builder");
            Object builder = applicationContext.getBeanProvider(builderClass).getIfAvailable();
            if (builder != null) {
                return builderClass.getMethod("build").invoke(builder);
            }
            throw new IllegalStateException("No Spring AI ChatClient bean");
        } catch (Exception exception) {
            throw new IllegalStateException("No Spring AI ChatClient bean", exception);
        }
    }

    private String promptText(AiChatRequest request) {
        StringBuilder builder = new StringBuilder();
        if (StringUtils.hasText(request == null ? null : request.getSystemPrompt())) {
            builder.append(request.getSystemPrompt()).append("\n\n");
        }
        if (StringUtils.hasText(request == null ? null : request.getUserMessage())) {
            builder.append(request.getUserMessage());
        } else if (request != null && request.getMessages() != null) {
            request.getMessages().forEach(message -> builder.append(message.role()).append(": ").append(message.content()).append("\n"));
        }
        return builder.toString().trim();
    }

    private AiCallLog buildCallLog(AiChatRequest request, String traceId) {
        AiCallLog log = new AiCallLog();
        log.setUserId(currentUserIdOrNull());
        log.setProvider(provider());
        log.setModelName(aiProperties.getSpringAi() == null ? null : aiProperties.getSpringAi().getModel());
        log.setRequestType(StringUtils.hasText(request == null ? null : request.getRequestType())
                ? request.getRequestType()
                : "AI_GATEWAY_CHAT");
        log.setPromptVersion(request == null ? null : request.getPromptVersion());
        log.setTraceId(traceId);
        log.setCreatedAt(LocalDateTime.now());
        return log;
    }

    private void record(AiCallLog callLog, AiChatResponse response, boolean success, String error, long start) {
        try {
            callLog.setSuccess(success ? 1 : 0);
            callLog.setLatencyMs(System.currentTimeMillis() - start);
            if (response != null) {
                callLog.setRequestId(response.getRequestId());
            }
            if (!success) {
                callLog.setErrorCode("AI_GATEWAY_FAILED");
                callLog.setErrorMessage(abbreviate(error, 500));
            }
            aiCallLogService.record(callLog);
        } catch (RuntimeException ignored) {
            // AI logging must never break the model call path.
        }
    }

    private Long currentUserIdOrNull() {
        try {
            return UserContext.getCurrentUserId();
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private String abbreviate(String value, int limit) {
        if (!StringUtils.hasText(value) || value.length() <= limit) {
            return value;
        }
        return value.substring(0, limit);
    }
}
