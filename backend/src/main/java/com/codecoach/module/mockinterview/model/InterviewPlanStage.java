package com.codecoach.module.mockinterview.model;

import java.util.List;
import lombok.Data;

@Data
public class InterviewPlanStage {

    private String stage;

    private String stageName;

    private String objective;

    private Integer suggestedRounds;

    private List<String> focusPoints;

    private List<String> ragSources;

    private List<String> scoringDimensions;
}
