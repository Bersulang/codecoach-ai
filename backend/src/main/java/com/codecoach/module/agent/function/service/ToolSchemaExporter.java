package com.codecoach.module.agent.function.service;

import com.codecoach.module.agent.tool.dto.ToolDefinition;
import com.codecoach.module.agent.tool.enums.ToolExecutionMode;
import com.codecoach.module.agent.tool.enums.ToolRiskLevel;
import com.codecoach.module.agent.tool.enums.ToolType;
import com.codecoach.module.agent.tool.service.AgentToolRegistry;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class ToolSchemaExporter {

    private static final Set<String> FUNCTION_WHITELIST = Set.of(
            "GET_ABILITY_SUMMARY",
            "GET_RECENT_TRAINING_SUMMARY",
            "SEARCH_KNOWLEDGE",
            "SEARCH_USER_DOCUMENTS",
            "GET_RESUME_RISK_SUMMARY",
            "GET_USER_MEMORY_SUMMARY",
            "GET_MOCK_INTERVIEW_SUMMARY",
            "GET_REPORT_REPLAY_DATA"
    );

    private final AgentToolRegistry agentToolRegistry;

    public ToolSchemaExporter(AgentToolRegistry agentToolRegistry) {
        this.agentToolRegistry = agentToolRegistry;
    }

    public List<Map<String, Object>> exportOpenAiTools() {
        return agentToolRegistry.listDefinitions().stream()
                .filter(this::exportable)
                .map(this::toOpenAiTool)
                .toList();
    }

    public boolean isExportable(String toolName) {
        return FUNCTION_WHITELIST.contains(toolName);
    }

    private boolean exportable(ToolDefinition definition) {
        return definition != null
                && FUNCTION_WHITELIST.contains(definition.getToolName())
                && definition.getToolType() == ToolType.QUERY
                && definition.getRiskLevel() == ToolRiskLevel.LOW
                && definition.getExecutionMode() == ToolExecutionMode.AUTO_EXECUTE;
    }

    private Map<String, Object> toOpenAiTool(ToolDefinition definition) {
        Map<String, Object> function = new LinkedHashMap<>();
        function.put("name", definition.getToolName());
        function.put("description", definition.getDescription());
        function.put("parameters", parameters(definition.getToolName()));
        return mapOf("type", "function", "function", function);
    }

    private Map<String, Object> parameters(String toolName) {
        Map<String, Object> properties = new LinkedHashMap<>();
        List<String> required = List.of();
        if ("SEARCH_KNOWLEDGE".equals(toolName) || "SEARCH_USER_DOCUMENTS".equals(toolName)) {
            properties.put("query", mapOf("type", "string", "description", "Java 面试知识检索关键词，不包含用户隐私原文"));
            properties.put("topK", mapOf("type", "integer", "description", "返回条数，1 到 8", "minimum", 1, "maximum", 8));
            required = List.of("query");
        } else if ("GET_USER_MEMORY_SUMMARY".equals(toolName)) {
            properties.put("query", mapOf("type", "string", "description", "可选语义检索问题，例如 Redis 或 项目风险"));
            properties.put("topK", mapOf("type", "integer", "description", "语义记忆返回条数，1 到 5", "minimum", 1, "maximum", 5));
        }
        return mapOf(
                "type", "object",
                "properties", properties,
                "required", required,
                "additionalProperties", false
        );
    }

    private Map<String, Object> mapOf(Object... keysAndValues) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < keysAndValues.length; i += 2) {
            map.put(String.valueOf(keysAndValues[i]), keysAndValues[i + 1]);
        }
        return map;
    }
}
