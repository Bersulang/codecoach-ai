package com.codecoach.module.agent.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AgentReviewResult {

    private String summary;

    private ScoreOverview scoreOverview;

    private List<RadarDimension> radarDimensions = new ArrayList<>();

    private List<String> keyFindings = new ArrayList<>();

    private List<String> recurringWeaknesses = new ArrayList<>();

    private List<HighRiskAnswer> highRiskAnswers = new ArrayList<>();

    private List<StagePerformance> stagePerformance = new ArrayList<>();

    private List<QaReplayItem> qaReplay = new ArrayList<>();

    private List<String> causeAnalysis = new ArrayList<>();

    private List<NextAction> nextActions = new ArrayList<>();

    private List<Recommendation> recommendedArticles = new ArrayList<>();

    private List<Recommendation> recommendedTrainings = new ArrayList<>();

    private List<String> resumeRisks = new ArrayList<>();

    private List<String> memoryUpdates = new ArrayList<>();

    private String confidence;

    private String sampleQuality;

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public ScoreOverview getScoreOverview() { return scoreOverview; }
    public void setScoreOverview(ScoreOverview scoreOverview) { this.scoreOverview = scoreOverview; }
    public List<RadarDimension> getRadarDimensions() { return radarDimensions; }
    public void setRadarDimensions(List<RadarDimension> radarDimensions) { this.radarDimensions = radarDimensions; }
    public List<String> getKeyFindings() { return keyFindings; }
    public void setKeyFindings(List<String> keyFindings) { this.keyFindings = keyFindings; }
    public List<String> getRecurringWeaknesses() { return recurringWeaknesses; }
    public void setRecurringWeaknesses(List<String> recurringWeaknesses) { this.recurringWeaknesses = recurringWeaknesses; }
    public List<HighRiskAnswer> getHighRiskAnswers() { return highRiskAnswers; }
    public void setHighRiskAnswers(List<HighRiskAnswer> highRiskAnswers) { this.highRiskAnswers = highRiskAnswers; }
    public List<StagePerformance> getStagePerformance() { return stagePerformance; }
    public void setStagePerformance(List<StagePerformance> stagePerformance) { this.stagePerformance = stagePerformance; }
    public List<QaReplayItem> getQaReplay() { return qaReplay; }
    public void setQaReplay(List<QaReplayItem> qaReplay) { this.qaReplay = qaReplay; }
    public List<String> getCauseAnalysis() { return causeAnalysis; }
    public void setCauseAnalysis(List<String> causeAnalysis) { this.causeAnalysis = causeAnalysis; }
    public List<NextAction> getNextActions() { return nextActions; }
    public void setNextActions(List<NextAction> nextActions) { this.nextActions = nextActions; }
    public List<Recommendation> getRecommendedArticles() { return recommendedArticles; }
    public void setRecommendedArticles(List<Recommendation> recommendedArticles) { this.recommendedArticles = recommendedArticles; }
    public List<Recommendation> getRecommendedTrainings() { return recommendedTrainings; }
    public void setRecommendedTrainings(List<Recommendation> recommendedTrainings) { this.recommendedTrainings = recommendedTrainings; }
    public List<String> getResumeRisks() { return resumeRisks; }
    public void setResumeRisks(List<String> resumeRisks) { this.resumeRisks = resumeRisks; }
    public List<String> getMemoryUpdates() { return memoryUpdates; }
    public void setMemoryUpdates(List<String> memoryUpdates) { this.memoryUpdates = memoryUpdates; }
    public String getConfidence() { return confidence; }
    public void setConfidence(String confidence) { this.confidence = confidence; }
    public String getSampleQuality() { return sampleQuality; }
    public void setSampleQuality(String sampleQuality) { this.sampleQuality = sampleQuality; }

    public static class ScoreOverview {
        private Integer score;
        private String level;
        private String explanation;

        public Integer getScore() { return score; }
        public void setScore(Integer score) { this.score = score; }
        public String getLevel() { return level; }
        public void setLevel(String level) { this.level = level; }
        public String getExplanation() { return explanation; }
        public void setExplanation(String explanation) { this.explanation = explanation; }
    }

    public static class RadarDimension {
        private String name;
        private Integer score;
        private String evidence;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Integer getScore() { return score; }
        public void setScore(Integer score) { this.score = score; }
        public String getEvidence() { return evidence; }
        public void setEvidence(String evidence) { this.evidence = evidence; }
    }

    public static class HighRiskAnswer {
        private String question;
        private String answerSummary;
        private String riskType;
        private String riskLevel;
        private String reason;
        private String betterDirection;
        private NextAction relatedAction;

        public String getQuestion() { return question; }
        public void setQuestion(String question) { this.question = question; }
        public String getAnswerSummary() { return answerSummary; }
        public void setAnswerSummary(String answerSummary) { this.answerSummary = answerSummary; }
        public String getRiskType() { return riskType; }
        public void setRiskType(String riskType) { this.riskType = riskType; }
        public String getRiskLevel() { return riskLevel; }
        public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
        public String getBetterDirection() { return betterDirection; }
        public void setBetterDirection(String betterDirection) { this.betterDirection = betterDirection; }
        public NextAction getRelatedAction() { return relatedAction; }
        public void setRelatedAction(NextAction relatedAction) { this.relatedAction = relatedAction; }
    }

    public static class StagePerformance {
        private String stage;
        private String stageName;
        private Integer score;
        private String comment;
        private List<String> weaknessTags = new ArrayList<>();

        public String getStage() { return stage; }
        public void setStage(String stage) { this.stage = stage; }
        public String getStageName() { return stageName; }
        public void setStageName(String stageName) { this.stageName = stageName; }
        public Integer getScore() { return score; }
        public void setScore(Integer score) { this.score = score; }
        public String getComment() { return comment; }
        public void setComment(String comment) { this.comment = comment; }
        public List<String> getWeaknessTags() { return weaknessTags; }
        public void setWeaknessTags(List<String> weaknessTags) { this.weaknessTags = weaknessTags; }
    }

    public static class QaReplayItem {
        private String sourceType;
        private Long sourceId;
        private String question;
        private String answerSummary;
        private String aiFollowUp;
        private String quality;
        private List<String> mainProblems = new ArrayList<>();
        private String suggestedExpression;

        public String getSourceType() { return sourceType; }
        public void setSourceType(String sourceType) { this.sourceType = sourceType; }
        public Long getSourceId() { return sourceId; }
        public void setSourceId(Long sourceId) { this.sourceId = sourceId; }
        public String getQuestion() { return question; }
        public void setQuestion(String question) { this.question = question; }
        public String getAnswerSummary() { return answerSummary; }
        public void setAnswerSummary(String answerSummary) { this.answerSummary = answerSummary; }
        public String getAiFollowUp() { return aiFollowUp; }
        public void setAiFollowUp(String aiFollowUp) { this.aiFollowUp = aiFollowUp; }
        public String getQuality() { return quality; }
        public void setQuality(String quality) { this.quality = quality; }
        public List<String> getMainProblems() { return mainProblems; }
        public void setMainProblems(List<String> mainProblems) { this.mainProblems = mainProblems; }
        public String getSuggestedExpression() { return suggestedExpression; }
        public void setSuggestedExpression(String suggestedExpression) { this.suggestedExpression = suggestedExpression; }
    }

    public static class Recommendation {
        private Long id;
        private String title;
        private String reason;
        private String targetPath;
        private String sourceType;
        private Map<String, Object> metadata;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
        public String getTargetPath() { return targetPath; }
        public void setTargetPath(String targetPath) { this.targetPath = targetPath; }
        public String getSourceType() { return sourceType; }
        public void setSourceType(String sourceType) { this.sourceType = sourceType; }
        public Map<String, Object> getMetadata() { return metadata; }
        public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    }

    public static class NextAction {
        private String type;
        private String title;
        private String reason;
        private Integer priority;
        private String targetPath;
        private String toolName;
        private Map<String, Object> params;

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
        public String getToolName() { return toolName; }
        public void setToolName(String toolName) { this.toolName = toolName; }
        public Map<String, Object> getParams() { return params; }
        public void setParams(Map<String, Object> params) { this.params = params; }
    }
}
