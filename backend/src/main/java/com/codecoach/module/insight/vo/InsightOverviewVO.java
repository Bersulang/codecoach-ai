package com.codecoach.module.insight.vo;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public class InsightOverviewVO {

    private Long totalTrainingCount;

    private Long projectTrainingCount;

    private Long questionTrainingCount;

    private Integer averageScore;

    private Integer recentAverageScore;

    private String bestDimension;

    private String weakestDimension;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime lastTrainingAt;

    public InsightOverviewVO(
            Long totalTrainingCount,
            Long projectTrainingCount,
            Long questionTrainingCount,
            Integer averageScore,
            Integer recentAverageScore,
            String bestDimension,
            String weakestDimension,
            LocalDateTime lastTrainingAt
    ) {
        this.totalTrainingCount = totalTrainingCount;
        this.projectTrainingCount = projectTrainingCount;
        this.questionTrainingCount = questionTrainingCount;
        this.averageScore = averageScore;
        this.recentAverageScore = recentAverageScore;
        this.bestDimension = bestDimension;
        this.weakestDimension = weakestDimension;
        this.lastTrainingAt = lastTrainingAt;
    }

    public Long getTotalTrainingCount() {
        return totalTrainingCount;
    }

    public void setTotalTrainingCount(Long totalTrainingCount) {
        this.totalTrainingCount = totalTrainingCount;
    }

    public Long getProjectTrainingCount() {
        return projectTrainingCount;
    }

    public void setProjectTrainingCount(Long projectTrainingCount) {
        this.projectTrainingCount = projectTrainingCount;
    }

    public Long getQuestionTrainingCount() {
        return questionTrainingCount;
    }

    public void setQuestionTrainingCount(Long questionTrainingCount) {
        this.questionTrainingCount = questionTrainingCount;
    }

    public Integer getAverageScore() {
        return averageScore;
    }

    public void setAverageScore(Integer averageScore) {
        this.averageScore = averageScore;
    }

    public Integer getRecentAverageScore() {
        return recentAverageScore;
    }

    public void setRecentAverageScore(Integer recentAverageScore) {
        this.recentAverageScore = recentAverageScore;
    }

    public String getBestDimension() {
        return bestDimension;
    }

    public void setBestDimension(String bestDimension) {
        this.bestDimension = bestDimension;
    }

    public String getWeakestDimension() {
        return weakestDimension;
    }

    public void setWeakestDimension(String weakestDimension) {
        this.weakestDimension = weakestDimension;
    }

    public LocalDateTime getLastTrainingAt() {
        return lastTrainingAt;
    }

    public void setLastTrainingAt(LocalDateTime lastTrainingAt) {
        this.lastTrainingAt = lastTrainingAt;
    }
}
