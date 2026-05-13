package com.codecoach.module.agent.tool.service;

import com.codecoach.common.exception.BusinessException;
import com.codecoach.common.result.ResultCode;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.codecoach.module.agent.runtime.entity.AgentRun;
import com.codecoach.module.agent.runtime.dto.AgentStepRecord;
import com.codecoach.module.agent.runtime.enums.AgentStepStatus;
import com.codecoach.module.agent.runtime.enums.AgentStepType;
import com.codecoach.module.agent.runtime.mapper.AgentRunMapper;
import com.codecoach.module.agent.runtime.service.AgentTraceService;
import com.codecoach.module.agent.tool.dto.ToolDefinition;
import com.codecoach.module.agent.tool.dto.ToolExecuteRequest;
import com.codecoach.module.agent.tool.dto.ToolExecuteResult;
import com.codecoach.module.agent.tool.enums.ToolDisplayType;
import com.codecoach.module.agent.tool.enums.ToolRiskLevel;
import com.codecoach.module.agent.runtime.support.AgentExecutionContext;
import com.codecoach.module.agent.runtime.support.AgentRuntimeContextHolder;
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
    private final AgentTraceService agentTraceService;
    private final AgentRunMapper agentRunMapper;

    public AgentToolExecutor(
            AgentToolRegistry agentToolRegistry,
            AgentToolTraceService agentToolTraceService,
            AgentTraceService agentTraceService,
            AgentRunMapper agentRunMapper
    ) {
        this.agentToolRegistry = agentToolRegistry;
        this.agentToolTraceService = agentToolTraceService;
        this.agentTraceService = agentTraceService;
        this.agentRunMapper = agentRunMapper;
    }

    public ToolExecuteResult execute(ToolExecuteRequest request) {
        long startTime = System.currentTimeMillis();
        String traceId = UUID.randomUUID().toString();
        Long userId = UserContext.getCurrentUserId();
        AgentExecutionContext context = AgentRuntimeContextHolder.get();
        String agentType = request == null ? null : request.getAgentType();
        if (agentType == null && context != null) {
            agentType = context.getAgentType();
        }
        String runId = request == null ? null : request.getRunId();
        if (runId == null && context != null) {
            runId = context.getRunId();
        }
        String stepId = request == null ? null : request.getStepId();
        String parentTraceId = request == null ? null : request.getTraceId();
        if (parentTraceId == null && context != null) {
            parentTraceId = context.getTraceId();
        }
        if (!canAttachRun(runId, userId)) {
            runId = null;
            stepId = null;
            parentTraceId = null;
        }
        boolean temporaryContext = false;
        if (context == null && runId != null) {
            AgentRuntimeContextHolder.set(new AgentExecutionContext(runId, parentTraceId, userId, agentType));
            temporaryContext = true;
        }
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
            recordToolStep(runId, definition, result, latencyMs);
            agentToolTraceService.record(traceId, runId, stepId, parentTraceId, userId, agentType, definition, params, result, latencyMs);
            if (temporaryContext) {
                AgentRuntimeContextHolder.clear();
            }
        }
    }

    private void recordToolStep(String runId, ToolDefinition definition, ToolExecuteResult result, long latencyMs) {
        if (runId == null) {
            return;
        }
        AgentStepRecord step = new AgentStepRecord();
        step.setStepType(AgentStepType.TOOL_EXECUTE);
        step.setStepName("Execute agent tool");
        step.setToolName(definition == null ? "UNKNOWN" : definition.getToolName());
        step.setInputSummary(definition == null ? null : "toolName=" + definition.getToolName());
        step.setOutputSummary(result == null ? null : result.getMessage());
        step.setStatus(result != null && result.isSuccess() ? AgentStepStatus.SUCCEEDED : AgentStepStatus.FAILED);
        step.setErrorCode(result == null ? "UNKNOWN_ERROR" : result.getErrorCode());
        step.setLatencyMs(latencyMs);
        agentTraceService.recordStep(runId, step);
    }

    private boolean canAttachRun(String runId, Long userId) {
        if (runId == null) {
            return true;
        }
        try {
            Long count = agentRunMapper.selectCount(new LambdaQueryWrapper<AgentRun>()
                    .eq(AgentRun::getRunId, runId)
                    .eq(AgentRun::getUserId, userId));
            return count != null && count > 0;
        } catch (RuntimeException exception) {
            return false;
        }
    }
}
