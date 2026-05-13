package com.codecoach.module.agent.tool.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.codecoach.common.exception.BusinessException;
import com.codecoach.common.result.ResultCode;
import com.codecoach.module.agent.service.AgentReviewService;
import com.codecoach.module.agent.tool.dto.ToolDefinition;
import com.codecoach.module.agent.tool.dto.ToolExecuteResult;
import com.codecoach.module.agent.tool.enums.ToolDisplayType;
import com.codecoach.module.agent.tool.enums.ToolExecutionMode;
import com.codecoach.module.agent.tool.enums.ToolRiskLevel;
import com.codecoach.module.agent.tool.enums.ToolType;
import com.codecoach.module.agent.tool.service.AgentTool;
import com.codecoach.module.agent.vo.AgentReviewVO;
import com.codecoach.module.insight.entity.UserAbilitySnapshot;
import com.codecoach.module.insight.mapper.UserAbilitySnapshotMapper;
import com.codecoach.module.interview.dto.InterviewSessionCreateRequest;
import com.codecoach.module.interview.entity.InterviewSession;
import com.codecoach.module.interview.mapper.InterviewSessionMapper;
import com.codecoach.module.interview.service.InterviewSessionService;
import com.codecoach.module.interview.vo.InterviewSessionCreateResponse;
import com.codecoach.module.knowledge.entity.KnowledgeTopic;
import com.codecoach.module.knowledge.mapper.KnowledgeTopicMapper;
import com.codecoach.module.memory.service.UserMemoryService;
import com.codecoach.module.memory.model.MemorySemanticHit;
import com.codecoach.module.memory.vo.UserMemoryItemVO;
import com.codecoach.module.memory.vo.UserMemorySummaryVO;
import com.codecoach.module.mockinterview.dto.MockInterviewCreateRequest;
import com.codecoach.module.mockinterview.entity.MockInterviewSession;
import com.codecoach.module.mockinterview.mapper.MockInterviewSessionMapper;
import com.codecoach.module.mockinterview.service.MockInterviewSessionService;
import com.codecoach.module.mockinterview.vo.MockInterviewCreateResponse;
import com.codecoach.module.project.entity.Project;
import com.codecoach.module.project.mapper.ProjectMapper;
import com.codecoach.module.question.dto.QuestionSessionCreateRequest;
import com.codecoach.module.question.entity.QuestionTrainingSession;
import com.codecoach.module.question.mapper.QuestionTrainingSessionMapper;
import com.codecoach.module.question.service.QuestionSessionService;
import com.codecoach.module.question.vo.QuestionSessionCreateResponse;
import com.codecoach.module.rag.constant.RagConstants;
import com.codecoach.module.rag.model.RagRetrievedChunk;
import com.codecoach.module.rag.model.RagSearchRequest;
import com.codecoach.module.rag.model.RagSearchResponse;
import com.codecoach.module.rag.service.RagRetrievalService;
import com.codecoach.module.resume.entity.ResumeProfile;
import com.codecoach.module.resume.mapper.ResumeProfileMapper;
import com.codecoach.module.resume.model.ResumeAnalysisResult;
import com.codecoach.module.resume.service.ResumeService;
import com.codecoach.module.resume.vo.ResumeProfileVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
public class AgentToolConfiguration {

    private static final int NOT_DELETED = 0;
    private static final String STATUS_ENABLED = "ENABLED";

    private final ProjectMapper projectMapper;
    private final KnowledgeTopicMapper knowledgeTopicMapper;
    private final ResumeProfileMapper resumeProfileMapper;
    private final UserAbilitySnapshotMapper userAbilitySnapshotMapper;
    private final InterviewSessionMapper interviewSessionMapper;
    private final QuestionTrainingSessionMapper questionTrainingSessionMapper;
    private final MockInterviewSessionMapper mockInterviewSessionMapper;
    private final InterviewSessionService interviewSessionService;
    private final QuestionSessionService questionSessionService;
    private final MockInterviewSessionService mockInterviewSessionService;
    private final AgentReviewService agentReviewService;
    private final ResumeService resumeService;
    private final RagRetrievalService ragRetrievalService;
    private final UserMemoryService userMemoryService;
    private final ObjectMapper objectMapper;

    public AgentToolConfiguration(
            ProjectMapper projectMapper,
            KnowledgeTopicMapper knowledgeTopicMapper,
            ResumeProfileMapper resumeProfileMapper,
            UserAbilitySnapshotMapper userAbilitySnapshotMapper,
            InterviewSessionMapper interviewSessionMapper,
            QuestionTrainingSessionMapper questionTrainingSessionMapper,
            MockInterviewSessionMapper mockInterviewSessionMapper,
            InterviewSessionService interviewSessionService,
            QuestionSessionService questionSessionService,
            MockInterviewSessionService mockInterviewSessionService,
            AgentReviewService agentReviewService,
            ResumeService resumeService,
            RagRetrievalService ragRetrievalService,
            UserMemoryService userMemoryService,
            ObjectMapper objectMapper
    ) {
        this.projectMapper = projectMapper;
        this.knowledgeTopicMapper = knowledgeTopicMapper;
        this.resumeProfileMapper = resumeProfileMapper;
        this.userAbilitySnapshotMapper = userAbilitySnapshotMapper;
        this.interviewSessionMapper = interviewSessionMapper;
        this.questionTrainingSessionMapper = questionTrainingSessionMapper;
        this.mockInterviewSessionMapper = mockInterviewSessionMapper;
        this.interviewSessionService = interviewSessionService;
        this.questionSessionService = questionSessionService;
        this.mockInterviewSessionService = mockInterviewSessionService;
        this.agentReviewService = agentReviewService;
        this.resumeService = resumeService;
        this.ragRetrievalService = ragRetrievalService;
        this.userMemoryService = userMemoryService;
        this.objectMapper = objectMapper;
    }

    @Bean
    public AgentTool goDashboardTool() {
        return navigation("GO_DASHBOARD", "回到工作台", "查看你的训练入口和最近进展", "/dashboard");
    }

    @Bean
    public AgentTool goProjectsTool() {
        return navigation("GO_PROJECTS", "项目档案", "管理项目并开始项目拷打", "/projects");
    }

    @Bean
    public AgentTool goQuestionsTool() {
        return navigation("GO_QUESTIONS", "八股问答", "练习 Java 后端基础题", "/questions");
    }

    @Bean
    public AgentTool goLearnTool() {
        return navigation("GO_LEARN", "知识学习", "按主题补齐知识点", "/learn");
    }

    @Bean
    public AgentTool goInsightsTool() {
        return navigation("GO_INSIGHTS", "成长洞察", "查看能力画像和薄弱维度", "/insights");
    }

    @Bean
    public AgentTool goDocumentsTool() {
        return navigation("GO_DOCUMENTS", "我的文档", "上传文档并用于训练增强", "/documents");
    }

    @Bean
    public AgentTool goResumesTool() {
        return navigation("GO_RESUMES", "简历训练", "分析简历风险和项目追问点", "/resumes");
    }

    @Bean
    public AgentTool goAgentReviewTool() {
        return navigation("GO_AGENT_REVIEW", "复盘 Agent", "系统总结最近训练问题", "/agent-review");
    }

    @Bean
    public AgentTool goMockInterviewsTool() {
        return navigation("GO_MOCK_INTERVIEWS", "模拟面试", "进入完整技术面模拟", "/mock-interviews");
    }

    @Bean
    public AgentTool goHistoryTool() {
        return navigation("GO_HISTORY", "训练历史", "查看最近训练和报告", "/history");
    }

    @Bean
    public AgentTool goProfileTool() {
        return navigation("GO_PROFILE", "个人中心", "维护账号和个人信息", "/profile");
    }

    @Bean
    public AgentTool loginTool() {
        return navigation("LOGIN", "登录后继续", "解锁个性化训练建议", "/login");
    }

    @Bean
    public AgentTool getAbilitySummaryTool() {
        return query("GET_ABILITY_SUMMARY", "查看能力画像摘要", "获取最近薄弱维度和能力快照", "/insights",
                (userId, params) -> {
                    List<UserAbilitySnapshot> snapshots = userAbilitySnapshotMapper.selectList(new LambdaQueryWrapper<UserAbilitySnapshot>()
                            .eq(UserAbilitySnapshot::getUserId, userId)
                            .orderByDesc(UserAbilitySnapshot::getCreatedAt)
                            .orderByDesc(UserAbilitySnapshot::getId)
                            .last("LIMIT 8"));
                    List<Map<String, Object>> dimensions = snapshots.stream()
                            .map(snapshot -> mapOf(
                                    "dimension", firstText(snapshot.getDimensionName(), snapshot.getCategory(), snapshot.getDimensionCode()),
                                    "score", snapshot.getScore(),
                                    "createdAt", snapshot.getCreatedAt()))
                            .toList();
                    String message = dimensions.isEmpty()
                            ? "暂时没有能力画像数据，先完成一轮训练后再查看。"
                            : "已获取最近能力画像摘要。";
                    return ToolExecuteResult.success(message, mapOf("dimensions", dimensions), "/insights", ToolDisplayType.SUMMARY);
                });
    }

    @Bean
    public AgentTool getRecentTrainingSummaryTool() {
        return query("GET_RECENT_TRAINING_SUMMARY", "查看最近训练摘要", "汇总最近项目、八股和模拟面试训练", "/history",
                (userId, params) -> {
                    LocalDateTime since = LocalDateTime.now().minusDays(30);
                    long projectCount = interviewSessionMapper.selectCount(new LambdaQueryWrapper<InterviewSession>()
                            .eq(InterviewSession::getUserId, userId)
                            .eq(InterviewSession::getIsDeleted, NOT_DELETED)
                            .ge(InterviewSession::getCreatedAt, since));
                    long questionCount = questionTrainingSessionMapper.selectCount(new LambdaQueryWrapper<QuestionTrainingSession>()
                            .eq(QuestionTrainingSession::getUserId, userId)
                            .eq(QuestionTrainingSession::getIsDeleted, NOT_DELETED)
                            .ge(QuestionTrainingSession::getCreatedAt, since));
                    long mockCount = mockInterviewSessionMapper.selectCount(new LambdaQueryWrapper<MockInterviewSession>()
                            .eq(MockInterviewSession::getUserId, userId)
                            .eq(MockInterviewSession::getIsDeleted, NOT_DELETED)
                            .ge(MockInterviewSession::getCreatedAt, since));
                    long total = projectCount + questionCount + mockCount;
                    return ToolExecuteResult.success(
                            total == 0 ? "近 30 天还没有训练记录。" : "已获取近 30 天训练摘要。",
                            mapOf("projectTrainingCount", projectCount, "questionTrainingCount", questionCount, "mockInterviewCount", mockCount, "total", total),
                            "/history",
                            ToolDisplayType.SUMMARY);
                });
    }

    @Bean
    public AgentTool getUserMemorySummaryTool() {
        return query("GET_USER_MEMORY_SUMMARY", "查看长期记忆摘要", "读取当前用户长期训练记忆摘要", "/insights",
                (userId, params) -> {
                    UserMemorySummaryVO summary = userMemoryService.getSummary(userId);
                    Map<String, Object> data = mapOf(
                            "targetRole", summary.getTargetRole(),
                            "topWeaknesses", toMemorySummaryItems(summary.getTopWeaknesses()),
                            "topResumeRisks", toMemorySummaryItems(summary.getTopResumeRisks()),
                            "topProjectRisks", toMemorySummaryItems(summary.getTopProjectRisks()),
                            "recentNextActions", toMemorySummaryItems(summary.getRecentNextActions()),
                            "masteredTopics", toMemorySummaryItems(summary.getMasteredTopics()),
                            "semanticRecall", semanticMemoryRecall(userId, params),
                            "empty", summary.isEmpty());
                    return ToolExecuteResult.success(
                            summary.isEmpty() ? "暂时没有长期训练记忆摘要。" : "已获取长期训练记忆摘要。",
                            data,
                            "/insights",
                            ToolDisplayType.SUMMARY);
                });
    }

    @Bean
    public AgentTool getResumeRiskSummaryTool() {
        return query("GET_RESUME_RISK_SUMMARY", "查看简历风险摘要", "读取最近一次简历分析风险点", "/resumes",
                (userId, params) -> {
                    ResumeProfile resume = latestAnalyzedResume(userId);
                    if (resume == null || !StringUtils.hasText(resume.getAnalysisResult())) {
                        return ToolExecuteResult.success("暂时没有可用的简历风险摘要。", mapOf("riskCount", 0), "/resumes", ToolDisplayType.SUMMARY);
                    }
                    ResumeAnalysisResult result = objectMapper.readValue(resume.getAnalysisResult(), ResumeAnalysisResult.class);
                    int riskCount = result.getRiskPoints() == null ? 0 : result.getRiskPoints().size();
                    String topRisk = result.getRiskPoints() == null || result.getRiskPoints().isEmpty()
                            ? null
                            : firstText(result.getRiskPoints().get(0).getType(), result.getRiskPoints().get(0).getLevel(), result.getRiskPoints().get(0).getSuggestion());
                    return ToolExecuteResult.success(
                            riskCount == 0 ? "最近简历分析没有明显风险点。" : "已获取最近简历风险摘要。",
                            mapOf("resumeId", resume.getId(), "riskCount", riskCount, "topRisk", topRisk),
                            "/resumes",
                            ToolDisplayType.SUMMARY);
                });
    }

    @Bean
    public AgentTool searchKnowledgeTool() {
        return searchTool("SEARCH_KNOWLEDGE", "检索知识文章", "从站内知识文章中检索相关内容", "/learn", RagConstants.SOURCE_TYPE_KNOWLEDGE_ARTICLE);
    }

    @Bean
    public AgentTool searchUserDocumentsTool() {
        return searchTool("SEARCH_USER_DOCUMENTS", "检索用户文档", "从当前用户上传文档中检索相关内容", "/documents", RagConstants.SOURCE_TYPE_USER_UPLOAD);
    }

    @Bean
    public AgentTool startProjectTrainingTool() {
        return command("START_PROJECT_TRAINING", "开始项目拷打", "从项目档案选择一个项目训练", "/projects",
                (userId, params) -> validateProject(userId, AgentToolSupport.longParam(params, "projectId")),
                (userId, params) -> {
                    Project project = resolveProject(userId, AgentToolSupport.longParam(params, "projectId"));
                    if (project == null) {
                        return ToolExecuteResult.success("你还没有项目档案，先创建一个项目后再开始训练。", mapOf(), "/projects", ToolDisplayType.NAVIGATION);
                    }
                    InterviewSessionCreateRequest request = new InterviewSessionCreateRequest();
                    request.setProjectId(project.getId());
                    request.setTargetRole(AgentToolSupport.stringParam(params, "targetRole", AgentToolSupport.DEFAULT_TARGET_ROLE));
                    request.setDifficulty(AgentToolSupport.stringParam(params, "difficulty", AgentToolSupport.DEFAULT_DIFFICULTY));
                    InterviewSessionCreateResponse response = interviewSessionService.createSession(request);
                    return ToolExecuteResult.success(
                            "项目拷打训练已创建。",
                            mapOf("sessionId", response.getSessionId(), "projectId", project.getId()),
                            "/interviews/" + response.getSessionId(),
                            ToolDisplayType.SESSION_CREATED);
                });
    }

    @Bean
    public AgentTool startQuestionTrainingTool() {
        return command("START_QUESTION_TRAINING", "开始八股训练", "选择主题开启一轮问答", "/questions",
                (userId, params) -> validateTopic(AgentToolSupport.longParam(params, "topicId")),
                (userId, params) -> {
                    KnowledgeTopic topic = resolveTopic(AgentToolSupport.longParam(params, "topicId"));
                    if (topic == null) {
                        return ToolExecuteResult.success("暂时没有可用知识点，先进入八股问答页查看。", mapOf(), "/questions", ToolDisplayType.NAVIGATION);
                    }
                    QuestionSessionCreateRequest request = new QuestionSessionCreateRequest();
                    request.setTopicId(topic.getId());
                    request.setTargetRole(AgentToolSupport.stringParam(params, "targetRole", AgentToolSupport.DEFAULT_TARGET_ROLE));
                    request.setDifficulty(AgentToolSupport.stringParam(params, "difficulty", AgentToolSupport.DEFAULT_DIFFICULTY));
                    QuestionSessionCreateResponse response = questionSessionService.createSession(request);
                    return ToolExecuteResult.success(
                            "八股训练已创建。",
                            mapOf("sessionId", response.getSessionId(), "topicId", response.getTopicId()),
                            "/question-sessions/" + response.getSessionId(),
                            ToolDisplayType.SESSION_CREATED);
                });
    }

    @Bean
    public AgentTool startMockInterviewTool() {
        return command("START_MOCK_INTERVIEW", "开始模拟面试", "开启一场综合技术一面", "/mock-interviews",
                (userId, params) -> {
                    validateProject(userId, AgentToolSupport.longParam(params, "projectId"));
                    validateResume(userId, AgentToolSupport.longParam(params, "resumeId"));
                },
                (userId, params) -> {
                    Project project = resolveProject(userId, AgentToolSupport.longParam(params, "projectId"));
                    MockInterviewCreateRequest request = new MockInterviewCreateRequest();
                    request.setInterviewType(AgentToolSupport.stringParam(params, "interviewType", "COMPREHENSIVE_TECHNICAL"));
                    request.setTargetRole(AgentToolSupport.stringParam(params, "targetRole", AgentToolSupport.DEFAULT_TARGET_ROLE));
                    request.setDifficulty(AgentToolSupport.stringParam(params, "difficulty", AgentToolSupport.DEFAULT_DIFFICULTY));
                    request.setMaxRound(AgentToolSupport.intParam(params, "maxRound", 6));
                    request.setProjectId(project == null ? null : project.getId());
                    request.setResumeId(AgentToolSupport.longParam(params, "resumeId"));
                    MockInterviewCreateResponse response = mockInterviewSessionService.createSession(request);
                    return ToolExecuteResult.success(
                            "模拟面试已创建。",
                            mapOf("sessionId", response.getSessionId(), "projectId", request.getProjectId()),
                            "/mock-interviews/" + response.getSessionId(),
                            ToolDisplayType.SESSION_CREATED);
                });
    }

    @Bean
    public AgentTool generateAgentReviewTool() {
        return command("GENERATE_AGENT_REVIEW", "生成复盘", "进入复盘 Agent 汇总问题", "/agent-review",
                null,
                (userId, params) -> {
                    AgentReviewVO review = agentReviewService.generateReview(AgentToolSupport.stringParam(params, "scopeType", "RECENT_10"));
                    return ToolExecuteResult.success(
                            "复盘已生成。",
                            mapOf("reviewId", review.getId(), "summary", review.getSummary()),
                            "/agent-review",
                            ToolDisplayType.REVIEW_CREATED);
                });
    }

    @Bean
    public AgentTool analyzeResumeTool() {
        return command("ANALYZE_RESUME", "分析简历", "进入简历训练并查看风险点", "/resumes",
                (userId, params) -> validateResume(userId, AgentToolSupport.longParam(params, "resumeId")),
                (userId, params) -> {
                    Long resumeId = AgentToolSupport.longParam(params, "resumeId");
                    if (resumeId == null) {
                        return ToolExecuteResult.success("先进入简历训练选择一份简历再分析。", mapOf(), "/resumes", ToolDisplayType.NAVIGATION);
                    }
                    ResumeProfileVO resume = resumeService.analyze(resumeId);
                    return ToolExecuteResult.success(
                            "简历分析已完成。",
                            mapOf("resumeId", resume.getId(), "analysisStatus", resume.getAnalysisStatus()),
                            "/resumes",
                            ToolDisplayType.SUMMARY);
                });
    }

    @Bean
    public AgentTool createProjectFromResumeTool() {
        return new SimpleAgentTool(
                definition("CREATE_PROJECT_FROM_RESUME", ToolType.COMMAND, ToolRiskLevel.MEDIUM, ToolExecutionMode.SUGGEST_ONLY,
                        ToolDisplayType.NAVIGATION, "从简历生成项目档案", "进入简历训练生成项目草稿", "/resumes", true, false, Map.of()),
                null,
                (userId, params) -> ToolExecuteResult.success("请进入简历训练页选择项目经历生成草稿。", mapOf(), "/resumes", ToolDisplayType.NAVIGATION)
        );
    }

    private AgentTool navigation(String name, String title, String description, String targetPath) {
        return new SimpleAgentTool(
                definition(name, ToolType.NAVIGATION, ToolRiskLevel.LOW, ToolExecutionMode.SUGGEST_ONLY,
                        ToolDisplayType.NAVIGATION, title, description, targetPath, true, false, Map.of()),
                null,
                (userId, params) -> ToolExecuteResult.success(title, mapOf(), targetPath, ToolDisplayType.NAVIGATION)
        );
    }

    private AgentTool query(String name, String title, String description, String targetPath, ToolFunction executor) {
        return new SimpleAgentTool(
                definition(name, ToolType.QUERY, ToolRiskLevel.LOW, ToolExecutionMode.AUTO_EXECUTE,
                        ToolDisplayType.SUMMARY, title, description, targetPath, true, false, Map.of()),
                null,
                (userId, params) -> executeToolFunction(executor, userId, params)
        );
    }

    private AgentTool command(String name, String title, String description, String targetPath, ToolValidator validator, ToolFunction executor) {
        return new SimpleAgentTool(
                definition(name, ToolType.COMMAND, ToolRiskLevel.MEDIUM, ToolExecutionMode.EXECUTE_AFTER_CONFIRM,
                        ToolDisplayType.NAVIGATION, title, description, targetPath, true, true, Map.of()),
                validator == null ? null : validator::validate,
                (userId, params) -> executeToolFunction(executor, userId, params)
        );
    }

    private AgentTool searchTool(String name, String title, String description, String targetPath, String sourceType) {
        return query(name, title, description, targetPath, (userId, params) -> {
            String query = AgentToolSupport.stringParam(params, "query", null);
            if (!StringUtils.hasText(query)) {
                return ToolExecuteResult.failure("请输入要检索的内容。", "QUERY_REQUIRED", ToolDisplayType.ERROR);
            }
            RagSearchRequest request = new RagSearchRequest();
            request.setQuery(query);
            request.setTopK(Math.min(AgentToolSupport.intParam(params, "topK", 5), 8));
            request.setSourceTypes(List.of(sourceType));
            RagSearchResponse response = ragRetrievalService.search(request);
            List<Map<String, Object>> chunks = response.getChunks() == null
                    ? List.of()
                    : response.getChunks().stream().map(this::toSearchResult).toList();
            return ToolExecuteResult.success(
                    chunks.isEmpty() ? "没有检索到相关内容。" : "已检索到相关内容。",
                    mapOf("query", query, "resultCount", chunks.size(), "chunks", chunks),
                    targetPath,
                    ToolDisplayType.SUMMARY);
        });
    }

    private ToolDefinition definition(
            String name,
            ToolType type,
            ToolRiskLevel riskLevel,
            ToolExecutionMode executionMode,
            ToolDisplayType displayType,
            String title,
            String description,
            String targetPath,
            boolean requiresLogin,
            boolean requiresConfirmation,
            Map<String, Object> defaultParams
    ) {
        return new ToolDefinition(name, type, riskLevel, executionMode, displayType, title, description, targetPath,
                requiresLogin, requiresConfirmation, defaultParams);
    }

    private ToolExecuteResult executeToolFunction(ToolFunction function, Long userId, Map<String, Object> params) {
        try {
            return function.apply(userId, params);
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR.getCode(), "Tool 执行失败，请稍后重试");
        }
    }

    private Project resolveProject(Long userId, Long projectId) {
        if (projectId != null) {
            Project project = projectMapper.selectById(projectId);
            if (project == null || Integer.valueOf(1).equals(project.getIsDeleted())) {
                throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "项目不存在");
            }
            if (!userId.equals(project.getUserId())) {
                throw new BusinessException(ResultCode.FORBIDDEN);
            }
            return project;
        }
        return projectMapper.selectList(new LambdaQueryWrapper<Project>()
                        .eq(Project::getUserId, userId)
                        .eq(Project::getIsDeleted, NOT_DELETED)
                        .orderByDesc(Project::getUpdatedAt)
                        .orderByDesc(Project::getId)
                        .last("LIMIT 1"))
                .stream()
                .findFirst()
                .orElse(null);
    }

    private void validateProject(Long userId, Long projectId) {
        if (projectId != null) {
            resolveProject(userId, projectId);
        }
    }

    private KnowledgeTopic resolveTopic(Long topicId) {
        if (topicId != null) {
            KnowledgeTopic topic = knowledgeTopicMapper.selectById(topicId);
            if (topic == null
                    || Integer.valueOf(1).equals(topic.getIsDeleted())
                    || !STATUS_ENABLED.equals(topic.getStatus())) {
                throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "知识点不存在");
            }
            return topic;
        }
        return knowledgeTopicMapper.selectList(new LambdaQueryWrapper<KnowledgeTopic>()
                        .eq(KnowledgeTopic::getIsDeleted, NOT_DELETED)
                        .eq(KnowledgeTopic::getStatus, STATUS_ENABLED)
                        .orderByAsc(KnowledgeTopic::getSortOrder)
                        .orderByAsc(KnowledgeTopic::getId)
                        .last("LIMIT 1"))
                .stream()
                .findFirst()
                .orElse(null);
    }

    private void validateTopic(Long topicId) {
        if (topicId != null) {
            resolveTopic(topicId);
        }
    }

    private void validateResume(Long userId, Long resumeId) {
        if (resumeId == null) {
            return;
        }
        ResumeProfile resume = resumeProfileMapper.selectById(resumeId);
        if (resume == null || Integer.valueOf(1).equals(resume.getIsDeleted())) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "简历不存在");
        }
        if (!userId.equals(resume.getUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
    }

    private ResumeProfile latestAnalyzedResume(Long userId) {
        return resumeProfileMapper.selectList(new LambdaQueryWrapper<ResumeProfile>()
                        .eq(ResumeProfile::getUserId, userId)
                        .eq(ResumeProfile::getIsDeleted, NOT_DELETED)
                        .eq(ResumeProfile::getAnalysisStatus, "ANALYZED")
                        .orderByDesc(ResumeProfile::getAnalyzedAt)
                        .orderByDesc(ResumeProfile::getId)
                        .last("LIMIT 1"))
                .stream()
                .findFirst()
                .orElse(null);
    }

    private Map<String, Object> toSearchResult(RagRetrievedChunk chunk) {
        return mapOf(
                "chunkId", chunk.getChunkId(),
                "documentId", chunk.getDocumentId(),
                "title", chunk.getTitle(),
                "sourceType", chunk.getSourceType(),
                "score", chunk.getScore(),
                "section", chunk.getSection(),
                "articleId", chunk.getArticleId(),
                "topicId", chunk.getTopicId());
    }

    private List<Map<String, Object>> toMemorySummaryItems(List<UserMemoryItemVO> items) {
        if (items == null) {
            return List.of();
        }
        return items.stream()
                .map(item -> mapOf(
                        "value", item.getValue(),
                        "confidence", item.getConfidence(),
                        "weight", item.getWeight(),
                        "lastReinforcedAt", item.getLastReinforcedAt()))
                .toList();
    }

    private List<Map<String, Object>> semanticMemoryRecall(Long userId, Map<String, Object> params) {
        String query = AgentToolSupport.stringParam(params, "query", null);
        if (!StringUtils.hasText(query)) {
            return List.of();
        }
        int topK = Math.min(AgentToolSupport.intParam(params, "topK", 3), 5);
        List<MemorySemanticHit> hits = userMemoryService.semanticSearch(userId, query, topK);
        return hits.stream()
                .map(hit -> mapOf(
                        "memoryId", hit.memoryId(),
                        "memoryType", hit.memoryType(),
                        "value", hit.value(),
                        "confidence", hit.confidence(),
                        "weight", hit.weight(),
                        "score", hit.score()))
                .toList();
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private Map<String, Object> mapOf(Object... keysAndValues) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < keysAndValues.length; i += 2) {
            map.put(String.valueOf(keysAndValues[i]), keysAndValues[i + 1]);
        }
        return map;
    }

    @FunctionalInterface
    private interface ToolFunction {
        ToolExecuteResult apply(Long userId, Map<String, Object> params) throws Exception;
    }

    @FunctionalInterface
    private interface ToolValidator {
        void validate(Long userId, Map<String, Object> params);
    }
}
