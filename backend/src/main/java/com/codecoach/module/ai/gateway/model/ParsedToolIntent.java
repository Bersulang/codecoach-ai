package com.codecoach.module.ai.gateway.model;

import java.util.Map;

public record ParsedToolIntent(String toolName, String reason, Map<String, Object> params) {
}
