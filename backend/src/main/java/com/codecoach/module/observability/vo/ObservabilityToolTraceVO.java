package com.codecoach.module.observability.vo;

import java.time.LocalDateTime;

public record ObservabilityToolTraceVO(
        String traceId,
        String runId,
        String agentType,
        String toolName,
        String toolType,
        Boolean success,
        Long latencyMs,
        String errorCode,
        LocalDateTime createdAt,
        String inputSummary,
        String outputSummary
) {
}
