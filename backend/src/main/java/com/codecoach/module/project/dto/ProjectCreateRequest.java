package com.codecoach.module.project.dto;

import jakarta.validation.constraints.NotBlank;

public class ProjectCreateRequest {

    @NotBlank(message = "项目名称不能为空")
    private String name;

    @NotBlank(message = "项目描述不能为空")
    private String description;

    @NotBlank(message = "技术栈不能为空")
    private String techStack;

    private String role;

    private String highlights;

    private String difficulties;

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
}
