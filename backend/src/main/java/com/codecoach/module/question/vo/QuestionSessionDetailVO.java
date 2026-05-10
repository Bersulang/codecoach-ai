package com.codecoach.module.question.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import java.util.List;

public class QuestionSessionDetailVO {

    private Long id;

    private Long topicId;

    private String category;

    private String topicName;

    private String topicDescription;

    private String targetRole;

    private String difficulty;

    private String status;

    private Integer currentRound;

    private Integer maxRound;

    private Integer totalScore;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime endedAt;

    private List<QuestionMessageVO> messages;

    public QuestionSessionDetailVO(
            Long id,
            Long topicId,
            String category,
            String topicName,
            String topicDescription,
            String targetRole,
            String difficulty,
            String status,
            Integer currentRound,
            Integer maxRound,
            Integer totalScore,
            LocalDateTime createdAt,
            LocalDateTime endedAt,
            List<QuestionMessageVO> messages
    ) {
        this.id = id;
        this.topicId = topicId;
        this.category = category;
        this.topicName = topicName;
        this.topicDescription = topicDescription;
        this.targetRole = targetRole;
        this.difficulty = difficulty;
        this.status = status;
        this.currentRound = currentRound;
        this.maxRound = maxRound;
        this.totalScore = totalScore;
        this.createdAt = createdAt;
        this.endedAt = endedAt;
        this.messages = messages;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getTopicDescription() {
        return topicDescription;
    }

    public void setTopicDescription(String topicDescription) {
        this.topicDescription = topicDescription;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public Integer getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(Integer totalScore) {
        this.totalScore = totalScore;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(LocalDateTime endedAt) {
        this.endedAt = endedAt;
    }

    public List<QuestionMessageVO> getMessages() {
        return messages;
    }

    public void setMessages(List<QuestionMessageVO> messages) {
        this.messages = messages;
    }
}
