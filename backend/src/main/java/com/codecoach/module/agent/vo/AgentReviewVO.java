package com.codecoach.module.agent.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class AgentReviewVO {

    private Long id;
    private String scopeType;
    private String summary;
    private Map<String, Object> scoreOverview;
    private List<Map<String, Object>> radarDimensions;
    private List<String> keyFindings;
    private List<String> recurringWeaknesses;
    private List<Map<String, Object>> highRiskAnswers;
    private List<Map<String, Object>> stagePerformance;
    private List<Map<String, Object>> qaReplay;
    private List<String> causeAnalysis;
    private List<String> resumeRisks;
    private List<NextActionVO> nextActions;
    private List<Map<String, Object>> recommendedArticles;
    private List<Map<String, Object>> recommendedTrainings;
    private List<String> memoryUpdates;
    private String confidence;
    private String sampleQuality;
    private Map<String, Object> sourceSnapshot;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getScopeType() { return scopeType; }
    public void setScopeType(String scopeType) { this.scopeType = scopeType; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public Map<String, Object> getScoreOverview() { return scoreOverview; }
    public void setScoreOverview(Map<String, Object> scoreOverview) { this.scoreOverview = scoreOverview; }
    public List<Map<String, Object>> getRadarDimensions() { return radarDimensions; }
    public void setRadarDimensions(List<Map<String, Object>> radarDimensions) { this.radarDimensions = radarDimensions; }
    public List<String> getKeyFindings() { return keyFindings; }
    public void setKeyFindings(List<String> keyFindings) { this.keyFindings = keyFindings; }
    public List<String> getRecurringWeaknesses() { return recurringWeaknesses; }
    public void setRecurringWeaknesses(List<String> recurringWeaknesses) { this.recurringWeaknesses = recurringWeaknesses; }
    public List<Map<String, Object>> getHighRiskAnswers() { return highRiskAnswers; }
    public void setHighRiskAnswers(List<Map<String, Object>> highRiskAnswers) { this.highRiskAnswers = highRiskAnswers; }
    public List<Map<String, Object>> getStagePerformance() { return stagePerformance; }
    public void setStagePerformance(List<Map<String, Object>> stagePerformance) { this.stagePerformance = stagePerformance; }
    public List<Map<String, Object>> getQaReplay() { return qaReplay; }
    public void setQaReplay(List<Map<String, Object>> qaReplay) { this.qaReplay = qaReplay; }
    public List<String> getCauseAnalysis() { return causeAnalysis; }
    public void setCauseAnalysis(List<String> causeAnalysis) { this.causeAnalysis = causeAnalysis; }
    public List<String> getResumeRisks() { return resumeRisks; }
    public void setResumeRisks(List<String> resumeRisks) { this.resumeRisks = resumeRisks; }
    public List<NextActionVO> getNextActions() { return nextActions; }
    public void setNextActions(List<NextActionVO> nextActions) { this.nextActions = nextActions; }
    public List<Map<String, Object>> getRecommendedArticles() { return recommendedArticles; }
    public void setRecommendedArticles(List<Map<String, Object>> recommendedArticles) { this.recommendedArticles = recommendedArticles; }
    public List<Map<String, Object>> getRecommendedTrainings() { return recommendedTrainings; }
    public void setRecommendedTrainings(List<Map<String, Object>> recommendedTrainings) { this.recommendedTrainings = recommendedTrainings; }
    public List<String> getMemoryUpdates() { return memoryUpdates; }
    public void setMemoryUpdates(List<String> memoryUpdates) { this.memoryUpdates = memoryUpdates; }
    public String getConfidence() { return confidence; }
    public void setConfidence(String confidence) { this.confidence = confidence; }
    public String getSampleQuality() { return sampleQuality; }
    public void setSampleQuality(String sampleQuality) { this.sampleQuality = sampleQuality; }
    public Map<String, Object> getSourceSnapshot() { return sourceSnapshot; }
    public void setSourceSnapshot(Map<String, Object> sourceSnapshot) { this.sourceSnapshot = sourceSnapshot; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
