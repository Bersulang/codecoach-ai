package com.codecoach.module.knowledge.vo;

import java.util.List;

public class KnowledgeTopicVO {

    private Long id;

    private String category;

    private String name;

    private String description;

    private String difficulty;

    private String interviewFocus;

    private List<String> tags;

    private Integer sortOrder;

    public KnowledgeTopicVO(
            Long id,
            String category,
            String name,
            String description,
            String difficulty,
            String interviewFocus,
            List<String> tags,
            Integer sortOrder
    ) {
        this.id = id;
        this.category = category;
        this.name = name;
        this.description = description;
        this.difficulty = difficulty;
        this.interviewFocus = interviewFocus;
        this.tags = tags;
        this.sortOrder = sortOrder;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
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

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public String getInterviewFocus() {
        return interviewFocus;
    }

    public void setInterviewFocus(String interviewFocus) {
        this.interviewFocus = interviewFocus;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}
