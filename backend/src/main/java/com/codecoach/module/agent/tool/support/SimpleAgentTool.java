package com.codecoach.module.agent.tool.support;

import com.codecoach.module.agent.tool.dto.ToolDefinition;
import com.codecoach.module.agent.tool.dto.ToolExecuteResult;
import com.codecoach.module.agent.tool.service.AgentTool;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

public class SimpleAgentTool implements AgentTool {

    private final ToolDefinition definition;
    private final BiConsumer<Long, Map<String, Object>> validator;
    private final BiFunction<Long, Map<String, Object>, ToolExecuteResult> executor;

    public SimpleAgentTool(
            ToolDefinition definition,
            BiConsumer<Long, Map<String, Object>> validator,
            BiFunction<Long, Map<String, Object>, ToolExecuteResult> executor
    ) {
        this.definition = definition;
        this.validator = validator;
        this.executor = executor;
    }

    @Override
    public ToolDefinition definition() {
        return definition;
    }

    @Override
    public void validate(Long userId, Map<String, Object> params) {
        if (validator != null) {
            validator.accept(userId, params);
        }
    }

    @Override
    public ToolExecuteResult execute(Long userId, Map<String, Object> params) {
        return executor.apply(userId, params);
    }
}
