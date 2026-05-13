package com.codecoach.module.agent.runtime.service;

import com.codecoach.module.agent.runtime.dto.AgentContext;
import com.codecoach.module.agent.runtime.dto.AgentRequest;
import com.codecoach.module.agent.runtime.dto.AgentResponse;
import com.codecoach.module.agent.runtime.dto.AgentStepRecord;
import com.codecoach.module.agent.runtime.enums.AgentRunStatus;
import com.codecoach.module.agent.runtime.enums.AgentStepStatus;
import com.codecoach.module.agent.runtime.enums.AgentStepType;
import com.codecoach.module.agent.runtime.support.AgentExecutionContext;
import com.codecoach.module.agent.runtime.support.AgentRuntimeContextHolder;
import com.codecoach.module.agent.runtime.support.AgentTraceSanitizer;
import com.codecoach.module.agent.tool.dto.ToolDefinition;
import com.codecoach.module.agent.tool.dto.ToolExecuteRequest;
import com.codecoach.module.agent.tool.dto.ToolExecuteResult;
import com.codecoach.module.agent.tool.enums.ToolExecutionMode;
import com.codecoach.module.agent.tool.enums.ToolRiskLevel;
import com.codecoach.module.agent.tool.service.AgentToolExecutor;
import com.codecoach.module.agent.tool.service.AgentTool;
import com.codecoach.module.agent.tool.service.AgentToolRegistry;
import com.codecoach.module.guide.dto.GuideChatRequest;
import com.codecoach.module.guide.service.GuideChatService;
import com.codecoach.module.guide.vo.GuideActionCardVO;
import com.codecoach.module.guide.vo.GuideChatResponseVO;
import com.codecoach.security.LoginUser;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AgentRuntime {

    private static final Logger log = LoggerFactory.getLogger(AgentRuntime.class);
    private static final String AGENT_TYPE_GUIDE = "GUIDE";
    private static final int GUIDE_MAX_AUTO_TOOL_STEPS = 3;
    private static final Set<String> GUIDE_AUTO_TOOLS = Set.of(
            "GET_ABILITY_SUMMARY",
            "GET_RECENT_TRAINING_SUMMARY",
            "GET_RESUME_RISK_SUMMARY",
            "SEARCH_KNOWLEDGE",
            "SEARCH_USER_DOCUMENTS"
    );
    private static final Set<String> GUIDE_SUGGEST_ONLY_TOOLS = Set.of(
            "START_PROJECT_TRAINING",
            "START_QUESTION_TRAINING",
            "START_MOCK_INTERVIEW",
            "GENERATE_AGENT_REVIEW",
            "ANALYZE_RESUME",
            "CREATE_PROJECT_FROM_RESUME"
    );
    private static final Set<String> GUIDE_FORBIDDEN_TOOLS = Set.of(
            "DELETE_PROJECT",
            "DELETE_DOCUMENT",
            "DELETE_RESUME",
            "MODIFY_PROFILE",
            "CLEAR_HISTORY",
            "UPDATE_REPORT"
    );

    private final GuideChatService guideChatService;
    private final AgentToolRegistry agentToolRegistry;
    private final AgentToolExecutor agentToolExecutor;
    private final AgentTraceService agentTraceService;
    private final AgentTraceSanitizer sanitizer;

    public AgentRuntime(
            GuideChatService guideChatService,
            AgentToolRegistry agentToolRegistry,
            AgentToolExecutor agentToolExecutor,
            AgentTraceService agentTraceService,
            AgentTraceSanitizer sanitizer
    ) {
        this.guideChatService = guideChatService;
        this.agentToolRegistry = agentToolRegistry;
        this.agentToolExecutor = agentToolExecutor;
        this.agentTraceService = agentTraceService;
        this.sanitizer = sanitizer;
    }

    public AgentResponse run(AgentRequest request) {
        String agentType = StringUtils.hasText(request == null ? null : request.getAgentType())
                ? request.getAgentType()
                : AGENT_TYPE_GUIDE;
        if (!AGENT_TYPE_GUIDE.equals(agentType)) {
            return fallback(null, null, AgentRunStatus.FAILED.name());
        }

        long startTime = System.currentTimeMillis();
        String runId = UUID.randomUUID().toString();
        String traceId = UUID.randomUUID().toString();
        Long userId = currentUserId();
        AgentExecutionContext executionContext = new AgentExecutionContext(runId, traceId, userId, agentType);
        AgentRuntimeContextHolder.set(executionContext);
        try {
            safelyCreateRun(runId, traceId, userId, agentType, request);
            AgentContext context = buildContext(runId, traceId, userId, agentType, request);
            record(AgentStepType.CONTEXT_BUILD, "Build guide context", null,
                    "path=" + context.getCurrentPath() + ", loggedIn=" + context.isLoggedIn(),
                    "tools=" + context.getAvailableTools().size() + ", page=" + context.getPageDescription(),
                    AgentStepStatus.SUCCEEDED, null, null, 0);

            long intentStart = System.currentTimeMillis();
            String intent = detectIntent(context.getUserMessage());
            record(AgentStepType.INTENT_DETECT, "Detect guide intent", null,
                    "messageChars=" + safeLength(context.getUserMessage()),
                    "intent=" + intent,
                    AgentStepStatus.SUCCEEDED, null, null, elapsed(intentStart));

            AgentResponse response = executeGuideReact(context, request, intent);
            response.setRunId(runId);
            response.setTraceId(traceId);
            response.setStatus(AgentRunStatus.SUCCEEDED.name());
            attachTrace(response.getActions(), runId, traceId);

            safelyFinishRun(runId, AgentRunStatus.SUCCEEDED, response.getAnswer(), null, null, elapsed(startTime));
            return response;
        } catch (Exception exception) {
            log.warn("Agent runtime failed, runId={}, agentType={}", runId, agentType, exception);
            record(AgentStepType.FALLBACK, "Guide fallback", null,
                    "agentType=" + agentType,
                    "fallback actions=GO_DASHBOARD,GO_AGENT_REVIEW",
                    AgentStepStatus.FAILED, "AGENT_RUNTIME_FAILED", sanitizer.errorMessage(exception), elapsed(startTime));
            safelyFinishRun(runId, AgentRunStatus.FAILED, null, "AGENT_RUNTIME_FAILED", sanitizer.errorMessage(exception), elapsed(startTime));
            AgentResponse response = fallback(runId, traceId, AgentRunStatus.FAILED.name());
            attachTrace(response.getActions(), runId, traceId);
            return response;
        } finally {
            AgentRuntimeContextHolder.clear();
        }
    }

    private void safelyCreateRun(String runId, String traceId, Long userId, String agentType, AgentRequest request) {
        try {
            agentTraceService.createRun(runId, traceId, userId, agentType, inputSummary(request));
        } catch (RuntimeException exception) {
            log.warn("Failed to create agent run trace, runId={}", runId, exception);
        }
    }

    private void safelyFinishRun(
            String runId,
            AgentRunStatus status,
            String outputSummary,
            String errorCode,
            String errorMessage,
            long latencyMs
    ) {
        try {
            agentTraceService.finishRun(runId, status, outputSummary, errorCode, errorMessage, latencyMs);
        } catch (RuntimeException exception) {
            log.warn("Failed to finish agent run trace, runId={}", runId, exception);
        }
    }

    private AgentContext buildContext(String runId, String traceId, Long userId, String agentType, AgentRequest request) {
        AgentContext context = new AgentContext();
        context.setRunId(runId);
        context.setTraceId(traceId);
        context.setUserId(userId);
        context.setAgentType(agentType);
        context.setCurrentPath(normalizePath(request == null ? null : request.getCurrentPath()));
        context.setUserMessage(normalizeMessage(request == null ? null : request.getMessage()));
        context.setLoggedIn(userId != null);
        context.setAvailableTools(agentToolRegistry.listActions());
        context.setPageDescription(pageDescription(context.getCurrentPath()));
        context.setUserSummary(userId == null ? "guest" : "logged_in_user");
        return context;
    }

    private GuideChatResponseVO executeGuide(AgentRequest request) {
        GuideChatRequest guideRequest = new GuideChatRequest();
        guideRequest.setMessage(request == null ? null : request.getMessage());
        guideRequest.setCurrentPath(request == null ? null : request.getCurrentPath());
        guideRequest.setPageTitle(request == null ? null : request.getPageTitle());
        return guideChatService.chat(guideRequest);
    }

    private AgentResponse executeGuideReact(AgentContext context, AgentRequest request, String intent) {
        if (context == null || !context.isLoggedIn()) {
            GuideChatResponseVO guideResponse = executeGuide(request);
            attachTrace(guideResponse, context == null ? null : context.getRunId(), context == null ? null : context.getTraceId());
            record(AgentStepType.TOOL_SELECT, "Select guide actions", "none",
                    "intent=" + intent + ", loggedIn=false",
                    "guest response uses action cards only",
                    AgentStepStatus.SKIPPED, null, null, 0);
            record(AgentStepType.RESPONSE_COMPOSE, "Compose guide response", null,
                    "source=guestFallback",
                    guideResponse == null ? null : guideResponse.getAnswer(),
                    AgentStepStatus.SUCCEEDED, null, null, 0);
            return toAgentResponse(guideResponse, context == null ? null : context.getRunId(),
                    context == null ? null : context.getTraceId(), AgentRunStatus.SUCCEEDED.name());
        }

        long intentStart = System.currentTimeMillis();
        GuideToolPlan plan = buildGuideToolPlan(context, intent);
        record(AgentStepType.INTENT_DETECT, "Generate guide tool intents", null,
                "intent=" + intent,
                "reasonSummary=" + plan.reasonSummary() + ", toolIntents=" + toolNames(plan.toolIntents())
                        + ", suggestedActions=" + String.join(",", plan.suggestedActions()),
                AgentStepStatus.SUCCEEDED, null, null, elapsed(intentStart));

        long selectStart = System.currentTimeMillis();
        ToolSelection selection = selectGuideTools(plan);
        record(AgentStepType.TOOL_SELECT, "Select guide auto tools", toolNames(selection.autoIntents()),
                "requested=" + toolNames(plan.toolIntents()) + ", maxSteps=" + GUIDE_MAX_AUTO_TOOL_STEPS,
                "auto=" + toolNames(selection.autoIntents())
                        + ", suggested=" + String.join(",", selection.suggestedActions())
                        + ", rejected=" + String.join(",", selection.rejectedSummaries())
                        + ", truncated=" + selection.truncatedCount(),
                AgentStepStatus.SUCCEEDED, null, null, elapsed(selectStart));

        List<ToolObservation> observations = new ArrayList<>();
        for (ToolIntent toolIntent : selection.autoIntents()) {
            observations.add(executeToolIntent(context, toolIntent));
        }

        if (!selection.rejectedSummaries().isEmpty()) {
            record(AgentStepType.OBSERVATION, "Rejected unsafe tool intents", null,
                    "rejectedCount=" + selection.rejectedSummaries().size(),
                    String.join("; ", selection.rejectedSummaries()),
                    AgentStepStatus.SKIPPED, "TOOL_INTENT_REJECTED", null, 0);
        }
        if (selection.truncatedCount() > 0) {
            record(AgentStepType.OBSERVATION, "Guide max step limit reached", null,
                    "maxSteps=" + GUIDE_MAX_AUTO_TOOL_STEPS,
                    "truncatedToolIntents=" + selection.truncatedCount(),
                    AgentStepStatus.SKIPPED, "MAX_STEP_LIMIT_REACHED", null, 0);
        }

        long composeStart = System.currentTimeMillis();
        AgentResponse response = composeGuideResponse(context, intent, plan, selection, observations);
        record(AgentStepType.RESPONSE_COMPOSE, "Compose guide response from observations", null,
                "observationCount=" + observations.size() + ", actionCount=" + response.getActions().size(),
                response.getAnswer(),
                AgentStepStatus.SUCCEEDED, null, null, elapsed(composeStart));
        return response;
    }

    private GuideToolPlan buildGuideToolPlan(AgentContext context, String intent) {
        String message = context == null ? null : context.getUserMessage();
        List<ToolIntent> toolIntents = new ArrayList<>();
        List<String> suggestedActions = new ArrayList<>();
        String reasonSummary;
        switch (intent) {
            case "PAGE_HELP" -> {
                reasonSummary = "用户需要当前页面说明，优先返回导航动作。";
                suggestedActions.addAll(pageActionNames(context == null ? null : context.getCurrentPath()));
            }
            case "PROJECT_TRAINING" -> {
                reasonSummary = "用户想练项目，需要先看近期训练和可补充素材。";
                toolIntents.add(new ToolIntent("GET_RECENT_TRAINING_SUMMARY", "判断最近是否已经进行项目训练", Map.of()));
                toolIntents.add(new ToolIntent("GET_ABILITY_SUMMARY", "识别项目表达相关薄弱维度", Map.of()));
                suggestedActions.add("GO_INSIGHTS");
                suggestedActions.add("START_PROJECT_TRAINING");
            }
            case "QUESTION_TRAINING" -> {
                reasonSummary = "用户想练八股，需要结合薄弱维度并检索知识文章。";
                toolIntents.add(new ToolIntent("GET_ABILITY_SUMMARY", "识别适合专项训练的薄弱知识点", Map.of()));
                toolIntents.add(new ToolIntent("SEARCH_KNOWLEDGE", "查找可复习的站内知识文章", Map.of("query", knowledgeQuery(message), "topK", 3)));
                toolIntents.add(new ToolIntent("GET_RECENT_TRAINING_SUMMARY", "确认最近八股训练频率", Map.of()));
                suggestedActions.add("GO_INSIGHTS");
                suggestedActions.add("START_QUESTION_TRAINING");
            }
            case "MOCK_INTERVIEW" -> {
                reasonSummary = "用户想做模拟面试，需要先看训练和简历风险，执行动作交由用户确认。";
                toolIntents.add(new ToolIntent("GET_RECENT_TRAINING_SUMMARY", "判断是否适合直接进入综合模拟", Map.of()));
                toolIntents.add(new ToolIntent("GET_RESUME_RISK_SUMMARY", "读取简历风险作为模拟面试准备依据", Map.of()));
                suggestedActions.add("START_MOCK_INTERVIEW");
                suggestedActions.add("GO_AGENT_REVIEW");
            }
            case "RESUME" -> {
                reasonSummary = "用户关注简历，需要读取风险摘要但不自动触发分析。";
                toolIntents.add(new ToolIntent("GET_RESUME_RISK_SUMMARY", "了解最近简历分析风险", Map.of()));
                toolIntents.add(new ToolIntent("GET_RECENT_TRAINING_SUMMARY", "结合训练情况安排简历追问", Map.of()));
                suggestedActions.add("ANALYZE_RESUME");
                suggestedActions.add("START_PROJECT_TRAINING");
            }
            case "REVIEW" -> {
                reasonSummary = "用户想复盘，需要读取近期训练摘要，复盘生成必须用户确认。";
                toolIntents.add(new ToolIntent("GET_RECENT_TRAINING_SUMMARY", "汇总最近训练活跃度", Map.of()));
                toolIntents.add(new ToolIntent("GET_ABILITY_SUMMARY", "补充近期能力画像", Map.of()));
                suggestedActions.add("GENERATE_AGENT_REVIEW");
                suggestedActions.add("GO_HISTORY");
            }
            case "NEXT_STEP" -> {
                reasonSummary = "用户需要下一步建议，需要综合能力画像、近期训练和简历风险。";
                toolIntents.add(new ToolIntent("GET_ABILITY_SUMMARY", "需要了解用户当前薄弱维度", Map.of()));
                toolIntents.add(new ToolIntent("GET_RECENT_TRAINING_SUMMARY", "需要了解最近训练频率", Map.of()));
                toolIntents.add(new ToolIntent("GET_RESUME_RISK_SUMMARY", "需要了解简历风险是否影响下一步训练", Map.of()));
                if (mentionsKnowledgeNeed(message)) {
                    toolIntents.add(new ToolIntent("SEARCH_KNOWLEDGE", "用户提到具体知识点，检索可复习材料", Map.of("query", knowledgeQuery(message), "topK", 3)));
                }
                suggestedActions.add("GO_INSIGHTS");
                suggestedActions.add("START_QUESTION_TRAINING");
                suggestedActions.add("START_PROJECT_TRAINING");
            }
            default -> {
                reasonSummary = "用户是通用训练咨询，先读取低风险摘要再给入口。";
                toolIntents.add(new ToolIntent("GET_ABILITY_SUMMARY", "获取个性化能力画像依据", Map.of()));
                toolIntents.add(new ToolIntent("GET_RECENT_TRAINING_SUMMARY", "获取近期训练依据", Map.of()));
                suggestedActions.add("GO_DASHBOARD");
                suggestedActions.add("GO_AGENT_REVIEW");
            }
        }
        return new GuideToolPlan(reasonSummary, toolIntents, suggestedActions);
    }

    private ToolSelection selectGuideTools(GuideToolPlan plan) {
        List<ToolIntent> autoIntents = new ArrayList<>();
        List<String> suggestedActions = new ArrayList<>();
        List<String> rejected = new ArrayList<>();
        int truncated = 0;

        for (String actionName : plan.suggestedActions()) {
            if (isSuggestableAction(actionName) && !suggestedActions.contains(actionName)) {
                suggestedActions.add(actionName);
            } else if (!isNavigationTool(actionName)) {
                rejected.add(actionName + ": action is not suggestable");
            }
        }

        for (ToolIntent intent : plan.toolIntents()) {
            String toolName = intent.toolName();
            AgentTool tool = agentToolRegistry.getTool(toolName);
            if (tool == null) {
                rejected.add(toolName + ": unknown or forbidden");
                continue;
            }
            ToolDefinition definition = tool.definition();
            if (GUIDE_FORBIDDEN_TOOLS.contains(toolName) || definition.getRiskLevel() == ToolRiskLevel.HIGH) {
                rejected.add(toolName + ": high risk forbidden");
                continue;
            }
            if (isGuideAutoTool(definition)) {
                if (autoIntents.size() >= GUIDE_MAX_AUTO_TOOL_STEPS) {
                    truncated++;
                    continue;
                }
                autoIntents.add(intent);
                continue;
            }
            if (isSuggestableAction(toolName)) {
                if (!suggestedActions.contains(toolName)) {
                    suggestedActions.add(toolName);
                }
                continue;
            }
            rejected.add(toolName + ": not allowed for guide auto execution");
        }
        return new ToolSelection(autoIntents, suggestedActions, rejected, truncated);
    }

    private ToolObservation executeToolIntent(AgentContext context, ToolIntent intent) {
        long startTime = System.currentTimeMillis();
        ToolExecuteRequest request = new ToolExecuteRequest();
        request.setToolName(intent.toolName());
        request.setAgentType(context.getAgentType());
        request.setRunId(context.getRunId());
        request.setTraceId(context.getTraceId());
        request.setConfirmed(Boolean.FALSE);
        request.setParams(intent.params());
        try {
            ToolExecuteResult result = agentToolExecutor.execute(request);
            String summary = summarizeObservation(intent.toolName(), result);
            record(AgentStepType.OBSERVATION, "Summarize tool observation", intent.toolName(),
                    "reason=" + intent.reason(),
                    summary,
                    result != null && result.isSuccess() ? AgentStepStatus.SUCCEEDED : AgentStepStatus.FAILED,
                    result == null ? "EMPTY_TOOL_RESULT" : result.getErrorCode(),
                    null,
                    elapsed(startTime));
            return new ToolObservation(intent.toolName(), result != null && result.isSuccess(), summary,
                    result == null ? null : result.getData(), result == null ? "EMPTY_TOOL_RESULT" : result.getErrorCode());
        } catch (Exception exception) {
            String message = intent.toolName() + " 暂时不可用。";
            record(AgentStepType.OBSERVATION, "Summarize failed tool observation", intent.toolName(),
                    "reason=" + intent.reason(),
                    message,
                    AgentStepStatus.FAILED,
                    "TOOL_EXECUTION_FAILED",
                    sanitizer.errorMessage(exception),
                    elapsed(startTime));
            return new ToolObservation(intent.toolName(), false, message, Map.of(), "TOOL_EXECUTION_FAILED");
        }
    }

    private AgentResponse composeGuideResponse(
            AgentContext context,
            String intent,
            GuideToolPlan plan,
            ToolSelection selection,
            List<ToolObservation> observations
    ) {
        AgentResponse response = new AgentResponse();
        response.setPersonalized(true);
        response.setObservations(observations.stream().map(ToolObservation::summary).toList());
        response.setActions(selection.suggestedActions().stream()
                .map(this::safeNullableAction)
                .filter(Objects::nonNull)
                .limit(3)
                .toList());

        if (response.getActions().isEmpty()) {
            response.setActions(defaultActions(intent));
        }

        List<ToolObservation> successful = observations.stream().filter(ToolObservation::success).toList();
        boolean partialFailure = !observations.isEmpty() && successful.size() < observations.size();
        if (!observations.isEmpty() && successful.isEmpty()) {
            record(AgentStepType.FALLBACK, "Guide react fallback", null,
                    "reason=allToolsFailed",
                    "using generic navigation actions",
                    AgentStepStatus.SKIPPED,
                    "ALL_TOOLS_FAILED",
                    null,
                    0);
            response.setAnswer("部分训练数据暂时不可用。建议先回工作台看最近进展，再从成长洞察或复盘 Agent 选择下一步。");
            response.setActions(List.of(safeAction("GO_DASHBOARD"), safeAction("GO_AGENT_REVIEW")));
            return response;
        }

        String ability = observationSummary(successful, "GET_ABILITY_SUMMARY");
        String training = observationSummary(successful, "GET_RECENT_TRAINING_SUMMARY");
        String resume = observationSummary(successful, "GET_RESUME_RISK_SUMMARY");
        String knowledge = observationSummary(successful, "SEARCH_KNOWLEDGE");
        String documents = observationSummary(successful, "SEARCH_USER_DOCUMENTS");
        String prefix = partialFailure ? "部分数据暂时不可用。 " : "";

        String answer;
        switch (intent) {
            case "PAGE_HELP" -> answer = pageDescription(context.getCurrentPath()) + " 你可以从下面入口继续。";
            case "PROJECT_TRAINING" -> answer = prefix + firstUseful(
                    combine(training, ability, "建议先做一轮项目拷打，把项目表达、技术取舍和追问链路练顺。"),
                    "建议先从项目拷打开一轮，结束后再回成长洞察看薄弱维度。");
            case "QUESTION_TRAINING" -> answer = prefix + firstUseful(
                    combine(ability, knowledge, "建议先做一轮八股训练，用回答报告验证表达是否变稳。"),
                    "建议先做一轮八股训练，再到成长洞察确认薄弱知识点。");
            case "MOCK_INTERVIEW" -> answer = prefix + firstUseful(
                    combine(training, resume, "如果近期训练不多，先做一场综合模拟面试，结束后用报告复盘。"),
                    "建议开始一场综合模拟面试，结束后再根据报告拆专项训练。");
            case "RESUME" -> answer = prefix + firstUseful(
                    combine(resume, "建议先处理简历风险，再用项目拷打验证这些风险点能否讲清楚。"),
                    "建议先进入简历训练查看风险点，再围绕项目经历做追问练习。");
            case "REVIEW" -> answer = prefix + firstUseful(
                    combine(training, ability, "建议生成一次复盘，把重复问题整理成下一轮训练清单。"),
                    "建议进入复盘 Agent，总结最近训练问题后再选择专项训练。");
            case "NEXT_STEP" -> answer = prefix + composeNextStepAnswer(ability, training, resume, knowledge, documents);
            default -> answer = prefix + firstUseful(
                    combine(ability, training, "建议先查看成长洞察，再选择八股或项目训练做一次验证。"),
                    "建议先回工作台看最近进展，再进入成长洞察或复盘 Agent 确定下一步。");
        }
        response.setAnswer(truncate(answer, 180));
        return response;
    }

    private String composeNextStepAnswer(String ability, String training, String resume, String knowledge, String documents) {
        if (StringUtils.hasText(ability)) {
            StringBuilder builder = new StringBuilder();
            builder.append(ability);
            if (StringUtils.hasText(training)) {
                builder.append(" ").append(training);
            }
            if (StringUtils.hasText(knowledge)) {
                builder.append(" ").append(knowledge);
            }
            builder.append(" 建议先看成长洞察确认证据，再做一次八股专项或项目拷打。");
            return builder.toString();
        }
        if (StringUtils.hasText(training)) {
            return training + " 建议先做一次项目拷打或八股训练，产出报告后再看成长洞察。";
        }
        if (StringUtils.hasText(resume)) {
            return resume + " 建议先处理简历风险，再进入项目拷打验证表达。";
        }
        if (StringUtils.hasText(documents)) {
            return documents + " 建议先把材料转成项目档案，再开始项目拷打。";
        }
        return "当前个性化数据还不够。建议先完成一次八股训练或项目拷打，再回来看成长洞察。";
    }

    private void record(
            AgentStepType stepType,
            String stepName,
            String toolName,
            String inputSummary,
            String outputSummary,
            AgentStepStatus status,
            String errorCode,
            String errorMessage,
            long latencyMs
    ) {
        AgentExecutionContext context = AgentRuntimeContextHolder.get();
        if (context == null) {
            return;
        }
        AgentStepRecord record = new AgentStepRecord();
        record.setStepType(stepType);
        record.setStepName(stepName);
        record.setToolName(toolName);
        record.setInputSummary(inputSummary);
        record.setOutputSummary(outputSummary);
        record.setStatus(status);
        record.setErrorCode(errorCode);
        record.setErrorMessage(errorMessage);
        record.setLatencyMs(latencyMs);
        agentTraceService.recordStep(context.getRunId(), record);
    }

    private AgentResponse toAgentResponse(GuideChatResponseVO guideResponse, String runId, String traceId, String status) {
        AgentResponse response = new AgentResponse();
        response.setAnswer(guideResponse == null ? null : guideResponse.getAnswer());
        response.setPersonalized(guideResponse == null ? Boolean.FALSE : guideResponse.getPersonalized());
        response.setActions(guideResponse == null ? List.of() : guideResponse.getActions());
        response.setRunId(runId);
        response.setTraceId(traceId);
        response.setStatus(status);
        return response;
    }

    private AgentResponse fallback(String runId, String traceId, String status) {
        AgentResponse response = new AgentResponse();
        response.setAnswer("我暂时无法生成个性化建议，但你可以先从项目训练、八股问答或复盘 Agent 开始。");
        response.setPersonalized(false);
        response.setActions(List.of(safeAction("GO_DASHBOARD"), safeAction("GO_AGENT_REVIEW")));
        response.setRunId(runId);
        response.setTraceId(traceId);
        response.setStatus(status);
        return response;
    }

    private GuideActionCardVO safeAction(String toolName) {
        AgentTool tool = agentToolRegistry.requireTool(toolName);
        return GuideActionCardVO.fromDefinition(tool.definition());
    }

    private GuideActionCardVO safeNullableAction(String toolName) {
        AgentTool tool = agentToolRegistry.getTool(toolName);
        return tool == null ? null : GuideActionCardVO.fromDefinition(tool.definition());
    }

    private List<GuideActionCardVO> defaultActions(String intent) {
        return switch (intent) {
            case "QUESTION_TRAINING" -> List.of(safeAction("GO_INSIGHTS"), safeAction("START_QUESTION_TRAINING"));
            case "PROJECT_TRAINING" -> List.of(safeAction("GO_INSIGHTS"), safeAction("START_PROJECT_TRAINING"));
            case "MOCK_INTERVIEW" -> List.of(safeAction("START_MOCK_INTERVIEW"), safeAction("GO_AGENT_REVIEW"));
            case "RESUME" -> List.of(safeAction("ANALYZE_RESUME"), safeAction("START_PROJECT_TRAINING"));
            case "REVIEW" -> List.of(safeAction("GENERATE_AGENT_REVIEW"), safeAction("GO_HISTORY"));
            default -> List.of(safeAction("GO_INSIGHTS"), safeAction("START_QUESTION_TRAINING"), safeAction("START_PROJECT_TRAINING"));
        };
    }

    private boolean isGuideAutoTool(ToolDefinition definition) {
        return definition != null
                && GUIDE_AUTO_TOOLS.contains(definition.getToolName())
                && definition.getRiskLevel() == ToolRiskLevel.LOW
                && definition.getExecutionMode() == ToolExecutionMode.AUTO_EXECUTE;
    }

    private boolean isSuggestableAction(String toolName) {
        AgentTool tool = agentToolRegistry.getTool(toolName);
        if (tool == null) {
            return false;
        }
        ToolDefinition definition = tool.definition();
        return GUIDE_SUGGEST_ONLY_TOOLS.contains(toolName)
                || isNavigationTool(toolName)
                || definition.getExecutionMode() == ToolExecutionMode.SUGGEST_ONLY
                || definition.getExecutionMode() == ToolExecutionMode.EXECUTE_AFTER_CONFIRM;
    }

    private boolean isNavigationTool(String toolName) {
        return StringUtils.hasText(toolName) && (toolName.startsWith("GO_") || "LOGIN".equals(toolName));
    }

    private String summarizeObservation(String toolName, ToolExecuteResult result) {
        if (result == null) {
            return toolName + " 暂时没有返回结果。";
        }
        if (!result.isSuccess()) {
            return result.getMessage();
        }
        Map<String, Object> data = result.getData() == null ? Map.of() : result.getData();
        return switch (toolName) {
            case "GET_ABILITY_SUMMARY" -> summarizeAbility(data, result.getMessage());
            case "GET_RECENT_TRAINING_SUMMARY" -> summarizeTraining(data, result.getMessage());
            case "GET_RESUME_RISK_SUMMARY" -> summarizeResumeRisk(data, result.getMessage());
            case "SEARCH_KNOWLEDGE" -> summarizeSearch(data, "知识文章", result.getMessage());
            case "SEARCH_USER_DOCUMENTS" -> summarizeSearch(data, "用户文档", result.getMessage());
            default -> result.getMessage();
        };
    }

    private String summarizeAbility(Map<String, Object> data, String fallback) {
        Object value = data.get("dimensions");
        if (!(value instanceof List<?> dimensions) || dimensions.isEmpty()) {
            return fallback;
        }
        List<DimensionSummary> weak = dimensions.stream()
                .filter(Map.class::isInstance)
                .map(item -> dimensionSummary((Map<?, ?>) item))
                .filter(item -> StringUtils.hasText(item.name()))
                .sorted(Comparator.comparing(DimensionSummary::score, Comparator.nullsLast(Integer::compareTo)))
                .filter(item -> item.score() == null || item.score() < 80)
                .limit(3)
                .toList();
        if (weak.isEmpty()) {
            return "能力画像暂未出现明显低分维度。";
        }
        return "能力画像显示 " + String.join("、", weak.stream().map(DimensionSummary::display).toList()) + " 偏弱。";
    }

    private DimensionSummary dimensionSummary(Map<?, ?> source) {
        String name = textValue(source.get("dimension"));
        Integer score = intValue(source.get("score"));
        return new DimensionSummary(name, score);
    }

    private String summarizeTraining(Map<String, Object> data, String fallback) {
        Long total = longValue(data.get("total"));
        if (total == null) {
            return fallback;
        }
        if (total == 0) {
            return "近 30 天还没有训练记录。";
        }
        return "近 30 天完成项目训练 " + numberText(data.get("projectTrainingCount"))
                + " 次、八股训练 " + numberText(data.get("questionTrainingCount"))
                + " 次、模拟面试 " + numberText(data.get("mockInterviewCount")) + " 次。";
    }

    private String summarizeResumeRisk(Map<String, Object> data, String fallback) {
        Long riskCount = longValue(data.get("riskCount"));
        if (riskCount == null) {
            return fallback;
        }
        if (riskCount == 0) {
            return "暂时没有可用或明显的简历风险摘要。";
        }
        String topRisk = sanitizer.summarizeText(textValue(data.get("topRisk")));
        if (StringUtils.hasText(topRisk)) {
            return "最近简历分析有 " + riskCount + " 个风险点，优先关注：" + topRisk + "。";
        }
        return "最近简历分析有 " + riskCount + " 个风险点。";
    }

    private String summarizeSearch(Map<String, Object> data, String label, String fallback) {
        Long count = longValue(data.get("resultCount"));
        if (count == null || count == 0) {
            return fallback;
        }
        Object value = data.get("chunks");
        if (!(value instanceof List<?> chunks) || chunks.isEmpty()) {
            return "检索到 " + count + " 条" + label + "结果。";
        }
        List<String> titles = chunks.stream()
                .filter(Map.class::isInstance)
                .map(item -> searchTitle((Map<?, ?>) item))
                .filter(StringUtils::hasText)
                .distinct()
                .limit(2)
                .toList();
        if (titles.isEmpty()) {
            return "检索到 " + count + " 条" + label + "结果。";
        }
        return "检索到 " + count + " 条" + label + "结果：" + String.join("、", titles) + "。";
    }

    private String searchTitle(Map<?, ?> chunk) {
        String title = firstText(textValue(chunk.get("title")), textValue(chunk.get("section")));
        return sanitizer.summarizeText(title);
    }

    private String observationSummary(List<ToolObservation> observations, String toolName) {
        return observations.stream()
                .filter(observation -> toolName.equals(observation.toolName()))
                .map(ToolObservation::summary)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse(null);
    }

    private String combine(String... parts) {
        List<String> safeParts = new ArrayList<>();
        for (String part : parts) {
            if (StringUtils.hasText(part)) {
                safeParts.add(part.trim());
            }
        }
        return String.join(" ", safeParts);
    }

    private String firstUseful(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private String toolNames(List<ToolIntent> intents) {
        if (intents == null || intents.isEmpty()) {
            return "none";
        }
        return String.join(",", intents.stream().map(ToolIntent::toolName).toList());
    }

    private List<String> pageActionNames(String currentPath) {
        String path = normalizePath(currentPath);
        if ("/projects".equals(path)) {
            return List.of("GO_PROJECTS", "START_PROJECT_TRAINING");
        }
        if ("/questions".equals(path)) {
            return List.of("START_QUESTION_TRAINING", "GO_INSIGHTS");
        }
        if ("/learn".equals(path)) {
            return List.of("GO_LEARN", "GO_INSIGHTS");
        }
        if ("/insights".equals(path)) {
            return List.of("GO_INSIGHTS", "START_QUESTION_TRAINING");
        }
        if ("/documents".equals(path)) {
            return List.of("GO_DOCUMENTS", "GO_PROJECTS");
        }
        if ("/resumes".equals(path)) {
            return List.of("ANALYZE_RESUME", "START_PROJECT_TRAINING");
        }
        if ("/agent-review".equals(path)) {
            return List.of("GENERATE_AGENT_REVIEW", "GO_HISTORY");
        }
        if ("/history".equals(path)) {
            return List.of("GO_HISTORY", "GO_AGENT_REVIEW");
        }
        if ("/profile".equals(path)) {
            return List.of("GO_PROFILE", "GO_DASHBOARD");
        }
        if ("/mock-interviews".equals(path)) {
            return List.of("START_MOCK_INTERVIEW", "GO_INSIGHTS");
        }
        return List.of("GO_DASHBOARD");
    }

    private boolean mentionsKnowledgeNeed(String message) {
        return containsAny(message == null ? "" : message, "redis", "缓存", "jvm", "mysql", "索引", "事务", "线程池", "锁", "aop");
    }

    private String knowledgeQuery(String message) {
        if (!StringUtils.hasText(message)) {
            return "Java 后端面试";
        }
        String normalized = message.toLowerCase(Locale.ROOT);
        if (normalized.contains("redis") || normalized.contains("缓存")) {
            if (normalized.contains("击穿")) {
                return "Redis 缓存击穿";
            }
            if (normalized.contains("穿透")) {
                return "Redis 缓存穿透";
            }
            return "Redis 缓存";
        }
        if (normalized.contains("jvm")) {
            return "JVM 内存 垃圾回收";
        }
        if (normalized.contains("mysql") || normalized.contains("索引")) {
            return "MySQL 索引";
        }
        if (normalized.contains("事务")) {
            return "Spring 事务";
        }
        if (normalized.contains("线程池")) {
            return "Java 线程池";
        }
        return truncate(message, 40);
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private String textValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String numberText(Object value) {
        Long number = longValue(value);
        return number == null ? "0" : String.valueOf(number);
    }

    private Long longValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && StringUtils.hasText(text)) {
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private Integer intValue(Object value) {
        Long number = longValue(value);
        if (number == null) {
            return null;
        }
        return Math.toIntExact(Math.max(Math.min(number, Integer.MAX_VALUE), Integer.MIN_VALUE));
    }

    private String truncate(String text, int limit) {
        if (!StringUtils.hasText(text)) {
            return text;
        }
        String trimmed = text.trim();
        return trimmed.length() > limit ? trimmed.substring(0, limit) + "..." : trimmed;
    }

    private void attachTrace(GuideChatResponseVO response, String runId, String traceId) {
        if (response == null) {
            return;
        }
        response.setRunId(runId);
        response.setTraceId(traceId);
        response.setStatus(AgentRunStatus.SUCCEEDED.name());
        attachTrace(response.getActions(), runId, traceId);
    }

    private void attachTrace(List<GuideActionCardVO> actions, String runId, String traceId) {
        if (actions == null) {
            return;
        }
        for (GuideActionCardVO action : actions) {
            action.setRunId(runId);
            action.setTraceId(traceId);
        }
    }

    private String inputSummary(AgentRequest request) {
        return "messageChars=" + safeLength(request == null ? null : request.getMessage())
                + ", path=" + normalizePath(request == null ? null : request.getCurrentPath())
                + ", pageTitle=" + sanitizer.summarizeText(request == null ? null : request.getPageTitle());
    }

    private String detectIntent(String message) {
        if (!StringUtils.hasText(message)) {
            return "NEXT_STEP";
        }
        String normalized = message.toLowerCase(Locale.ROOT);
        if (containsAny(normalized, "这个页面", "当前页面", "页面怎么用", "干什么", "怎么用")) {
            return "PAGE_HELP";
        }
        if (containsAny(normalized, "项目", "拷打")) {
            return "PROJECT_TRAINING";
        }
        if (containsAny(normalized, "八股", "基础题", "面试题", "题库")) {
            return "QUESTION_TRAINING";
        }
        if (containsAny(normalized, "模拟面试", "真实面试", "综合面试", "技术面")) {
            return "MOCK_INTERVIEW";
        }
        if (containsAny(normalized, "简历", "履历", "风险")) {
            return "RESUME";
        }
        if (containsAny(normalized, "复盘", "总结", "最近问题", "回顾")) {
            return "REVIEW";
        }
        if (containsAny(normalized, "下一步", "不知道", "开始", "推荐", "怎么练")) {
            return "NEXT_STEP";
        }
        return "GENERAL_GUIDE";
    }

    private boolean containsAny(String message, String... keywords) {
        for (String keyword : keywords) {
            if (message.contains(keyword.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private String normalizeMessage(String message) {
        if (!StringUtils.hasText(message)) {
            return "我下一步该做什么？";
        }
        String trimmed = message.trim();
        return trimmed.length() > 240 ? trimmed.substring(0, 240) : trimmed;
    }

    private String normalizePath(String currentPath) {
        if (!StringUtils.hasText(currentPath)) {
            return "/";
        }
        String path = currentPath.trim();
        int queryIndex = path.indexOf('?');
        if (queryIndex >= 0) {
            path = path.substring(0, queryIndex);
        }
        int hashIndex = path.indexOf('#');
        if (hashIndex >= 0) {
            path = path.substring(0, hashIndex);
        }
        if (path.startsWith("/learn/articles/")) {
            return "/learn";
        }
        if (path.startsWith("/question-sessions/")) {
            return "/questions";
        }
        if (path.startsWith("/question-reports/")) {
            return "/history";
        }
        if (path.startsWith("/interviews/") || path.startsWith("/reports/")) {
            return "/projects";
        }
        if (path.startsWith("/mock-interviews/")) {
            return "/mock-interviews";
        }
        if (path.startsWith("/projects/")) {
            return "/projects";
        }
        return path;
    }

    private String pageDescription(String currentPath) {
        return switch (normalizePath(currentPath)) {
            case "/" -> "首页用于了解 CodeCoach AI 的训练能力和进入登录。";
            case "/dashboard" -> "工作台汇总你的项目、训练、洞察和下一步入口。";
            case "/mock-interviews" -> "真实模拟面试页用于创建完整 Java 后端技术面，并在结束后生成综合报告。";
            case "/projects" -> "项目档案页用于管理真实项目，并从项目出发开始拷打训练。";
            case "/questions" -> "八股问答页用于练 Java 后端基础题，适合补齐概念表达和追问能力。";
            case "/learn" -> "知识学习页用于按主题阅读知识文章，并和训练中的薄弱点衔接。";
            case "/insights" -> "成长洞察页展示能力画像、薄弱维度和学习推荐。";
            case "/documents" -> "我的文档页用于上传项目说明、学习笔记、简历材料，并接入 RAG 增强训练。";
            case "/resumes" -> "简历训练页用于上传和分析简历，提炼风险点与可追问项目。";
            case "/agent-review" -> "复盘 Agent 用于系统总结最近训练问题和下一步行动。";
            case "/history" -> "训练历史页用于查看最近项目拷打、八股训练和报告记录。";
            case "/profile" -> "个人中心用于维护你的账号信息和头像。";
            default -> "这个页面属于 CodeCoach AI 的训练流程。如果不确定下一步，可以先回工作台。";
        };
    }

    private Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof LoginUser loginUser) {
            return loginUser.getUserId();
        }
        return null;
    }

    private long elapsed(long startTime) {
        return System.currentTimeMillis() - startTime;
    }

    private int safeLength(String text) {
        return text == null ? 0 : text.length();
    }

    private record GuideToolPlan(
            String reasonSummary,
            List<ToolIntent> toolIntents,
            List<String> suggestedActions
    ) {
    }

    private record ToolIntent(
            String toolName,
            String reason,
            Map<String, Object> params
    ) {
        private ToolIntent {
            params = params == null ? Map.of() : new LinkedHashMap<>(params);
        }
    }

    private record ToolSelection(
            List<ToolIntent> autoIntents,
            List<String> suggestedActions,
            List<String> rejectedSummaries,
            int truncatedCount
    ) {
    }

    private record ToolObservation(
            String toolName,
            boolean success,
            String summary,
            Map<String, Object> data,
            String errorCode
    ) {
        private ToolObservation {
            data = data == null ? Map.of() : new LinkedHashMap<>(data);
        }
    }

    private record DimensionSummary(String name, Integer score) {
        private String display() {
            return score == null ? name : name + "(" + score + ")";
        }
    }
}
