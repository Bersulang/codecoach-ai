package com.codecoach.module.ai.service.impl;

import com.codecoach.common.exception.BusinessException;
import com.codecoach.module.ai.config.AiProperties;
import com.codecoach.module.ai.dto.InterviewContext;
import com.codecoach.module.ai.service.AiInterviewService;
import com.codecoach.module.ai.vo.FeedbackAndQuestionResult;
import com.codecoach.module.ai.vo.ReportGenerateResult;
import com.codecoach.module.project.entity.Project;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
@ConditionalOnProperty(prefix = "ai", name = "provider", havingValue = "openai-compatible")
public class OpenAiCompatibleAiInterviewServiceImpl implements AiInterviewService {

    private static final Logger log = LoggerFactory.getLogger(OpenAiCompatibleAiInterviewServiceImpl.class);

    private static final Integer AI_CALL_FAILED_CODE = 3003;

    private static final String AI_CALL_FAILED_MESSAGE = "AI 调用失败，请稍后重试";

    private static final String SYSTEM_PROMPT = "你是一个严厉但专业的 Java 后端面试官，"
            + "需要围绕候选人的项目经历进行追问、反馈和总结。回答必须准确、具体、直接。";

    private final AiProperties aiProperties;

    private final ObjectMapper objectMapper;

    private final RestClient restClient;

    public OpenAiCompatibleAiInterviewServiceImpl(AiProperties aiProperties, ObjectMapper objectMapper) {
        this.aiProperties = aiProperties;
        this.objectMapper = objectMapper;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        Duration timeout = Duration.ofSeconds(resolveTimeoutSeconds(aiProperties));
        requestFactory.setConnectTimeout(timeout);
        requestFactory.setReadTimeout(timeout);
        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
    }

    @Override
    public String generateFirstQuestion(Project project, String targetRole, String difficulty) {
        String prompt = """
                请根据以下项目资料，为一次项目拷打训练生成第一道面试问题。

                项目名称：%s
                项目描述：%s
                技术栈：%s
                负责模块：%s
                项目亮点：%s
                项目难点：%s
                目标岗位：%s
                难度：%s

                输出要求：
                1. 只输出一道面试问题文本；
                2. 不要输出 JSON；
                3. 不要输出解释、编号或多余前后缀。
                """.formatted(
                projectValue(project == null ? null : project.getName()),
                projectValue(project == null ? null : project.getDescription()),
                projectValue(project == null ? null : project.getTechStack()),
                projectValue(project == null ? null : project.getRole()),
                projectValue(project == null ? null : project.getHighlights()),
                projectValue(project == null ? null : project.getDifficulties()),
                textValue(targetRole),
                textValue(difficulty)
        );

        String content = chat(List.of(systemMessage(), userMessage(prompt)), 0.7);
        if (!StringUtils.hasText(content)) {
            throw aiCallFailed("AI returned empty first question", null);
        }
        return content.trim();
    }

    @Override
    public FeedbackAndQuestionResult generateFeedbackAndNextQuestion(InterviewContext context) {
        String prompt = """
                请根据以下训练上下文，评价用户本轮回答，并给出下一轮追问。

                项目资料：
                %s

                训练信息：
                目标岗位：%s
                难度：%s
                当前轮次：%s

                历史问答：
                %s

                本轮问题：%s
                用户回答：%s

                输出要求：
                1. 只输出 JSON；
                2. 不要输出 Markdown 代码块；
                3. JSON 格式必须为：
                {
                  "feedback": "对用户本轮回答的评价",
                  "nextQuestion": "下一轮追问"
                }
                """.formatted(
                buildProjectText(context == null ? null : context.getProject()),
                context == null ? "" : textValue(context.getTargetRole()),
                context == null ? "" : textValue(context.getDifficulty()),
                context == null ? "" : textValue(context.getRoundNo()),
                buildQaRecordsText(context == null ? null : context.getQaRecords()),
                context == null ? "" : textValue(context.getCurrentQuestion()),
                context == null ? "" : textValue(context.getUserAnswer())
        );

        String content = chat(List.of(systemMessage(), userMessage(prompt)), 0.7);
        FeedbackAndQuestionResult result = parseJson(content, FeedbackAndQuestionResult.class);
        if (!StringUtils.hasText(result.getFeedback()) || !StringUtils.hasText(result.getNextQuestion())) {
            throw aiCallFailed("AI feedback result is incomplete", null);
        }
        return result;
    }

    @Override
    public ReportGenerateResult generateReport(InterviewContext context) {
        String prompt = """
                请根据以下项目拷打训练上下文，生成结构化训练报告。

                项目资料：
                %s

                训练信息：
                目标岗位：%s
                难度：%s
                当前轮次：%s

                历史问答：
                %s

                输出要求：
                1. 只输出 JSON；
                2. 不要输出 Markdown 代码块；
                3. totalScore 为 0 到 100 的整数；
                4. JSON 格式必须为：
                {
                  "totalScore": 78,
                  "summary": "总体评价",
                  "strengths": ["优点1", "优点2"],
                  "weaknesses": ["薄弱点1", "薄弱点2"],
                  "suggestions": ["建议1", "建议2"],
                  "qaReview": [
                    {
                      "question": "问题",
                      "answer": "回答",
                      "feedback": "反馈"
                    }
                  ]
                }
                """.formatted(
                buildProjectText(context == null ? null : context.getProject()),
                context == null ? "" : textValue(context.getTargetRole()),
                context == null ? "" : textValue(context.getDifficulty()),
                context == null ? "" : textValue(context.getRoundNo()),
                buildQaRecordsText(context == null ? null : context.getQaRecords())
        );

        String content = chat(List.of(systemMessage(), userMessage(prompt)), 0.7);
        ReportGenerateResult result = parseJson(content, ReportGenerateResult.class);
        validateReport(result);
        return result;
    }

    private String chat(List<ChatMessage> messages, double temperature) {
        AiProperties.OpenAiCompatible config = getConfig();
        String endpoint = resolveChatCompletionsEndpoint(config.getBaseUrl());
        OpenAiChatRequest request = new OpenAiChatRequest(config.getModel(), messages, temperature);

        try {
            OpenAiChatResponse response = restClient.post()
                    .uri(endpoint)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + config.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(OpenAiChatResponse.class);
            return extractContent(response);
        } catch (RestClientException ex) {
            throw aiCallFailed("OpenAI-compatible chat completions request failed", ex);
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
            throw aiCallFailed("AI response has no choices", null);
        }
        OpenAiChoice choice = response.getChoices().get(0);
        if (choice == null || choice.getMessage() == null || !StringUtils.hasText(choice.getMessage().getContent())) {
            throw aiCallFailed("AI response content is empty", null);
        }
        return choice.getMessage().getContent().trim();
    }

    private <T> T parseJson(String content, Class<T> clazz) {
        try {
            return objectMapper.readValue(normalizeJsonContent(content), clazz);
        } catch (JsonProcessingException ex) {
            throw aiCallFailed("AI response JSON parse failed", ex);
        }
    }

    private void validateReport(ReportGenerateResult result) {
        if (result == null
                || result.getTotalScore() == null
                || !StringUtils.hasText(result.getSummary())
                || result.getStrengths() == null
                || result.getWeaknesses() == null
                || result.getSuggestions() == null
                || result.getQaReview() == null) {
            throw aiCallFailed("AI report result is incomplete", null);
        }
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

    private String buildProjectText(Project project) {
        if (project == null) {
            return "项目资料为空";
        }
        return """
                项目名称：%s
                项目描述：%s
                技术栈：%s
                负责模块：%s
                项目亮点：%s
                项目难点：%s
                """.formatted(
                projectValue(project.getName()),
                projectValue(project.getDescription()),
                projectValue(project.getTechStack()),
                projectValue(project.getRole()),
                projectValue(project.getHighlights()),
                projectValue(project.getDifficulties())
        );
    }

    private String buildQaRecordsText(List<InterviewContext.QaRecord> qaRecords) {
        if (qaRecords == null || qaRecords.isEmpty()) {
            return "暂无历史问答";
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

    private String normalizeJsonContent(String content) {
        if (!StringUtils.hasText(content)) {
            throw aiCallFailed("AI JSON response is empty", null);
        }

        String trimmed = content.trim();
        if (trimmed.startsWith("```")) {
            int firstLineEnd = trimmed.indexOf('\n');
            if (firstLineEnd >= 0) {
                trimmed = trimmed.substring(firstLineEnd + 1).trim();
            }
            if (trimmed.endsWith("```")) {
                trimmed = trimmed.substring(0, trimmed.length() - 3).trim();
            }
        }
        return trimmed;
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

        private List<OpenAiChoice> choices;

        public List<OpenAiChoice> getChoices() {
            return choices;
        }

        public void setChoices(List<OpenAiChoice> choices) {
            this.choices = choices;
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
}
