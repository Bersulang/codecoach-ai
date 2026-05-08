package com.codecoach.module.interview.dto;

import jakarta.validation.constraints.NotBlank;

public class InterviewAnswerRequest {

    @NotBlank(message = "回答不能为空")
    private String answer;

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }
}
