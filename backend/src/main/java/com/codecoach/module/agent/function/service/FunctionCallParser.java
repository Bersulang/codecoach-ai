package com.codecoach.module.agent.function.service;

import com.codecoach.module.agent.function.model.FunctionToolCall;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class FunctionCallParser {

    private final ObjectMapper objectMapper;

    public FunctionCallParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<FunctionToolCall> parse(String rawModelResponse) {
        if (!StringUtils.hasText(rawModelResponse)) {
            return List.of();
        }
        try {
            JsonNode root = objectMapper.readTree(rawModelResponse);
            JsonNode calls = root.has("tool_calls") ? root.get("tool_calls") : root.get("toolCalls");
            if (calls == null && root.has("choices") && root.get("choices").isArray() && !root.get("choices").isEmpty()) {
                JsonNode message = root.get("choices").get(0).get("message");
                calls = message == null ? null : message.get("tool_calls");
            }
            if (calls == null || !calls.isArray()) {
                return List.of();
            }
            List<FunctionToolCall> result = new ArrayList<>();
            for (JsonNode call : calls) {
                JsonNode function = call.get("function");
                String name = function == null ? text(call, "name") : text(function, "name");
                if (!StringUtils.hasText(name)) {
                    continue;
                }
                String argumentsText = function == null ? null : text(function, "arguments");
                Map<String, Object> arguments = parseArguments(argumentsText, function == null ? call.get("arguments") : function.get("arguments"));
                result.add(new FunctionToolCall(text(call, "id"), name, arguments));
            }
            return result;
        } catch (Exception exception) {
            return List.of();
        }
    }

    private Map<String, Object> parseArguments(String text, JsonNode node) {
        try {
            if (StringUtils.hasText(text)) {
                return objectMapper.readValue(text, new TypeReference<>() {
                });
            }
            if (node != null && node.isObject()) {
                return objectMapper.convertValue(node, new TypeReference<>() {
                });
            }
        } catch (Exception ignored) {
            return Map.of();
        }
        return Map.of();
    }

    private String text(JsonNode node, String field) {
        return node == null || !node.has(field) || node.get(field).isNull() ? null : node.get(field).asText();
    }
}
