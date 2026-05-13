package com.codecoach.module.memory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.codecoach.module.agent.entity.AgentReview;
import com.codecoach.module.agent.vo.NextActionVO;
import com.codecoach.module.insight.entity.UserAbilitySnapshot;
import com.codecoach.module.interview.entity.InterviewSession;
import com.codecoach.module.knowledge.entity.KnowledgeTopic;
import com.codecoach.module.memory.entity.UserMemory;
import com.codecoach.module.memory.mapper.UserMemoryMapper;
import com.codecoach.module.memory.model.MemoryConfidence;
import com.codecoach.module.memory.model.MemorySemanticHit;
import com.codecoach.module.memory.model.MemorySinkCommand;
import com.codecoach.module.memory.model.MemorySourceTypes;
import com.codecoach.module.memory.model.MemoryTypes;
import com.codecoach.module.memory.service.UserMemoryService;
import com.codecoach.module.memory.service.UserSemanticMemoryService;
import com.codecoach.module.memory.vo.UserMemoryItemVO;
import com.codecoach.module.memory.vo.UserMemorySummaryVO;
import com.codecoach.module.mockinterview.entity.MockInterviewReport;
import com.codecoach.module.mockinterview.entity.MockInterviewSession;
import com.codecoach.module.question.entity.QuestionTrainingReport;
import com.codecoach.module.question.entity.QuestionTrainingSession;
import com.codecoach.module.report.entity.InterviewReport;
import com.codecoach.module.resume.entity.ResumeProfile;
import com.codecoach.module.resume.model.ResumeAnalysisResult;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class UserMemoryServiceImpl implements UserMemoryService {

    private static final Logger log = LoggerFactory.getLogger(UserMemoryServiceImpl.class);
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_ARCHIVED = "ARCHIVED";
    private static final String STATUS_INACCURATE = "INACCURATE";
    private static final int SUMMARY_LIMIT = 80;
    private static final int VALUE_LIMIT = 220;
    private static final int KEY_LIMIT = 120;
    private static final int MAX_WEIGHT = 99;
    private static final Pattern EMAIL_PATTERN = Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");
    private static final Pattern PHONE_PATTERN = Pattern.compile("(?<!\\d)(?:\\+?86[-\\s]?)?1[3-9]\\d{9}(?!\\d)");
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };
    private static final TypeReference<List<NextActionVO>> NEXT_ACTION_LIST = new TypeReference<>() {
    };
    private static final TypeReference<List<Map<String, Object>>> MAP_LIST = new TypeReference<>() {
    };

    private final UserMemoryMapper userMemoryMapper;
    private final ObjectMapper objectMapper;
    private final UserSemanticMemoryService userSemanticMemoryService;

    public UserMemoryServiceImpl(
            UserMemoryMapper userMemoryMapper,
            ObjectMapper objectMapper,
            UserSemanticMemoryService userSemanticMemoryService
    ) {
        this.userMemoryMapper = userMemoryMapper;
        this.objectMapper = objectMapper;
        this.userSemanticMemoryService = userSemanticMemoryService;
    }

    @Override
    @Transactional
    public void reinforce(MemorySinkCommand command) {
        if (command == null || command.getUserId() == null || !StringUtils.hasText(command.getMemoryType())) {
            return;
        }
        String value = sanitizeMemoryValue(command.getMemoryValue(), VALUE_LIMIT);
        if (!isPersistableMemory(value)) {
            return;
        }
        String key = normalizeKey(StringUtils.hasText(command.getMemoryKey()) ? command.getMemoryKey() : value);
        if (!StringUtils.hasText(key)) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        UserMemory existing = userMemoryMapper.selectOne(new LambdaQueryWrapper<UserMemory>()
                .eq(UserMemory::getUserId, command.getUserId())
                .eq(UserMemory::getMemoryType, command.getMemoryType().trim())
                .eq(UserMemory::getMemoryKey, key)
                .last("LIMIT 1"));
        if (existing == null) {
            UserMemory memory = new UserMemory();
            memory.setUserId(command.getUserId());
            memory.setMemoryType(command.getMemoryType().trim());
            memory.setMemoryKey(key);
            memory.setMemoryValue(value);
            memory.setSourceType(normalizeSourceType(command.getSourceType()));
            memory.setSourceId(command.getSourceId());
            memory.setConfidence(normalizeConfidence(command.getConfidence()));
            memory.setWeight(normalizeWeightDelta(command.getWeightDelta()));
            memory.setStatus(STATUS_ACTIVE);
            memory.setSourceCount(1);
            memory.setSourceSummary(buildSourceSummary(command.getSourceType(), command.getSourceId(), 1));
            memory.setLastReinforcedAt(now);
            memory.setCreatedAt(now);
            memory.setUpdatedAt(now);
            userMemoryMapper.insert(memory);
            return;
        }
        existing.setMemoryValue(selectBetterValue(existing.getMemoryValue(), value));
        existing.setSourceType(normalizeSourceType(command.getSourceType()));
        existing.setSourceId(command.getSourceId());
        existing.setConfidence(strongerConfidence(existing.getConfidence(), command.getConfidence()));
        existing.setWeight(Math.min(MAX_WEIGHT, safeInt(existing.getWeight(), 1) + normalizeWeightDelta(command.getWeightDelta())));
        existing.setStatus(STATUS_ACTIVE);
        existing.setSourceCount(safeInt(existing.getSourceCount(), 0) + 1);
        existing.setSourceSummary(buildSourceSummary(command.getSourceType(), command.getSourceId(), existing.getSourceCount()));
        existing.setLastReinforcedAt(now);
        existing.setUpdatedAt(now);
        userMemoryMapper.updateById(existing);
    }

    @Override
    public UserMemorySummaryVO getSummary(Long userId) {
        UserMemorySummaryVO summary = new UserMemorySummaryVO();
        if (userId == null) {
            summary.setEmpty(true);
            return summary;
        }
        List<UserMemory> memories = userMemoryMapper.selectList(new LambdaQueryWrapper<UserMemory>()
                .eq(UserMemory::getUserId, userId)
                .eq(UserMemory::getStatus, STATUS_ACTIVE)
                .orderByDesc(UserMemory::getWeight)
                .orderByDesc(UserMemory::getLastReinforcedAt)
                .orderByDesc(UserMemory::getId)
                .last("LIMIT " + SUMMARY_LIMIT));
        summary.setGoals(top(memories, MemoryTypes.USER_GOAL, 2));
        summary.setTopWeaknesses(top(memories, MemoryTypes.WEAKNESS, 5));
        summary.setTopResumeRisks(top(memories, MemoryTypes.RESUME_RISK, 4));
        summary.setTopProjectRisks(top(memories, MemoryTypes.PROJECT_RISK, 4));
        summary.setRecentNextActions(recent(memories, MemoryTypes.NEXT_ACTION, 4));
        summary.setMasteredTopics(top(memories, MemoryTypes.MASTERED, 4));
        summary.setTargetRole(summary.getGoals().isEmpty() ? null : summary.getGoals().get(0).getValue());
        summary.setEmpty(memories.isEmpty());
        return summary;
    }

    @Override
    public boolean archiveMemory(Long userId, Long memoryId) {
        return updateMemoryStatus(userId, memoryId, STATUS_ARCHIVED);
    }

    @Override
    public boolean markMemoryInaccurate(Long userId, Long memoryId) {
        return updateMemoryStatus(userId, memoryId, STATUS_INACCURATE);
    }

    private boolean updateMemoryStatus(Long userId, Long memoryId, String status) {
        if (userId == null || memoryId == null || !StringUtils.hasText(status)) {
            return false;
        }
        UserMemory memory = userMemoryMapper.selectById(memoryId);
        if (memory == null || !userId.equals(memory.getUserId())) {
            return false;
        }
        memory.setStatus(status);
        memory.setUpdatedAt(LocalDateTime.now());
        userMemoryMapper.updateById(memory);
        return true;
    }

    @Override
    public List<MemorySemanticHit> semanticSearch(Long userId, String query, int topK) {
        List<MemorySemanticHit> hits = userSemanticMemoryService.search(userId, query, topK);
        if (hits.isEmpty() && userId != null && StringUtils.hasText(query)) {
            return fallbackKeywordSearch(userId, query, topK);
        }
        return hits;
    }

    @Override
    public int indexActiveSemanticMemory(Long userId) {
        return userSemanticMemoryService.indexActiveMemories(userId);
    }

    @Override
    public void sinkProjectReport(InterviewReport report, InterviewSession session) {
        if (report == null || report.getUserId() == null || report.getId() == null) {
            return;
        }
        Long userId = report.getUserId();
        Long sourceId = report.getId();
        reinforceText(userId, MemoryTypes.USER_GOAL, session == null ? null : session.getTargetRole(),
                MemorySourceTypes.PROJECT_REPORT, sourceId, MemoryConfidence.MEDIUM, 1);
        parseStringList(report.getWeaknesses()).forEach(value ->
                reinforceText(userId, MemoryTypes.WEAKNESS, value, MemorySourceTypes.PROJECT_REPORT, sourceId, MemoryConfidence.MEDIUM, 2));
        parseStringList(report.getWeaknesses()).stream()
                .filter(this::looksLikeProjectRisk)
                .forEach(value -> reinforceText(userId, MemoryTypes.PROJECT_RISK, value, MemorySourceTypes.PROJECT_REPORT, sourceId, MemoryConfidence.MEDIUM, 2));
        parseStringList(report.getSuggestions()).forEach(value ->
                reinforceText(userId, MemoryTypes.NEXT_ACTION, value, MemorySourceTypes.PROJECT_REPORT, sourceId, MemoryConfidence.MEDIUM, 1));
        if (safeInt(report.getTotalScore(), 0) >= 85) {
            parseStringList(report.getStrengths()).stream().limit(2).forEach(value ->
                    reinforceText(userId, MemoryTypes.MASTERED, value, MemorySourceTypes.PROJECT_REPORT, sourceId, MemoryConfidence.LOW, 1));
        }
    }

    @Override
    public void sinkQuestionReport(QuestionTrainingReport report, QuestionTrainingSession session, KnowledgeTopic topic) {
        if (report == null || report.getUserId() == null || report.getId() == null) {
            return;
        }
        Long userId = report.getUserId();
        Long sourceId = report.getId();
        reinforceText(userId, MemoryTypes.USER_GOAL, session == null ? null : session.getTargetRole(),
                MemorySourceTypes.QUESTION_REPORT, sourceId, MemoryConfidence.MEDIUM, 1);
        merge(parseStringList(report.getWeaknesses()), parseStringList(report.getKnowledgeGaps())).forEach(value ->
                reinforceText(userId, MemoryTypes.WEAKNESS, qualifyTopic(topic, value), MemorySourceTypes.QUESTION_REPORT, sourceId, MemoryConfidence.MEDIUM, 2));
        parseStringList(report.getSuggestions()).forEach(value ->
                reinforceText(userId, MemoryTypes.NEXT_ACTION, value, MemorySourceTypes.QUESTION_REPORT, sourceId, MemoryConfidence.MEDIUM, 1));
        if (safeInt(report.getTotalScore(), 0) >= 85) {
            String mastered = topic == null ? null : firstText(topic.getName(), topic.getCategory());
            reinforceText(userId, MemoryTypes.MASTERED, mastered, MemorySourceTypes.QUESTION_REPORT, sourceId, MemoryConfidence.MEDIUM, 1);
        }
    }

    @Override
    public void sinkMockInterviewReport(MockInterviewReport report, MockInterviewSession session) {
        if (report == null || report.getUserId() == null || report.getId() == null) {
            return;
        }
        Long userId = report.getUserId();
        Long sourceId = report.getId();
        reinforceText(userId, MemoryTypes.USER_GOAL, session == null ? null : session.getTargetRole(),
                MemorySourceTypes.MOCK_INTERVIEW_REPORT, sourceId, MemoryConfidence.HIGH, 1);
        merge(parseStringList(report.getWeaknessTags()), parseStringList(report.getWeaknesses())).forEach(value ->
                reinforceText(userId, MemoryTypes.WEAKNESS, value, MemorySourceTypes.MOCK_INTERVIEW_REPORT, sourceId, MemoryConfidence.HIGH, 2));
        merge(parseStringList(report.getHighRiskAnswers()), parseStringList(report.getWeaknesses())).stream()
                .filter(this::looksLikeProjectRisk)
                .forEach(value -> reinforceText(userId, MemoryTypes.PROJECT_RISK, value, MemorySourceTypes.MOCK_INTERVIEW_REPORT, sourceId, MemoryConfidence.HIGH, 2));
        parseStringList(report.getNextActions()).forEach(value ->
                reinforceText(userId, MemoryTypes.NEXT_ACTION, value, MemorySourceTypes.MOCK_INTERVIEW_REPORT, sourceId, MemoryConfidence.HIGH, 1));
    }

    @Override
    public void sinkResumeAnalysis(ResumeProfile profile, ResumeAnalysisResult result) {
        if (profile == null || result == null || profile.getUserId() == null || profile.getId() == null) {
            return;
        }
        Long userId = profile.getUserId();
        Long sourceId = profile.getId();
        reinforceText(userId, MemoryTypes.USER_GOAL, profile.getTargetRole(), MemorySourceTypes.RESUME_ANALYSIS, sourceId, MemoryConfidence.HIGH, 1);
        if (result.getRiskPoints() != null) {
            result.getRiskPoints().stream().filter(Objects::nonNull).limit(8).forEach(risk -> {
                String value = firstText(joinRisk(risk.getType(), risk.getSuggestion()), risk.getEvidence(), risk.getType());
                reinforceText(userId, MemoryTypes.RESUME_RISK, value, MemorySourceTypes.RESUME_ANALYSIS, sourceId, confidenceFromRiskLevel(risk.getLevel()), 2);
            });
        }
        if (result.getProjectExperiences() != null) {
            result.getProjectExperiences().stream().filter(Objects::nonNull).limit(5).forEach(project ->
                    safeList(project.getRiskPoints()).stream().limit(4).forEach(risk ->
                            reinforceText(userId, MemoryTypes.PROJECT_RISK, qualifyProject(project.getProjectName(), risk),
                                    MemorySourceTypes.RESUME_ANALYSIS, sourceId, MemoryConfidence.HIGH, 2)));
        }
    }

    @Override
    public void sinkAgentReview(AgentReview review) {
        if (review == null || review.getUserId() == null || review.getId() == null) {
            return;
        }
        Long userId = review.getUserId();
        Long sourceId = review.getId();
        parseStringList(review.getRecurringWeaknesses()).forEach(value ->
                reinforceText(userId, MemoryTypes.WEAKNESS, value, MemorySourceTypes.AGENT_REVIEW, sourceId, normalizeConfidence(review.getConfidence()), 3));
        parseStringList(review.getResumeRisks()).forEach(value ->
                reinforceText(userId, MemoryTypes.RESUME_RISK, value, MemorySourceTypes.AGENT_REVIEW, sourceId, normalizeConfidence(review.getConfidence()), 2));
        parseStringList(review.getCauseAnalysis()).stream()
                .filter(this::looksLikeProjectRisk)
                .forEach(value -> reinforceText(userId, MemoryTypes.PROJECT_RISK, value, MemorySourceTypes.AGENT_REVIEW, sourceId, normalizeConfidence(review.getConfidence()), 1));
        parseMapList(review.getHighRiskAnswers()).forEach(item -> {
            String value = firstText(stringValue(item.get("reason")), stringValue(item.get("answerSummary")), stringValue(item.get("riskType")));
            String type = looksLikeProjectRisk(value) ? MemoryTypes.PROJECT_RISK : MemoryTypes.WEAKNESS;
            reinforceText(userId, type, value, MemorySourceTypes.AGENT_REVIEW, sourceId, normalizeConfidence(review.getConfidence()), 2);
        });
        parseNextActions(review.getNextActions()).forEach(action -> {
            String value = firstText(joinRisk(action.getTitle(), action.getReason()), action.getTitle(), action.getReason());
            reinforceText(userId, MemoryTypes.NEXT_ACTION, value, MemorySourceTypes.AGENT_REVIEW, sourceId, normalizeConfidence(review.getConfidence()), 2);
        });
    }

    @Override
    public void sinkAbilitySnapshot(UserAbilitySnapshot snapshot) {
        if (snapshot == null || snapshot.getUserId() == null || snapshot.getId() == null) {
            return;
        }
        Integer score = snapshot.getScore();
        String dimension = firstText(snapshot.getDimensionName(), snapshot.getCategory(), snapshot.getDimensionCode());
        if (score != null && score >= 85) {
            reinforceText(snapshot.getUserId(), MemoryTypes.MASTERED, dimension,
                    MemorySourceTypes.ABILITY_SNAPSHOT, snapshot.getId(), MemoryConfidence.MEDIUM, 1);
            return;
        }
        if (score == null || score < 75) {
            List<String> tags = parseStringList(snapshot.getWeaknessTags());
            if (tags.isEmpty()) {
                reinforceText(snapshot.getUserId(), MemoryTypes.WEAKNESS, dimension,
                        MemorySourceTypes.ABILITY_SNAPSHOT, snapshot.getId(), MemoryConfidence.MEDIUM, 1);
            } else {
                tags.stream().limit(4).forEach(tag -> reinforceText(snapshot.getUserId(), MemoryTypes.WEAKNESS,
                        qualifyTopic(snapshot.getCategory(), tag), MemorySourceTypes.ABILITY_SNAPSHOT, snapshot.getId(), MemoryConfidence.MEDIUM, 1));
            }
        }
    }

    private void reinforceText(Long userId, String type, String value, String sourceType, Long sourceId, String confidence, int weightDelta) {
        MemorySinkCommand command = new MemorySinkCommand();
        command.setUserId(userId);
        command.setMemoryType(type);
        command.setMemoryValue(value);
        command.setSourceType(sourceType);
        command.setSourceId(sourceId);
        command.setConfidence(confidence);
        command.setWeightDelta(weightDelta);
        reinforce(command);
    }

    private List<UserMemoryItemVO> top(List<UserMemory> memories, String type, int limit) {
        return memories.stream()
                .filter(memory -> type.equals(memory.getMemoryType()))
                .sorted(Comparator.comparing((UserMemory memory) -> safeInt(memory.getWeight(), 0)).reversed()
                        .thenComparing(UserMemory::getLastReinforcedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(limit)
                .map(this::toItem)
                .toList();
    }

    private List<UserMemoryItemVO> recent(List<UserMemory> memories, String type, int limit) {
        return memories.stream()
                .filter(memory -> type.equals(memory.getMemoryType()))
                .sorted(Comparator.comparing(UserMemory::getLastReinforcedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(limit)
                .map(this::toItem)
                .toList();
    }

    private UserMemoryItemVO toItem(UserMemory memory) {
        return new UserMemoryItemVO(
                memory.getId(),
                memory.getMemoryType(),
                memory.getMemoryKey(),
                memory.getMemoryValue(),
                memory.getConfidence(),
                memory.getWeight(),
                memory.getLastReinforcedAt()
        );
    }

    private List<MemorySemanticHit> fallbackKeywordSearch(Long userId, String query, int topK) {
        String normalized = query.trim().toLowerCase(Locale.ROOT);
        return userMemoryMapper.selectList(new LambdaQueryWrapper<UserMemory>()
                        .eq(UserMemory::getUserId, userId)
                        .eq(UserMemory::getStatus, STATUS_ACTIVE)
                        .orderByDesc(UserMemory::getWeight)
                        .orderByDesc(UserMemory::getLastReinforcedAt)
                        .last("LIMIT 80"))
                .stream()
                .filter(memory -> StringUtils.hasText(memory.getMemoryValue())
                        && memory.getMemoryValue().toLowerCase(Locale.ROOT).contains(normalized))
                .limit(Math.min(Math.max(topK, 1), 8))
                .map(memory -> new MemorySemanticHit(
                        memory.getId(),
                        memory.getMemoryType(),
                        memory.getMemoryValue(),
                        memory.getConfidence(),
                        memory.getWeight(),
                        null))
                .toList();
    }

    private List<String> parseStringList(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            List<String> values = objectMapper.readValue(json, STRING_LIST);
            return safeList(values);
        } catch (Exception exception) {
            log.debug("Failed to parse memory source list");
            return List.of();
        }
    }

    private List<Map<String, Object>> parseMapList(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            List<Map<String, Object>> values = objectMapper.readValue(json, MAP_LIST);
            return values == null ? List.of() : values;
        } catch (Exception exception) {
            return List.of();
        }
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private List<NextActionVO> parseNextActions(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            List<NextActionVO> values = objectMapper.readValue(json, NEXT_ACTION_LIST);
            return values == null ? List.of() : values.stream().filter(Objects::nonNull).toList();
        } catch (Exception exception) {
            return List.of();
        }
    }

    private List<String> safeList(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .filter(StringUtils::hasText)
                .map(value -> sanitizeMemoryValue(value, VALUE_LIMIT))
                .filter(this::isPersistableMemory)
                .distinct()
                .toList();
    }

    private List<String> merge(List<String> first, List<String> second) {
        Set<String> merged = new LinkedHashSet<>();
        if (first != null) {
            merged.addAll(first);
        }
        if (second != null) {
            merged.addAll(second);
        }
        return new ArrayList<>(merged);
    }

    private String sanitizeMemoryValue(String value, int limit) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String sanitized = value.replaceAll("[\\r\\n\\t]+", " ").replaceAll("\\s{2,}", " ").trim();
        sanitized = EMAIL_PATTERN.matcher(sanitized).replaceAll("[邮箱]");
        sanitized = PHONE_PATTERN.matcher(sanitized).replaceAll("[手机号]");
        sanitized = sanitized.replaceAll("(?i)(api[_-]?key|secret|token)\\s*[:=]\\s*\\S+", "$1=[已隐藏]");
        return abbreviate(sanitized, limit);
    }

    private boolean isPersistableMemory(String value) {
        if (!StringUtils.hasText(value) || value.trim().length() < 2) {
            return false;
        }
        String text = value.trim();
        return !text.contains("样本不足")
                && !text.contains("暂无足够")
                && !text.contains("暂不识别")
                && !text.contains("上传并分析简历后")
                && !text.contains("本阶段未发现明显扣分项");
    }

    private String normalizeKey(String value) {
        String sanitized = sanitizeMemoryValue(value, KEY_LIMIT);
        if (!StringUtils.hasText(sanitized)) {
            return null;
        }
        return sanitized.toLowerCase(Locale.ROOT)
                .replaceAll("[`\"'，。！？!?:：；;、（）()\\[\\]{}<>《》]", " ")
                .replaceAll("\\s{2,}", " ")
                .trim();
    }

    private String normalizeSourceType(String value) {
        return StringUtils.hasText(value) ? abbreviate(value.trim(), 64) : null;
    }

    private String normalizeConfidence(String value) {
        if (MemoryConfidence.HIGH.equals(value) || MemoryConfidence.MEDIUM.equals(value) || MemoryConfidence.LOW.equals(value)) {
            return value;
        }
        return MemoryConfidence.MEDIUM;
    }

    private String strongerConfidence(String first, String second) {
        return confidenceRank(second) > confidenceRank(first) ? normalizeConfidence(second) : normalizeConfidence(first);
    }

    private int confidenceRank(String value) {
        return switch (normalizeConfidence(value)) {
            case MemoryConfidence.HIGH -> 3;
            case MemoryConfidence.MEDIUM -> 2;
            default -> 1;
        };
    }

    private String confidenceFromRiskLevel(String level) {
        if ("HIGH".equalsIgnoreCase(level) || "高".equals(level)) {
            return MemoryConfidence.HIGH;
        }
        if ("LOW".equalsIgnoreCase(level) || "低".equals(level)) {
            return MemoryConfidence.LOW;
        }
        return MemoryConfidence.MEDIUM;
    }

    private int normalizeWeightDelta(Integer value) {
        return value == null || value < 1 ? 1 : Math.min(value, 5);
    }

    private int safeInt(Integer value, int defaultValue) {
        return value == null ? defaultValue : value;
    }

    private String selectBetterValue(String current, String candidate) {
        if (!StringUtils.hasText(current)) {
            return candidate;
        }
        if (!StringUtils.hasText(candidate)) {
            return current;
        }
        return candidate.length() > current.length() && candidate.length() <= VALUE_LIMIT ? candidate : current;
    }

    private String buildSourceSummary(String sourceType, Long sourceId, int count) {
        String source = StringUtils.hasText(sourceType) ? sourceType.trim() : "UNKNOWN";
        String latest = sourceId == null ? source : source + "#" + sourceId;
        return abbreviate("累计强化 " + count + " 次；最近来源：" + latest, 512);
    }

    private boolean looksLikeProjectRisk(String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        return value.contains("项目")
                || value.contains("简历")
                || value.contains("个人贡献")
                || value.contains("链路")
                || value.contains("技术细节")
                || value.contains("落地")
                || value.contains("指标")
                || value.contains("场景设计");
    }

    private String qualifyTopic(KnowledgeTopic topic, String value) {
        if (topic == null) {
            return value;
        }
        return qualifyTopic(firstText(topic.getName(), topic.getCategory()), value);
    }

    private String qualifyTopic(String prefix, String value) {
        if (!StringUtils.hasText(prefix) || !StringUtils.hasText(value) || value.contains(prefix)) {
            return value;
        }
        return prefix.trim() + "：" + value.trim();
    }

    private String qualifyProject(String projectName, String value) {
        if (!StringUtils.hasText(projectName) || !StringUtils.hasText(value) || value.contains(projectName)) {
            return value;
        }
        return projectName.trim() + "：" + value.trim();
    }

    private String joinRisk(String first, String second) {
        if (!StringUtils.hasText(first)) {
            return second;
        }
        if (!StringUtils.hasText(second)) {
            return first;
        }
        return first.trim() + "：" + second.trim();
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private String abbreviate(String value, int limit) {
        if (!StringUtils.hasText(value) || value.length() <= limit) {
            return value;
        }
        return value.substring(0, limit);
    }
}
