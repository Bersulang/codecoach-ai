package com.codecoach.module.mockinterview.vo;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MockInterviewSessionDetailVO {

    private Long sessionId;

    private String interviewType;

    private String targetRole;

    private String difficulty;

    private Long projectId;

    private String projectName;

    private Long resumeId;

    private String resumeTitle;

    private String status;

    private Integer currentRound;

    private Integer maxRound;

    private String currentStage;

    private Long reportId;

    private List<MockInterviewMessageVO> messages;
}
