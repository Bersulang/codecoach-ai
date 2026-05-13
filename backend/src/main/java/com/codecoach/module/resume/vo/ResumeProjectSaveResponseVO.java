package com.codecoach.module.resume.vo;

public class ResumeProjectSaveResponseVO {

    private Long projectId;

    private ResumeProfileVO resume;

    public ResumeProjectSaveResponseVO() {
    }

    public ResumeProjectSaveResponseVO(Long projectId, ResumeProfileVO resume) {
        this.projectId = projectId;
        this.resume = resume;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public ResumeProfileVO getResume() {
        return resume;
    }

    public void setResume(ResumeProfileVO resume) {
        this.resume = resume;
    }
}
