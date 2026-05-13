package com.codecoach.module.guide.service.impl;

import com.codecoach.module.ai.config.AiProperties;
import com.codecoach.module.ai.gateway.AiModelGateway;
import com.codecoach.module.ai.gateway.AiModelGatewayRouter;
import com.codecoach.module.ai.gateway.model.AiChatMessage;
import com.codecoach.module.ai.gateway.model.AiStructuredChatRequest;
import com.codecoach.module.guide.model.GuideAiSuggestion;
import com.codecoach.module.guide.service.AiGuideService;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@Profile("!mock")
public class OpenAiCompatibleGuideServiceImpl implements AiGuideService {

    private static final String SYSTEM_PROMPT = "你是 CodeCoach AI 产品内训练向导。"
            + "你只帮助用户理解站内训练路径、选择下一步训练动作和导航到站内功能。"
            + "不要闲聊，不要生成外部链接，不要编造用户数据，不要输出未授权操作。"
            + "必须只输出 JSON 对象。";

    private final AiProperties aiProperties;
    private final AiModelGatewayRouter aiModelGatewayRouter;

    public OpenAiCompatibleGuideServiceImpl(
            AiProperties aiProperties,
            AiModelGatewayRouter aiModelGatewayRouter
    ) {
        this.aiProperties = aiProperties;
        this.aiModelGatewayRouter = aiModelGatewayRouter;
    }

    @Override
    public GuideAiSuggestion suggest(String prompt) {
        AiStructuredChatRequest request = new AiStructuredChatRequest();
        request.setMessages(List.of(
                new AiChatMessage("system", SYSTEM_PROMPT),
                new AiChatMessage("user", prompt)
        ));
        request.setTemperature(0.2);
        request.setRequestType("GUIDE_AGENT_COMPOSE");
        request.setPromptVersion("v2-gateway");
        return callPrimaryWithFallback(request);
    }

    private GuideAiSuggestion callPrimaryWithFallback(AiStructuredChatRequest request) {
        String primary = aiProperties.getGateway() == null ? null : aiProperties.getGateway().getPrimary();
        AiModelGateway gateway = aiModelGatewayRouter.require(StringUtils.hasText(primary) ? primary : aiProperties.getProvider());
        try {
            return gateway.structuredChat(request, GuideAiSuggestion.class);
        } catch (RuntimeException exception) {
            if ("openai-compatible".equalsIgnoreCase(gateway.provider())) {
                throw exception;
            }
            return aiModelGatewayRouter.require("openai-compatible").structuredChat(request, GuideAiSuggestion.class);
        }
    }
}
