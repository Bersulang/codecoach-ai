package com.codecoach.module.resume.dto;

import jakarta.validation.constraints.NotNull;

public class ResumeProjectLinkRequest {

    @NotNull(message = "项目ID不能为空")
    private Long projectId;

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
}
