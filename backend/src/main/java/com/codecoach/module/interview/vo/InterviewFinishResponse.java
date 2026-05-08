package com.codecoach.module.interview.vo;

public class InterviewFinishResponse {

    private Long reportId;

    private Long sessionId;

    private Integer totalScore;

    public InterviewFinishResponse(Long reportId, Long sessionId, Integer totalScore) {
        this.reportId = reportId;
        this.sessionId = sessionId;
        this.totalScore = totalScore;
    }

    public Long getReportId() {
        return reportId;
    }

    public void setReportId(Long reportId) {
        this.reportId = reportId;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public Integer getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(Integer totalScore) {
        this.totalScore = totalScore;
    }
}
