package com.codecoach.module.agent.service.impl;

import com.codecoach.common.exception.BusinessException;
import com.codecoach.module.agent.model.AgentReviewContext;
import com.codecoach.module.agent.model.AgentReviewResult;
import com.codecoach.module.agent.service.AiAgentReviewService;
import com.codecoach.module.ai.config.AiProperties;
import com.codecoach.module.ai.enums.AiRequestType;
import com.codecoach.module.ai.gateway.AiModelGateway;
import com.codecoach.module.ai.gateway.AiModelGatewayRouter;
import com.codecoach.module.ai.gateway.model.AiChatMessage;
import com.codecoach.module.ai.gateway.model.AiStructuredChatRequest;
import com.codecoach.module.ai.service.PromptTemplateService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@Profile("!mock")
public class OpenAiCompatibleAgentReviewServiceImpl implements AiAgentReviewService {

    private static final int AI_CALL_FAILED_CODE = 3003;
    private static final String AI_CALL_FAILED_MESSAGE = "AI 调用失败，请稍后重试";
    private static final String TEMPLATE = "agent_review.md";
    private static final String PROMPT_VERSION = "v1";
    private static final String SYSTEM_PROMPT = "你是 CodeCoach AI 的面试复盘 Agent，只做基于用户训练数据的结构化复盘。";

    private final AiProperties aiProperties;
    private final PromptTemplateService promptTemplateService;
    private final AiModelGatewayRouter aiModelGatewayRouter;

    public OpenAiCompatibleAgentReviewServiceImpl(
            AiProperties aiProperties,
            PromptTemplateService promptTemplateService,
            AiModelGatewayRouter aiModelGatewayRouter
    ) {
        this.aiProperties = aiProperties;
        this.promptTemplateService = promptTemplateService;
        this.aiModelGatewayRouter = aiModelGatewayRouter;
    }

    @Override
    public AgentReviewResult generateReview(AgentReviewContext context) {
        try {
            String prompt = promptTemplateService.render(TEMPLATE, promptVariables(context));
            AiStructuredChatRequest request = new AiStructuredChatRequest();
            request.setMessages(List.of(
                    new AiChatMessage("system", SYSTEM_PROMPT),
                    new AiChatMessage("user", prompt)
            ));
            request.setTemperature(0.4);
            request.setRequestType(AiRequestType.AGENT_REVIEW.name());
            request.setPromptVersion(PROMPT_VERSION + "-gateway");
            AgentReviewResult result = callPrimaryWithFallback(request);
            if (result == null || !StringUtils.hasText(result.getSummary())) {
                throw new BusinessException(AI_CALL_FAILED_CODE, AI_CALL_FAILED_MESSAGE);
            }
            return result;
        } catch (BusinessException exception) {
            if (exception instanceof BusinessException businessException) {
                throw businessException;
            }
            throw new BusinessException(AI_CALL_FAILED_CODE, AI_CALL_FAILED_MESSAGE);
        } catch (Exception exception) {
            throw new BusinessException(AI_CALL_FAILED_CODE, AI_CALL_FAILED_MESSAGE);
        }
    }

    private AgentReviewResult callPrimaryWithFallback(AiStructuredChatRequest request) {
        String primary = aiProperties.getGateway() == null ? null : aiProperties.getGateway().getPrimary();
        AiModelGateway gateway = aiModelGatewayRouter.require(StringUtils.hasText(primary) ? primary : aiProperties.getProvider());
        try {
            return gateway.structuredChat(request, AgentReviewResult.class);
        } catch (RuntimeException exception) {
            if ("openai-compatible".equalsIgnoreCase(gateway.provider())) {
                throw exception;
            }
            return aiModelGatewayRouter.require("openai-compatible").structuredChat(request, AgentReviewResult.class);
        }
    }

    private Map<String, Object> promptVariables(AgentReviewContext context) {
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("scopeType", safe(context.getScopeType()));
        variables.put("sourceSnapshot", safe(context.getSourceSnapshotJson()));
        variables.put("projectReports", safe(context.getProjectReportsJson()));
        variables.put("questionReports", safe(context.getQuestionReportsJson()));
        variables.put("mockInterviewReports", safe(context.getMockInterviewReportsJson()));
        variables.put("qaReplay", safe(context.getQaReplayJson()));
        variables.put("abilitySnapshots", safe(context.getAbilitySnapshotsJson()));
        variables.put("resumeRisks", safe(context.getResumeRisksJson()));
        variables.put("memorySummary", safe(context.getMemorySummaryJson()));
        variables.put("ragArticles", safe(context.getRagArticlesJson()));
        variables.put("ragDocuments", safe(context.getRagDocumentsJson()));
        variables.put("toolEvidence", safe(context.getToolEvidenceJson()));
        return variables;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

}
