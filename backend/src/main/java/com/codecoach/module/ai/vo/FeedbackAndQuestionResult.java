package com.codecoach.module.ai.vo;

public class FeedbackAndQuestionResult {

    private String feedback;

    private String nextQuestion;

    public FeedbackAndQuestionResult() {
    }

    public FeedbackAndQuestionResult(String feedback, String nextQuestion) {
        this.feedback = feedback;
        this.nextQuestion = nextQuestion;
    }

    public String getFeedback() {
        return feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }

    public String getNextQuestion() {
        return nextQuestion;
    }

    public void setNextQuestion(String nextQuestion) {
        this.nextQuestion = nextQuestion;
    }
}
