package com.codecoach.module.observability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.codecoach.common.exception.BusinessException;
import com.codecoach.common.result.ResultCode;
import com.codecoach.module.agent.runtime.entity.AgentRun;
import com.codecoach.module.agent.runtime.entity.AgentStep;
import com.codecoach.module.agent.runtime.mapper.AgentRunMapper;
import com.codecoach.module.agent.runtime.mapper.AgentStepMapper;
import com.codecoach.module.agent.tool.entity.AgentToolTrace;
import com.codecoach.module.agent.tool.mapper.AgentToolTraceMapper;
import com.codecoach.module.ai.entity.AiCallLog;
import com.codecoach.module.ai.mapper.AiCallLogMapper;
import com.codecoach.module.observability.service.ObservabilityService;
import com.codecoach.module.observability.vo.ObservabilityAgentRunVO;
import com.codecoach.module.observability.vo.ObservabilityAgentStepVO;
import com.codecoach.module.observability.vo.ObservabilityAiCallVO;
import com.codecoach.module.observability.vo.ObservabilityErrorItemVO;
import com.codecoach.module.observability.vo.ObservabilityLatencyItemVO;
import com.codecoach.module.observability.vo.ObservabilitySummaryVO;
import com.codecoach.module.observability.vo.ObservabilityToolTraceVO;
import com.codecoach.security.UserContext;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ObservabilityServiceImpl implements ObservabilityService {

    private static final int DEFAULT_LIMIT = 100;
    private static final int MAX_LIMIT = 200;
    private static final int SUMMARY_WINDOW_HOURS = 24;
    private static final int SUMMARY_SAMPLE_LIMIT = 500;
    private static final int SUMMARY_TEXT_LIMIT = 240;
    private static final int ERROR_TEXT_LIMIT = 160;

    private final AgentRunMapper agentRunMapper;
    private final AgentStepMapper agentStepMapper;
    private final AgentToolTraceMapper agentToolTraceMapper;
    private final AiCallLogMapper aiCallLogMapper;

    public ObservabilityServiceImpl(
            AgentRunMapper agentRunMapper,
            AgentStepMapper agentStepMapper,
            AgentToolTraceMapper agentToolTraceMapper,
            AiCallLogMapper aiCallLogMapper
    ) {
        this.agentRunMapper = agentRunMapper;
        this.agentStepMapper = agentStepMapper;
        this.agentToolTraceMapper = agentToolTraceMapper;
        this.aiCallLogMapper = aiCallLogMapper;
    }

    @Override
    public ObservabilitySummaryVO summary() {
        Long userId = UserContext.getCurrentUserId();
        LocalDateTime since = LocalDateTime.now().minusHours(SUMMARY_WINDOW_HOURS);
        Long agentRunCount = agentRunMapper.selectCount(new LambdaQueryWrapper<AgentRun>()
                .eq(AgentRun::getUserId, userId)
                .ge(AgentRun::getCreatedAt, since));
        Long toolCallCount = agentToolTraceMapper.selectCount(new LambdaQueryWrapper<AgentToolTrace>()
                .eq(AgentToolTrace::getUserId, userId)
                .ge(AgentToolTrace::getCreatedAt, since));
        Long aiCallCount = aiCallLogMapper.selectCount(new LambdaQueryWrapper<AiCallLog>()
                .eq(AiCallLog::getUserId, userId)
                .ge(AiCallLog::getCreatedAt, since));
        Long runFailures = agentRunMapper.selectCount(new LambdaQueryWrapper<AgentRun>()
                .eq(AgentRun::getUserId, userId)
                .ge(AgentRun::getCreatedAt, since)
                .in(AgentRun::getStatus, List.of("FAILED", "CANCELLED")));
        Long toolFailures = agentToolTraceMapper.selectCount(new LambdaQueryWrapper<AgentToolTrace>()
                .eq(AgentToolTrace::getUserId, userId)
                .ge(AgentToolTrace::getCreatedAt, since)
                .eq(AgentToolTrace::getSuccess, 0));
        Long aiFailures = aiCallLogMapper.selectCount(new LambdaQueryWrapper<AiCallLog>()
                .eq(AiCallLog::getUserId, userId)
                .ge(AiCallLog::getCreatedAt, since)
                .eq(AiCallLog::getSuccess, 0));

        List<AgentRun> runs = agentRunMapper.selectList(new LambdaQueryWrapper<AgentRun>()
                .eq(AgentRun::getUserId, userId)
                .ge(AgentRun::getCreatedAt, since)
                .orderByDesc(AgentRun::getCreatedAt)
                .last("LIMIT " + SUMMARY_SAMPLE_LIMIT));
        List<AgentToolTrace> tools = agentToolTraceMapper.selectList(new LambdaQueryWrapper<AgentToolTrace>()
                .eq(AgentToolTrace::getUserId, userId)
                .ge(AgentToolTrace::getCreatedAt, since)
                .orderByDesc(AgentToolTrace::getCreatedAt)
                .last("LIMIT " + SUMMARY_SAMPLE_LIMIT));
        List<AiCallLog> aiCalls = aiCallLogMapper.selectList(new LambdaQueryWrapper<AiCallLog>()
                .eq(AiCallLog::getUserId, userId)
                .ge(AiCallLog::getCreatedAt, since)
                .orderByDesc(AiCallLog::getCreatedAt)
                .last("LIMIT " + SUMMARY_SAMPLE_LIMIT));

        List<ObservabilityErrorItemVO> errors = new ArrayList<>();
        runs.stream()
                .filter(this::isFailedRun)
                .limit(6)
                .map(run -> new ObservabilityErrorItemVO(
                        "AgentRun",
                        run.getRunId(),
                        safeText(run.getAgentType(), ERROR_TEXT_LIMIT),
                        safeText(run.getErrorCode(), ERROR_TEXT_LIMIT),
                        run.getCreatedAt()))
                .forEach(errors::add);
        tools.stream()
                .filter(trace -> !isSuccess(trace.getSuccess()))
                .limit(6)
                .map(trace -> new ObservabilityErrorItemVO(
                        "ToolTrace",
                        trace.getTraceId(),
                        safeText(trace.getToolName(), ERROR_TEXT_LIMIT),
                        safeText(trace.getErrorCode(), ERROR_TEXT_LIMIT),
                        trace.getCreatedAt()))
                .forEach(errors::add);
        aiCalls.stream()
                .filter(call -> !isSuccess(call.getSuccess()))
                .limit(6)
                .map(call -> new ObservabilityErrorItemVO(
                        "AICall",
                        null,
                        safeText(call.getRequestType(), ERROR_TEXT_LIMIT),
                        safeText(call.getErrorCode(), ERROR_TEXT_LIMIT),
                        call.getCreatedAt()))
                .forEach(errors::add);
        errors.sort(Comparator.comparing(ObservabilityErrorItemVO::createdAt, Comparator.nullsLast(Comparator.reverseOrder())));

        List<ObservabilityLatencyItemVO> slowestItems = new ArrayList<>();
        runs.stream()
                .filter(run -> run.getLatencyMs() != null)
                .map(run -> new ObservabilityLatencyItemVO("AgentRun", run.getRunId(), safeText(run.getAgentType(), ERROR_TEXT_LIMIT), run.getLatencyMs()))
                .forEach(slowestItems::add);
        tools.stream()
                .filter(trace -> trace.getLatencyMs() != null)
                .map(trace -> new ObservabilityLatencyItemVO("ToolTrace", trace.getTraceId(), safeText(trace.getToolName(), ERROR_TEXT_LIMIT), trace.getLatencyMs()))
                .forEach(slowestItems::add);
        aiCalls.stream()
                .filter(call -> call.getLatencyMs() != null)
                .map(call -> new ObservabilityLatencyItemVO("AICall", null, safeText(call.getRequestType(), ERROR_TEXT_LIMIT), call.getLatencyMs()))
                .forEach(slowestItems::add);
        slowestItems.sort(Comparator.comparing(ObservabilityLatencyItemVO::latencyMs, Comparator.nullsLast(Comparator.reverseOrder())));

        return new ObservabilitySummaryVO(
                since,
                SUMMARY_WINDOW_HOURS,
                agentRunCount,
                aiCallCount,
                toolCallCount,
                averageLatency(runs.stream().map(AgentRun::getLatencyMs).toList()),
                averageLatency(aiCalls.stream().map(AiCallLog::getLatencyMs).toList()),
                runFailures + toolFailures + aiFailures,
                errors.stream().limit(8).toList(),
                slowestItems.stream().limit(8).toList()
        );
    }

    @Override
    public List<ObservabilityAgentRunVO> listAgentRuns(String agentType, String status, Integer limit) {
        Long userId = UserContext.getCurrentUserId();
        LambdaQueryWrapper<AgentRun> wrapper = new LambdaQueryWrapper<AgentRun>()
                .eq(AgentRun::getUserId, userId)
                .orderByDesc(AgentRun::getCreatedAt)
                .orderByDesc(AgentRun::getId)
                .last("LIMIT " + normalizeLimit(limit));
        if (StringUtils.hasText(agentType)) {
            wrapper.eq(AgentRun::getAgentType, agentType.trim());
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(AgentRun::getStatus, status.trim());
        }
        return agentRunMapper.selectList(wrapper).stream().map(this::toRunVO).toList();
    }

    @Override
    public ObservabilityAgentRunVO getAgentRun(String runId) {
        return toRunVO(getRunForCurrentUser(runId));
    }

    @Override
    public List<ObservabilityAgentStepVO> listAgentSteps(String runId) {
        getRunForCurrentUser(runId);
        Long userId = UserContext.getCurrentUserId();
        return agentStepMapper.selectList(new LambdaQueryWrapper<AgentStep>()
                        .eq(AgentStep::getRunId, runId)
                        .eq(AgentStep::getUserId, userId)
                        .orderByAsc(AgentStep::getCreatedAt)
                        .orderByAsc(AgentStep::getId))
                .stream()
                .map(this::toStepVO)
                .toList();
    }

    @Override
    public List<ObservabilityToolTraceVO> listToolTraces(String runId, String agentType, String toolName, Boolean success, Integer limit) {
        Long userId = UserContext.getCurrentUserId();
        LambdaQueryWrapper<AgentToolTrace> wrapper = new LambdaQueryWrapper<AgentToolTrace>()
                .eq(AgentToolTrace::getUserId, userId)
                .orderByDesc(AgentToolTrace::getCreatedAt)
                .orderByDesc(AgentToolTrace::getId)
                .last("LIMIT " + normalizeLimit(limit));
        if (StringUtils.hasText(runId)) {
            wrapper.eq(AgentToolTrace::getRunId, runId.trim());
        }
        if (StringUtils.hasText(agentType)) {
            wrapper.eq(AgentToolTrace::getAgentType, agentType.trim());
        }
        if (StringUtils.hasText(toolName)) {
            wrapper.eq(AgentToolTrace::getToolName, toolName.trim());
        }
        if (success != null) {
            wrapper.eq(AgentToolTrace::getSuccess, success ? 1 : 0);
        }
        return agentToolTraceMapper.selectList(wrapper).stream().map(this::toToolTraceVO).toList();
    }

    @Override
    public List<ObservabilityAiCallVO> listAiCalls(String requestType, Boolean success, Integer limit) {
        Long userId = UserContext.getCurrentUserId();
        LambdaQueryWrapper<AiCallLog> wrapper = new LambdaQueryWrapper<AiCallLog>()
                .eq(AiCallLog::getUserId, userId)
                .orderByDesc(AiCallLog::getCreatedAt)
                .orderByDesc(AiCallLog::getId)
                .last("LIMIT " + normalizeLimit(limit));
        if (StringUtils.hasText(requestType)) {
            wrapper.eq(AiCallLog::getRequestType, requestType.trim());
        }
        if (success != null) {
            wrapper.eq(AiCallLog::getSuccess, success ? 1 : 0);
        }
        return aiCallLogMapper.selectList(wrapper).stream().map(this::toAiCallVO).toList();
    }

    private AgentRun getRunForCurrentUser(String runId) {
        Long userId = UserContext.getCurrentUserId();
        AgentRun run = agentRunMapper.selectOne(new LambdaQueryWrapper<AgentRun>()
                .eq(AgentRun::getRunId, runId)
                .eq(AgentRun::getUserId, userId)
                .last("LIMIT 1"));
        if (run == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        return run;
    }

    private ObservabilityAgentRunVO toRunVO(AgentRun run) {
        return new ObservabilityAgentRunVO(
                run.getRunId(),
                run.getTraceId(),
                safeText(run.getAgentType(), ERROR_TEXT_LIMIT),
                safeText(run.getStatus(), ERROR_TEXT_LIMIT),
                run.getLatencyMs(),
                safeText(run.getInputSummary(), SUMMARY_TEXT_LIMIT),
                safeText(run.getOutputSummary(), SUMMARY_TEXT_LIMIT),
                safeText(run.getErrorCode(), ERROR_TEXT_LIMIT),
                safeText(run.getErrorMessage(), ERROR_TEXT_LIMIT),
                run.getCreatedAt()
        );
    }

    private ObservabilityAgentStepVO toStepVO(AgentStep step) {
        return new ObservabilityAgentStepVO(
                step.getStepId(),
                step.getRunId(),
                safeText(step.getStepType(), ERROR_TEXT_LIMIT),
                safeText(step.getStepName(), ERROR_TEXT_LIMIT),
                safeText(step.getToolName(), ERROR_TEXT_LIMIT),
                safeText(step.getStatus(), ERROR_TEXT_LIMIT),
                step.getLatencyMs(),
                safeText(step.getInputSummary(), SUMMARY_TEXT_LIMIT),
                safeText(step.getOutputSummary(), SUMMARY_TEXT_LIMIT),
                safeText(step.getErrorCode(), ERROR_TEXT_LIMIT),
                step.getCreatedAt()
        );
    }

    private ObservabilityToolTraceVO toToolTraceVO(AgentToolTrace trace) {
        return new ObservabilityToolTraceVO(
                trace.getTraceId(),
                trace.getRunId(),
                safeText(trace.getAgentType(), ERROR_TEXT_LIMIT),
                safeText(trace.getToolName(), ERROR_TEXT_LIMIT),
                safeText(trace.getToolType(), ERROR_TEXT_LIMIT),
                isSuccess(trace.getSuccess()),
                trace.getLatencyMs(),
                safeText(trace.getErrorCode(), ERROR_TEXT_LIMIT),
                trace.getCreatedAt(),
                safeText(trace.getInputSummary(), SUMMARY_TEXT_LIMIT),
                safeText(trace.getOutputSummary(), SUMMARY_TEXT_LIMIT)
        );
    }

    private ObservabilityAiCallVO toAiCallVO(AiCallLog call) {
        return new ObservabilityAiCallVO(
                safeText(call.getProvider(), ERROR_TEXT_LIMIT),
                safeText(call.getModelName(), ERROR_TEXT_LIMIT),
                safeText(call.getRequestType(), ERROR_TEXT_LIMIT),
                safeText(call.getPromptVersion(), ERROR_TEXT_LIMIT),
                call.getLatencyMs(),
                isSuccess(call.getSuccess()),
                safeText(call.getErrorCode(), ERROR_TEXT_LIMIT),
                call.getCreatedAt()
        );
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    private boolean isSuccess(Integer value) {
        return Objects.equals(value, 1);
    }

    private boolean isFailedRun(AgentRun run) {
        return "FAILED".equalsIgnoreCase(run.getStatus()) || "CANCELLED".equalsIgnoreCase(run.getStatus());
    }

    private Long averageLatency(List<Long> values) {
        return values.stream()
                .filter(Objects::nonNull)
                .mapToLong(Long::longValue)
                .average()
                .stream()
                .mapToLong(Math::round)
                .boxed()
                .findFirst()
                .orElse(null);
    }

    private String safeText(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value
                .replaceAll("(?i)authorization\\s*[:=]\\s*\\S+", "authorization:[REDACTED]")
                .replaceAll("(?i)(api[_-]?key|secret|token)\\s*[:=]\\s*\\S+", "$1:[REDACTED]")
                .replaceAll("\\s+", " ")
                .trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength) + "...";
    }
}
