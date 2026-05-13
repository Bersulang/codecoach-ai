package com.codecoach.module.observability.vo;

import java.time.LocalDateTime;

public record ObservabilitySingleFlightTraceVO(
        String traceId,
        String requestKey,
        String action,
        Boolean success,
        Long latencyMs,
        String fallbackReason,
        LocalDateTime createdAt
) {
}
