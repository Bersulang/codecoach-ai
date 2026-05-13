package com.codecoach.module.mockinterview.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MockInterviewFinishResponse {

    private Long reportId;

    private Long sessionId;

    private Integer totalScore;
}
