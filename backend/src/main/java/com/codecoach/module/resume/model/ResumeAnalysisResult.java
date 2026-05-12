package com.codecoach.module.resume.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.ArrayList;
import java.util.List;

public class ResumeAnalysisResult {

    private String summary;
    private List<SkillItem> skills = new ArrayList<>();
    private List<ProjectExperienceItem> projectExperiences = new ArrayList<>();
    private List<RiskPointItem> riskPoints = new ArrayList<>();
    @JsonDeserialize(using = FlexibleStringListDeserializer.class)
    private List<String> interviewQuestions = new ArrayList<>();
    @JsonDeserialize(using = FlexibleStringListDeserializer.class)
    private List<String> optimizationSuggestions = new ArrayList<>();

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public List<SkillItem> getSkills() { return skills; }
    public void setSkills(List<SkillItem> skills) { this.skills = skills; }
    public List<ProjectExperienceItem> getProjectExperiences() { return projectExperiences; }
    public void setProjectExperiences(List<ProjectExperienceItem> projectExperiences) { this.projectExperiences = projectExperiences; }
    public List<RiskPointItem> getRiskPoints() { return riskPoints; }
    public void setRiskPoints(List<RiskPointItem> riskPoints) { this.riskPoints = riskPoints; }
    public List<String> getInterviewQuestions() { return interviewQuestions; }
    public void setInterviewQuestions(List<String> interviewQuestions) { this.interviewQuestions = interviewQuestions; }
    public List<String> getOptimizationSuggestions() { return optimizationSuggestions; }
    public void setOptimizationSuggestions(List<String> optimizationSuggestions) { this.optimizationSuggestions = optimizationSuggestions; }

    public static class SkillItem {
        private String name;
        private String category;
        private String riskLevel;
        private String reason;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public String getRiskLevel() { return riskLevel; }
        public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }

    public static class ProjectExperienceItem {
        private String projectName;
        private String description;
        @JsonDeserialize(using = FlexibleStringListDeserializer.class)
        private List<String> techStack = new ArrayList<>();
        private String role;
        @JsonDeserialize(using = FlexibleStringListDeserializer.class)
        private List<String> highlights = new ArrayList<>();
        @JsonDeserialize(using = FlexibleStringListDeserializer.class)
        private List<String> riskPoints = new ArrayList<>();
        @JsonDeserialize(using = FlexibleStringListDeserializer.class)
        private List<String> recommendedQuestions = new ArrayList<>();
        private String possibleProjectName;

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
        public String getPossibleProjectName() { return possibleProjectName; }
        public void setPossibleProjectName(String possibleProjectName) { this.possibleProjectName = possibleProjectName; }
    }

    public static class RiskPointItem {
        private String type;
        private String level;
        private String evidence;
        private String suggestion;

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getLevel() { return level; }
        public void setLevel(String level) { this.level = level; }
        public String getEvidence() { return evidence; }
        public void setEvidence(String evidence) { this.evidence = evidence; }
        public String getSuggestion() { return suggestion; }
        public void setSuggestion(String suggestion) { this.suggestion = suggestion; }
    }
}
