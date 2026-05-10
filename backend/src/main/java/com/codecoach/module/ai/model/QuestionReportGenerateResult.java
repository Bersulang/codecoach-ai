package com.codecoach.module.ai.model;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuestionReportGenerateResult {

    private Integer totalScore;

    private String summary;

    private List<String> strengths;

    private List<String> weaknesses;

    private List<String> suggestions;

    private List<String> knowledgeGaps;

    private List<QuestionQaReviewItem> qaReview;
}
