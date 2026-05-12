package com.codecoach.module.agent.model;

import java.util.ArrayList;
import java.util.List;

public class AgentReviewResult {

    private String summary;

    private List<String> keyFindings = new ArrayList<>();

    private List<String> recurringWeaknesses = new ArrayList<>();

    private List<String> causeAnalysis = new ArrayList<>();

    private List<NextAction> nextActions = new ArrayList<>();

    private List<String> resumeRisks = new ArrayList<>();

    private String confidence;

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public List<String> getKeyFindings() { return keyFindings; }
    public void setKeyFindings(List<String> keyFindings) { this.keyFindings = keyFindings; }
    public List<String> getRecurringWeaknesses() { return recurringWeaknesses; }
    public void setRecurringWeaknesses(List<String> recurringWeaknesses) { this.recurringWeaknesses = recurringWeaknesses; }
    public List<String> getCauseAnalysis() { return causeAnalysis; }
    public void setCauseAnalysis(List<String> causeAnalysis) { this.causeAnalysis = causeAnalysis; }
    public List<NextAction> getNextActions() { return nextActions; }
    public void setNextActions(List<NextAction> nextActions) { this.nextActions = nextActions; }
    public List<String> getResumeRisks() { return resumeRisks; }
    public void setResumeRisks(List<String> resumeRisks) { this.resumeRisks = resumeRisks; }
    public String getConfidence() { return confidence; }
    public void setConfidence(String confidence) { this.confidence = confidence; }

    public static class NextAction {
        private String type;
        private String title;
        private String reason;
        private Integer priority;
        private String targetPath;

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
        public Integer getPriority() { return priority; }
        public void setPriority(Integer priority) { this.priority = priority; }
        public String getTargetPath() { return targetPath; }
        public void setTargetPath(String targetPath) { this.targetPath = targetPath; }
    }
}
