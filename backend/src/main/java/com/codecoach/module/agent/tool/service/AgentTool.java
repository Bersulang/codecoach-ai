package com.codecoach.module.agent.tool.service;

import com.codecoach.module.agent.tool.dto.ToolDefinition;
import com.codecoach.module.agent.tool.dto.ToolExecuteResult;
import java.util.Map;

public interface AgentTool {

    ToolDefinition definition();

    default void validate(Long userId, Map<String, Object> params) {
    }

    ToolExecuteResult execute(Long userId, Map<String, Object> params);
}
