package com.codecoach.module.interview.vo;

public class InterviewAnswerResponse {

    private InterviewMessageVO userAnswer;

    private InterviewMessageVO aiFeedback;

    private InterviewMessageVO nextQuestion;

    private Boolean finished;

    private Long reportId;

    private Integer totalScore;

    public InterviewAnswerResponse(
            InterviewMessageVO userAnswer,
            InterviewMessageVO aiFeedback,
            InterviewMessageVO nextQuestion,
            Boolean finished,
            Long reportId,
            Integer totalScore
    ) {
        this.userAnswer = userAnswer;
        this.aiFeedback = aiFeedback;
        this.nextQuestion = nextQuestion;
        this.finished = finished;
        this.reportId = reportId;
        this.totalScore = totalScore;
    }

    public InterviewMessageVO getUserAnswer() {
        return userAnswer;
    }

    public void setUserAnswer(InterviewMessageVO userAnswer) {
        this.userAnswer = userAnswer;
    }

    public InterviewMessageVO getAiFeedback() {
        return aiFeedback;
    }

    public void setAiFeedback(InterviewMessageVO aiFeedback) {
        this.aiFeedback = aiFeedback;
    }

    public InterviewMessageVO getNextQuestion() {
        return nextQuestion;
    }

    public void setNextQuestion(InterviewMessageVO nextQuestion) {
        this.nextQuestion = nextQuestion;
    }

    public Boolean getFinished() {
        return finished;
    }

    public void setFinished(Boolean finished) {
        this.finished = finished;
    }

    public Long getReportId() {
        return reportId;
    }

    public void setReportId(Long reportId) {
        this.reportId = reportId;
    }

    public Integer getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(Integer totalScore) {
        this.totalScore = totalScore;
    }
}
