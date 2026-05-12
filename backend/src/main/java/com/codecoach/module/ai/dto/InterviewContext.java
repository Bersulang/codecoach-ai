package com.codecoach.module.ai.dto;

import com.codecoach.module.project.entity.Project;

import java.util.List;

public class InterviewContext {

    private Project project;

    private Long userId;

    private Long projectId;

    private Long resumeId;

    private Long resumeProjectId;

    private Long resumeDocumentId;

    private Long sessionId;

    private String targetRole;

    private String difficulty;

    private Integer roundNo;

    private Integer maxRound;

    private String currentQuestion;

    private String userAnswer;

    private String ragContext;

    private List<QaRecord> qaRecords;

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public Long getResumeId() {
        return resumeId;
    }

    public void setResumeId(Long resumeId) {
        this.resumeId = resumeId;
    }

    public Long getResumeProjectId() {
        return resumeProjectId;
    }

    public void setResumeProjectId(Long resumeProjectId) {
        this.resumeProjectId = resumeProjectId;
    }

    public Long getResumeDocumentId() {
        return resumeDocumentId;
    }

    public void setResumeDocumentId(Long resumeDocumentId) {
        this.resumeDocumentId = resumeDocumentId;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
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

    public Integer getMaxRound() {
        return maxRound;
    }

    public void setMaxRound(Integer maxRound) {
        this.maxRound = maxRound;
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

    public String getRagContext() {
        return ragContext;
    }

    public void setRagContext(String ragContext) {
        this.ragContext = ragContext;
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
