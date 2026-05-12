package com.codecoach.module.guide.service.impl;

import com.codecoach.module.ai.config.AiProperties;
import com.codecoach.module.ai.support.AiJsonParser;
import com.codecoach.module.guide.model.GuideAiSuggestion;
import com.codecoach.module.guide.service.AiGuideService;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Service
@ConditionalOnProperty(prefix = "ai", name = "provider", havingValue = "openai-compatible")
public class OpenAiCompatibleGuideServiceImpl implements AiGuideService {

    private static final String SYSTEM_PROMPT = "你是 CodeCoach AI 产品内训练向导。"
            + "你只帮助用户理解站内训练路径、选择下一步训练动作和导航到站内功能。"
            + "不要闲聊，不要生成外部链接，不要编造用户数据，不要输出未授权操作。"
            + "必须只输出 JSON 对象。";

    private final AiProperties aiProperties;
    private final AiJsonParser aiJsonParser;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public OpenAiCompatibleGuideServiceImpl(
            AiProperties aiProperties,
            AiJsonParser aiJsonParser,
            ObjectMapper objectMapper
    ) {
        this.aiProperties = aiProperties;
        this.aiJsonParser = aiJsonParser;
        this.objectMapper = objectMapper;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        Duration timeout = Duration.ofSeconds(resolveTimeoutSeconds(aiProperties));
        requestFactory.setConnectTimeout(timeout);
        requestFactory.setReadTimeout(timeout);
        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
    }

    @Override
    public GuideAiSuggestion suggest(String prompt) {
        AiProperties.OpenAiCompatible config = getConfig();
        ResponseEntity<String> response = restClient.post()
                .uri(resolveChatCompletionsEndpoint(config.getBaseUrl()))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + config.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ChatRequest(config.getModel(), List.of(
                        new ChatMessage("system", SYSTEM_PROMPT),
                        new ChatMessage("user", prompt)
                ), 0.2))
                .retrieve()
                .toEntity(String.class);
        ChatResponse chatResponse = readResponse(response.getBody());
        String content = chatResponse.choices() == null || chatResponse.choices().isEmpty()
                ? null
                : chatResponse.choices().get(0).message().content();
        return aiJsonParser.parseObject(content, GuideAiSuggestion.class);
    }

    private AiProperties.OpenAiCompatible getConfig() {
        AiProperties.OpenAiCompatible config = aiProperties.getOpenAiCompatible();
        if (config == null
                || !StringUtils.hasText(config.getBaseUrl())
                || !StringUtils.hasText(config.getApiKey())
                || !StringUtils.hasText(config.getModel())) {
            throw new IllegalStateException("AI guide config missing");
        }
        return config;
    }

    private ChatResponse readResponse(String body) {
        try {
            return objectMapper.readValue(body, ChatResponse.class);
        } catch (Exception exception) {
            throw new IllegalStateException("AI guide response parse failed", exception);
        }
    }

    private int resolveTimeoutSeconds(AiProperties properties) {
        Integer timeout = properties == null || properties.getOpenAiCompatible() == null
                ? null
                : properties.getOpenAiCompatible().getTimeoutSeconds();
        return timeout == null || timeout < 1 ? 30 : Math.min(timeout, 60);
    }

    private String resolveChatCompletionsEndpoint(String baseUrl) {
        String normalized = StringUtils.hasText(baseUrl) ? baseUrl.trim() : "";
        if (normalized.endsWith("/v1/chat/completions") || normalized.endsWith("/chat/completions")) {
            return normalized;
        }
        if (normalized.endsWith("/")) {
            return normalized + "chat/completions";
        }
        return normalized + "/chat/completions";
    }

    private record ChatMessage(String role, String content) {
    }

    private record ChatRequest(String model, List<ChatMessage> messages, double temperature) {
    }

    private record ChatResponse(List<Choice> choices) {
    }

    private record Choice(ChatMessageContent message) {
    }

    private record ChatMessageContent(String content) {
    }
}
