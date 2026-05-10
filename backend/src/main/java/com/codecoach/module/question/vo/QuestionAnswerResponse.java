package com.codecoach.module.question.vo;

public class QuestionAnswerResponse {

    private QuestionMessageVO userAnswer;

    private QuestionMessageVO aiFeedback;

    private QuestionMessageVO referenceAnswer;

    private QuestionMessageVO nextQuestion;

    private Boolean finished;

    private Long reportId;

    private Integer totalScore;

    public QuestionAnswerResponse(
            QuestionMessageVO userAnswer,
            QuestionMessageVO aiFeedback,
            QuestionMessageVO referenceAnswer,
            QuestionMessageVO nextQuestion,
            Boolean finished,
            Long reportId,
            Integer totalScore
    ) {
        this.userAnswer = userAnswer;
        this.aiFeedback = aiFeedback;
        this.referenceAnswer = referenceAnswer;
        this.nextQuestion = nextQuestion;
        this.finished = finished;
        this.reportId = reportId;
        this.totalScore = totalScore;
    }

    public QuestionMessageVO getUserAnswer() {
        return userAnswer;
    }

    public void setUserAnswer(QuestionMessageVO userAnswer) {
        this.userAnswer = userAnswer;
    }

    public QuestionMessageVO getAiFeedback() {
        return aiFeedback;
    }

    public void setAiFeedback(QuestionMessageVO aiFeedback) {
        this.aiFeedback = aiFeedback;
    }

    public QuestionMessageVO getReferenceAnswer() {
        return referenceAnswer;
    }

    public void setReferenceAnswer(QuestionMessageVO referenceAnswer) {
        this.referenceAnswer = referenceAnswer;
    }

    public QuestionMessageVO getNextQuestion() {
        return nextQuestion;
    }

    public void setNextQuestion(QuestionMessageVO nextQuestion) {
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
