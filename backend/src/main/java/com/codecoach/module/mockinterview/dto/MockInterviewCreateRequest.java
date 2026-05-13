package com.codecoach.module.mockinterview.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MockInterviewCreateRequest {

    private String interviewType;

    @NotBlank
    private String targetRole;

    private String difficulty;

    @Min(4)
    @Max(12)
    private Integer maxRound;

    private Long projectId;

    private Long resumeId;
}
