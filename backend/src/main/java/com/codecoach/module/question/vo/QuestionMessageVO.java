package com.codecoach.module.question.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

public class QuestionMessageVO {

    private Long messageId;

    private String role;

    private String messageType;

    private String content;

    private Integer roundNo;

    private Integer score;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdAt;

    public QuestionMessageVO(
            Long messageId,
            String role,
            String messageType,
            String content,
            Integer roundNo,
            Integer score,
            LocalDateTime createdAt
    ) {
        this.messageId = messageId;
        this.role = role;
        this.messageType = messageType;
        this.content = content;
        this.roundNo = roundNo;
        this.score = score;
        this.createdAt = createdAt;
    }

    public Long getMessageId() {
        return messageId;
    }

    public void setMessageId(Long messageId) {
        this.messageId = messageId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getRoundNo() {
        return roundNo;
    }

    public void setRoundNo(Integer roundNo) {
        this.roundNo = roundNo;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
