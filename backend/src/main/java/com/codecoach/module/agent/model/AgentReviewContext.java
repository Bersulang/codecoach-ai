package com.codecoach.module.agent.model;

public class AgentReviewContext {

    private Long userId;

    private String scopeType;

    private String sourceSnapshotJson;

    private String projectReportsJson;

    private String questionReportsJson;

    private String abilitySnapshotsJson;

    private String resumeRisksJson;

    private String memorySummaryJson;

    private String ragArticlesJson;

    private String ragDocumentsJson;

    private String mockInterviewReportsJson;

    private String qaReplayJson;

    private String toolEvidenceJson;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getScopeType() { return scopeType; }
    public void setScopeType(String scopeType) { this.scopeType = scopeType; }
    public String getSourceSnapshotJson() { return sourceSnapshotJson; }
    public void setSourceSnapshotJson(String sourceSnapshotJson) { this.sourceSnapshotJson = sourceSnapshotJson; }
    public String getProjectReportsJson() { return projectReportsJson; }
    public void setProjectReportsJson(String projectReportsJson) { this.projectReportsJson = projectReportsJson; }
    public String getQuestionReportsJson() { return questionReportsJson; }
    public void setQuestionReportsJson(String questionReportsJson) { this.questionReportsJson = questionReportsJson; }
    public String getAbilitySnapshotsJson() { return abilitySnapshotsJson; }
    public void setAbilitySnapshotsJson(String abilitySnapshotsJson) { this.abilitySnapshotsJson = abilitySnapshotsJson; }
    public String getResumeRisksJson() { return resumeRisksJson; }
    public void setResumeRisksJson(String resumeRisksJson) { this.resumeRisksJson = resumeRisksJson; }
    public String getMemorySummaryJson() { return memorySummaryJson; }
    public void setMemorySummaryJson(String memorySummaryJson) { this.memorySummaryJson = memorySummaryJson; }
    public String getRagArticlesJson() { return ragArticlesJson; }
    public void setRagArticlesJson(String ragArticlesJson) { this.ragArticlesJson = ragArticlesJson; }
    public String getRagDocumentsJson() { return ragDocumentsJson; }
    public void setRagDocumentsJson(String ragDocumentsJson) { this.ragDocumentsJson = ragDocumentsJson; }
    public String getMockInterviewReportsJson() { return mockInterviewReportsJson; }
    public void setMockInterviewReportsJson(String mockInterviewReportsJson) { this.mockInterviewReportsJson = mockInterviewReportsJson; }
    public String getQaReplayJson() { return qaReplayJson; }
    public void setQaReplayJson(String qaReplayJson) { this.qaReplayJson = qaReplayJson; }
    public String getToolEvidenceJson() { return toolEvidenceJson; }
    public void setToolEvidenceJson(String toolEvidenceJson) { this.toolEvidenceJson = toolEvidenceJson; }
}
