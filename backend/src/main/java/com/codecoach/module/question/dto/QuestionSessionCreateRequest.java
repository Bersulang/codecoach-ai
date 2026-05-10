package com.codecoach.module.question.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class QuestionSessionCreateRequest {

    @NotNull(message = "知识点不能为空")
    private Long topicId;

    @NotBlank(message = "目标岗位不能为空")
    private String targetRole;

    private String difficulty = "NORMAL";

    public Long getTopicId() {
        return topicId;
    }

    public void setTopicId(Long topicId) {
        this.topicId = topicId;
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
}
