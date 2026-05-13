package com.codecoach.module.mockinterview.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MockInterviewHistoryVO {

    private Long sessionId;

    private String interviewType;

    private String targetRole;

    private String difficulty;

    private String status;

    private Integer currentRound;

    private Integer maxRound;

    private String currentStage;

    private Integer totalScore;

    private Long reportId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdAt;
}
