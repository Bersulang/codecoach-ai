package com.codecoach.module.question.dto;

import jakarta.validation.constraints.NotBlank;

public class QuestionAnswerRequest {

    @NotBlank(message = "回答不能为空")
    private String answer;

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }
}
