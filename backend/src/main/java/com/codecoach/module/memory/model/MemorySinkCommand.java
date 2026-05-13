package com.codecoach.module.memory.model;

import lombok.Data;

@Data
public class MemorySinkCommand {

    private Long userId;

    private String memoryType;

    private String memoryKey;

    private String memoryValue;

    private String sourceType;

    private Long sourceId;

    private String confidence = MemoryConfidence.MEDIUM;

    private Integer weightDelta = 1;
}
