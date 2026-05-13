package com.codecoach.module.memory.model;

public record MemorySemanticHit(
        Long memoryId,
        String memoryType,
        String value,
        String confidence,
        Integer weight,
        Double score
) {
}
