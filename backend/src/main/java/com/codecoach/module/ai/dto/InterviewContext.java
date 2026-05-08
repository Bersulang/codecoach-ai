package com.codecoach.module.ai.dto;

import com.codecoach.module.project.entity.Project;

import java.util.List;

public class InterviewContext {

    private Project project;

    private String targetRole;

    private String difficulty;

    private Integer roundNo;

    private String currentQuestion;

    private String userAnswer;

    private List<QaRecord> qaRecords;

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public String getTargetRole() {
        return targetRole;
    }

    public void setTargetRole(String targetRole) {
        this.targetRole = targetRole;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public Integer getRoundNo() {
        return roundNo;
    }

    public void setRoundNo(Integer roundNo) {
        this.roundNo = roundNo;
    }

    public String getCurrentQuestion() {
        return currentQuestion;
    }

    public void setCurrentQuestion(String currentQuestion) {
        this.currentQuestion = currentQuestion;
    }

    public String getUserAnswer() {
        return userAnswer;
    }

    public void setUserAnswer(String userAnswer) {
        this.userAnswer = userAnswer;
    }

    public List<QaRecord> getQaRecords() {
        return qaRecords;
    }

    public void setQaRecords(List<QaRecord> qaRecords) {
        this.qaRecords = qaRecords;
    }

    public static class QaRecord {

        private String question;

        private String answer;

        private String feedback;

        public QaRecord() {
        }

        public QaRecord(String question, String answer, String feedback) {
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
