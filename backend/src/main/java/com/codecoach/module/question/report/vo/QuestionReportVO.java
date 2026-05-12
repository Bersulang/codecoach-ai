package com.codecoach.module.question.report.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import java.util.List;

public class QuestionReportVO {

    private Long id;

    private Long sessionId;

    private Long topicId;

    private String category;

    private String topicName;

    private String targetRole;

    private String difficulty;

    private Integer totalScore;

    private String sampleSufficiency;

    private String answerQuality;

    private Integer answerCount;

    private Integer validAnswerCount;

    private List<String> deductionReasons;

    private List<String> nextActions;

    private String summary;

    private List<String> strengths;

    private List<String> weaknesses;

    private List<String> suggestions;

    private List<String> knowledgeGaps;

    private List<QuestionQaReviewVO> qaReview;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdAt;

    public QuestionReportVO(
            Long id,
            Long sessionId,
            Long topicId,
            String category,
            String topicName,
            String targetRole,
            String difficulty,
            Integer totalScore,
            String sampleSufficiency,
            String answerQuality,
            Integer answerCount,
            Integer validAnswerCount,
            List<String> deductionReasons,
            List<String> nextActions,
            String summary,
            List<String> strengths,
            List<String> weaknesses,
            List<String> suggestions,
            List<String> knowledgeGaps,
            List<QuestionQaReviewVO> qaReview,
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.sessionId = sessionId;
        this.topicId = topicId;
        this.category = category;
        this.topicName = topicName;
        this.targetRole = targetRole;
        this.difficulty = difficulty;
        this.totalScore = totalScore;
        this.sampleSufficiency = sampleSufficiency;
        this.answerQuality = answerQuality;
        this.answerCount = answerCount;
        this.validAnswerCount = validAnswerCount;
        this.deductionReasons = deductionReasons;
        this.nextActions = nextActions;
        this.summary = summary;
        this.strengths = strengths;
        this.weaknesses = weaknesses;
        this.suggestions = suggestions;
        this.knowledgeGaps = knowledgeGaps;
        this.qaReview = qaReview;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public Long getTopicId() {
        return topicId;
    }

    public void setTopicId(Long topicId) {
        this.topicId = topicId;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getTopicName() {
        return topicName;
    }

    public void setTopicName(String topicName) {
        this.topicName = topicName;
    }

    public String getTargetRole() {
        return targetRole;
    }

    public void setTargetRole(String targetRole) {
        this.targetRole = targetRole;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public Integer getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(Integer totalScore) {
        this.totalScore = totalScore;
    }

    public String getSampleSufficiency() {
        return sampleSufficiency;
    }

    public void setSampleSufficiency(String sampleSufficiency) {
        this.sampleSufficiency = sampleSufficiency;
    }

    public String getAnswerQuality() {
        return answerQuality;
    }

    public void setAnswerQuality(String answerQuality) {
        this.answerQuality = answerQuality;
    }

    public Integer getAnswerCount() {
        return answerCount;
    }

    public void setAnswerCount(Integer answerCount) {
        this.answerCount = answerCount;
    }

    public Integer getValidAnswerCount() {
        return validAnswerCount;
    }

    public void setValidAnswerCount(Integer validAnswerCount) {
        this.validAnswerCount = validAnswerCount;
    }

    public List<String> getDeductionReasons() {
        return deductionReasons;
    }

    public void setDeductionReasons(List<String> deductionReasons) {
        this.deductionReasons = deductionReasons;
    }

    public List<String> getNextActions() {
        return nextActions;
    }

    public void setNextActions(List<String> nextActions) {
        this.nextActions = nextActions;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public List<String> getStrengths() {
        return strengths;
    }

    public void setStrengths(List<String> strengths) {
        this.strengths = strengths;
    }

    public List<String> getWeaknesses() {
        return weaknesses;
    }

    public void setWeaknesses(List<String> weaknesses) {
        this.weaknesses = weaknesses;
    }

    public List<String> getSuggestions() {
        return suggestions;
    }

    public void setSuggestions(List<String> suggestions) {
        this.suggestions = suggestions;
    }

    public List<String> getKnowledgeGaps() {
        return knowledgeGaps;
    }

    public void setKnowledgeGaps(List<String> knowledgeGaps) {
        this.knowledgeGaps = knowledgeGaps;
    }

    public List<QuestionQaReviewVO> getQaReview() {
        return qaReview;
    }

    public void setQaReview(List<QuestionQaReviewVO> qaReview) {
        this.qaReview = qaReview;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
