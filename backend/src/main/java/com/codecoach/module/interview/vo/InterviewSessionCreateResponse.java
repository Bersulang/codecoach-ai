package com.codecoach.module.interview.vo;

public class InterviewSessionCreateResponse {

    private Long sessionId;

    private InterviewMessageVO firstQuestion;

    public InterviewSessionCreateResponse(Long sessionId, InterviewMessageVO firstQuestion) {
        this.sessionId = sessionId;
        this.firstQuestion = firstQuestion;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public InterviewMessageVO getFirstQuestion() {
        return firstQuestion;
    }

    public void setFirstQuestion(InterviewMessageVO firstQuestion) {
        this.firstQuestion = firstQuestion;
    }
}
