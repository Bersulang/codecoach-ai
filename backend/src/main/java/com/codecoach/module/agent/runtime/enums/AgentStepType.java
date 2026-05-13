package com.codecoach.module.agent.runtime.enums;

public enum AgentStepType {
    CONTEXT_BUILD,
    INTENT_DETECT,
    LLM_CALL,
    TOOL_SELECT,
    TOOL_EXECUTE,
    OBSERVATION,
    RESPONSE_COMPOSE,
    FALLBACK
}
