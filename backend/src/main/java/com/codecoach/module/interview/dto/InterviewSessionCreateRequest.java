package com.codecoach.module.interview.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public class InterviewSessionCreateRequest {

    @NotNull(message = "项目ID不能为空")
    private Long projectId;

    @NotBlank(message = "目标岗位不能为空")
    private String targetRole;

    @NotBlank(message = "难度不能为空")
    @Pattern(regexp = "EASY|NORMAL|HARD", message = "难度只支持EASY、NORMAL、HARD")
    private String difficulty;

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
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
}
