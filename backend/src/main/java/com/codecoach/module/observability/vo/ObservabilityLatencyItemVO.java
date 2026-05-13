package com.codecoach.module.observability.vo;

public record ObservabilityLatencyItemVO(
        String source,
        String targetId,
        String name,
        Long latencyMs
) {
}
