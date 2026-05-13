package com.codecoach.module.observability.vo;

import java.time.LocalDateTime;

public record ObservabilityAgentStepVO(
        String stepId,
        String runId,
        String stepType,
        String stepName,
        String toolName,
        String status,
        Long latencyMs,
        String inputSummary,
        String outputSummary,
        String errorCode,
        LocalDateTime createdAt
) {
}
