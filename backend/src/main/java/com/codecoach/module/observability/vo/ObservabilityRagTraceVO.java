package com.codecoach.module.observability.vo;

import java.time.LocalDateTime;

public record ObservabilityRagTraceVO(
        String traceId,
        String query,
        String rewrittenQuery,
        String sourceTypes,
        Integer topK,
        Integer hitCount,
        Double avgScore,
        Integer contextChars,
        Boolean success,
        String fallbackReason,
        Long latencyMs,
        LocalDateTime createdAt
) {
}
