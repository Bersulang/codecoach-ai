package com.codecoach.module.mcp.service;

import com.codecoach.module.agent.function.model.FunctionToolCall;
import com.codecoach.module.agent.function.service.FunctionCallExecutorAdapter;
import com.codecoach.module.agent.function.service.ToolSchemaExporter;
import com.codecoach.module.agent.tool.dto.ToolExecuteResult;
import com.codecoach.module.observability.trace.TraceContextHolder;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class McpToolAdapter {

    private final ToolSchemaExporter toolSchemaExporter;
    private final FunctionCallExecutorAdapter executorAdapter;

    public McpToolAdapter(ToolSchemaExporter toolSchemaExporter, FunctionCallExecutorAdapter executorAdapter) {
        this.toolSchemaExporter = toolSchemaExporter;
        this.executorAdapter = executorAdapter;
    }

    public List<Map<String, Object>> listTools() {
        return toolSchemaExporter.exportOpenAiTools();
    }

    public ToolExecuteResult callTool(String name, Map<String, Object> arguments) {
        TraceContextHolder.getOrCreateTraceId();
        return executorAdapter.execute(new FunctionToolCall("mcp-" + name, name, arguments == null ? Map.of() : arguments), "MCP_POC");
    }
}
