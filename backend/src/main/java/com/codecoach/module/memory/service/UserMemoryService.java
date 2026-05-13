package com.codecoach.module.memory.service;

import com.codecoach.module.agent.entity.AgentReview;
import com.codecoach.module.insight.entity.UserAbilitySnapshot;
import com.codecoach.module.interview.entity.InterviewSession;
import com.codecoach.module.knowledge.entity.KnowledgeTopic;
import com.codecoach.module.memory.model.MemorySinkCommand;
import com.codecoach.module.memory.vo.UserMemorySummaryVO;
import com.codecoach.module.mockinterview.entity.MockInterviewReport;
import com.codecoach.module.mockinterview.entity.MockInterviewSession;
import com.codecoach.module.question.entity.QuestionTrainingReport;
import com.codecoach.module.question.entity.QuestionTrainingSession;
import com.codecoach.module.report.entity.InterviewReport;
import com.codecoach.module.resume.entity.ResumeProfile;
import com.codecoach.module.resume.model.ResumeAnalysisResult;

public interface UserMemoryService {

    void reinforce(MemorySinkCommand command);

    UserMemorySummaryVO getSummary(Long userId);

    void sinkProjectReport(InterviewReport report, InterviewSession session);

    void sinkQuestionReport(QuestionTrainingReport report, QuestionTrainingSession session, KnowledgeTopic topic);

    void sinkMockInterviewReport(MockInterviewReport report, MockInterviewSession session);

    void sinkResumeAnalysis(ResumeProfile profile, ResumeAnalysisResult result);

    void sinkAgentReview(AgentReview review);

    void sinkAbilitySnapshot(UserAbilitySnapshot snapshot);
}
