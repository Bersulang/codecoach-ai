package com.codecoach.module.resume.vo;

import java.util.List;

public class ResumeProjectDraftVO {

    private Long resumeId;

    private Long resumeProjectId;

    private String name;

    private String description;

    private String techStack;

    private String role;

    private String highlights;

    private String difficulties;

    private List<String> riskPoints;

    private List<String> pendingItems;

    private String safetyNotice;

    public Long getResumeId() {
        return resumeId;
    }

    public void setResumeId(Long resumeId) {
        this.resumeId = resumeId;
    }

    public Long getResumeProjectId() {
        return resumeProjectId;
    }

    public void setResumeProjectId(Long resumeProjectId) {
        this.resumeProjectId = resumeProjectId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTechStack() {
        return techStack;
    }

    public void setTechStack(String techStack) {
        this.techStack = techStack;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getHighlights() {
        return highlights;
    }

    public void setHighlights(String highlights) {
        this.highlights = highlights;
    }

    public String getDifficulties() {
        return difficulties;
    }

    public void setDifficulties(String difficulties) {
        this.difficulties = difficulties;
    }

    public List<String> getRiskPoints() {
        return riskPoints;
    }

    public void setRiskPoints(List<String> riskPoints) {
        this.riskPoints = riskPoints;
    }

    public List<String> getPendingItems() {
        return pendingItems;
    }

    public void setPendingItems(List<String> pendingItems) {
        this.pendingItems = pendingItems;
    }

    public String getSafetyNotice() {
        return safetyNotice;
    }

    public void setSafetyNotice(String safetyNotice) {
        this.safetyNotice = safetyNotice;
    }
}
