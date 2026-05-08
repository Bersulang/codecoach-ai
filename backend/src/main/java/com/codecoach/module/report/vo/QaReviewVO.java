package com.codecoach.module.report.vo;

public class QaReviewVO {

    private String question;

    private String answer;

    private String feedback;

    public QaReviewVO() {
    }

    public QaReviewVO(String question, String answer, String feedback) {
        this.question = question;
        this.answer = answer;
        this.feedback = feedback;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public String getFeedback() {
        return feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }
}
