package com.codecoach.module.insight.vo;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public class WeaknessInsightVO {

    private String keyword;

    private Integer count;

    private String relatedDimension;

    private String latestEvidence;

    private String latestSourceType;

    private Long latestSourceId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime latestAt;

    public WeaknessInsightVO(
            String keyword,
            Integer count,
            String relatedDimension,
            String latestEvidence,
            String latestSourceType,
            Long latestSourceId,
            LocalDateTime latestAt
    ) {
        this.keyword = keyword;
        this.count = count;
        this.relatedDimension = relatedDimension;
        this.latestEvidence = latestEvidence;
        this.latestSourceType = latestSourceType;
        this.latestSourceId = latestSourceId;
        this.latestAt = latestAt;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public String getRelatedDimension() {
        return relatedDimension;
    }

    public void setRelatedDimension(String relatedDimension) {
        this.relatedDimension = relatedDimension;
    }

    public String getLatestEvidence() {
        return latestEvidence;
    }

    public void setLatestEvidence(String latestEvidence) {
        this.latestEvidence = latestEvidence;
    }

    public String getLatestSourceType() {
        return latestSourceType;
    }

    public void setLatestSourceType(String latestSourceType) {
        this.latestSourceType = latestSourceType;
    }

    public Long getLatestSourceId() {
        return latestSourceId;
    }

    public void setLatestSourceId(Long latestSourceId) {
        this.latestSourceId = latestSourceId;
    }

    public LocalDateTime getLatestAt() {
        return latestAt;
    }

    public void setLatestAt(LocalDateTime latestAt) {
        this.latestAt = latestAt;
    }
}
