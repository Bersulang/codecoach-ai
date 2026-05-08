package com.codecoach.module.ai.service;

import com.codecoach.module.ai.dto.InterviewContext;
import com.codecoach.module.ai.vo.FeedbackAndQuestionResult;
import com.codecoach.module.ai.vo.ReportGenerateResult;
import com.codecoach.module.project.entity.Project;

public interface AiInterviewService {

    String generateFirstQuestion(Project project, String targetRole, String difficulty);

    FeedbackAndQuestionResult generateFeedbackAndNextQuestion(InterviewContext context);

    ReportGenerateResult generateReport(InterviewContext context);
}
