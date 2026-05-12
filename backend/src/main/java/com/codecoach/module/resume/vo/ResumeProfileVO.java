package com.codecoach.module.resume.vo;

import com.codecoach.module.resume.model.ResumeAnalysisResult;
import java.time.LocalDateTime;
import java.util.List;

public class ResumeProfileVO {

    private Long id;
    private Long documentId;
    private String documentTitle;
    private String title;
    private String targetRole;
    private String analysisStatus;
    private String summary;
    private ResumeAnalysisResult analysisResult;
    private String errorMessage;
    private LocalDateTime analyzedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ResumeProjectExperienceVO> projectExperiences;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long documentId) { this.documentId = documentId; }
    public String getDocumentTitle() { return documentTitle; }
    public void setDocumentTitle(String documentTitle) { this.documentTitle = documentTitle; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getTargetRole() { return targetRole; }
    public void setTargetRole(String targetRole) { this.targetRole = targetRole; }
    public String getAnalysisStatus() { return analysisStatus; }
    public void setAnalysisStatus(String analysisStatus) { this.analysisStatus = analysisStatus; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public ResumeAnalysisResult getAnalysisResult() { return analysisResult; }
    public void setAnalysisResult(ResumeAnalysisResult analysisResult) { this.analysisResult = analysisResult; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public LocalDateTime getAnalyzedAt() { return analyzedAt; }
    public void setAnalyzedAt(LocalDateTime analyzedAt) { this.analyzedAt = analyzedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public List<ResumeProjectExperienceVO> getProjectExperiences() { return projectExperiences; }
    public void setProjectExperiences(List<ResumeProjectExperienceVO> projectExperiences) { this.projectExperiences = projectExperiences; }
}
