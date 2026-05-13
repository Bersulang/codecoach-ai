package com.codecoach.module.agent.multi;

import com.codecoach.module.agent.runtime.dto.AgentStepRecord;
import com.codecoach.module.agent.runtime.enums.AgentRunStatus;
import com.codecoach.module.agent.runtime.enums.AgentStepStatus;
import com.codecoach.module.agent.runtime.enums.AgentStepType;
import com.codecoach.module.agent.runtime.service.AgentTraceService;
import com.codecoach.module.agent.runtime.support.AgentExecutionContext;
import com.codecoach.module.agent.runtime.support.AgentRuntimeContextHolder;
import com.codecoach.module.agent.runtime.support.AgentTraceSanitizer;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class MultiAgentCoordinator {

    public static final String AGENT_TYPE_REVIEW_MULTI = "AGENT_REVIEW_MULTI";

    private final AgentTraceService agentTraceService;
    private final AgentTraceSanitizer sanitizer;

    public MultiAgentCoordinator(AgentTraceService agentTraceService, AgentTraceSanitizer sanitizer) {
        this.agentTraceService = agentTraceService;
        this.sanitizer = sanitizer;
    }

    public Session start(Long userId, String inputSummary) {
        String runId = UUID.randomUUID().toString();
        String traceId = UUID.randomUUID().toString();
        AgentExecutionContext previous = AgentRuntimeContextHolder.get();
        AgentExecutionContext context = new AgentExecutionContext(runId, traceId, userId, AGENT_TYPE_REVIEW_MULTI);
        AgentRuntimeContextHolder.set(context);
        agentTraceService.createRun(runId, traceId, userId, AGENT_TYPE_REVIEW_MULTI, inputSummary);
        return new Session(runId, traceId, previous, System.currentTimeMillis());
    }

    public void recordRole(Session session, MultiAgentRole role, String inputSummary, String outputSummary, AgentStepStatus status, long latencyMs) {
        if (session == null || role == null) {
            return;
        }
        AgentStepRecord record = new AgentStepRecord();
        record.setStepType(role.stepType());
        record.setStepName(role.displayName());
        record.setInputSummary(inputSummary);
        record.setOutputSummary(outputSummary);
        record.setStatus(status == null ? AgentStepStatus.SUCCEEDED : status);
        record.setLatencyMs(latencyMs);
        agentTraceService.recordStep(session.runId(), record);
    }

    public void finish(Session session, AgentRunStatus status, String outputSummary, String errorCode, String errorMessage) {
        if (session == null) {
            return;
        }
        agentTraceService.finishRun(
                session.runId(),
                status == null ? AgentRunStatus.SUCCEEDED : status,
                outputSummary,
                errorCode,
                sanitizer.errorMessage(errorMessage),
                System.currentTimeMillis() - session.startedAt()
        );
        if (session.previousContext() == null) {
            AgentRuntimeContextHolder.clear();
        } else {
            AgentRuntimeContextHolder.set(session.previousContext());
        }
    }

    public record Session(String runId, String traceId, AgentExecutionContext previousContext, long startedAt) {
    }

    public enum MultiAgentRole {
        RETRIEVER("Retriever Agent", AgentStepType.RAG_RETRIEVE),
        EVALUATOR("Evaluator Agent", AgentStepType.OBSERVATION),
        COACH("Coach Agent", AgentStepType.RESPONSE_COMPOSE);

        private final String displayName;
        private final AgentStepType stepType;

        MultiAgentRole(String displayName, AgentStepType stepType) {
            this.displayName = displayName;
            this.stepType = stepType;
        }

        public String displayName() {
            return displayName;
        }

        public AgentStepType stepType() {
            return stepType;
        }
    }
}
