package com.codecoach.module.rag.model;

import java.util.Map;

public record RagEvaluationResult(
        int hitCount,
        Double avgScore,
        boolean emptyHit,
        Map<String, Long> sourceTypeDistribution,
        int contextChars
) {
}
