package com.codecoach.module.agent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.codecoach.common.exception.BusinessException;
import com.codecoach.common.result.ResultCode;
import com.codecoach.module.agent.entity.AgentReview;
import com.codecoach.module.agent.mapper.AgentReviewMapper;
import com.codecoach.module.agent.model.AgentReviewContext;
import com.codecoach.module.agent.model.AgentReviewResult;
import com.codecoach.module.agent.service.AgentReviewService;
import com.codecoach.module.agent.service.AiAgentReviewService;
import com.codecoach.module.agent.vo.AgentReviewListItemVO;
import com.codecoach.module.agent.vo.AgentReviewVO;
import com.codecoach.module.agent.vo.NextActionVO;
import com.codecoach.module.insight.entity.UserAbilitySnapshot;
import com.codecoach.module.insight.mapper.UserAbilitySnapshotMapper;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AgentReviewServiceImpl implements AgentReviewService {

    private static final Logger log = LoggerFactory.getLogger(AgentReviewServiceImpl.class);

    private static final int REVIEW_NOT_FOUND_CODE = 8001;
    private static final String SCOPE_RECENT_10 = "RECENT_10";
    private static final int RECENT_REPORT_LIMIT = 10;
    private static final int SNAPSHOT_LIMIT = 50;
    private static final int HISTORY_LIMIT = 20;
    private static final int RAG_LIMIT = 5;
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<List<NextActionVO>> NEXT_ACTION_LIST_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final AgentReviewMapper agentReviewMapper;
    private final InterviewReportMapper interviewReportMapper;
    private final QuestionTrainingReportMapper questionTrainingReportMapper;
    private final UserAbilitySnapshotMapper userAbilitySnapshotMapper;
    private final ResumeProfileMapper resumeProfileMapper;
    private final RagRetrievalService ragRetrievalService;
    private final AiAgentReviewService aiAgentReviewService;
    private final ObjectMapper objectMapper;

    public AgentReviewServiceImpl(
            AgentReviewMapper agentReviewMapper,
            InterviewReportMapper interviewReportMapper,
            QuestionTrainingReportMapper questionTrainingReportMapper,
            UserAbilitySnapshotMapper userAbilitySnapshotMapper,
            ResumeProfileMapper resumeProfileMapper,
            RagRetrievalService ragRetrievalService,
            AiAgentReviewService aiAgentReviewService,
            ObjectMapper objectMapper
    ) {
        this.agentReviewMapper = agentReviewMapper;
        this.interviewReportMapper = interviewReportMapper;
        this.questionTrainingReportMapper = questionTrainingReportMapper;
        this.userAbilitySnapshotMapper = userAbilitySnapshotMapper;
        this.resumeProfileMapper = resumeProfileMapper;
        this.ragRetrievalService = ragRetrievalService;
        this.aiAgentReviewService = aiAgentReviewService;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public AgentReviewVO generateReview(String scopeType) {
        Long userId = UserContext.getCurrentUserId();
        String normalizedScope = normalizeScope(scopeType);

        ReviewInput input = buildReviewInput(userId, normalizedScope);
        AgentReviewResult result = input.hasCoreData()
                ? aiAgentReviewService.generateReview(input.context())
                : buildLowDataResult();
        normalizeResult(result, input);

        AgentReview review = new AgentReview();
        review.setUserId(userId);
        review.setScopeType(normalizedScope);
        review.setSummary(result.getSummary());
        review.setKeyFindings(toJson(result.getKeyFindings()));
        review.setRecurringWeaknesses(toJson(result.getRecurringWeaknesses()));
        review.setCauseAnalysis(toJson(result.getCauseAnalysis()));
        review.setResumeRisks(toJson(result.getResumeRisks()));
        review.setNextActions(toJson(toNextActionVOs(result.getNextActions())));
        review.setConfidence(normalizeConfidence(result.getConfidence(), input));
        review.setSourceSnapshot(input.context().getSourceSnapshotJson());
        review.setCreatedAt(LocalDateTime.now());
        agentReviewMapper.insert(review);
        return toVO(review);
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

    private ReviewInput buildReviewInput(Long userId, String scopeType) {
        List<InterviewReport> projectReports = listProjectReports(userId);
        List<QuestionTrainingReport> questionReports = listQuestionReports(userId);
        List<UserAbilitySnapshot> snapshots = listAbilitySnapshots(userId);
        ResumeProfile resume = latestAnalyzedResume(userId);
        List<ResumeRisk> resumeRisks = parseResumeRisks(resume);
        List<RagArticle> ragArticles = retrieveRagArticles(projectReports, questionReports, snapshots, resumeRisks);

        Map<String, Object> sourceSnapshot = new LinkedHashMap<>();
        sourceSnapshot.put("projectReportCount", projectReports.size());
        sourceSnapshot.put("questionReportCount", questionReports.size());
        sourceSnapshot.put("abilitySnapshotCount", snapshots.size());
        sourceSnapshot.put("hasResumeAnalysis", resume != null);
        sourceSnapshot.put("resumeRiskCount", resumeRisks.size());
        sourceSnapshot.put("ragArticleCount", ragArticles.size());
        sourceSnapshot.put("scopeType", scopeType);

        AgentReviewContext context = new AgentReviewContext();
        context.setUserId(userId);
        context.setScopeType(scopeType);
        context.setSourceSnapshotJson(toJson(sourceSnapshot));
        context.setProjectReportsJson(toJson(projectReports.stream().map(this::toProjectReportEvidence).toList()));
        context.setQuestionReportsJson(toJson(questionReports.stream().map(this::toQuestionReportEvidence).toList()));
        context.setAbilitySnapshotsJson(toJson(snapshots.stream().map(this::toSnapshotEvidence).toList()));
        context.setResumeRisksJson(toJson(resumeRisks));
        context.setRagArticlesJson(toJson(ragArticles));

        return new ReviewInput(context, projectReports.size(), questionReports.size(), snapshots.size(), resumeRisks.size(), ragArticles);
    }

    private List<InterviewReport> listProjectReports(Long userId) {
        return interviewReportMapper.selectList(new LambdaQueryWrapper<InterviewReport>()
                .eq(InterviewReport::getUserId, userId)
                .orderByDesc(InterviewReport::getCreatedAt)
                .orderByDesc(InterviewReport::getId)
                .last("LIMIT " + RECENT_REPORT_LIMIT));
    }

    private List<QuestionTrainingReport> listQuestionReports(Long userId) {
        return questionTrainingReportMapper.selectList(new LambdaQueryWrapper<QuestionTrainingReport>()
                .eq(QuestionTrainingReport::getUserId, userId)
                .orderByDesc(QuestionTrainingReport::getCreatedAt)
                .orderByDesc(QuestionTrainingReport::getId)
                .last("LIMIT " + RECENT_REPORT_LIMIT));
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
            List<UserAbilitySnapshot> snapshots,
            List<ResumeRisk> resumeRisks
    ) {
        String query = buildRagQuery(projectReports, questionReports, snapshots, resumeRisks);
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
            return articles.values().stream().limit(RAG_LIMIT).toList();
        } catch (Exception exception) {
            log.warn("Agent review RAG retrieval failed: {}", abbreviate(exception.getMessage(), 200));
            return List.of();
        }
    }

    private String buildRagQuery(
            List<InterviewReport> projectReports,
            List<QuestionTrainingReport> questionReports,
            List<UserAbilitySnapshot> snapshots,
            List<ResumeRisk> resumeRisks
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
        resumeRisks.stream().limit(5).forEach(risk -> {
            addIfText(parts, risk.type());
            addIfText(parts, risk.evidence());
            addIfText(parts, risk.suggestion());
        });
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

    private AgentReviewResult buildLowDataResult() {
        AgentReviewResult result = new AgentReviewResult();
        result.setSummary("当前训练数据较少，建议先完成一次项目拷打和一次八股训练后再生成复盘。");
        result.setKeyFindings(List.of("暂无足够训练报告和能力画像，无法判断反复问题模式。"));
        result.setRecurringWeaknesses(List.of("样本不足，暂不识别稳定薄弱点。"));
        result.setCauseAnalysis(List.of("复盘 Agent 需要至少一些训练报告、能力快照或简历风险点作为证据。"));
        result.setResumeRisks(List.of("上传并分析简历后，复盘会结合简历风险点。"));
        result.setNextActions(List.of(
                action("TRAIN_PROJECT", "完成一次项目拷打训练", "项目训练能暴露个人贡献、技术细节和工程权衡表达问题。", 1, "/projects"),
                action("TRAIN_QUESTION", "完成一次八股专项训练", "八股训练能帮助系统识别概念、原理和场景短板。", 2, "/questions"),
                action("UPLOAD_DOCUMENT", "上传简历或项目材料", "有真实材料后，复盘会结合简历风险点和用户文档 RAG。", 3, "/documents")
        ));
        result.setConfidence("LOW");
        return result;
    }

    private void normalizeResult(AgentReviewResult result, ReviewInput input) {
        if (!StringUtils.hasText(result.getSummary())) {
            result.setSummary(buildLowDataResult().getSummary());
        }
        result.setKeyFindings(nonEmpty(result.getKeyFindings(), buildLowDataResult().getKeyFindings()));
        result.setRecurringWeaknesses(nonEmpty(result.getRecurringWeaknesses(), buildLowDataResult().getRecurringWeaknesses()));
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
        result.setNextActions(actions.stream()
                .filter(Objects::nonNull)
                .map(this::sanitizeAction)
                .sorted(Comparator.comparing(action -> action.getPriority() == null ? 99 : action.getPriority()))
                .limit(5)
                .toList());
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
        return action;
    }

    private String defaultPath(String type) {
        return switch (type) {
            case "LEARN" -> "/learn";
            case "TRAIN_PROJECT" -> "/projects";
            case "REVIEW_RESUME" -> "/resumes";
            case "UPLOAD_DOCUMENT" -> "/documents";
            default -> "/questions";
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

    private String normalizeScope(String scopeType) {
        return StringUtils.hasText(scopeType) ? scopeType.trim() : SCOPE_RECENT_10;
    }

    private AgentReviewVO toVO(AgentReview review) {
        AgentReviewVO vo = new AgentReviewVO();
        vo.setId(review.getId());
        vo.setScopeType(review.getScopeType());
        vo.setSummary(review.getSummary());
        vo.setKeyFindings(parseStringList(review.getKeyFindings()));
        vo.setRecurringWeaknesses(parseStringList(review.getRecurringWeaknesses()));
        vo.setCauseAnalysis(parseStringList(review.getCauseAnalysis()));
        vo.setResumeRisks(parseStringList(review.getResumeRisks()));
        vo.setNextActions(parseNextActions(review.getNextActions()));
        vo.setConfidence(review.getConfidence());
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

    private record ReviewInput(
            AgentReviewContext context,
            int projectReportCount,
            int questionReportCount,
            int snapshotCount,
            int resumeRiskCount,
            List<RagArticle> ragArticles
    ) {
        private int totalReports() {
            return projectReportCount + questionReportCount;
        }

        private boolean hasCoreData() {
            return totalReports() > 0 || snapshotCount > 0 || resumeRiskCount > 0;
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
}
