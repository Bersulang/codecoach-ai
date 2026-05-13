package com.codecoach.module.agent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.codecoach.common.exception.BusinessException;
import com.codecoach.common.concurrency.SingleFlightService;
import com.codecoach.common.result.ResultCode;
import com.codecoach.module.agent.entity.AgentReview;
import com.codecoach.module.agent.mapper.AgentReviewMapper;
import com.codecoach.module.agent.model.AgentReviewContext;
import com.codecoach.module.agent.model.AgentReviewResult;
import com.codecoach.module.agent.multi.MultiAgentCoordinator;
import com.codecoach.module.agent.runtime.enums.AgentRunStatus;
import com.codecoach.module.agent.runtime.enums.AgentStepStatus;
import com.codecoach.module.agent.service.AgentReviewService;
import com.codecoach.module.agent.service.AiAgentReviewService;
import com.codecoach.module.agent.tool.dto.ToolDefinition;
import com.codecoach.module.agent.tool.dto.ToolExecuteResult;
import com.codecoach.module.agent.tool.enums.ToolDisplayType;
import com.codecoach.module.agent.tool.enums.ToolExecutionMode;
import com.codecoach.module.agent.tool.enums.ToolRiskLevel;
import com.codecoach.module.agent.tool.enums.ToolType;
import com.codecoach.module.agent.tool.service.AgentToolTraceService;
import com.codecoach.module.agent.vo.AgentReviewListItemVO;
import com.codecoach.module.agent.vo.AgentReviewVO;
import com.codecoach.module.agent.vo.NextActionVO;
import com.codecoach.module.insight.entity.UserAbilitySnapshot;
import com.codecoach.module.insight.mapper.UserAbilitySnapshotMapper;
import com.codecoach.module.memory.vo.UserMemoryItemVO;
import com.codecoach.module.memory.vo.UserMemorySummaryVO;
import com.codecoach.module.mockinterview.entity.MockInterviewMessage;
import com.codecoach.module.mockinterview.entity.MockInterviewReport;
import com.codecoach.module.mockinterview.mapper.MockInterviewMessageMapper;
import com.codecoach.module.mockinterview.mapper.MockInterviewReportMapper;
import com.codecoach.module.memory.service.UserMemoryService;
import com.codecoach.module.question.entity.QuestionTrainingReport;
import com.codecoach.module.question.mapper.QuestionTrainingReportMapper;
import com.codecoach.module.rag.constant.RagConstants;
import com.codecoach.module.rag.model.RagRetrievedChunk;
import com.codecoach.module.rag.model.RagSearchRequest;
import com.codecoach.module.rag.model.RagSearchResponse;
import com.codecoach.module.rag.service.RagRetrievalService;
import com.codecoach.module.report.entity.InterviewReport;
import com.codecoach.module.report.mapper.InterviewReportMapper;
import com.codecoach.module.resume.entity.ResumeProfile;
import com.codecoach.module.resume.mapper.ResumeProfileMapper;
import com.codecoach.module.resume.model.ResumeAnalysisResult;
import com.codecoach.security.UserContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AgentReviewServiceImpl implements AgentReviewService {

    private static final Logger log = LoggerFactory.getLogger(AgentReviewServiceImpl.class);

    private static final int REVIEW_NOT_FOUND_CODE = 8001;
    private static final int REVIEW_GENERATING_CODE = 8002;
    private static final String SCOPE_RECENT_10 = "RECENT_10";
    private static final int RECENT_REPORT_LIMIT = 10;
    private static final int RECENT_MOCK_REPORT_LIMIT = 5;
    private static final int SNAPSHOT_LIMIT = 50;
    private static final int HISTORY_LIMIT = 20;
    private static final int RAG_LIMIT = 5;
    private static final Duration REVIEW_LOCK_TTL = Duration.ofMinutes(5);
    private static final Duration REVIEW_CACHE_TTL = Duration.ofMinutes(3);
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<List<NextActionVO>> NEXT_ACTION_LIST_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<List<Map<String, Object>>> MAP_LIST_TYPE = new TypeReference<>() {
    };

    private final AgentReviewMapper agentReviewMapper;
    private final InterviewReportMapper interviewReportMapper;
    private final QuestionTrainingReportMapper questionTrainingReportMapper;
    private final MockInterviewReportMapper mockInterviewReportMapper;
    private final MockInterviewMessageMapper mockInterviewMessageMapper;
    private final UserAbilitySnapshotMapper userAbilitySnapshotMapper;
    private final ResumeProfileMapper resumeProfileMapper;
    private final RagRetrievalService ragRetrievalService;
    private final AiAgentReviewService aiAgentReviewService;
    private final ObjectMapper objectMapper;
    private final UserMemoryService userMemoryService;
    private final MultiAgentCoordinator multiAgentCoordinator;
    private final AgentToolTraceService agentToolTraceService;
    private final StringRedisTemplate stringRedisTemplate;
    private final SingleFlightService singleFlightService;

    public AgentReviewServiceImpl(
            AgentReviewMapper agentReviewMapper,
            InterviewReportMapper interviewReportMapper,
            QuestionTrainingReportMapper questionTrainingReportMapper,
            MockInterviewReportMapper mockInterviewReportMapper,
            MockInterviewMessageMapper mockInterviewMessageMapper,
            UserAbilitySnapshotMapper userAbilitySnapshotMapper,
            ResumeProfileMapper resumeProfileMapper,
            RagRetrievalService ragRetrievalService,
            AiAgentReviewService aiAgentReviewService,
            ObjectMapper objectMapper,
            UserMemoryService userMemoryService,
            MultiAgentCoordinator multiAgentCoordinator,
            AgentToolTraceService agentToolTraceService,
            StringRedisTemplate stringRedisTemplate,
            SingleFlightService singleFlightService
    ) {
        this.agentReviewMapper = agentReviewMapper;
        this.interviewReportMapper = interviewReportMapper;
        this.questionTrainingReportMapper = questionTrainingReportMapper;
        this.mockInterviewReportMapper = mockInterviewReportMapper;
        this.mockInterviewMessageMapper = mockInterviewMessageMapper;
        this.userAbilitySnapshotMapper = userAbilitySnapshotMapper;
        this.resumeProfileMapper = resumeProfileMapper;
        this.ragRetrievalService = ragRetrievalService;
        this.aiAgentReviewService = aiAgentReviewService;
        this.objectMapper = objectMapper;
        this.userMemoryService = userMemoryService;
        this.multiAgentCoordinator = multiAgentCoordinator;
        this.agentToolTraceService = agentToolTraceService;
        this.stringRedisTemplate = stringRedisTemplate;
        this.singleFlightService = singleFlightService;
    }

    @Override
    @Transactional
    public AgentReviewVO generateReview(String scopeType) {
        Long userId = UserContext.getCurrentUserId();
        String normalizedScope = normalizeScope(scopeType);
        String requestKey = "agent_review:" + userId + ":" + normalizedScope;
        AgentReview review = singleFlightService.execute(
                requestKey,
                REVIEW_LOCK_TTL,
                REVIEW_CACHE_TTL,
                AgentReview.class,
                () -> generateReviewInternal(userId, normalizedScope, requestKey),
                () -> {
                    AgentReview cached = latestCachedReview(userId, normalizedScope);
                    return cached != null ? cached : latestReview(userId, normalizedScope);
                },
                REVIEW_GENERATING_CODE,
                "综合复盘正在生成中，请稍后刷新。"
        );
        return toVO(review);
    }

    private AgentReview generateReviewInternal(Long userId, String normalizedScope, String requestKey) {
        MultiAgentCoordinator.Session session = multiAgentCoordinator.start(userId, "scopeType=" + normalizedScope);
        try {
            long retrieveStart = System.currentTimeMillis();
            ReviewInput input = buildReviewInput(userId, normalizedScope, session);
            multiAgentCoordinator.recordRole(session, MultiAgentCoordinator.MultiAgentRole.RETRIEVER,
                    "scopeType=" + normalizedScope,
                    "projectReports=" + input.projectReportCount() + ", questionReports=" + input.questionReportCount()
                            + ", mockReports=" + input.mockReportCount() + ", snapshots=" + input.snapshotCount()
                            + ", resumeRisks=" + input.resumeRiskCount() + ", memories=" + input.memoryCount()
                            + ", ragArticles=" + input.ragArticles().size() + ", ragDocuments=" + input.ragDocuments().size(),
                    AgentStepStatus.SUCCEEDED,
                    elapsed(retrieveStart));

            long evaluateStart = System.currentTimeMillis();
            AgentReviewResult result = input.hasCoreData()
                    ? aiAgentReviewService.generateReview(input.context())
                    : buildLowDataResult();
            normalizeResult(result, input);
            multiAgentCoordinator.recordRole(session, MultiAgentCoordinator.MultiAgentRole.EVALUATOR,
                    "hasCoreData=" + input.hasCoreData(),
                    "weaknesses=" + safeSize(result.getRecurringWeaknesses()) + ", nextActions=" + safeSize(result.getNextActions()),
                    AgentStepStatus.SUCCEEDED,
                    elapsed(evaluateStart));

            long coachStart = System.currentTimeMillis();
            AgentReview review = new AgentReview();
            review.setUserId(userId);
            review.setScopeType(normalizedScope);
            review.setSummary(result.getSummary());
            review.setScoreOverview(toJson(result.getScoreOverview()));
            review.setRadarDimensions(toJson(result.getRadarDimensions()));
            review.setKeyFindings(toJson(result.getKeyFindings()));
            review.setRecurringWeaknesses(toJson(result.getRecurringWeaknesses()));
            review.setHighRiskAnswers(toJson(result.getHighRiskAnswers()));
            review.setStagePerformance(toJson(result.getStagePerformance()));
            review.setQaReplay(toJson(result.getQaReplay()));
            review.setCauseAnalysis(toJson(result.getCauseAnalysis()));
            review.setResumeRisks(toJson(result.getResumeRisks()));
            review.setNextActions(toJson(toNextActionVOs(result.getNextActions())));
            review.setRecommendedArticles(toJson(result.getRecommendedArticles()));
            review.setRecommendedTrainings(toJson(result.getRecommendedTrainings()));
            review.setMemoryUpdates(toJson(result.getMemoryUpdates()));
            review.setConfidence(normalizeConfidence(result.getConfidence(), input));
            review.setSampleQuality(normalizeSampleQuality(result.getSampleQuality(), input));
            review.setSourceSnapshot(input.context().getSourceSnapshotJson());
            review.setCreatedAt(LocalDateTime.now());
            agentReviewMapper.insert(review);
            try {
                userMemoryService.sinkAgentReview(review);
                userMemoryService.indexActiveSemanticMemory(userId);
            } catch (Exception exception) {
                log.warn("Agent review memory sink failed, reviewId={}: {}", review.getId(), abbreviate(exception.getMessage(), 200));
            }
            multiAgentCoordinator.recordRole(session, MultiAgentCoordinator.MultiAgentRole.COACH,
                    "reviewId=" + review.getId(),
                    result.getSummary(),
                    AgentStepStatus.SUCCEEDED,
                    elapsed(coachStart));
            multiAgentCoordinator.finish(session, AgentRunStatus.SUCCEEDED, result.getSummary(), null, null);
            cacheReviewId(requestKey, review.getId());
            return review;
        } catch (RuntimeException exception) {
            multiAgentCoordinator.finish(session, AgentRunStatus.FAILED, null, "AGENT_REVIEW_MULTI_FAILED", exception.getMessage());
            throw exception;
        }
    }

    @Override
    public List<AgentReviewListItemVO> listReviews(Integer limit) {
        Long userId = UserContext.getCurrentUserId();
        int normalizedLimit = limit == null || limit < 1 ? HISTORY_LIMIT : Math.min(limit, 50);
        return agentReviewMapper.selectList(new LambdaQueryWrapper<AgentReview>()
                        .eq(AgentReview::getUserId, userId)
                        .orderByDesc(AgentReview::getCreatedAt)
                        .orderByDesc(AgentReview::getId)
                        .last("LIMIT " + normalizedLimit))
                .stream()
                .map(review -> new AgentReviewListItemVO(
                        review.getId(),
                        review.getScopeType(),
                        review.getSummary(),
                        review.getConfidence(),
                        review.getCreatedAt()
                ))
                .toList();
    }

    @Override
    public AgentReviewVO getReview(Long reviewId) {
        Long userId = UserContext.getCurrentUserId();
        AgentReview review = agentReviewMapper.selectById(reviewId);
        if (review == null) {
            throw new BusinessException(REVIEW_NOT_FOUND_CODE, "复盘不存在");
        }
        if (!userId.equals(review.getUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        return toVO(review);
    }

    private AgentReview latestCachedReview(Long userId, String scopeType) {
        try {
            String reviewId = stringRedisTemplate.opsForValue().get(cacheKey(userId, scopeType));
            if (!StringUtils.hasText(reviewId)) {
                return null;
            }
            AgentReview review = agentReviewMapper.selectById(Long.parseLong(reviewId));
            return review != null && userId.equals(review.getUserId()) ? review : null;
        } catch (Exception exception) {
            return null;
        }
    }

    private AgentReview latestReview(Long userId, String scopeType) {
        return agentReviewMapper.selectList(new LambdaQueryWrapper<AgentReview>()
                        .eq(AgentReview::getUserId, userId)
                        .eq(AgentReview::getScopeType, scopeType)
                        .orderByDesc(AgentReview::getCreatedAt)
                        .orderByDesc(AgentReview::getId)
                        .last("LIMIT 1"))
                .stream()
                .findFirst()
                .orElse(null);
    }

    private void cacheReviewId(String requestKey, Long reviewId) {
        if (reviewId == null) {
            return;
        }
        try {
            stringRedisTemplate.opsForValue().set(cacheKey(requestKey), String.valueOf(reviewId), REVIEW_CACHE_TTL);
        } catch (Exception exception) {
            log.warn("Redis review result cache failed: {}", abbreviate(exception.getMessage(), 120));
        }
    }

    private String cacheKey(Long userId, String scopeType) {
        return cacheKey("agent_review:" + userId + ":" + scopeType);
    }

    private String cacheKey(String requestKey) {
        return "cache:" + requestKey;
    }

    private ReviewInput buildReviewInput(Long userId, String scopeType, MultiAgentCoordinator.Session session) {
        List<InterviewReport> projectReports = listProjectReports(userId, scopeType);
        List<QuestionTrainingReport> questionReports = listQuestionReports(userId, scopeType);
        List<MockInterviewReport> mockReports = listMockReports(userId, scopeType);
        List<UserAbilitySnapshot> snapshots = listAbilitySnapshots(userId);
        ResumeProfile resume = latestAnalyzedResume(userId);
        List<ResumeRisk> resumeRisks = parseResumeRisks(resume);
        UserMemorySummaryVO memorySummary = safeMemorySummary(userId);
        List<QaReplayEvidence> qaReplay = buildQaReplay(projectReports, questionReports, mockReports);
        List<RagArticle> ragArticles = retrieveRagArticles(projectReports, questionReports, mockReports, snapshots, resumeRisks, memorySummary, session);
        List<RagDocument> ragDocuments = retrieveUserDocuments(projectReports, questionReports, mockReports, snapshots, resumeRisks, memorySummary, session);
        List<Map<String, Object>> toolEvidence = collectToolEvidence(userId, scopeType, ragArticles, ragDocuments, memorySummary, session);

        Map<String, Object> sourceSnapshot = new LinkedHashMap<>();
        sourceSnapshot.put("projectReportCount", projectReports.size());
        sourceSnapshot.put("questionReportCount", questionReports.size());
        sourceSnapshot.put("mockInterviewReportCount", mockReports.size());
        sourceSnapshot.put("abilitySnapshotCount", snapshots.size());
        sourceSnapshot.put("hasResumeAnalysis", resume != null);
        sourceSnapshot.put("resumeRiskCount", resumeRisks.size());
        sourceSnapshot.put("memoryItemCount", memoryCount(memorySummary));
        sourceSnapshot.put("ragArticleCount", ragArticles.size());
        sourceSnapshot.put("ragDocumentCount", ragDocuments.size());
        sourceSnapshot.put("qaReplayCount", qaReplay.size());
        sourceSnapshot.put("scopeType", scopeType);

        AgentReviewContext context = new AgentReviewContext();
        context.setUserId(userId);
        context.setScopeType(scopeType);
        context.setSourceSnapshotJson(toJson(sourceSnapshot));
        context.setProjectReportsJson(toJson(projectReports.stream().map(this::toProjectReportEvidence).toList()));
        context.setQuestionReportsJson(toJson(questionReports.stream().map(this::toQuestionReportEvidence).toList()));
        context.setMockInterviewReportsJson(toJson(mockReports.stream().map(this::toMockReportEvidence).toList()));
        context.setQaReplayJson(toJson(qaReplay));
        context.setAbilitySnapshotsJson(toJson(snapshots.stream().map(this::toSnapshotEvidence).toList()));
        context.setResumeRisksJson(toJson(resumeRisks));
        context.setMemorySummaryJson(toJson(toMemoryEvidence(memorySummary)));
        context.setRagArticlesJson(toJson(ragArticles));
        context.setRagDocumentsJson(toJson(ragDocuments));
        context.setToolEvidenceJson(toJson(toolEvidence));

        return new ReviewInput(context, projectReports.size(), questionReports.size(), mockReports.size(), snapshots.size(),
                resumeRisks.size(), memoryCount(memorySummary), ragArticles, ragDocuments, qaReplay);
    }

    private List<InterviewReport> listProjectReports(Long userId, String scopeType) {
        LambdaQueryWrapper<InterviewReport> wrapper = new LambdaQueryWrapper<InterviewReport>()
                .eq(InterviewReport::getUserId, userId)
                .orderByDesc(InterviewReport::getCreatedAt)
                .orderByDesc(InterviewReport::getId);
        applySince(wrapper, InterviewReport::getCreatedAt, scopeType);
        wrapper.last(limitClause(scopeType, RECENT_REPORT_LIMIT));
        return interviewReportMapper.selectList(wrapper);
    }

    private List<QuestionTrainingReport> listQuestionReports(Long userId, String scopeType) {
        LambdaQueryWrapper<QuestionTrainingReport> wrapper = new LambdaQueryWrapper<QuestionTrainingReport>()
                .eq(QuestionTrainingReport::getUserId, userId)
                .orderByDesc(QuestionTrainingReport::getCreatedAt)
                .orderByDesc(QuestionTrainingReport::getId);
        applySince(wrapper, QuestionTrainingReport::getCreatedAt, scopeType);
        wrapper.last(limitClause(scopeType, RECENT_REPORT_LIMIT));
        return questionTrainingReportMapper.selectList(wrapper);
    }

    private List<MockInterviewReport> listMockReports(Long userId, String scopeType) {
        LambdaQueryWrapper<MockInterviewReport> wrapper = new LambdaQueryWrapper<MockInterviewReport>()
                .eq(MockInterviewReport::getUserId, userId)
                .orderByDesc(MockInterviewReport::getCreatedAt)
                .orderByDesc(MockInterviewReport::getId);
        applySince(wrapper, MockInterviewReport::getCreatedAt, scopeType);
        wrapper.last(limitClause(scopeType, RECENT_MOCK_REPORT_LIMIT));
        return mockInterviewReportMapper.selectList(wrapper);
    }

    private List<UserAbilitySnapshot> listAbilitySnapshots(Long userId) {
        return userAbilitySnapshotMapper.selectList(new LambdaQueryWrapper<UserAbilitySnapshot>()
                .eq(UserAbilitySnapshot::getUserId, userId)
                .orderByDesc(UserAbilitySnapshot::getCreatedAt)
                .orderByDesc(UserAbilitySnapshot::getId)
                .last("LIMIT " + SNAPSHOT_LIMIT));
    }

    private ResumeProfile latestAnalyzedResume(Long userId) {
        return resumeProfileMapper.selectList(new LambdaQueryWrapper<ResumeProfile>()
                        .eq(ResumeProfile::getUserId, userId)
                        .eq(ResumeProfile::getIsDeleted, 0)
                        .eq(ResumeProfile::getAnalysisStatus, "ANALYZED")
                        .orderByDesc(ResumeProfile::getAnalyzedAt)
                        .orderByDesc(ResumeProfile::getId)
                        .last("LIMIT 1"))
                .stream()
                .findFirst()
                .orElse(null);
    }

    private List<ResumeRisk> parseResumeRisks(ResumeProfile resume) {
        if (resume == null || !StringUtils.hasText(resume.getAnalysisResult())) {
            return List.of();
        }
        try {
            ResumeAnalysisResult result = objectMapper.readValue(resume.getAnalysisResult(), ResumeAnalysisResult.class);
            if (result.getRiskPoints() == null) {
                return List.of();
            }
            return result.getRiskPoints().stream()
                    .filter(risk -> StringUtils.hasText(risk.getEvidence()) || StringUtils.hasText(risk.getSuggestion()))
                    .limit(8)
                    .map(risk -> new ResumeRisk(risk.getType(), risk.getLevel(), risk.getEvidence(), risk.getSuggestion()))
                    .toList();
        } catch (Exception exception) {
            log.warn("Failed to parse resume risks for agent review, resumeId={}", resume.getId());
            return List.of();
        }
    }

    private List<RagArticle> retrieveRagArticles(
            List<InterviewReport> projectReports,
            List<QuestionTrainingReport> questionReports,
            List<MockInterviewReport> mockReports,
            List<UserAbilitySnapshot> snapshots,
            List<ResumeRisk> resumeRisks,
            UserMemorySummaryVO memorySummary,
            MultiAgentCoordinator.Session session
    ) {
        String query = buildRagQuery(projectReports, questionReports, mockReports, snapshots, resumeRisks, memorySummary);
        if (!StringUtils.hasText(query)) {
            return List.of();
        }
        try {
            RagSearchRequest request = new RagSearchRequest();
            request.setQuery(query);
            request.setSourceTypes(List.of(RagConstants.SOURCE_TYPE_KNOWLEDGE_ARTICLE));
            request.setTopK(RAG_LIMIT);
            RagSearchResponse response = ragRetrievalService.search(request);
            if (response == null || response.getChunks() == null) {
                return List.of();
            }
            Map<Long, RagArticle> articles = new LinkedHashMap<>();
            for (RagRetrievedChunk chunk : response.getChunks()) {
                Long key = chunk.getArticleId() == null ? chunk.getChunkId() : chunk.getArticleId();
                if (key == null || articles.containsKey(key)) {
                    continue;
                }
                articles.put(key, new RagArticle(
                        chunk.getArticleId(),
                        chunk.getTopicId(),
                        chunk.getTitle(),
                        chunk.getCategory(),
                        chunk.getTopicName(),
                        chunk.getSection(),
                        chunk.getScore(),
                        chunk.getArticleId() == null ? "/learn" : "/learn/articles/" + chunk.getArticleId()
                ));
            }
            List<RagArticle> result = articles.values().stream().limit(RAG_LIMIT).toList();
            recordReviewTool(session, "SEARCH_KNOWLEDGE", Map.of("query", query, "topK", RAG_LIMIT),
                    ToolExecuteResult.success(result.isEmpty() ? "未检索到知识文章。" : "已检索到知识文章。",
                            Map.of("resultCount", result.size()), "/learn", ToolDisplayType.SUMMARY));
            return result;
        } catch (Exception exception) {
            log.warn("Agent review RAG retrieval failed: {}", abbreviate(exception.getMessage(), 200));
            recordReviewTool(session, "SEARCH_KNOWLEDGE", Map.of("query", query, "topK", RAG_LIMIT),
                    ToolExecuteResult.failure("知识文章检索失败，已降级继续复盘。", "RAG_KNOWLEDGE_FAILED", ToolDisplayType.ERROR));
            return List.of();
        }
    }

    private List<RagDocument> retrieveUserDocuments(
            List<InterviewReport> projectReports,
            List<QuestionTrainingReport> questionReports,
            List<MockInterviewReport> mockReports,
            List<UserAbilitySnapshot> snapshots,
            List<ResumeRisk> resumeRisks,
            UserMemorySummaryVO memorySummary,
            MultiAgentCoordinator.Session session
    ) {
        String query = buildRagQuery(projectReports, questionReports, mockReports, snapshots, resumeRisks, memorySummary);
        if (!StringUtils.hasText(query)) {
            return List.of();
        }
        try {
            RagSearchRequest request = new RagSearchRequest();
            request.setQuery(query);
            request.setSourceTypes(List.of(RagConstants.SOURCE_TYPE_USER_UPLOAD));
            request.setTopK(3);
            RagSearchResponse response = ragRetrievalService.search(request);
            List<RagDocument> result = response == null || response.getChunks() == null
                    ? List.of()
                    : response.getChunks().stream()
                    .limit(3)
                    .map(chunk -> new RagDocument(chunk.getDocumentId(), chunk.getTitle(), chunk.getSourceType(),
                            chunk.getSection(), chunk.getScore(), "/documents"))
                    .toList();
            recordReviewTool(session, "SEARCH_USER_DOCUMENTS", Map.of("query", query, "topK", 3),
                    ToolExecuteResult.success(result.isEmpty() ? "未检索到用户文档证据。" : "已检索到用户文档证据。",
                            Map.of("resultCount", result.size()), "/documents", ToolDisplayType.SUMMARY));
            return result;
        } catch (Exception exception) {
            log.warn("Agent review user document RAG retrieval failed: {}", abbreviate(exception.getMessage(), 200));
            recordReviewTool(session, "SEARCH_USER_DOCUMENTS", Map.of("query", query, "topK", 3),
                    ToolExecuteResult.failure("用户文档检索失败，已降级继续复盘。", "RAG_DOCUMENT_FAILED", ToolDisplayType.ERROR));
            return List.of();
        }
    }

    private String buildRagQuery(
            List<InterviewReport> projectReports,
            List<QuestionTrainingReport> questionReports,
            List<MockInterviewReport> mockReports,
            List<UserAbilitySnapshot> snapshots,
            List<ResumeRisk> resumeRisks,
            UserMemorySummaryVO memorySummary
    ) {
        Set<String> parts = new LinkedHashSet<>();
        snapshots.stream().limit(20).forEach(snapshot -> {
            addIfText(parts, snapshot.getCategory());
            addIfText(parts, snapshot.getDimensionName());
            parseStringList(snapshot.getWeaknessTags()).forEach(parts::add);
        });
        projectReports.stream().limit(5).forEach(report -> parseStringList(report.getWeaknesses()).forEach(parts::add));
        questionReports.stream().limit(5).forEach(report -> {
            parseStringList(report.getWeaknesses()).forEach(parts::add);
            parseStringList(report.getKnowledgeGaps()).forEach(parts::add);
        });
        mockReports.stream().limit(3).forEach(report -> {
            parseStringList(report.getWeaknessTags()).forEach(parts::add);
            parseStringList(report.getWeaknesses()).forEach(parts::add);
            parseStringList(report.getHighRiskAnswers()).forEach(parts::add);
        });
        resumeRisks.stream().limit(5).forEach(risk -> {
            addIfText(parts, risk.type());
            addIfText(parts, risk.evidence());
            addIfText(parts, risk.suggestion());
        });
        if (memorySummary != null) {
            memorySummary.getTopWeaknesses().stream().limit(4).map(UserMemoryItemVO::getValue).forEach(parts::add);
            memorySummary.getTopProjectRisks().stream().limit(3).map(UserMemoryItemVO::getValue).forEach(parts::add);
        }
        return abbreviate(String.join(" ", parts), 800);
    }

    private Map<String, Object> toProjectReportEvidence(InterviewReport report) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", report.getId());
        item.put("score", report.getTotalScore());
        item.put("summary", abbreviate(report.getSummary(), 180));
        item.put("weaknesses", parseStringList(report.getWeaknesses()).stream().limit(5).toList());
        item.put("suggestions", parseStringList(report.getSuggestions()).stream().limit(4).toList());
        item.put("createdAt", report.getCreatedAt());
        return item;
    }

    private Map<String, Object> toQuestionReportEvidence(QuestionTrainingReport report) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", report.getId());
        item.put("topicId", report.getTopicId());
        item.put("score", report.getTotalScore());
        item.put("summary", abbreviate(report.getSummary(), 180));
        item.put("weaknesses", parseStringList(report.getWeaknesses()).stream().limit(5).toList());
        item.put("knowledgeGaps", parseStringList(report.getKnowledgeGaps()).stream().limit(5).toList());
        item.put("suggestions", parseStringList(report.getSuggestions()).stream().limit(4).toList());
        item.put("createdAt", report.getCreatedAt());
        return item;
    }

    private Map<String, Object> toMockReportEvidence(MockInterviewReport report) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", report.getId());
        item.put("sessionId", report.getSessionId());
        item.put("score", report.getTotalScore());
        item.put("sampleSufficiency", report.getSampleSufficiency());
        item.put("summary", abbreviate(report.getSummary(), 180));
        item.put("stagePerformances", parseMapList(report.getStagePerformances()).stream().limit(6).toList());
        item.put("weaknesses", parseStringList(report.getWeaknesses()).stream().limit(5).toList());
        item.put("highRiskAnswers", parseStringList(report.getHighRiskAnswers()).stream().limit(4).toList());
        item.put("nextActions", parseStringList(report.getNextActions()).stream().limit(4).toList());
        item.put("createdAt", report.getCreatedAt());
        return item;
    }

    private Map<String, Object> toSnapshotEvidence(UserAbilitySnapshot snapshot) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("dimension", snapshot.getDimensionName());
        item.put("category", snapshot.getCategory());
        item.put("score", snapshot.getScore());
        item.put("sourceType", snapshot.getSourceType());
        item.put("weaknessTags", parseStringList(snapshot.getWeaknessTags()).stream().limit(5).toList());
        item.put("evidence", abbreviate(snapshot.getEvidence(), 160));
        item.put("createdAt", snapshot.getCreatedAt());
        return item;
    }

    private UserMemorySummaryVO safeMemorySummary(Long userId) {
        try {
            UserMemorySummaryVO summary = userMemoryService.getSummary(userId);
            return summary == null ? new UserMemorySummaryVO() : summary;
        } catch (Exception exception) {
            log.warn("Agent review memory summary failed: {}", abbreviate(exception.getMessage(), 200));
            return new UserMemorySummaryVO();
        }
    }

    private Map<String, Object> toMemoryEvidence(UserMemorySummaryVO summary) {
        if (summary == null) {
            return Map.of("empty", true);
        }
        return mapOf(
                "targetRole", summary.getTargetRole(),
                "topWeaknesses", memoryItems(summary.getTopWeaknesses(), 5),
                "topResumeRisks", memoryItems(summary.getTopResumeRisks(), 4),
                "topProjectRisks", memoryItems(summary.getTopProjectRisks(), 4),
                "recentNextActions", memoryItems(summary.getRecentNextActions(), 4),
                "masteredTopics", memoryItems(summary.getMasteredTopics(), 4),
                "empty", summary.isEmpty()
        );
    }

    private List<Map<String, Object>> memoryItems(List<UserMemoryItemVO> items, int limit) {
        if (items == null) {
            return List.of();
        }
        return items.stream()
                .limit(limit)
                .map(item -> mapOf("value", item.getValue(), "confidence", item.getConfidence(), "weight", item.getWeight()))
                .toList();
    }

    private int memoryCount(UserMemorySummaryVO summary) {
        if (summary == null) {
            return 0;
        }
        return safeSize(summary.getTopWeaknesses())
                + safeSize(summary.getTopResumeRisks())
                + safeSize(summary.getTopProjectRisks())
                + safeSize(summary.getRecentNextActions())
                + safeSize(summary.getMasteredTopics());
    }

    private List<QaReplayEvidence> buildQaReplay(
            List<InterviewReport> projectReports,
            List<QuestionTrainingReport> questionReports,
            List<MockInterviewReport> mockReports
    ) {
        List<QaReplayEvidence> items = new ArrayList<>();
        projectReports.stream().findFirst().ifPresent(report -> parseMapList(report.getQaReview()).stream().limit(2)
                .map(item -> qaReplay("PROJECT_TRAINING", report.getId(), item))
                .forEach(items::add));
        questionReports.stream().findFirst().ifPresent(report -> parseMapList(report.getQaReview()).stream().limit(2)
                .map(item -> qaReplay("QUESTION_TRAINING", report.getId(), item))
                .forEach(items::add));
        mockReports.stream().findFirst().ifPresent(report -> items.addAll(mockInterviewReplay(report)));
        return items.stream().limit(8).toList();
    }

    private List<QaReplayEvidence> mockInterviewReplay(MockInterviewReport report) {
        if (report == null || report.getSessionId() == null) {
            return List.of();
        }
        List<MockInterviewMessage> messages = mockInterviewMessageMapper.selectList(new LambdaQueryWrapper<MockInterviewMessage>()
                .eq(MockInterviewMessage::getUserId, report.getUserId())
                .eq(MockInterviewMessage::getSessionId, report.getSessionId())
                .orderByAsc(MockInterviewMessage::getRoundNo)
                .orderByAsc(MockInterviewMessage::getCreatedAt)
                .last("LIMIT 12"));
        List<QaReplayEvidence> result = new ArrayList<>();
        MockInterviewMessage lastAssistant = null;
        for (MockInterviewMessage message : messages) {
            if ("ASSISTANT".equalsIgnoreCase(message.getRole())) {
                lastAssistant = message;
                continue;
            }
            if (!"USER".equalsIgnoreCase(message.getRole()) || lastAssistant == null) {
                continue;
            }
            result.add(new QaReplayEvidence(
                    "MOCK_INTERVIEW",
                    report.getId(),
                    abbreviate(lastAssistant.getContent(), 180),
                    abbreviate(message.getContent(), 180),
                    null,
                    scoreQuality(message.getScore()),
                    List.of(),
                    null
            ));
            if (result.size() >= 3) {
                break;
            }
        }
        return result;
    }

    private QaReplayEvidence qaReplay(String sourceType, Long sourceId, Map<String, Object> item) {
        String feedback = stringValue(item.get("feedback"));
        return new QaReplayEvidence(
                sourceType,
                sourceId,
                abbreviate(stringValue(item.get("question")), 180),
                abbreviate(firstText(stringValue(item.get("answer")), stringValue(item.get("answerSummary"))), 180),
                null,
                qualityFromFeedback(feedback),
                feedback == null ? List.of() : List.of(abbreviate(feedback, 120)),
                abbreviate(firstText(stringValue(item.get("referenceAnswer")), feedback), 180)
        );
    }

    private List<Map<String, Object>> collectToolEvidence(
            Long userId,
            String scopeType,
            List<RagArticle> ragArticles,
            List<RagDocument> ragDocuments,
            UserMemorySummaryVO memorySummary,
            MultiAgentCoordinator.Session session
    ) {
        List<Map<String, Object>> evidence = new ArrayList<>();
        evidence.add(recordReviewTool(session, "GET_RECENT_TRAINING_SUMMARY", Map.of("scopeType", scopeType),
                ToolExecuteResult.success("已聚合最近训练摘要。", Map.of("scopeType", scopeType), "/history", ToolDisplayType.SUMMARY)));
        evidence.add(recordReviewTool(session, "GET_ABILITY_SUMMARY", Map.of(),
                ToolExecuteResult.success("已聚合能力画像摘要。", Map.of("source", "user_ability_snapshot"), "/insights", ToolDisplayType.SUMMARY)));
        evidence.add(recordReviewTool(session, "GET_RESUME_RISK_SUMMARY", Map.of(),
                ToolExecuteResult.success("已聚合简历风险摘要。", Map.of("source", "resume_profile"), "/resumes", ToolDisplayType.SUMMARY)));
        evidence.add(recordReviewTool(session, "GET_USER_MEMORY_SUMMARY", Map.of("query", "recent interview weakness"),
                ToolExecuteResult.success(memorySummary == null || memorySummary.isEmpty() ? "长期记忆为空。" : "已读取长期记忆摘要。",
                        Map.of("memoryItemCount", memoryCount(memorySummary)), "/insights", ToolDisplayType.SUMMARY)));
        evidence.add(recordReviewTool(session, "GET_MOCK_INTERVIEW_SUMMARY", Map.of(),
                ToolExecuteResult.success("已聚合模拟面试阶段表现。", Map.of("source", "mock_interview_report"), "/mock-interviews", ToolDisplayType.SUMMARY)));
        evidence.add(recordReviewTool(session, "GET_REPORT_REPLAY_DATA", Map.of(),
                ToolExecuteResult.success("已聚合问答回放摘要。", Map.of("privacy", "summary_only"), "/agent-review", ToolDisplayType.SUMMARY)));
        evidence.add(mapOf("toolName", "SEARCH_KNOWLEDGE", "resultCount", ragArticles.size(), "targetPath", "/learn"));
        evidence.add(mapOf("toolName", "SEARCH_USER_DOCUMENTS", "resultCount", ragDocuments.size(), "targetPath", "/documents"));
        return evidence.stream().filter(Objects::nonNull).toList();
    }

    private Map<String, Object> recordReviewTool(
            MultiAgentCoordinator.Session session,
            String toolName,
            Map<String, Object> params,
            ToolExecuteResult result
    ) {
        String traceId = UUID.randomUUID().toString();
        try {
            agentToolTraceService.record(
                    traceId,
                    session == null ? null : session.runId(),
                    null,
                    session == null ? null : session.traceId(),
                    UserContext.getCurrentUserId(),
                    MultiAgentCoordinator.AGENT_TYPE_REVIEW_MULTI,
                    toolDefinition(toolName),
                    params,
                    result,
                    0
            );
        } catch (Exception exception) {
            log.warn("Failed to record review tool trace, toolName={}", toolName);
        }
        return mapOf(
                "toolName", toolName,
                "success", result != null && result.isSuccess(),
                "message", result == null ? null : result.getMessage(),
                "targetPath", result == null ? null : result.getTargetPath(),
                "traceId", traceId
        );
    }

    private ToolDefinition toolDefinition(String toolName) {
        return new ToolDefinition(
                toolName,
                toolName != null && toolName.startsWith("SEARCH_") ? ToolType.QUERY : ToolType.QUERY,
                ToolRiskLevel.LOW,
                ToolExecutionMode.AUTO_EXECUTE,
                ToolDisplayType.SUMMARY,
                toolName,
                "Review Agent read-only evidence tool",
                "/agent-review",
                true,
                false,
                Map.of()
        );
    }

    private AgentReviewResult buildLowDataResult() {
        AgentReviewResult result = new AgentReviewResult();
        result.setSummary("当前训练数据较少，建议先完成一次项目拷打和一次八股训练后再生成复盘。");
        AgentReviewResult.ScoreOverview score = new AgentReviewResult.ScoreOverview();
        score.setScore(null);
        score.setLevel("LOW_DATA");
        score.setExplanation("训练报告、能力画像或简历风险证据不足，暂不生成综合分数。");
        result.setScoreOverview(score);
        result.setRadarDimensions(List.of());
        result.setKeyFindings(List.of("暂无足够训练报告和能力画像，无法判断反复问题模式。"));
        result.setRecurringWeaknesses(List.of("样本不足，暂不识别稳定薄弱点。"));
        result.setHighRiskAnswers(List.of());
        result.setStagePerformance(List.of());
        result.setQaReplay(List.of());
        result.setCauseAnalysis(List.of("复盘 Agent 需要至少一些训练报告、能力快照或简历风险点作为证据。"));
        result.setResumeRisks(List.of("上传并分析简历后，复盘会结合简历风险点。"));
        result.setNextActions(List.of(
                action("TRAIN_PROJECT", "完成一次项目拷打训练", "项目训练能暴露个人贡献、技术细节和工程权衡表达问题。", 1, "/projects"),
                action("TRAIN_QUESTION", "完成一次八股专项训练", "八股训练能帮助系统识别概念、原理和场景短板。", 2, "/questions"),
                action("UPLOAD_DOCUMENT", "上传简历或项目材料", "有真实材料后，复盘会结合简历风险点和用户文档 RAG。", 3, "/documents")
        ));
        result.setRecommendedArticles(List.of());
        result.setRecommendedTrainings(List.of(recommendation(null, "项目拷打训练", "先补充一次项目表达样本。", "/projects", "TRAINING"),
                recommendation(null, "八股专项训练", "补充一次基础知识问答样本。", "/questions", "TRAINING")));
        result.setMemoryUpdates(List.of());
        result.setConfidence("LOW");
        result.setSampleQuality("INSUFFICIENT");
        return result;
    }

    private void normalizeResult(AgentReviewResult result, ReviewInput input) {
        if (!StringUtils.hasText(result.getSummary())) {
            result.setSummary(buildLowDataResult().getSummary());
        }
        if (result.getScoreOverview() == null) {
            result.setScoreOverview(buildScoreOverview(input));
        }
        if (result.getRadarDimensions() == null || result.getRadarDimensions().isEmpty()) {
            result.setRadarDimensions(buildRadarDimensions(input));
        }
        result.setKeyFindings(nonEmpty(result.getKeyFindings(), buildLowDataResult().getKeyFindings()));
        result.setRecurringWeaknesses(nonEmpty(result.getRecurringWeaknesses(), buildLowDataResult().getRecurringWeaknesses()));
        if (result.getHighRiskAnswers() == null) {
            result.setHighRiskAnswers(List.of());
        }
        if (result.getStagePerformance() == null || result.getStagePerformance().isEmpty()) {
            result.setStagePerformance(buildStagePerformance(input));
        }
        if (result.getQaReplay() == null || result.getQaReplay().isEmpty()) {
            result.setQaReplay(input.qaReplay().stream().map(this::toQaReplayItem).toList());
        }
        result.setCauseAnalysis(nonEmpty(result.getCauseAnalysis(), buildLowDataResult().getCauseAnalysis()));
        if (result.getResumeRisks() == null || result.getResumeRisks().isEmpty()) {
            result.setResumeRisks(input.resumeRiskCount() > 0
                    ? List.of("简历存在风险点，建议优先用项目拷打覆盖高风险项目。")
                    : List.of("上传并分析简历后，复盘会结合简历风险点。"));
        }
        List<AgentReviewResult.NextAction> actions = result.getNextActions() == null
                ? new ArrayList<>()
                : new ArrayList<>(result.getNextActions());
        if (actions.isEmpty()) {
            actions.add(action("TRAIN_QUESTION", "完成一次八股专项训练", "用训练验证本次复盘中的知识短板。", 2, "/questions"));
        }
        if (!input.ragArticles().isEmpty()) {
            RagArticle first = input.ragArticles().get(0);
            actions.add(0, action("LEARN",
                    "学习：" + textOrDefault(first.title(), "推荐知识卡片"),
                    "该知识卡片与近期薄弱点和能力画像最相关。",
                    1,
                    first.targetPath()));
        }
        if (input.mockReportCount() == 0) {
            actions.add(action("MOCK_INTERVIEW", "完成一次真实模拟面试", "综合模拟面试能验证开场、项目深挖、八股追问和收尾节奏。", 4, "/mock-interviews"));
        }
        result.setNextActions(actions.stream()
                .filter(Objects::nonNull)
                .map(this::sanitizeAction)
                .sorted(Comparator.comparing(action -> action.getPriority() == null ? 99 : action.getPriority()))
                .limit(5)
                .toList());
        if (result.getRecommendedArticles() == null || result.getRecommendedArticles().isEmpty()) {
            result.setRecommendedArticles(input.ragArticles().stream()
                    .map(article -> recommendation(article.articleId(), textOrDefault(article.title(), "推荐知识文章"),
                            "与近期薄弱点、简历风险或长期记忆相关。", article.targetPath(), "KNOWLEDGE_ARTICLE"))
                    .toList());
        }
        if (result.getRecommendedTrainings() == null || result.getRecommendedTrainings().isEmpty()) {
            result.setRecommendedTrainings(List.of(
                    recommendation(null, "八股专项训练", "验证技术基础和追问应对。", "/questions", "TRAINING"),
                    recommendation(null, "项目拷打训练", "验证项目细节、工程权衡和简历可信度。", "/projects", "TRAINING"),
                    recommendation(null, "真实模拟面试", "验证完整面试节奏。", "/mock-interviews", "TRAINING")
            ));
        }
        if (result.getMemoryUpdates() == null || result.getMemoryUpdates().isEmpty()) {
            result.setMemoryUpdates(result.getRecurringWeaknesses().stream().limit(3).map(value -> "强化弱点：" + value).toList());
        }
    }

    private AgentReviewResult.NextAction sanitizeAction(AgentReviewResult.NextAction action) {
        if (!StringUtils.hasText(action.getType())) {
            action.setType("TRAIN_QUESTION");
        }
        if (!StringUtils.hasText(action.getTitle())) {
            action.setTitle("继续一次专项训练");
        }
        if (!StringUtils.hasText(action.getReason())) {
            action.setReason("用于验证复盘中识别出的薄弱点是否已经补齐。");
        }
        if (action.getPriority() == null || action.getPriority() < 1) {
            action.setPriority(3);
        }
        if (!StringUtils.hasText(action.getTargetPath()) || !action.getTargetPath().startsWith("/")) {
            action.setTargetPath(defaultPath(action.getType()));
        }
        if (!StringUtils.hasText(action.getToolName())) {
            action.setToolName(defaultToolName(action.getType()));
        }
        if (action.getParams() == null) {
            action.setParams(Map.of());
        }
        return action;
    }

    private AgentReviewResult.ScoreOverview buildScoreOverview(ReviewInput input) {
        AgentReviewResult.ScoreOverview overview = new AgentReviewResult.ScoreOverview();
        List<Integer> scores = new ArrayList<>();
        collectScores(parseMapList(input.context().getProjectReportsJson()), scores);
        collectScores(parseMapList(input.context().getQuestionReportsJson()), scores);
        collectScores(parseMapList(input.context().getMockInterviewReportsJson()), scores);
        collectScores(parseMapList(input.context().getAbilitySnapshotsJson()), scores);
        if (scores.isEmpty()) {
            overview.setLevel("LOW_DATA");
            overview.setExplanation("样本不足，暂不生成综合分数。");
            return overview;
        }
        int average = (int) Math.round(scores.stream().mapToInt(Integer::intValue).average().orElse(0));
        overview.setScore(average);
        overview.setLevel(average >= 85 ? "STRONG" : average >= 70 ? "MEDIUM" : "RISKY");
        overview.setExplanation("基于训练报告、模拟面试和能力画像分数归一化生成。");
        return overview;
    }

    private List<AgentReviewResult.RadarDimension> buildRadarDimensions(ReviewInput input) {
        List<AgentReviewResult.RadarDimension> dimensions = new ArrayList<>();
        List<Map<String, Object>> snapshots = parseMapList(input.context().getAbilitySnapshotsJson());
        addRadar(dimensions, "技术基础", averageByKeyword(snapshots, List.of("基础", "Java", "JVM", "MySQL", "Redis", "Spring")), "来自八股训练和能力画像");
        addRadar(dimensions, "项目表达", averageByKeyword(snapshots, List.of("项目", "表达", "工程")), "来自项目训练和能力画像");
        addRadar(dimensions, "简历可信度", input.resumeRiskCount() > 0 ? 60 : null, "来自简历风险和项目训练交叉验证");
        addRadar(dimensions, "工程思维", averageByKeyword(snapshots, List.of("工程", "异常", "性能", "设计")), "来自工程权衡、异常处理和指标表达");
        addRadar(dimensions, "追问应对", averageByKeyword(snapshots, List.of("追问", "面试", "应对")), "来自模拟面试阶段表现");
        addRadar(dimensions, "表达结构", averageByKeyword(snapshots, List.of("表达", "结构", "沟通")), "来自问答回放和报告反馈");
        if (dimensions.stream().noneMatch(item -> item.getScore() != null)) {
            return List.of();
        }
        return dimensions;
    }

    private List<AgentReviewResult.StagePerformance> buildStagePerformance(ReviewInput input) {
        return parseMapList(input.context().getMockInterviewReportsJson()).stream()
                .findFirst()
                .map(report -> parseStageItems(report.get("stagePerformances")))
                .orElse(List.of());
    }

    private List<AgentReviewResult.StagePerformance> parseStageItems(Object value) {
        if (!(value instanceof List<?> values)) {
            return List.of();
        }
        return values.stream()
                .filter(Map.class::isInstance)
                .map(item -> {
                    Map<?, ?> map = (Map<?, ?>) item;
                    AgentReviewResult.StagePerformance stage = new AgentReviewResult.StagePerformance();
                    stage.setStage(stringValue(map.get("stage")));
                    stage.setStageName(firstText(stringValue(map.get("stageName")), stringValue(map.get("stage"))));
                    stage.setScore(intValue(map.get("score")));
                    stage.setComment(abbreviate(stringValue(map.get("comment")), 160));
                    stage.setWeaknessTags(List.of());
                    return stage;
                })
                .limit(6)
                .toList();
    }

    private AgentReviewResult.QaReplayItem toQaReplayItem(QaReplayEvidence evidence) {
        AgentReviewResult.QaReplayItem item = new AgentReviewResult.QaReplayItem();
        item.setSourceType(evidence.sourceType());
        item.setSourceId(evidence.sourceId());
        item.setQuestion(evidence.question());
        item.setAnswerSummary(evidence.answerSummary());
        item.setAiFollowUp(evidence.aiFollowUp());
        item.setQuality(evidence.quality());
        item.setMainProblems(evidence.mainProblems());
        item.setSuggestedExpression(evidence.suggestedExpression());
        return item;
    }

    private AgentReviewResult.Recommendation recommendation(Long id, String title, String reason, String targetPath, String sourceType) {
        AgentReviewResult.Recommendation recommendation = new AgentReviewResult.Recommendation();
        recommendation.setId(id);
        recommendation.setTitle(title);
        recommendation.setReason(reason);
        recommendation.setTargetPath(targetPath);
        recommendation.setSourceType(sourceType);
        recommendation.setMetadata(Map.of());
        return recommendation;
    }

    private String defaultPath(String type) {
        return switch (type) {
            case "LEARN" -> "/learn";
            case "TRAIN_PROJECT" -> "/projects";
            case "MOCK_INTERVIEW" -> "/mock-interviews";
            case "REVIEW_RESUME" -> "/resumes";
            case "UPLOAD_DOCUMENT" -> "/documents";
            case "VIEW_MEMORY" -> "/insights";
            case "VIEW_REPORT_REPLAY" -> "/agent-review";
            default -> "/questions";
        };
    }

    private String defaultToolName(String type) {
        return switch (type) {
            case "LEARN" -> "SEARCH_KNOWLEDGE";
            case "TRAIN_PROJECT" -> "START_PROJECT_TRAINING";
            case "MOCK_INTERVIEW" -> "START_MOCK_INTERVIEW";
            case "REVIEW_RESUME" -> "ANALYZE_RESUME";
            case "UPLOAD_DOCUMENT" -> "GO_DOCUMENTS";
            case "VIEW_MEMORY" -> "GET_USER_MEMORY_SUMMARY";
            case "VIEW_REPORT_REPLAY" -> "GET_REPORT_REPLAY_DATA";
            default -> "START_QUESTION_TRAINING";
        };
    }

    private AgentReviewResult.NextAction action(String type, String title, String reason, int priority, String targetPath) {
        AgentReviewResult.NextAction action = new AgentReviewResult.NextAction();
        action.setType(type);
        action.setTitle(title);
        action.setReason(reason);
        action.setPriority(priority);
        action.setTargetPath(targetPath);
        return action;
    }

    private String normalizeConfidence(String value, ReviewInput input) {
        if (!input.hasCoreData()) {
            return "LOW";
        }
        if ("HIGH".equals(value) || "MEDIUM".equals(value) || "LOW".equals(value)) {
            return value;
        }
        if (input.totalReports() >= 4 && input.snapshotCount() >= 6 && input.resumeRiskCount() > 0) {
            return "HIGH";
        }
        return input.totalReports() >= 2 && input.snapshotCount() > 0 ? "MEDIUM" : "LOW";
    }

    private String normalizeSampleQuality(String value, ReviewInput input) {
        if ("ENOUGH".equals(value) || "LIMITED".equals(value) || "INSUFFICIENT".equals(value)) {
            return value;
        }
        if (!input.hasCoreData() || input.totalReports() < 2) {
            return "INSUFFICIENT";
        }
        if (input.totalReports() >= 4 && input.snapshotCount() >= 4 && (input.mockReportCount() > 0 || input.memoryCount() > 0)) {
            return "ENOUGH";
        }
        return "LIMITED";
    }

    private String normalizeScope(String scopeType) {
        return StringUtils.hasText(scopeType) ? scopeType.trim() : SCOPE_RECENT_10;
    }

    private <T> void applySince(LambdaQueryWrapper<T> wrapper, SFunction<T, LocalDateTime> column, String scopeType) {
        LocalDateTime since = switch (scopeType) {
            case "RECENT_7_DAYS" -> LocalDateTime.now().minusDays(7);
            case "RECENT_30_DAYS" -> LocalDateTime.now().minusDays(30);
            default -> null;
        };
        if (since != null) {
            wrapper.ge(column, since);
        }
    }

    private String limitClause(String scopeType, int defaultLimit) {
        if ("ALL".equals(scopeType) || "ALL_TRAINING".equals(scopeType)) {
            return "LIMIT 50";
        }
        return "LIMIT " + defaultLimit;
    }

    private AgentReviewVO toVO(AgentReview review) {
        AgentReviewVO vo = new AgentReviewVO();
        vo.setId(review.getId());
        vo.setScopeType(review.getScopeType());
        vo.setSummary(review.getSummary());
        vo.setScoreOverview(parseMap(review.getScoreOverview()));
        vo.setRadarDimensions(parseMapList(review.getRadarDimensions()));
        vo.setKeyFindings(parseStringList(review.getKeyFindings()));
        vo.setRecurringWeaknesses(parseStringList(review.getRecurringWeaknesses()));
        vo.setHighRiskAnswers(parseMapList(review.getHighRiskAnswers()));
        vo.setStagePerformance(parseMapList(review.getStagePerformance()));
        vo.setQaReplay(parseMapList(review.getQaReplay()));
        vo.setCauseAnalysis(parseStringList(review.getCauseAnalysis()));
        vo.setResumeRisks(parseStringList(review.getResumeRisks()));
        vo.setNextActions(parseNextActions(review.getNextActions()));
        vo.setRecommendedArticles(parseMapList(review.getRecommendedArticles()));
        vo.setRecommendedTrainings(parseMapList(review.getRecommendedTrainings()));
        vo.setMemoryUpdates(parseStringList(review.getMemoryUpdates()));
        vo.setConfidence(review.getConfidence());
        vo.setSampleQuality(review.getSampleQuality());
        vo.setSourceSnapshot(parseMap(review.getSourceSnapshot()));
        vo.setCreatedAt(review.getCreatedAt());
        return vo;
    }

    private List<NextActionVO> toNextActionVOs(List<AgentReviewResult.NextAction> actions) {
        if (actions == null) {
            return List.of();
        }
        return actions.stream().map(action -> {
            NextActionVO vo = new NextActionVO();
            vo.setType(action.getType());
            vo.setTitle(action.getTitle());
            vo.setReason(action.getReason());
            vo.setPriority(action.getPriority());
            vo.setTargetPath(action.getTargetPath());
            vo.setToolName(action.getToolName());
            vo.setParams(action.getParams());
            return vo;
        }).toList();
    }

    private List<NextActionVO> parseNextActions(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, NEXT_ACTION_LIST_TYPE);
        } catch (Exception exception) {
            return List.of();
        }
    }

    private Map<String, Object> parseMap(String json) {
        if (!StringUtils.hasText(json)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (Exception exception) {
            return Map.of();
        }
    }

    private List<Map<String, Object>> parseMapList(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            List<Map<String, Object>> values = objectMapper.readValue(json, MAP_LIST_TYPE);
            return values == null ? List.of() : values;
        } catch (Exception exception) {
            return List.of();
        }
    }

    private List<String> parseStringList(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            List<String> values = objectMapper.readValue(json, STRING_LIST_TYPE);
            return values == null ? List.of() : values.stream().filter(StringUtils::hasText).map(String::trim).toList();
        } catch (Exception exception) {
            return List.of();
        }
    }

    private List<String> nonEmpty(List<String> values, List<String> fallback) {
        return values == null || values.isEmpty() ? fallback : values;
    }

    private void addIfText(Set<String> parts, String value) {
        if (StringUtils.hasText(value)) {
            parts.add(abbreviate(value.trim(), 120));
        }
    }

    private void collectScores(List<Map<String, Object>> items, List<Integer> scores) {
        items.forEach(item -> {
            Integer score = intValue(firstNonNull(item.get("score"), item.get("totalScore")));
            if (score != null) {
                scores.add(Math.max(0, Math.min(100, score)));
            }
        });
    }

    private Integer averageByKeyword(List<Map<String, Object>> snapshots, List<String> keywords) {
        List<Integer> scores = snapshots.stream()
                .filter(item -> {
                    String text = (stringValue(item.get("dimension")) + " " + stringValue(item.get("category"))).toLowerCase();
                    return keywords.stream().anyMatch(keyword -> text.contains(keyword.toLowerCase()));
                })
                .map(item -> intValue(item.get("score")))
                .filter(Objects::nonNull)
                .toList();
        if (scores.isEmpty()) {
            return null;
        }
        return (int) Math.round(scores.stream().mapToInt(Integer::intValue).average().orElse(0));
    }

    private void addRadar(List<AgentReviewResult.RadarDimension> dimensions, String name, Integer score, String evidence) {
        AgentReviewResult.RadarDimension item = new AgentReviewResult.RadarDimension();
        item.setName(name);
        item.setScore(score);
        item.setEvidence(evidence);
        dimensions.add(item);
    }

    private String scoreQuality(Integer score) {
        if (score == null) {
            return "UNKNOWN";
        }
        if (score >= 80) {
            return "HIGH";
        }
        if (score >= 65) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private String qualityFromFeedback(String feedback) {
        if (!StringUtils.hasText(feedback)) {
            return "UNKNOWN";
        }
        String text = feedback.toLowerCase();
        if (text.contains("高风险") || text.contains("不足") || text.contains("问题") || text.contains("缺少")) {
            return "LOW";
        }
        return "MEDIUM";
    }

    private Object firstNonNull(Object first, Object second) {
        return first == null ? second : first;
    }

    private Integer intValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text && StringUtils.hasText(text)) {
            try {
                return Integer.parseInt(text.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
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

    private String textOrDefault(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? List.of() : value);
        } catch (JsonProcessingException exception) {
            return "[]";
        }
    }

    private String abbreviate(String value, int limit) {
        if (!StringUtils.hasText(value) || value.length() <= limit) {
            return value;
        }
        return value.substring(0, limit);
    }

    private long elapsed(long startTime) {
        return System.currentTimeMillis() - startTime;
    }

    private int safeSize(List<?> values) {
        return values == null ? 0 : values.size();
    }

    private record ReviewInput(
            AgentReviewContext context,
            int projectReportCount,
            int questionReportCount,
            int mockReportCount,
            int snapshotCount,
            int resumeRiskCount,
            int memoryCount,
            List<RagArticle> ragArticles,
            List<RagDocument> ragDocuments,
            List<QaReplayEvidence> qaReplay
    ) {
        private int totalReports() {
            return projectReportCount + questionReportCount + mockReportCount;
        }

        private boolean hasCoreData() {
            return totalReports() > 0 || snapshotCount > 0 || resumeRiskCount > 0 || memoryCount > 0;
        }
    }

    private record ResumeRisk(String type, String level, String evidence, String suggestion) {
    }

    private record RagArticle(
            Long articleId,
            Long topicId,
            String title,
            String category,
            String topicName,
            String section,
            Double score,
            String targetPath
    ) {
    }

    private record RagDocument(
            Long documentId,
            String title,
            String sourceType,
            String section,
            Double score,
            String targetPath
    ) {
    }

    private record QaReplayEvidence(
            String sourceType,
            Long sourceId,
            String question,
            String answerSummary,
            String aiFollowUp,
            String quality,
            List<String> mainProblems,
            String suggestedExpression
    ) {
    }
}
