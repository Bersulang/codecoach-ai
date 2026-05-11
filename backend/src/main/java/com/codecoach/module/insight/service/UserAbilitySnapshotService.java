package com.codecoach.module.insight.service;

import com.codecoach.module.interview.entity.InterviewSession;
import com.codecoach.module.knowledge.entity.KnowledgeTopic;
import com.codecoach.module.question.entity.QuestionTrainingReport;
import com.codecoach.module.question.entity.QuestionTrainingSession;
import com.codecoach.module.report.entity.InterviewReport;

public interface UserAbilitySnapshotService {

    void createProjectReportSnapshots(InterviewReport report, InterviewSession session);

    void createQuestionReportSnapshot(QuestionTrainingReport report, QuestionTrainingSession session, KnowledgeTopic topic);
}
