package com.codecoach.module.observability.vo;

import java.time.LocalDateTime;

public record ObservabilityAiCallVO(
        String provider,
        String modelName,
        String requestType,
        String promptVersion,
        Long latencyMs,
        Boolean success,
        String errorCode,
        LocalDateTime createdAt
) {
}
