package com.codecoach.module.resume.service.impl;

import com.codecoach.common.exception.BusinessException;
import com.codecoach.module.ai.config.AiProperties;
import com.codecoach.module.ai.support.AiJsonParser;
import com.codecoach.module.resume.model.ResumeAnalysisResult;
import com.codecoach.module.resume.service.AiResumeAnalysisService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.List;
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

@Service
@ConditionalOnProperty(prefix = "ai", name = "provider", havingValue = "openai-compatible")
public class OpenAiCompatibleResumeAnalysisServiceImpl implements AiResumeAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(OpenAiCompatibleResumeAnalysisServiceImpl.class);
    private static final int AI_CALL_FAILED_CODE = 3003;
    private static final String AI_CALL_FAILED_MESSAGE = "AI 调用失败，请稍后重试";
    private static final int DEFAULT_RESUME_TIMEOUT_SECONDS = 300;

    private final AiProperties aiProperties;
    private final AiJsonParser aiJsonParser;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public OpenAiCompatibleResumeAnalysisServiceImpl(
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
    public ResumeAnalysisResult analyze(String resumeText, String targetRole) {
        long startTime = System.currentTimeMillis();
        int textLength = resumeText == null ? 0 : resumeText.length();
        try {
            log.info("Resume AI analysis started, textLength={}, targetRole={}",
                    textLength,
                    abbreviate(targetRole));
            String content = chat(buildPrompt(resumeText, targetRole));
            ResumeAnalysisResult result = aiJsonParser.parseObject(content, ResumeAnalysisResult.class);
            validate(result);
            log.info("Resume AI analysis finished, textLength={}, latencyMs={}",
                    textLength,
                    System.currentTimeMillis() - startTime);
            return result;
        } catch (BusinessException exception) {
            log.warn("Resume AI analysis business failed, textLength={}, latencyMs={}, error={}",
                    textLength,
                    System.currentTimeMillis() - startTime,
                    abbreviate(exception.getMessage()));
            throw exception;
        } catch (Exception exception) {
            log.warn("Resume AI analysis failed, textLength={}, latencyMs={}, error={}",
                    textLength,
                    System.currentTimeMillis() - startTime,
                    abbreviate(exception.getMessage()),
                    exception);
            throw new BusinessException(AI_CALL_FAILED_CODE, AI_CALL_FAILED_MESSAGE);
        }
    }

    private String chat(String prompt) throws Exception {
        AiProperties.OpenAiCompatible config = getConfig();
        String endpoint = resolveChatCompletionsEndpoint(config.getBaseUrl());
        ChatRequest request = new ChatRequest(config.getModel(), List.of(
                new ChatMessage("system", "你是严谨克制的技术面试训练教练，只分析简历可被追问的真实风险，不包装、不编造。"),
                new ChatMessage("user", prompt)
        ), 0.4);

        ResponseEntity<String> responseEntity = restClient.post()
                .uri(endpoint)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + config.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toEntity(String.class);
        JsonNode root = objectMapper.readTree(responseEntity.getBody());
        return root.path("choices").path(0).path("message").path("content").asText();
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

    private AiProperties.OpenAiCompatible getConfig() {
        AiProperties.OpenAiCompatible config = aiProperties.getOpenAiCompatible();
        if (config == null
                || !StringUtils.hasText(config.getBaseUrl())
                || !StringUtils.hasText(config.getApiKey())
                || !StringUtils.hasText(config.getModel())) {
            throw new BusinessException(AI_CALL_FAILED_CODE, AI_CALL_FAILED_MESSAGE);
        }
        return config;
    }

    private int resolveTimeoutSeconds(AiProperties properties) {
        Integer timeoutSeconds = properties == null || properties.getOpenAiCompatible() == null
                ? null
                : properties.getOpenAiCompatible().getTimeoutSeconds();
        if (timeoutSeconds == null || timeoutSeconds <= 0) {
            return DEFAULT_RESUME_TIMEOUT_SECONDS;
        }
        return Math.max(timeoutSeconds, DEFAULT_RESUME_TIMEOUT_SECONDS);
    }

    private String resolveChatCompletionsEndpoint(String baseUrl) {
        String normalized = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return normalized.endsWith("/chat/completions") ? normalized : normalized + "/chat/completions";
    }

    private String abbreviate(String value) {
        if (!StringUtils.hasText(value)) {
            return "unknown";
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 160 ? normalized : normalized.substring(0, 160);
    }

    private record ChatRequest(
            String model,
            List<ChatMessage> messages,
            Double temperature
    ) {
    }

    private record ChatMessage(String role, String content) {
    }
}
