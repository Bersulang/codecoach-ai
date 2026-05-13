package com.codecoach.module.mockinterview.model;

import java.util.List;
import lombok.Data;

@Data
public class InterviewPlan {

    private String planId;

    private String interviewType;

    private String targetRole;

    private String difficulty;

    private Integer totalRounds;

    private List<InterviewPlanStage> stages;
}
