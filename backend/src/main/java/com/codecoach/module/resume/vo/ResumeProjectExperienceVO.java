package com.codecoach.module.resume.vo;

import java.time.LocalDateTime;
import java.util.List;

public class ResumeProjectExperienceVO {

    private Long id;
    private Long projectId;
    private String projectName;
    private String description;
    private List<String> techStack;
    private String role;
    private List<String> highlights;
    private List<String> riskPoints;
    private List<String> recommendedQuestions;
    private String matchReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public List<String> getTechStack() { return techStack; }
    public void setTechStack(List<String> techStack) { this.techStack = techStack; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public List<String> getHighlights() { return highlights; }
    public void setHighlights(List<String> highlights) { this.highlights = highlights; }
    public List<String> getRiskPoints() { return riskPoints; }
    public void setRiskPoints(List<String> riskPoints) { this.riskPoints = riskPoints; }
    public List<String> getRecommendedQuestions() { return recommendedQuestions; }
    public void setRecommendedQuestions(List<String> recommendedQuestions) { this.recommendedQuestions = recommendedQuestions; }
    public String getMatchReason() { return matchReason; }
    public void setMatchReason(String matchReason) { this.matchReason = matchReason; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
