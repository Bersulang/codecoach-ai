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
import com.codecoach.module.agent.tool.service.AgentTool;
import com.codecoach.module.agent.tool.service.AgentToolRegistry;
import com.codecoach.module.guide.dto.GuideChatRequest;
import com.codecoach.module.guide.service.GuideChatService;
import com.codecoach.module.guide.vo.GuideActionCardVO;
import com.codecoach.module.guide.vo.GuideChatResponseVO;
import com.codecoach.security.LoginUser;
import java.util.List;
import java.util.Locale;
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

    private final GuideChatService guideChatService;
    private final AgentToolRegistry agentToolRegistry;
    private final AgentTraceService agentTraceService;
    private final AgentTraceSanitizer sanitizer;

    public AgentRuntime(
            GuideChatService guideChatService,
            AgentToolRegistry agentToolRegistry,
            AgentTraceService agentTraceService,
            AgentTraceSanitizer sanitizer
    ) {
        this.guideChatService = guideChatService;
        this.agentToolRegistry = agentToolRegistry;
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

            GuideChatResponseVO guideResponse = executeGuide(request);
            attachTrace(guideResponse, runId, traceId);

            String selectedTools = selectedTools(guideResponse);
            record(AgentStepType.TOOL_SELECT, "Select guide actions", selectedTools,
                    "intent=" + intent,
                    "selected=" + selectedTools,
                    AgentStepStatus.SUCCEEDED, null, null, 0);
            record(AgentStepType.RESPONSE_COMPOSE, "Compose guide response", null,
                    "actionCount=" + actionCount(guideResponse),
                    guideResponse == null ? null : guideResponse.getAnswer(),
                    AgentStepStatus.SUCCEEDED, null, null, 0);

            AgentResponse response = toAgentResponse(guideResponse, runId, traceId, AgentRunStatus.SUCCEEDED.name());
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

    private String selectedTools(GuideChatResponseVO response) {
        if (response == null || response.getActions() == null || response.getActions().isEmpty()) {
            return "none";
        }
        return String.join(",", response.getActions().stream()
                .map(GuideActionCardVO::getToolName)
                .filter(StringUtils::hasText)
                .toList());
    }

    private int actionCount(GuideChatResponseVO response) {
        return response == null || response.getActions() == null ? 0 : response.getActions().size();
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
}
