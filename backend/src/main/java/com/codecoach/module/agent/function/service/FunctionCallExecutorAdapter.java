package com.codecoach.module.agent.function.service;

import com.codecoach.module.agent.function.model.FunctionToolCall;
import com.codecoach.module.agent.tool.dto.ToolDefinition;
import com.codecoach.module.agent.tool.dto.ToolExecuteRequest;
import com.codecoach.module.agent.tool.dto.ToolExecuteResult;
import com.codecoach.module.agent.tool.enums.ToolDisplayType;
import com.codecoach.module.agent.tool.enums.ToolExecutionMode;
import com.codecoach.module.agent.tool.enums.ToolRiskLevel;
import com.codecoach.module.agent.tool.service.AgentTool;
import com.codecoach.module.agent.tool.service.AgentToolExecutor;
import com.codecoach.module.agent.tool.service.AgentToolRegistry;
import com.codecoach.module.observability.trace.TraceContextHolder;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class FunctionCallExecutorAdapter {

    private final AgentToolRegistry agentToolRegistry;
    private final AgentToolExecutor agentToolExecutor;
    private final ToolSchemaExporter toolSchemaExporter;

    public FunctionCallExecutorAdapter(
            AgentToolRegistry agentToolRegistry,
            AgentToolExecutor agentToolExecutor,
            ToolSchemaExporter toolSchemaExporter
    ) {
        this.agentToolRegistry = agentToolRegistry;
        this.agentToolExecutor = agentToolExecutor;
        this.toolSchemaExporter = toolSchemaExporter;
    }

    public ToolExecuteResult execute(FunctionToolCall call) {
        return execute(call, "FUNCTION_CALLING_POC");
    }

    public ToolExecuteResult execute(FunctionToolCall call, String agentType) {
        if (call == null || !toolSchemaExporter.isExportable(call.name())) {
            return ToolExecuteResult.failure("Function Calling 工具未在白名单中。", "FUNCTION_TOOL_NOT_ALLOWED", ToolDisplayType.ERROR);
        }
        AgentTool tool = agentToolRegistry.getTool(call.name());
        if (tool == null) {
            return ToolExecuteResult.failure("未知 Tool，已拒绝执行。", "UNKNOWN_TOOL", ToolDisplayType.ERROR);
        }
        ToolDefinition definition = tool.definition();
        if (definition.getRiskLevel() == ToolRiskLevel.HIGH) {
            return ToolExecuteResult.failure("高风险 Tool 禁止通过 Function Calling 执行。", "HIGH_RISK_FORBIDDEN", ToolDisplayType.ERROR);
        }
        if (definition.getRiskLevel() == ToolRiskLevel.MEDIUM
                || definition.getExecutionMode() != ToolExecutionMode.AUTO_EXECUTE
                || definition.isRequiresConfirmation()) {
            return ToolExecuteResult.failure("该 Tool 需要用户确认，不能由模型自动执行。", "CONFIRM_REQUIRED", ToolDisplayType.ERROR);
        }
        ToolExecuteRequest request = new ToolExecuteRequest();
        request.setToolName(call.name());
        request.setAgentType(agentType);
        request.setTraceId(TraceContextHolder.getOrCreateTraceId());
        request.setConfirmed(Boolean.FALSE);
        request.setParams(call.arguments() == null ? Map.of() : call.arguments());
        return agentToolExecutor.execute(request);
    }
}
