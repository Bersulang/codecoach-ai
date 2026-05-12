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

    @TableField("key_findings")
    private String keyFindings;

    @TableField("recurring_weaknesses")
    private String recurringWeaknesses;

    @TableField("cause_analysis")
    private String causeAnalysis;

    @TableField("resume_risks")
    private String resumeRisks;

    @TableField("next_actions")
    private String nextActions;

    @TableField("confidence")
    private String confidence;

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
    public String getKeyFindings() { return keyFindings; }
    public void setKeyFindings(String keyFindings) { this.keyFindings = keyFindings; }
    public String getRecurringWeaknesses() { return recurringWeaknesses; }
    public void setRecurringWeaknesses(String recurringWeaknesses) { this.recurringWeaknesses = recurringWeaknesses; }
    public String getCauseAnalysis() { return causeAnalysis; }
    public void setCauseAnalysis(String causeAnalysis) { this.causeAnalysis = causeAnalysis; }
    public String getResumeRisks() { return resumeRisks; }
    public void setResumeRisks(String resumeRisks) { this.resumeRisks = resumeRisks; }
    public String getNextActions() { return nextActions; }
    public void setNextActions(String nextActions) { this.nextActions = nextActions; }
    public String getConfidence() { return confidence; }
    public void setConfidence(String confidence) { this.confidence = confidence; }
    public String getSourceSnapshot() { return sourceSnapshot; }
    public void setSourceSnapshot(String sourceSnapshot) { this.sourceSnapshot = sourceSnapshot; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
