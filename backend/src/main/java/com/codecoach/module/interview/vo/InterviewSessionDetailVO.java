package com.codecoach.module.interview.vo;

import java.util.List;

public class InterviewSessionDetailVO {

    private Long id;

    private Long projectId;

    private String projectName;

    private String targetRole;

    private String difficulty;

    private String status;

    private Integer currentRound;

    private Integer maxRound;

    private List<InterviewMessageVO> messages;

    public InterviewSessionDetailVO(
            Long id,
            Long projectId,
            String projectName,
            String targetRole,
            String difficulty,
            String status,
            Integer currentRound,
            Integer maxRound,
            List<InterviewMessageVO> messages
    ) {
        this.id = id;
        this.projectId = projectId;
        this.projectName = projectName;
        this.targetRole = targetRole;
        this.difficulty = difficulty;
        this.status = status;
        this.currentRound = currentRound;
        this.maxRound = maxRound;
        this.messages = messages;
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

    public List<InterviewMessageVO> getMessages() {
        return messages;
    }

    public void setMessages(List<InterviewMessageVO> messages) {
        this.messages = messages;
    }
}
