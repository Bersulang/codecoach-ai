package com.codecoach.module.interview.vo;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public class InterviewSessionHistoryVO {

    private Long id;

    private Long projectId;

    private String projectName;

    private String targetRole;

    private String difficulty;

    private String status;

    private Integer currentRound;

    private Integer maxRound;

    private Integer totalScore;

    private Long reportId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime endedAt;

    public InterviewSessionHistoryVO(
            Long id,
            Long projectId,
            String projectName,
            String targetRole,
            String difficulty,
            String status,
            Integer currentRound,
            Integer maxRound,
            Integer totalScore,
            Long reportId,
            LocalDateTime createdAt,
            LocalDateTime endedAt
    ) {
        this.id = id;
        this.projectId = projectId;
        this.projectName = projectName;
        this.targetRole = targetRole;
        this.difficulty = difficulty;
        this.status = status;
        this.currentRound = currentRound;
        this.maxRound = maxRound;
        this.totalScore = totalScore;
        this.reportId = reportId;
        this.createdAt = createdAt;
        this.endedAt = endedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
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

    public Long getReportId() {
        return reportId;
    }

    public void setReportId(Long reportId) {
        this.reportId = reportId;
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
}
