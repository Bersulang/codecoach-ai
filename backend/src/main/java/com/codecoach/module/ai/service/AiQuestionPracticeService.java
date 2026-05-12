package com.codecoach.module.ai.service;

import com.codecoach.module.ai.model.QuestionFeedbackResult;
import com.codecoach.module.ai.model.QuestionPracticeContext;
import com.codecoach.module.ai.model.QuestionReportGenerateResult;

public interface AiQuestionPracticeService {

    String generateFirstQuestion(QuestionPracticeContext context);

    QuestionFeedbackResult generateFeedbackAndNextQuestion(QuestionPracticeContext context, boolean needNextQuestion);

    default QuestionFeedbackResult generateFeedbackAndNextQuestionStream(
            QuestionPracticeContext context,
            boolean needNextQuestion,
            AiTokenStreamHandler streamHandler
    ) {
        return generateFeedbackAndNextQuestion(context, needNextQuestion);
    }

    QuestionReportGenerateResult generateReport(QuestionPracticeContext context);
}
