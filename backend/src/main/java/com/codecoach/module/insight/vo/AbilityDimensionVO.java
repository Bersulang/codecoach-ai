package com.codecoach.module.insight.vo;

public class AbilityDimensionVO {

    private String dimensionCode;

    private String dimensionName;

    private String category;

    private Integer score;

    private String trend;

    private Integer evidenceCount;

    private String latestEvidence;

    public AbilityDimensionVO(
            String dimensionCode,
            String dimensionName,
            String category,
            Integer score,
            String trend,
            Integer evidenceCount,
            String latestEvidence
    ) {
        this.dimensionCode = dimensionCode;
        this.dimensionName = dimensionName;
        this.category = category;
        this.score = score;
        this.trend = trend;
        this.evidenceCount = evidenceCount;
        this.latestEvidence = latestEvidence;
    }

    public String getDimensionCode() {
        return dimensionCode;
    }

    public void setDimensionCode(String dimensionCode) {
        this.dimensionCode = dimensionCode;
    }

    public String getDimensionName() {
        return dimensionName;
    }

    public void setDimensionName(String dimensionName) {
        this.dimensionName = dimensionName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public String getTrend() {
        return trend;
    }

    public void setTrend(String trend) {
        this.trend = trend;
    }

    public Integer getEvidenceCount() {
        return evidenceCount;
    }

    public void setEvidenceCount(Integer evidenceCount) {
        this.evidenceCount = evidenceCount;
    }

    public String getLatestEvidence() {
        return latestEvidence;
    }

    public void setLatestEvidence(String latestEvidence) {
        this.latestEvidence = latestEvidence;
    }
}
