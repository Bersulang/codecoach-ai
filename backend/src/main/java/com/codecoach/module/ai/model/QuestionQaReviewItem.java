package com.codecoach.module.ai.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuestionQaReviewItem {

    private String question;

    private String answer;

    private String referenceAnswer;

    private String feedback;
}
