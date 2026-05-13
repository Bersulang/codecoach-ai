package com.codecoach.module.agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("agent_review")
public class AgentReview {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("scope_type")
    private String scopeType;

    @TableField("summary")
    private String summary;

    @TableField("score_overview")
    private String scoreOverview;

    @TableField("radar_dimensions")
    private String radarDimensions;

    @TableField("key_findings")
    private String keyFindings;

    @TableField("recurring_weaknesses")
    private String recurringWeaknesses;

    @TableField("high_risk_answers")
    private String highRiskAnswers;

    @TableField("stage_performance")
    private String stagePerformance;

    @TableField("qa_replay")
    private String qaReplay;

    @TableField("cause_analysis")
    private String causeAnalysis;

    @TableField("resume_risks")
    private String resumeRisks;

    @TableField("next_actions")
    private String nextActions;

    @TableField("recommended_articles")
    private String recommendedArticles;

    @TableField("recommended_trainings")
    private String recommendedTrainings;

    @TableField("memory_updates")
    private String memoryUpdates;

    @TableField("confidence")
    private String confidence;

    @TableField("sample_quality")
    private String sampleQuality;

    @TableField("source_snapshot")
    private String sourceSnapshot;

    @TableField("created_at")
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getScopeType() { return scopeType; }
    public void setScopeType(String scopeType) { this.scopeType = scopeType; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getScoreOverview() { return scoreOverview; }
    public void setScoreOverview(String scoreOverview) { this.scoreOverview = scoreOverview; }
    public String getRadarDimensions() { return radarDimensions; }
    public void setRadarDimensions(String radarDimensions) { this.radarDimensions = radarDimensions; }
    public String getKeyFindings() { return keyFindings; }
    public void setKeyFindings(String keyFindings) { this.keyFindings = keyFindings; }
    public String getRecurringWeaknesses() { return recurringWeaknesses; }
    public void setRecurringWeaknesses(String recurringWeaknesses) { this.recurringWeaknesses = recurringWeaknesses; }
    public String getHighRiskAnswers() { return highRiskAnswers; }
    public void setHighRiskAnswers(String highRiskAnswers) { this.highRiskAnswers = highRiskAnswers; }
    public String getStagePerformance() { return stagePerformance; }
    public void setStagePerformance(String stagePerformance) { this.stagePerformance = stagePerformance; }
    public String getQaReplay() { return qaReplay; }
    public void setQaReplay(String qaReplay) { this.qaReplay = qaReplay; }
    public String getCauseAnalysis() { return causeAnalysis; }
    public void setCauseAnalysis(String causeAnalysis) { this.causeAnalysis = causeAnalysis; }
    public String getResumeRisks() { return resumeRisks; }
    public void setResumeRisks(String resumeRisks) { this.resumeRisks = resumeRisks; }
    public String getNextActions() { return nextActions; }
    public void setNextActions(String nextActions) { this.nextActions = nextActions; }
    public String getRecommendedArticles() { return recommendedArticles; }
    public void setRecommendedArticles(String recommendedArticles) { this.recommendedArticles = recommendedArticles; }
    public String getRecommendedTrainings() { return recommendedTrainings; }
    public void setRecommendedTrainings(String recommendedTrainings) { this.recommendedTrainings = recommendedTrainings; }
    public String getMemoryUpdates() { return memoryUpdates; }
    public void setMemoryUpdates(String memoryUpdates) { this.memoryUpdates = memoryUpdates; }
    public String getConfidence() { return confidence; }
    public void setConfidence(String confidence) { this.confidence = confidence; }
    public String getSampleQuality() { return sampleQuality; }
    public void setSampleQuality(String sampleQuality) { this.sampleQuality = sampleQuality; }
    public String getSourceSnapshot() { return sourceSnapshot; }
    public void setSourceSnapshot(String sourceSnapshot) { this.sourceSnapshot = sourceSnapshot; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
