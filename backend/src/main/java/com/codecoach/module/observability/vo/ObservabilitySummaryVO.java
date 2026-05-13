package com.codecoach.module.observability.vo;

import java.time.LocalDateTime;
import java.util.List;

public record ObservabilitySummaryVO(
        LocalDateTime since,
        Integer windowHours,
        Long agentRunCount,
        Long aiCallCount,
        Long toolCallCount,
        Long averageAgentLatencyMs,
        Long averageLlmLatencyMs,
        Long failureCount,
        List<ObservabilityErrorItemVO> recentErrors,
        List<ObservabilityLatencyItemVO> slowestItems
) {
}
