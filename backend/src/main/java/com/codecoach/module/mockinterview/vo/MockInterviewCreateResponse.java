package com.codecoach.module.mockinterview.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MockInterviewCreateResponse {

    private Long sessionId;

    private MockInterviewMessageVO firstMessage;

    private MockInterviewPlanVO plan;
}
