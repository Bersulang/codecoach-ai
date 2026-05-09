package com.codecoach.module.ai.vo;

import java.util.List;

public class ReportGenerateResult {

    private Integer totalScore;

    private String summary;

    private List<String> strengths;

    private List<String> weaknesses;

    private List<String> suggestions;

    private List<QaReviewItem> qaReview;

    public ReportGenerateResult() {
    }

    public ReportGenerateResult(
            Integer totalScore,
            String summary,
            List<String> strengths,
            List<String> weaknesses,
            List<String> suggestions,
            List<QaReviewItem> qaReview
    ) {
        this.totalScore = totalScore;
        this.summary = summary;
        this.strengths = strengths;
        this.weaknesses = weaknesses;
        this.suggestions = suggestions;
        this.qaReview = qaReview;
    }

    public Integer getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(Integer totalScore) {
        this.totalScore = totalScore;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public List<String> getStrengths() {
        return strengths;
    }

    public void setStrengths(List<String> strengths) {
        this.strengths = strengths;
    }

    public List<String> getWeaknesses() {
        return weaknesses;
    }

    public void setWeaknesses(List<String> weaknesses) {
        this.weaknesses = weaknesses;
    }

    public List<String> getSuggestions() {
        return suggestions;
    }

    public void setSuggestions(List<String> suggestions) {
        this.suggestions = suggestions;
    }

    public List<QaReviewItem> getQaReview() {
        return qaReview;
    }

    public void setQaReview(List<QaReviewItem> qaReview) {
        this.qaReview = qaReview;
    }

    public static class QaReviewItem {

        private String question;

        private String answer;

        private String feedback;

        public QaReviewItem() {
        }

        public QaReviewItem(String question, String answer, String feedback) {
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
}
