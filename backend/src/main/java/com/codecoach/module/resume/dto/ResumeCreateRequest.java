package com.codecoach.module.resume.dto;

import jakarta.validation.constraints.NotNull;

public class ResumeCreateRequest {

    @NotNull(message = "文档ID不能为空")
    private Long documentId;

    private String title;

    private String targetRole;

    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long documentId) { this.documentId = documentId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getTargetRole() { return targetRole; }
    public void setTargetRole(String targetRole) { this.targetRole = targetRole; }
}
