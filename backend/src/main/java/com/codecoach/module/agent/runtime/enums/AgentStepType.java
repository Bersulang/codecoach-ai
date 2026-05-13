package com.codecoach.module.agent.runtime.enums;

public enum AgentStepType {
    CONTEXT_BUILD,
    PLAN_CREATE,
    RAG_RETRIEVE,
    QUESTION_GENERATE,
    ANSWER_OBSERVE,
    STAGE_ADJUST,
    REPORT_GENERATE,
    INTENT_DETECT,
    LLM_CALL,
    TOOL_SELECT,
    TOOL_EXECUTE,
    OBSERVATION,
    RESPONSE_COMPOSE,
    FALLBACK
}
