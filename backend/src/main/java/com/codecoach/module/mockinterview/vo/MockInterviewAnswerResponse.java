package com.codecoach.module.mockinterview.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MockInterviewAnswerResponse {

    private MockInterviewMessageVO userAnswer;

    private MockInterviewMessageVO nextQuestion;

    private Boolean finished;

    private Long reportId;

    private Integer totalScore;

    private String currentStage;

    private String currentStageGoal;

    private Integer currentStageProgress;

    private Integer currentStageSuggestedRounds;
}
