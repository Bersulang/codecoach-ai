package com.codecoach.module.resume.service.impl;

import com.codecoach.common.exception.BusinessException;
import com.codecoach.common.concurrency.SingleFlightService;
import com.codecoach.module.ai.gateway.AiModelGateway;
import com.codecoach.module.ai.gateway.AiModelGatewayRouter;
import com.codecoach.module.ai.gateway.model.AiChatMessage;
import com.codecoach.module.ai.gateway.model.AiStructuredChatRequest;
import com.codecoach.module.resume.model.ResumeAnalysisResult;
import com.codecoach.module.resume.service.AiResumeAnalysisService;
import java.time.Duration;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@Profile("!mock")
public class OpenAiCompatibleResumeAnalysisServiceImpl implements AiResumeAnalysisService {

    private static final int AI_CALL_FAILED_CODE = 3003;
    private static final String AI_CALL_FAILED_MESSAGE = "AI 调用失败，请稍后重试";
    private static final String SYSTEM_PROMPT = "你是严谨克制的技术面试训练教练，只分析简历可被追问的真实风险，不包装、不编造。";
    private static final Duration LOCK_TTL = Duration.ofMinutes(5);
    private static final Duration CACHE_TTL = Duration.ofMinutes(10);

    private final AiModelGatewayRouter aiModelGatewayRouter;
    private final SingleFlightService singleFlightService;

    public OpenAiCompatibleResumeAnalysisServiceImpl(
            AiModelGatewayRouter aiModelGatewayRouter,
            SingleFlightService singleFlightService
    ) {
        this.aiModelGatewayRouter = aiModelGatewayRouter;
        this.singleFlightService = singleFlightService;
    }

    @Override
    public ResumeAnalysisResult analyze(String resumeText, String targetRole) {
        try {
            return singleFlightService.execute(
                    requestKey(resumeText, targetRole),
                    LOCK_TTL,
                    CACHE_TTL,
                    ResumeAnalysisResult.class,
                    () -> {
                        AiStructuredChatRequest request = new AiStructuredChatRequest();
                        request.setMessages(List.of(
                                new AiChatMessage("system", SYSTEM_PROMPT),
                                new AiChatMessage("user", buildPrompt(resumeText, targetRole))
                        ));
                        request.setTemperature(0.4);
                        request.setRequestType("RESUME_ANALYSIS");
                        request.setPromptVersion("v2-gateway");
                        ResumeAnalysisResult result = gateway().structuredChat(request, ResumeAnalysisResult.class);
                        validate(result);
                        return result;
                    },
                    () -> null,
                    AI_CALL_FAILED_CODE,
                    AI_CALL_FAILED_MESSAGE
            );
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException(AI_CALL_FAILED_CODE, AI_CALL_FAILED_MESSAGE);
        }
    }

    private String buildPrompt(String resumeText, String targetRole) {
        return """
                请分析下面这份简历，目标不是夸奖或改写简历，而是判断真实技术面试中会如何被追问，并输出可进入训练的结构化结果。

                目标岗位：%s

                要求：
                - 只输出 JSON 对象，不要 Markdown。
                - 不要做 ATS 打分，不要整份改写简历，不要建议虚构指标。
                - 风险点要克制、具体、可训练；缺少数据时建议补充真实细节。
                - 手机号、邮箱、微信等隐私不要输出。
                - JSON 字段必须包含 summary、skills、projectExperiences、riskPoints、interviewQuestions、optimizationSuggestions。
                - skills 每项包含 name、category、riskLevel、reason。
                - projectExperiences 每项包含 projectName、description、techStack、role、highlights、riskPoints、recommendedQuestions、possibleProjectName。
                - riskPoints 每项包含 type、level、evidence、suggestion。
                - techStack、highlights、riskPoints、recommendedQuestions、interviewQuestions、optimizationSuggestions 必须是字符串数组；即使只有一项也要用数组，不要用单个字符串。
                - riskLevel / level 使用 LOW / MEDIUM / HIGH。

                简历内容：
                %s
                """.formatted(
                StringUtils.hasText(targetRole) ? targetRole.trim() : "未指定",
                StringUtils.hasText(resumeText) ? resumeText : "空"
        );
    }

    private void validate(ResumeAnalysisResult result) {
        if (result == null || !StringUtils.hasText(result.getSummary())) {
            throw new BusinessException(AI_CALL_FAILED_CODE, AI_CALL_FAILED_MESSAGE);
        }
        if (result.getSkills() == null) {
            result.setSkills(List.of());
        }
        if (result.getProjectExperiences() == null) {
            result.setProjectExperiences(List.of());
        }
        if (result.getRiskPoints() == null) {
            result.setRiskPoints(List.of());
        }
        if (result.getInterviewQuestions() == null) {
            result.setInterviewQuestions(List.of());
        }
        if (result.getOptimizationSuggestions() == null) {
            result.setOptimizationSuggestions(List.of());
        }
    }

    private AiModelGateway gateway() {
        String primary = aiModelGatewayRouter.primary("openai-compatible").provider();
        return aiModelGatewayRouter.require(StringUtils.hasText(primary) ? primary : "openai-compatible");
    }

    private String requestKey(String resumeText, String targetRole) {
        String keySource = (StringUtils.hasText(resumeText) ? resumeText.trim() : "")
                + "|"
                + (StringUtils.hasText(targetRole) ? targetRole.trim() : "");
        return "resume-analysis:" + Integer.toHexString(keySource.hashCode());
    }
}
