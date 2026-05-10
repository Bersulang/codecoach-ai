package com.codecoach.module.question.vo;

public class QuestionSessionCreateResponse {

    private Long sessionId;

    private Long topicId;

    private String category;

    private String topicName;

    private String targetRole;

    private String difficulty;

    private Integer currentRound;

    private Integer maxRound;

    private QuestionMessageVO firstQuestion;

    public QuestionSessionCreateResponse(
            Long sessionId,
            Long topicId,
            String category,
            String topicName,
            String targetRole,
            String difficulty,
            Integer currentRound,
            Integer maxRound,
            QuestionMessageVO firstQuestion
    ) {
        this.sessionId = sessionId;
        this.topicId = topicId;
        this.category = category;
        this.topicName = topicName;
        this.targetRole = targetRole;
        this.difficulty = difficulty;
        this.currentRound = currentRound;
        this.maxRound = maxRound;
        this.firstQuestion = firstQuestion;
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

    public Integer getCurrentRound() {
        return currentRound;
    }

    public void setCurrentRound(Integer currentRound) {
        this.currentRound = currentRound;
    }

    public Integer getMaxRound() {
        return maxRound;
    }

    public void setMaxRound(Integer maxRound) {
        this.maxRound = maxRound;
    }

    public QuestionMessageVO getFirstQuestion() {
        return firstQuestion;
    }

    public void setFirstQuestion(QuestionMessageVO firstQuestion) {
        this.firstQuestion = firstQuestion;
    }
}
