package com.codecoach.module.guide.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.codecoach.module.agent.entity.AgentReview;
import com.codecoach.module.agent.mapper.AgentReviewMapper;
import com.codecoach.module.agent.runtime.dto.AgentStepRecord;
import com.codecoach.module.agent.runtime.enums.AgentStepStatus;
import com.codecoach.module.agent.runtime.enums.AgentStepType;
import com.codecoach.module.agent.runtime.service.AgentTraceService;
import com.codecoach.module.agent.runtime.support.AgentExecutionContext;
import com.codecoach.module.agent.runtime.support.AgentRuntimeContextHolder;
import com.codecoach.module.agent.runtime.support.AgentTraceSanitizer;
import com.codecoach.module.agent.tool.service.AgentTool;
import com.codecoach.module.agent.tool.service.AgentToolRegistry;
import com.codecoach.module.document.entity.UserDocument;
import com.codecoach.module.document.mapper.UserDocumentMapper;
import com.codecoach.module.guide.dto.GuideChatRequest;
import com.codecoach.module.guide.model.GuideAiSuggestion;
import com.codecoach.module.guide.service.AiGuideService;
import com.codecoach.module.guide.service.GuideChatService;
import com.codecoach.module.guide.vo.GuideActionCardVO;
import com.codecoach.module.guide.vo.GuideChatResponseVO;
import com.codecoach.module.insight.entity.UserAbilitySnapshot;
import com.codecoach.module.insight.mapper.UserAbilitySnapshotMapper;
import com.codecoach.module.interview.entity.InterviewSession;
import com.codecoach.module.interview.mapper.InterviewSessionMapper;
import com.codecoach.module.project.entity.Project;
import com.codecoach.module.project.mapper.ProjectMapper;
import com.codecoach.module.question.entity.QuestionTrainingSession;
import com.codecoach.module.question.mapper.QuestionTrainingSessionMapper;
import com.codecoach.module.resume.entity.ResumeProfile;
import com.codecoach.module.resume.mapper.ResumeProfileMapper;
import com.codecoach.module.resume.model.ResumeAnalysisResult;
import com.codecoach.security.LoginUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class GuideChatServiceImpl implements GuideChatService {

    private static final int MESSAGE_LIMIT = 240;
    private static final int RECENT_LIMIT = 30;
    private static final int SNAPSHOT_LIMIT = 20;

    private final ProjectMapper projectMapper;
    private final UserDocumentMapper userDocumentMapper;
    private final ResumeProfileMapper resumeProfileMapper;
    private final InterviewSessionMapper interviewSessionMapper;
    private final QuestionTrainingSessionMapper questionTrainingSessionMapper;
    private final UserAbilitySnapshotMapper userAbilitySnapshotMapper;
    private final AgentReviewMapper agentReviewMapper;
    private final ObjectMapper objectMapper;
    private final ObjectProvider<AiGuideService> aiGuideServiceProvider;
    private final AgentToolRegistry agentToolRegistry;
    private final AgentTraceService agentTraceService;
    private final AgentTraceSanitizer agentTraceSanitizer;

    public GuideChatServiceImpl(
            ProjectMapper projectMapper,
            UserDocumentMapper userDocumentMapper,
            ResumeProfileMapper resumeProfileMapper,
            InterviewSessionMapper interviewSessionMapper,
            QuestionTrainingSessionMapper questionTrainingSessionMapper,
            UserAbilitySnapshotMapper userAbilitySnapshotMapper,
            AgentReviewMapper agentReviewMapper,
            ObjectMapper objectMapper,
            ObjectProvider<AiGuideService> aiGuideServiceProvider,
            AgentToolRegistry agentToolRegistry,
            AgentTraceService agentTraceService,
            AgentTraceSanitizer agentTraceSanitizer
    ) {
        this.projectMapper = projectMapper;
        this.userDocumentMapper = userDocumentMapper;
        this.resumeProfileMapper = resumeProfileMapper;
        this.interviewSessionMapper = interviewSessionMapper;
        this.questionTrainingSessionMapper = questionTrainingSessionMapper;
        this.userAbilitySnapshotMapper = userAbilitySnapshotMapper;
        this.agentReviewMapper = agentReviewMapper;
        this.objectMapper = objectMapper;
        this.aiGuideServiceProvider = aiGuideServiceProvider;
        this.agentToolRegistry = agentToolRegistry;
        this.agentTraceService = agentTraceService;
        this.agentTraceSanitizer = agentTraceSanitizer;
    }

    @Override
    public GuideChatResponseVO chat(GuideChatRequest request) {
        String message = normalizeMessage(request == null ? null : request.getMessage());
        String currentPath = normalizePath(request == null ? null : request.getCurrentPath());
        Long userId = currentUserId();
        if (userId == null) {
            return guestResponse(message, currentPath);
        }

        GuideUserSummary summary = buildSummary(userId);
        try {
            return authedResponse(message, currentPath, summary);
        } catch (Exception exception) {
            return new GuideChatResponseVO(
                    "训练向导暂时没有拿到完整状态。我先给你一个稳妥入口：回到工作台查看最近训练，再从项目、八股或复盘继续。",
                    false,
                    List.of(action("GO_DASHBOARD"), action("GO_AGENT_REVIEW"))
            );
        }
    }

    private GuideChatResponseVO guestResponse(String message, String currentPath) {
        if (isPageQuestion(message)) {
            return new GuideChatResponseVO(
                    pageDescription(currentPath) + " 登录后我可以结合你的训练报告、简历风险点和能力画像，推荐下一步训练动作。",
                    false,
                    List.of(action("LOGIN"))
            );
        }
        if (hasAny(message, "项目", "拷打", "八股", "简历", "复盘", "弱点", "下一步", "开始")) {
            return new GuideChatResponseVO(
                    "你可以先登录进入工作台。登录后，我会根据你的项目档案、训练报告、能力画像和简历分析，给出更具体的下一步训练建议。",
                    false,
                    List.of(action("LOGIN"))
            );
        }
        GuideChatResponseVO aiResponse = tryAiResponse(message, currentPath, null, false);
        if (aiResponse != null) {
            return aiResponse;
        }
        return new GuideChatResponseVO(
                "我是 CodeCoach Guide，负责帮你理解产品功能并找到下一步训练入口。登录后，我可以结合你的训练数据给出个性化建议。",
                false,
                List.of(action("LOGIN"))
        );
    }

    private GuideChatResponseVO authedResponse(String message, String currentPath, GuideUserSummary summary) {
        if (isPageQuestion(message)) {
            return new GuideChatResponseVO(
                    pageDescription(currentPath) + " 如果你想继续推进，我可以带你去最相关的训练入口。",
                    true,
                    pageActions(currentPath)
            );
        }
        if (hasAny(message, "项目", "拷打")) {
            return new GuideChatResponseVO(
                    summary.projectCount == 0
                            ? "你还没有项目档案。建议先补一个真实项目，再用项目拷打训练追问亮点、难点和技术取舍。"
                            : "你已经有 " + summary.projectCount + " 个项目档案，可以直接从项目列表选择一个开始拷打训练。",
                    true,
                    List.of(action(summary.projectCount == 0 ? "GO_PROJECTS" : "START_PROJECT_TRAINING"))
            );
        }
        if (hasAny(message, "模拟面试", "真实面试", "综合面试", "技术面")) {
            return new GuideChatResponseVO(
                    "建议直接开始一场综合技术一面。它会按开场、简历项目、技术基础、项目深挖和场景设计推进，结束后生成综合报告。",
                    true,
                    List.of(action("START_MOCK_INTERVIEW"), action("GO_MOCK_INTERVIEWS"), action("GO_AGENT_REVIEW"))
            );
        }
        if (hasAny(message, "八股", "基础题", "面试题", "题库")) {
            return new GuideChatResponseVO(
                    "八股训练适合补齐 Java 后端基础表达。你可以从八股问答开始一轮，结束后到成长洞察看薄弱维度。",
                    true,
                    List.of(action("START_QUESTION_TRAINING"), action("GO_INSIGHTS"))
            );
        }
        if (hasAny(message, "简历", "履历", "风险")) {
            String answer = summary.resumeCount == 0
                    ? "你还没有简历记录。建议先进入简历训练上传并分析简历，再围绕风险点生成项目追问。"
                    : "你已有 " + summary.resumeCount + " 份简历记录。" + resumeRiskSentence(summary) + " 可以先进入简历训练查看风险点。";
            return new GuideChatResponseVO(answer, true, List.of(action("ANALYZE_RESUME"), action("START_PROJECT_TRAINING")));
        }
        if (hasAny(message, "文档", "上传", "资料", "rag")) {
            String answer = summary.documentCount == 0
                    ? "你还没有上传文档。可以先把项目说明、学习笔记或简历材料放到我的文档，后续训练会更贴近你的素材。"
                    : "你已有 " + summary.documentCount + " 份文档。下一步可以用这些材料支撑项目档案、简历分析和训练追问。";
            return new GuideChatResponseVO(answer, true, List.of(action("GO_DOCUMENTS"), action("GO_PROJECTS")));
        }
        if (hasAny(message, "弱点", "薄弱", "短板", "能力", "最近应该练什么")) {
            return weaknessResponse(summary);
        }
        if (hasAny(message, "复盘", "总结", "最近问题", "回顾")) {
            String answer = StringUtils.hasText(summary.latestReviewSummary)
                    ? "你最近的复盘结论是：" + summary.latestReviewSummary + "。如果想系统梳理最近问题，可以继续进入复盘 Agent。"
                    : "你还没有可用的复盘摘要。建议生成一次复盘，把项目训练、八股训练和简历风险串起来看。";
            return new GuideChatResponseVO(answer, true, List.of(action("GENERATE_AGENT_REVIEW"), action("GO_HISTORY")));
        }
        if (hasAny(message, "计划", "规划")) {
            return new GuideChatResponseVO(
                    "当前我可以先帮你找到下一步训练动作。完整训练计划功能后续会加入。你现在可以先生成复盘，确定优先训练方向。",
                    true,
                    List.of(action("GENERATE_AGENT_REVIEW"), action("GO_INSIGHTS"))
            );
        }
        if (hasAny(message, "面试", "不会回答", "不知道怎么回答", "怎么回答")) {
            GuideChatResponseVO aiResponse = tryAiResponse(message, currentPath, summary, true);
            if (aiResponse != null) {
                return aiResponse;
            }
        }
        if (hasAny(message, "下一步", "不知道", "开始", "推荐", "怎么练")) {
            return nextStepResponse(summary);
        }
        GuideChatResponseVO aiResponse = tryAiResponse(message, currentPath, summary, true);
        if (aiResponse != null) {
            return aiResponse;
        }
        return new GuideChatResponseVO(
                "我更擅长回答产品内训练路径问题。你可以问“这个页面怎么用”“我下一步该做什么”“我想练项目/八股/简历”。",
                true,
                List.of(action("GO_DASHBOARD"), action("GO_AGENT_REVIEW"))
        );
    }

    private GuideChatResponseVO nextStepResponse(GuideUserSummary summary) {
        if (summary.projectCount == 0) {
            return new GuideChatResponseVO(
                    "建议先创建项目档案。CodeCoach 的项目拷打、简历追问和复盘，都需要一个具体项目作为训练抓手。",
                    true,
                    List.of(action("GO_PROJECTS"), action("GO_DOCUMENTS"))
            );
        }
        if (summary.recentTrainingCount == 0) {
            return new GuideChatResponseVO(
                    "你已经有项目档案，但最近还没有训练记录。建议先做一轮项目拷打，产出报告后再看成长洞察。",
                    true,
                    List.of(action("START_PROJECT_TRAINING"), action("GO_INSIGHTS"))
            );
        }
        if (!summary.lowDimensions.isEmpty()) {
            String weak = summary.lowDimensions.get(0);
            return new GuideChatResponseVO(
                    "建议优先补 `" + weak + "`。这是你近期能力画像里更靠前的薄弱项，可以先看学习推荐，再做一轮八股或项目训练验证表达。",
                    true,
                    List.of(action("GO_INSIGHTS"), action("START_QUESTION_TRAINING"), action("GO_LEARN"))
            );
        }
        if (summary.resumeCount == 0 && summary.documentCount > 0) {
            return new GuideChatResponseVO(
                    "你已经有文档素材，但还没有简历分析。建议进入简历训练，把材料转成可追问的风险点。",
                    true,
                    List.of(action("ANALYZE_RESUME"), action("GO_DOCUMENTS"))
            );
        }
        return new GuideChatResponseVO(
                "建议进入复盘 Agent，总结最近训练问题，再选择一个最影响面试表现的方向专项训练。",
                true,
                List.of(action("GENERATE_AGENT_REVIEW"), action("GO_HISTORY"), action("GO_INSIGHTS"))
        );
    }

    private GuideChatResponseVO weaknessResponse(GuideUserSummary summary) {
        if (summary.lowDimensions.isEmpty()) {
            return new GuideChatResponseVO(
                    summary.recentTrainingCount == 0
                            ? "暂时还没有足够训练数据判断薄弱点。先完成一轮项目拷打或八股训练，我再帮你看能力画像。"
                            : "暂时没有明显低分维度。建议进入成长洞察查看趋势，或生成复盘找重复出现的问题。",
                    true,
                    List.of(action("START_QUESTION_TRAINING"), action("GO_INSIGHTS"), action("GENERATE_AGENT_REVIEW"))
            );
        }
        String weakList = String.join("、", summary.lowDimensions.stream().limit(3).toList());
        return new GuideChatResponseVO(
                "你近期更值得优先关注：" + weakList + "。建议先看成长洞察里的证据，再用八股训练或项目拷打做针对性表达练习。",
                true,
                List.of(action("GO_INSIGHTS"), action("START_QUESTION_TRAINING"), action("START_PROJECT_TRAINING"))
        );
    }

    private GuideChatResponseVO tryAiResponse(
            String message,
            String currentPath,
            GuideUserSummary summary,
            boolean personalized
    ) {
        AiGuideService aiGuideService = aiGuideServiceProvider.getIfAvailable();
        if (aiGuideService == null) {
            recordRuntimeStep(
                    AgentStepType.LLM_CALL,
                    "Guide AI suggestion skipped",
                    null,
                    "provider=none",
                    "AI Guide service not configured",
                    AgentStepStatus.SKIPPED,
                    "AI_GUIDE_NOT_CONFIGURED",
                    null,
                    0
            );
            return null;
        }
        long startTime = System.currentTimeMillis();
        try {
            GuideAiSuggestion suggestion = aiGuideService.suggest(buildAiPrompt(message, currentPath, summary, personalized));
            GuideChatResponseVO response = sanitizeAiSuggestion(suggestion, personalized);
            recordRuntimeStep(
                    AgentStepType.LLM_CALL,
                    "Guide AI suggestion",
                    null,
                    "personalized=" + personalized + ", path=" + normalizePath(currentPath),
                    response == null ? "empty suggestion" : response.getAnswer(),
                    response == null ? AgentStepStatus.FAILED : AgentStepStatus.SUCCEEDED,
                    response == null ? "EMPTY_AI_SUGGESTION" : null,
                    null,
                    System.currentTimeMillis() - startTime
            );
            return response;
        } catch (Exception ignored) {
            recordRuntimeStep(
                    AgentStepType.LLM_CALL,
                    "Guide AI suggestion",
                    null,
                    "personalized=" + personalized + ", path=" + normalizePath(currentPath),
                    null,
                    AgentStepStatus.FAILED,
                    "AI_GUIDE_FAILED",
                    agentTraceSanitizer.errorMessage(ignored),
                    System.currentTimeMillis() - startTime
            );
            return null;
        }
    }

    private void recordRuntimeStep(
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

    private String buildAiPrompt(
            String message,
            String currentPath,
            GuideUserSummary summary,
            boolean personalized
    ) {
        String allowedActions = String.join(", ", agentToolRegistry.toolNames());
        String userSummary = personalized && summary != null
                ? "项目数=" + summary.projectCount
                + "，文档数=" + summary.documentCount
                + "，简历数=" + summary.resumeCount
                + "，近30天训练次数=" + summary.recentTrainingCount
                + "，低分维度=" + (summary.lowDimensions.isEmpty() ? "暂无" : String.join("、", summary.lowDimensions))
                + "，最近复盘摘要=" + safePromptText(summary.latestReviewSummary)
                + "，简历风险数量=" + summary.resumeRiskCount
                + "，简历首要风险=" + safePromptText(summary.topResumeRisk)
                : "未登录用户：不能使用任何个人训练、简历、文档、能力画像数据。";
        return """
                你需要理解用户在 CodeCoach AI 产品内的真实意图，并给出可执行的训练引导。

                用户问题：
                %s

                当前页面：
                %s

                当前页面说明：
                %s

                用户摘要：
                %s

                允许 action：
                %s

                输出要求：
                1. 只输出 JSON，不要 Markdown，不要解释 JSON。
                2. JSON 格式：{"answer":"一句到两句中文回答","actions":["ACTION_1","ACTION_2"]}。
                3. answer 必须简洁、具体，最多 90 个中文字符。
                4. actions 最多 3 个，只能从允许 action 中选择。
                5. 不要生成 URL、路径、外部链接、删除/修改类操作。
                6. 如果用户说“面试不知道怎么回答”“不会回答”“不知道从哪里练”，请结合摘要建议进入项目拷打、八股训练、成长洞察或复盘 Agent。
                7. 未登录用户只能解释产品功能并引导 LOGIN。
                """.formatted(
                safePromptText(message),
                safePromptText(normalizePath(currentPath)),
                safePromptText(pageDescription(currentPath)),
                userSummary,
                allowedActions
        );
    }

    private GuideChatResponseVO sanitizeAiSuggestion(GuideAiSuggestion suggestion, boolean personalized) {
        if (suggestion == null || !StringUtils.hasText(suggestion.getAnswer())) {
            return null;
        }
        Set<String> allowedForGuest = Set.of("LOGIN");
        List<GuideActionCardVO> actions = new ArrayList<>();
        if (suggestion.getActions() != null) {
            for (String actionName : suggestion.getActions()) {
                String toolName = parseToolName(actionName);
                if (toolName == null) {
                    continue;
                }
                if (!personalized && !allowedForGuest.contains(toolName)) {
                    continue;
                }
                actions.add(action(toolName));
                if (actions.size() >= 3) {
                    break;
                }
            }
        }
        if (actions.isEmpty()) {
            actions = personalized
                    ? List.of(action("GO_DASHBOARD"), action("GO_AGENT_REVIEW"))
                    : List.of(action("LOGIN"));
        }
        return new GuideChatResponseVO(truncate(suggestion.getAnswer(), 120), personalized, actions);
    }

    private String parseToolName(String actionName) {
        if (!StringUtils.hasText(actionName)) {
            return null;
        }
        String toolName = actionName.trim();
        return agentToolRegistry.getTool(toolName) == null ? null : toolName;
    }

    private GuideUserSummary buildSummary(Long userId) {
        GuideUserSummary summary = new GuideUserSummary();
        LocalDateTime since = LocalDateTime.now().minusDays(RECENT_LIMIT);
        summary.projectCount = countProjects(userId);
        summary.documentCount = countDocuments(userId);
        summary.resumeCount = countResumes(userId);
        summary.recentTrainingCount = countInterviewSessions(userId, since) + countQuestionSessions(userId, since);
        summary.lowDimensions = listLowDimensions(userId);
        fillLatestReview(summary, userId);
        fillResumeRisk(summary, userId);
        return summary;
    }

    private long countProjects(Long userId) {
        return projectMapper.selectCount(new LambdaQueryWrapper<Project>()
                .eq(Project::getUserId, userId)
                .eq(Project::getIsDeleted, 0));
    }

    private long countDocuments(Long userId) {
        return userDocumentMapper.selectCount(new LambdaQueryWrapper<UserDocument>()
                .eq(UserDocument::getUserId, userId)
                .eq(UserDocument::getIsDeleted, 0));
    }

    private long countResumes(Long userId) {
        return resumeProfileMapper.selectCount(new LambdaQueryWrapper<ResumeProfile>()
                .eq(ResumeProfile::getUserId, userId)
                .eq(ResumeProfile::getIsDeleted, 0));
    }

    private long countInterviewSessions(Long userId, LocalDateTime since) {
        return interviewSessionMapper.selectCount(new LambdaQueryWrapper<InterviewSession>()
                .eq(InterviewSession::getUserId, userId)
                .eq(InterviewSession::getIsDeleted, 0)
                .ge(InterviewSession::getCreatedAt, since));
    }

    private long countQuestionSessions(Long userId, LocalDateTime since) {
        return questionTrainingSessionMapper.selectCount(new LambdaQueryWrapper<QuestionTrainingSession>()
                .eq(QuestionTrainingSession::getUserId, userId)
                .eq(QuestionTrainingSession::getIsDeleted, 0)
                .ge(QuestionTrainingSession::getCreatedAt, since));
    }

    private List<String> listLowDimensions(Long userId) {
        List<UserAbilitySnapshot> snapshots = userAbilitySnapshotMapper.selectList(new LambdaQueryWrapper<UserAbilitySnapshot>()
                .eq(UserAbilitySnapshot::getUserId, userId)
                .isNotNull(UserAbilitySnapshot::getScore)
                .orderByAsc(UserAbilitySnapshot::getScore)
                .orderByDesc(UserAbilitySnapshot::getCreatedAt)
                .last("LIMIT " + SNAPSHOT_LIMIT));
        Map<String, Integer> dimensionScores = new LinkedHashMap<>();
        for (UserAbilitySnapshot snapshot : snapshots) {
            String name = firstText(snapshot.getDimensionName(), snapshot.getCategory(), snapshot.getDimensionCode());
            if (!StringUtils.hasText(name)) {
                continue;
            }
            dimensionScores.putIfAbsent(name, snapshot.getScore());
        }
        return dimensionScores.entrySet().stream()
                .filter(entry -> entry.getValue() == null || entry.getValue() < 80)
                .sorted(Map.Entry.comparingByValue(Comparator.nullsLast(Integer::compareTo)))
                .map(Map.Entry::getKey)
                .limit(3)
                .toList();
    }

    private void fillLatestReview(GuideUserSummary summary, Long userId) {
        AgentReview review = agentReviewMapper.selectList(new LambdaQueryWrapper<AgentReview>()
                        .eq(AgentReview::getUserId, userId)
                        .orderByDesc(AgentReview::getCreatedAt)
                        .orderByDesc(AgentReview::getId)
                        .last("LIMIT 1"))
                .stream()
                .findFirst()
                .orElse(null);
        if (review != null) {
            summary.latestReviewSummary = truncate(review.getSummary(), 90);
        }
    }

    private void fillResumeRisk(GuideUserSummary summary, Long userId) {
        ResumeProfile resume = resumeProfileMapper.selectList(new LambdaQueryWrapper<ResumeProfile>()
                        .eq(ResumeProfile::getUserId, userId)
                        .eq(ResumeProfile::getIsDeleted, 0)
                        .eq(ResumeProfile::getAnalysisStatus, "ANALYZED")
                        .orderByDesc(ResumeProfile::getAnalyzedAt)
                        .orderByDesc(ResumeProfile::getId)
                        .last("LIMIT 1"))
                .stream()
                .findFirst()
                .orElse(null);
        if (resume == null || !StringUtils.hasText(resume.getAnalysisResult())) {
            return;
        }
        try {
            ResumeAnalysisResult result = objectMapper.readValue(resume.getAnalysisResult(), ResumeAnalysisResult.class);
            if (result.getRiskPoints() == null || result.getRiskPoints().isEmpty()) {
                return;
            }
            summary.resumeRiskCount = result.getRiskPoints().size();
            summary.topResumeRisk = result.getRiskPoints().stream()
                    .filter(Objects::nonNull)
                    .map(risk -> firstText(risk.getType(), risk.getLevel(), risk.getSuggestion()))
                    .filter(StringUtils::hasText)
                    .findFirst()
                    .map(value -> truncate(value, 40))
                    .orElse(null);
        } catch (Exception ignored) {
            summary.resumeRiskCount = 0;
        }
    }

    private List<GuideActionCardVO> pageActions(String currentPath) {
        String path = normalizePath(currentPath);
        if ("/projects".equals(path)) {
            return List.of(action("GO_PROJECTS"), action("START_PROJECT_TRAINING"));
        }
        if ("/questions".equals(path)) {
            return List.of(action("START_QUESTION_TRAINING"), action("GO_INSIGHTS"));
        }
        if ("/learn".equals(path)) {
            return List.of(action("GO_LEARN"), action("GO_INSIGHTS"));
        }
        if ("/insights".equals(path)) {
            return List.of(action("GO_INSIGHTS"), action("START_QUESTION_TRAINING"));
        }
        if ("/documents".equals(path)) {
            return List.of(action("GO_DOCUMENTS"), action("GO_PROJECTS"));
        }
        if ("/resumes".equals(path)) {
            return List.of(action("ANALYZE_RESUME"), action("START_PROJECT_TRAINING"));
        }
        if ("/agent-review".equals(path)) {
            return List.of(action("GENERATE_AGENT_REVIEW"), action("GO_HISTORY"));
        }
        if ("/history".equals(path)) {
            return List.of(action("GO_HISTORY"), action("GO_AGENT_REVIEW"));
        }
        if ("/profile".equals(path)) {
            return List.of(action("GO_PROFILE"), action("GO_DASHBOARD"));
        }
        if ("/mock-interviews".equals(path)) {
            return List.of(action("START_MOCK_INTERVIEW"), action("GO_INSIGHTS"));
        }
        return List.of(action("GO_DASHBOARD"));
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

    private GuideActionCardVO action(String toolName) {
        AgentTool tool = agentToolRegistry.requireTool(toolName);
        return GuideActionCardVO.fromDefinition(tool.definition());
    }

    private boolean isPageQuestion(String message) {
        return hasAny(message, "这个页面", "当前页面", "页面怎么用", "干什么", "怎么用");
    }

    private boolean hasAny(String message, String... keywords) {
        if (!StringUtils.hasText(message)) {
            return false;
        }
        String normalized = message.toLowerCase(Locale.ROOT);
        for (String keyword : keywords) {
            if (normalized.contains(keyword.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof LoginUser loginUser) {
            return loginUser.getUserId();
        }
        return null;
    }

    private String normalizeMessage(String message) {
        if (!StringUtils.hasText(message)) {
            return "我下一步该做什么？";
        }
        String trimmed = message.trim();
        return trimmed.length() > MESSAGE_LIMIT ? trimmed.substring(0, MESSAGE_LIMIT) : trimmed;
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

    private String resumeRiskSentence(GuideUserSummary summary) {
        if (summary.resumeRiskCount <= 0) {
            return " 暂时没有读取到明确的高风险摘要。";
        }
        if (StringUtils.hasText(summary.topResumeRisk)) {
            return " 最近简历分析里有 " + summary.resumeRiskCount + " 个风险点，优先关注：" + summary.topResumeRisk + "。";
        }
        return " 最近简历分析里有 " + summary.resumeRiskCount + " 个风险点。";
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private String truncate(String text, int limit) {
        if (!StringUtils.hasText(text)) {
            return text;
        }
        String trimmed = text.trim();
        return trimmed.length() > limit ? trimmed.substring(0, limit) + "..." : trimmed;
    }

    private String safePromptText(String text) {
        if (!StringUtils.hasText(text)) {
            return "暂无";
        }
        return text.replaceAll("[\\r\\n]+", " ").trim();
    }

    private static class GuideUserSummary {
        private long projectCount;
        private long documentCount;
        private long resumeCount;
        private long recentTrainingCount;
        private int resumeRiskCount;
        private String topResumeRisk;
        private String latestReviewSummary;
        private List<String> lowDimensions = new ArrayList<>();
    }
}
