package com.codecoach.module.agent.runtime.service;

import com.codecoach.module.agent.runtime.dto.AgentStepRecord;
import com.codecoach.module.agent.runtime.entity.AgentRun;
import com.codecoach.module.agent.runtime.entity.AgentStep;
import com.codecoach.module.agent.runtime.enums.AgentRunStatus;

public interface AgentTraceService {

    AgentRun createRun(String runId, String traceId, Long userId, String agentType, String inputSummary);

    void finishRun(String runId, AgentRunStatus status, String outputSummary, String errorCode, String errorMessage, long latencyMs);

    AgentStep recordStep(String runId, AgentStepRecord record);
}
