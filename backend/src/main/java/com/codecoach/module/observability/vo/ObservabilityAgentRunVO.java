package com.codecoach.module.observability.vo;

import java.time.LocalDateTime;

public record ObservabilityAgentRunVO(
        String runId,
        String traceId,
        String agentType,
        String status,
        Long latencyMs,
        String inputSummary,
        String outputSummary,
        String errorCode,
        String errorMessage,
        LocalDateTime createdAt
) {
}
