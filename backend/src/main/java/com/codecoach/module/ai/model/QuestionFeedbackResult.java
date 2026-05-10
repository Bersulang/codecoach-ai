package com.codecoach.module.ai.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuestionFeedbackResult {

    private String feedback;

    private String referenceAnswer;

    private String nextQuestion;

    private Integer score;
}
