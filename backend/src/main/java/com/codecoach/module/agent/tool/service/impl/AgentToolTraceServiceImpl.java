package com.codecoach.module.agent.tool.service.impl;

import com.codecoach.module.agent.tool.dto.ToolDefinition;
import com.codecoach.module.agent.tool.dto.ToolExecuteResult;
import com.codecoach.module.agent.tool.entity.AgentToolTrace;
import com.codecoach.module.agent.tool.mapper.AgentToolTraceMapper;
import com.codecoach.module.agent.tool.service.AgentToolTraceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AgentToolTraceServiceImpl implements AgentToolTraceService {

    private static final Logger log = LoggerFactory.getLogger(AgentToolTraceServiceImpl.class);
    private static final int SUMMARY_LIMIT = 500;

    private final AgentToolTraceMapper agentToolTraceMapper;
    private final ObjectMapper objectMapper;

    public AgentToolTraceServiceImpl(AgentToolTraceMapper agentToolTraceMapper, ObjectMapper objectMapper) {
        this.agentToolTraceMapper = agentToolTraceMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(
            String traceId,
            String runId,
            String stepId,
            String parentTraceId,
            Long userId,
            String agentType,
            ToolDefinition definition,
            Map<String, Object> params,
            ToolExecuteResult result,
            long latencyMs
    ) {
        try {
            AgentToolTrace trace = new AgentToolTrace();
            trace.setTraceId(traceId);
            trace.setRunId(runId);
            trace.setStepId(stepId);
            trace.setParentTraceId(parentTraceId);
            trace.setUserId(userId);
            trace.setAgentType(StringUtils.hasText(agentType) ? agentType : "UNKNOWN");
            trace.setToolName(definition == null ? "UNKNOWN" : definition.getToolName());
            trace.setToolType(definition == null || definition.getToolType() == null ? null : definition.getToolType().name());
            trace.setInputSummary(toSummary(sanitizeParams(params)));
            trace.setOutputSummary(toSummary(sanitizeResult(result)));
            trace.setSuccess(result != null && result.isSuccess() ? 1 : 0);
            trace.setErrorCode(result == null ? "UNKNOWN_ERROR" : result.getErrorCode());
            trace.setLatencyMs(latencyMs);
            trace.setCreatedAt(LocalDateTime.now());
            agentToolTraceMapper.insert(trace);
        } catch (RuntimeException exception) {
            log.warn("Failed to persist agent tool trace, traceId={}", traceId, exception);
        }
    }

    private Map<String, Object> sanitizeParams(Map<String, Object> params) {
        Map<String, Object> safe = new LinkedHashMap<>();
        if (params == null || params.isEmpty()) {
            return safe;
        }
        params.forEach((key, value) -> {
            String normalizedKey = key == null ? "" : key.toLowerCase();
            if (normalizedKey.contains("key")
                    || normalizedKey.contains("token")
                    || normalizedKey.contains("content")
                    || normalizedKey.contains("document")
                    || normalizedKey.contains("resume")
                    || normalizedKey.contains("answer")) {
                safe.put(key, "[REDACTED]");
            } else {
                safe.put(key, compactValue(value));
            }
        });
        return safe;
    }

    private Map<String, Object> sanitizeResult(ToolExecuteResult result) {
        Map<String, Object> safe = new LinkedHashMap<>();
        if (result == null) {
            return safe;
        }
        safe.put("success", result.isSuccess());
        safe.put("message", compactValue(result.getMessage()));
        safe.put("targetPath", result.getTargetPath());
        safe.put("errorCode", result.getErrorCode());
        safe.put("displayType", result.getDisplayType());
        if (result.getData() != null) {
            safe.put("dataKeys", result.getData().keySet());
        }
        return safe;
    }

    private Object compactValue(Object value) {
        if (!(value instanceof String text)) {
            return value;
        }
        String trimmed = text.replaceAll("[\\r\\n]+", " ").trim();
        return trimmed.length() > 80 ? trimmed.substring(0, 80) + "..." : trimmed;
    }

    private String toSummary(Object value) {
        try {
            String json = objectMapper.writeValueAsString(value);
            return json.length() > SUMMARY_LIMIT ? json.substring(0, SUMMARY_LIMIT) + "..." : json;
        } catch (Exception exception) {
            return "{}";
        }
    }
}
