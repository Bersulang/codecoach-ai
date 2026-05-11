package com.codecoach.module.knowledge.vo;

import java.time.LocalDateTime;

public class KnowledgeArticleDetailVO {

    private Long id;

    private Long topicId;

    private String category;

    private String topicName;

    private String title;

    private String summary;

    private String content;

    private String version;

    private LocalDateTime updatedAt;

    public KnowledgeArticleDetailVO(
            Long id,
            Long topicId,
            String category,
            String topicName,
            String title,
            String summary,
            String content,
            String version,
            LocalDateTime updatedAt
    ) {
        this.id = id;
        this.topicId = topicId;
        this.category = category;
        this.topicName = topicName;
        this.title = title;
        this.summary = summary;
        this.content = content;
        this.version = version;
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

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
