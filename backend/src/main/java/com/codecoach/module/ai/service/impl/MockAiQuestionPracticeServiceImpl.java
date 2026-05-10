package com.codecoach.module.ai.service.impl;

import com.codecoach.module.ai.model.QuestionFeedbackResult;
import com.codecoach.module.ai.model.QuestionPracticeContext;
import com.codecoach.module.ai.model.QuestionQaReviewItem;
import com.codecoach.module.ai.model.QuestionReportGenerateResult;
import com.codecoach.module.ai.service.AiQuestionPracticeService;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@ConditionalOnProperty(prefix = "ai", name = "provider", havingValue = "mock")
public class MockAiQuestionPracticeServiceImpl implements AiQuestionPracticeService {

    @Override
    public String generateFirstQuestion(QuestionPracticeContext context) {
        String topicName = getTopicName(context);
        return "请你先说明一下「" + topicName + "」的核心概念、典型应用场景，以及面试中最容易被追问的关键点。";
    }

    @Override
    public QuestionFeedbackResult generateFeedbackAndNextQuestion(QuestionPracticeContext context, boolean needNextQuestion) {
        String topicName = getTopicName(context);
        String feedback = "你的回答已经覆盖了「" + topicName + "」的一部分核心含义，建议继续补充原理、边界条件和常见解决方案。";
        String referenceAnswer = "参考答案：「" + topicName + "」需要先说明概念定义，再说明产生原因、典型场景、解决思路和方案取舍。";
        String nextQuestion = needNextQuestion
                ? "围绕「" + topicName + "」，如果面试官继续追问底层原理或异常场景，你会如何展开说明？"
                : null;
        return new QuestionFeedbackResult(feedback, referenceAnswer, nextQuestion, 76);
    }

    @Override
    public QuestionReportGenerateResult generateReport(QuestionPracticeContext context) {
        String topicName = getTopicName(context);
        return new QuestionReportGenerateResult(
                78,
                "本次八股训练围绕「" + topicName + "」展开，回答具备基本方向，但仍需要加强结构化表达和关键细节。",
                List.of("能够围绕知识点给出基本解释"),
                List.of("对底层原理、边界条件和方案取舍说明还不够完整"),
                List.of("按定义、原因、方案、优缺点、场景的顺序整理一版标准回答"),
                List.of("底层原理", "异常场景", "工程取舍"),
                List.of(new QuestionQaReviewItem(
                        context == null ? "" : context.getCurrentQuestion(),
                        context == null ? "" : context.getUserAnswer(),
                        "参考答案应覆盖概念、原因、方案和优缺点。",
                        "回答方向正确，但技术细节还可以更深入。"
                ))
        );
    }

    private String getTopicName(QuestionPracticeContext context) {
        if (context == null || !StringUtils.hasText(context.getTopicName())) {
            return "这个知识点";
        }
        return context.getTopicName();
    }
}
