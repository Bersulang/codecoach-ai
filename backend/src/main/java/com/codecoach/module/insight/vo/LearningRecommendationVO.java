package com.codecoach.module.insight.vo;

public class LearningRecommendationVO {

    private String title;

    private String reason;

    private Long articleId;

    private Long topicId;

    private String category;

    private String topicName;

    private String section;

    private Double score;

    private String evidence;

    private String targetPath;

    public LearningRecommendationVO() {
    }

    public LearningRecommendationVO(
            String title,
            String reason,
            Long articleId,
            Long topicId,
            String category,
            String topicName,
            String section,
            Double score,
            String evidence,
            String targetPath
    ) {
        this.title = title;
        this.reason = reason;
        this.articleId = articleId;
        this.topicId = topicId;
        this.category = category;
        this.topicName = topicName;
        this.section = section;
        this.score = score;
        this.evidence = evidence;
        this.targetPath = targetPath;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Long getArticleId() {
        return articleId;
    }

    public void setArticleId(Long articleId) {
        this.articleId = articleId;
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

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public String getEvidence() {
        return evidence;
    }

    public void setEvidence(String evidence) {
        this.evidence = evidence;
    }

    public String getTargetPath() {
        return targetPath;
    }

    public void setTargetPath(String targetPath) {
        this.targetPath = targetPath;
    }
}
