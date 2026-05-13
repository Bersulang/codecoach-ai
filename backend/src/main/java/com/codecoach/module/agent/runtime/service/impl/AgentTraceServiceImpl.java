package com.codecoach.module.agent.runtime.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.codecoach.module.agent.runtime.dto.AgentStepRecord;
import com.codecoach.module.agent.runtime.entity.AgentRun;
import com.codecoach.module.agent.runtime.entity.AgentStep;
import com.codecoach.module.agent.runtime.enums.AgentRunStatus;
import com.codecoach.module.agent.runtime.enums.AgentStepStatus;
import com.codecoach.module.agent.runtime.enums.AgentStepType;
import com.codecoach.module.agent.runtime.mapper.AgentRunMapper;
import com.codecoach.module.agent.runtime.mapper.AgentStepMapper;
import com.codecoach.module.agent.runtime.service.AgentTraceService;
import com.codecoach.module.agent.runtime.support.AgentExecutionContext;
import com.codecoach.module.agent.runtime.support.AgentRuntimeContextHolder;
import com.codecoach.module.agent.runtime.support.AgentTraceSanitizer;
import java.time.LocalDateTime;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AgentTraceServiceImpl implements AgentTraceService {

    private static final Logger log = LoggerFactory.getLogger(AgentTraceServiceImpl.class);

    private final AgentRunMapper agentRunMapper;
    private final AgentStepMapper agentStepMapper;
    private final AgentTraceSanitizer sanitizer;

    public AgentTraceServiceImpl(
            AgentRunMapper agentRunMapper,
            AgentStepMapper agentStepMapper,
            AgentTraceSanitizer sanitizer
    ) {
        this.agentRunMapper = agentRunMapper;
        this.agentStepMapper = agentStepMapper;
        this.sanitizer = sanitizer;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AgentRun createRun(String runId, String traceId, Long userId, String agentType, String inputSummary) {
        AgentRun run = new AgentRun();
        run.setRunId(runId);
        run.setTraceId(traceId);
        run.setUserId(userId);
        run.setAgentType(agentType);
        run.setStatus(AgentRunStatus.RUNNING.name());
        run.setInputSummary(sanitizer.summarizeText(inputSummary));
        run.setCreatedAt(LocalDateTime.now());
        run.setUpdatedAt(run.getCreatedAt());
        agentRunMapper.insert(run);
        return run;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void finishRun(
            String runId,
            AgentRunStatus status,
            String outputSummary,
            String errorCode,
            String errorMessage,
            long latencyMs
    ) {
        agentRunMapper.update(null, new LambdaUpdateWrapper<AgentRun>()
                .eq(AgentRun::getRunId, runId)
                .set(AgentRun::getStatus, status.name())
                .set(AgentRun::getOutputSummary, sanitizer.summarizeText(outputSummary))
                .set(AgentRun::getErrorCode, sanitizer.summarizeText(errorCode))
                .set(AgentRun::getErrorMessage, sanitizer.errorMessage(errorMessage))
                .set(AgentRun::getLatencyMs, latencyMs)
                .set(AgentRun::getUpdatedAt, LocalDateTime.now()));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AgentStep recordStep(String runId, AgentStepRecord record) {
        try {
            AgentExecutionContext context = AgentRuntimeContextHolder.get();
            AgentStep step = new AgentStep();
            step.setStepId(UUID.randomUUID().toString());
            step.setRunId(runId);
            step.setTraceId(context == null ? null : context.getTraceId());
            step.setUserId(context == null ? null : context.getUserId());
            step.setAgentType(context == null ? "UNKNOWN" : context.getAgentType());
            step.setStepType(stepType(record).name());
            step.setStepName(StringUtils.hasText(record.getStepName()) ? record.getStepName() : stepType(record).name());
            step.setToolName(record.getToolName());
            step.setInputSummary(sanitizer.summarizeText(record.getInputSummary()));
            step.setOutputSummary(sanitizer.summarizeText(record.getOutputSummary()));
            step.setStatus(stepStatus(record).name());
            step.setErrorCode(sanitizer.summarizeText(record.getErrorCode()));
            step.setErrorMessage(sanitizer.errorMessage(record.getErrorMessage()));
            step.setLatencyMs(record.getLatencyMs());
            step.setCreatedAt(LocalDateTime.now());
            agentStepMapper.insert(step);
            return step;
        } catch (RuntimeException exception) {
            log.warn("Failed to persist agent step, runId={}", runId, exception);
            return null;
        }
    }

    private AgentStepType stepType(AgentStepRecord record) {
        return record == null || record.getStepType() == null ? AgentStepType.OBSERVATION : record.getStepType();
    }

    private AgentStepStatus stepStatus(AgentStepRecord record) {
        return record == null || record.getStatus() == null ? AgentStepStatus.SUCCEEDED : record.getStatus();
    }
}
