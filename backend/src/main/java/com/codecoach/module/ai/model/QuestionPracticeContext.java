package com.codecoach.module.ai.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuestionPracticeContext {

    private Long userId;

    private Long topicId;

    private Long sessionId;

    private String category;

    private String topicName;

    private String topicDescription;

    private String interviewFocus;

    private String tags;

    private String targetRole;

    private String difficulty;

    private Integer currentRound;

    private Integer maxRound;

    private String historyMessages;

    private String currentQuestion;

    private String userAnswer;
}
