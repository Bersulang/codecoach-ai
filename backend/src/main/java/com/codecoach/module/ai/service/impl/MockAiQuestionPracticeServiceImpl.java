package com.codecoach.module.ai.service.impl;

import com.codecoach.module.ai.model.QuestionPracticeContext;
import com.codecoach.module.ai.service.AiQuestionPracticeService;
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

    private String getTopicName(QuestionPracticeContext context) {
        if (context == null || !StringUtils.hasText(context.getTopicName())) {
            return "这个知识点";
        }
        return context.getTopicName();
    }
}
