package com.codecoach.module.interview.vo;

public class InterviewAnswerResponse {

    private InterviewMessageVO userAnswer;

    private InterviewMessageVO aiFeedback;

    private InterviewMessageVO nextQuestion;

    private Boolean finished;

    public InterviewAnswerResponse(
            InterviewMessageVO userAnswer,
            InterviewMessageVO aiFeedback,
            InterviewMessageVO nextQuestion,
            Boolean finished
    ) {
        this.userAnswer = userAnswer;
        this.aiFeedback = aiFeedback;
        this.nextQuestion = nextQuestion;
        this.finished = finished;
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
}
