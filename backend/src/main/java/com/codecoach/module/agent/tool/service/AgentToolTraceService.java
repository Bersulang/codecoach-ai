package com.codecoach.module.agent.tool.service;

import com.codecoach.module.agent.tool.dto.ToolDefinition;
import com.codecoach.module.agent.tool.dto.ToolExecuteResult;
import java.util.Map;

public interface AgentToolTraceService {

    void record(
            String traceId,
            Long userId,
            String agentType,
            ToolDefinition definition,
            Map<String, Object> params,
            ToolExecuteResult result,
            long latencyMs
    );
}
