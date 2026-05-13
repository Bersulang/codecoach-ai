package com.codecoach.module.agent.function.model;

import java.util.Map;

public record FunctionToolCall(String id, String name, Map<String, Object> arguments) {
}
