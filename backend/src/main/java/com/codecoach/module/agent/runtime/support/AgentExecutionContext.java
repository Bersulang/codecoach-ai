package com.codecoach.module.agent.runtime.support;

public class AgentExecutionContext {

    private final String runId;
    private final String traceId;
    private final Long userId;
    private final String agentType;

    public AgentExecutionContext(String runId, String traceId, Long userId, String agentType) {
        this.runId = runId;
        this.traceId = traceId;
        this.userId = userId;
        this.agentType = agentType;
    }

    public String getRunId() {
        return runId;
    }

    public String getTraceId() {
        return traceId;
    }

    public Long getUserId() {
        return userId;
    }

    public String getAgentType() {
        return agentType;
    }
}
