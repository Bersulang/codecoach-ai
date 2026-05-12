package com.codecoach.module.agent.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class AgentReviewVO {

    private Long id;
    private String scopeType;
    private String summary;
    private List<String> keyFindings;
    private List<String> recurringWeaknesses;
    private List<String> causeAnalysis;
    private List<String> resumeRisks;
    private List<NextActionVO> nextActions;
    private String confidence;
    private Map<String, Object> sourceSnapshot;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getScopeType() { return scopeType; }
    public void setScopeType(String scopeType) { this.scopeType = scopeType; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public List<String> getKeyFindings() { return keyFindings; }
    public void setKeyFindings(List<String> keyFindings) { this.keyFindings = keyFindings; }
    public List<String> getRecurringWeaknesses() { return recurringWeaknesses; }
    public void setRecurringWeaknesses(List<String> recurringWeaknesses) { this.recurringWeaknesses = recurringWeaknesses; }
    public List<String> getCauseAnalysis() { return causeAnalysis; }
    public void setCauseAnalysis(List<String> causeAnalysis) { this.causeAnalysis = causeAnalysis; }
    public List<String> getResumeRisks() { return resumeRisks; }
    public void setResumeRisks(List<String> resumeRisks) { this.resumeRisks = resumeRisks; }
    public List<NextActionVO> getNextActions() { return nextActions; }
    public void setNextActions(List<NextActionVO> nextActions) { this.nextActions = nextActions; }
    public String getConfidence() { return confidence; }
    public void setConfidence(String confidence) { this.confidence = confidence; }
    public Map<String, Object> getSourceSnapshot() { return sourceSnapshot; }
    public void setSourceSnapshot(Map<String, Object> sourceSnapshot) { this.sourceSnapshot = sourceSnapshot; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
