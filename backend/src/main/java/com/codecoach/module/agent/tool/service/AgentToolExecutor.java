package com.codecoach.module.agent.tool.service;

import com.codecoach.common.exception.BusinessException;
import com.codecoach.common.result.ResultCode;
import com.codecoach.module.agent.tool.dto.ToolDefinition;
import com.codecoach.module.agent.tool.dto.ToolExecuteRequest;
import com.codecoach.module.agent.tool.dto.ToolExecuteResult;
import com.codecoach.module.agent.tool.enums.ToolDisplayType;
import com.codecoach.module.agent.tool.enums.ToolRiskLevel;
import com.codecoach.security.UserContext;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AgentToolExecutor {

    private static final String ERROR_CONFIRM_REQUIRED = "CONFIRM_REQUIRED";
    private static final String ERROR_HIGH_RISK_FORBIDDEN = "HIGH_RISK_FORBIDDEN";

    private final AgentToolRegistry agentToolRegistry;
    private final AgentToolTraceService agentToolTraceService;

    public AgentToolExecutor(AgentToolRegistry agentToolRegistry, AgentToolTraceService agentToolTraceService) {
        this.agentToolRegistry = agentToolRegistry;
        this.agentToolTraceService = agentToolTraceService;
    }

    public ToolExecuteResult execute(ToolExecuteRequest request) {
        long startTime = System.currentTimeMillis();
        String traceId = UUID.randomUUID().toString();
        Long userId = UserContext.getCurrentUserId();
        String agentType = request == null ? null : request.getAgentType();
        Map<String, Object> params = request == null ? Map.of() : request.getParams();
        AgentTool tool = agentToolRegistry.requireTool(request == null ? null : request.getToolName());
        ToolDefinition definition = tool.definition();
        ToolExecuteResult result = null;
        try {
            if (definition.getRiskLevel() == ToolRiskLevel.HIGH) {
                throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "高风险 Tool 第一版不允许执行");
            }
            if (definition.isRequiresConfirmation() && (request == null || !Boolean.TRUE.equals(request.getConfirmed()))) {
                result = ToolExecuteResult.failure("这个动作需要你确认后再执行", ERROR_CONFIRM_REQUIRED, ToolDisplayType.ERROR);
                result.setTraceId(traceId);
                return result;
            }
            tool.validate(userId, params);
            result = tool.execute(userId, params);
            if (result == null) {
                result = ToolExecuteResult.failure("Tool 没有返回结果", "EMPTY_RESULT", ToolDisplayType.ERROR);
            }
            result.setTraceId(traceId);
            return result;
        } catch (BusinessException exception) {
            result = ToolExecuteResult.failure(exception.getMessage(), String.valueOf(exception.getCode()), ToolDisplayType.ERROR);
            result.setTraceId(traceId);
            return result;
        } catch (RuntimeException exception) {
            result = ToolExecuteResult.failure("Tool 执行失败，请稍后重试或进入对应页面操作", "TOOL_EXECUTION_FAILED", ToolDisplayType.ERROR);
            result.setTraceId(traceId);
            return result;
        } finally {
            long latencyMs = System.currentTimeMillis() - startTime;
            agentToolTraceService.record(traceId, userId, agentType, definition, params, result, latencyMs);
        }
    }
}
