package com.codecoach.module.knowledge.vo;

import java.time.LocalDateTime;

public class KnowledgeArticleListVO {

    private Long id;

    private Long topicId;

    private String category;

    private String topicName;

    private String title;

    private String summary;

    private String version;

    private String status;

    private Integer sortOrder;

    private LocalDateTime updatedAt;

    public KnowledgeArticleListVO(
            Long id,
            Long topicId,
            String category,
            String topicName,
            String title,
            String summary,
            String version,
            String status,
            Integer sortOrder,
            LocalDateTime updatedAt
    ) {
        this.id = id;
        this.topicId = topicId;
        this.category = category;
        this.topicName = topicName;
        this.title = title;
        this.summary = summary;
        this.version = version;
        this.status = status;
        this.sortOrder = sortOrder;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTopicId() {
        return topicId;
    }

    public void setTopicId(Long topicId) {
        this.topicId = topicId;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getTopicName() {
        return topicName;
    }

    public void setTopicName(String topicName) {
        this.topicName = topicName;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
