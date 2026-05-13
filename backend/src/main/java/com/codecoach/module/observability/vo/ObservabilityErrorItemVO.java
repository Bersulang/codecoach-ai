package com.codecoach.module.observability.vo;

import java.time.LocalDateTime;

public record ObservabilityErrorItemVO(
        String source,
        String targetId,
        String name,
        String errorCode,
        LocalDateTime createdAt
) {
}
