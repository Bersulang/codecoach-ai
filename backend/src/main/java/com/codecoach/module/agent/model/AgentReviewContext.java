package com.codecoach.module.agent.model;

public class AgentReviewContext {

    private Long userId;

    private String scopeType;

    private String sourceSnapshotJson;

    private String projectReportsJson;

    private String questionReportsJson;

    private String abilitySnapshotsJson;

    private String resumeRisksJson;

    private String ragArticlesJson;

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
    public String getRagArticlesJson() { return ragArticlesJson; }
    public void setRagArticlesJson(String ragArticlesJson) { this.ragArticlesJson = ragArticlesJson; }
}
