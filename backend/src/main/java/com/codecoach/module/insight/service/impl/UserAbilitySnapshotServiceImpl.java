package com.codecoach.module.insight.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.codecoach.module.insight.constant.AbilityDimensionCodes;
import com.codecoach.module.insight.constant.AbilitySnapshotSourceType;
import com.codecoach.module.insight.entity.UserAbilitySnapshot;
import com.codecoach.module.insight.mapper.UserAbilitySnapshotMapper;
import com.codecoach.module.insight.service.UserAbilitySnapshotService;
import com.codecoach.module.interview.entity.InterviewSession;
import com.codecoach.module.knowledge.entity.KnowledgeTopic;
import com.codecoach.module.question.entity.QuestionTrainingReport;
import com.codecoach.module.question.entity.QuestionTrainingSession;
import com.codecoach.module.report.entity.InterviewReport;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class UserAbilitySnapshotServiceImpl implements UserAbilitySnapshotService {

    private static final Logger log = LoggerFactory.getLogger(UserAbilitySnapshotServiceImpl.class);

    private static final String CATEGORY_PROJECT_INTERVIEW = "项目面试";

    private static final String CATEGORY_GENERAL_EXPRESSION = "通用表达";

    private static final int MAX_WEAKNESS_TAGS = 10;

    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };

    private static final Map<String, String> QUESTION_CATEGORY_DIMENSION_CODE_MAP = Map.ofEntries(
            Map.entry("Java 基础", AbilityDimensionCodes.JAVA_BASIC),
            Map.entry("JVM", AbilityDimensionCodes.JVM),
            Map.entry("JUC", AbilityDimensionCodes.JUC),
            Map.entry("MySQL", AbilityDimensionCodes.MYSQL),
            Map.entry("Redis", AbilityDimensionCodes.REDIS),
            Map.entry("Spring", AbilityDimensionCodes.SPRING),
            Map.entry("MQ", AbilityDimensionCodes.MQ),
            Map.entry("计算机网络", AbilityDimensionCodes.NETWORK),
            Map.entry("操作系统", AbilityDimensionCodes.OS),
            Map.entry("分布式", AbilityDimensionCodes.DISTRIBUTED)
    );

    private final UserAbilitySnapshotMapper userAbilitySnapshotMapper;

    private final ObjectMapper objectMapper;

    public UserAbilitySnapshotServiceImpl(
            UserAbilitySnapshotMapper userAbilitySnapshotMapper,
            ObjectMapper objectMapper
    ) {
        this.userAbilitySnapshotMapper = userAbilitySnapshotMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public void createProjectReportSnapshots(InterviewReport report, InterviewSession session) {
        try {
            if (report == null || report.getId() == null) {
                return;
            }
            String weaknessTags = toJsonString(parseStringListJson(report.getWeaknesses()));
            String difficulty = session == null ? null : session.getDifficulty();

            createSnapshotIfAbsent(new AbilitySnapshotCreateCommand(
                    report.getUserId(),
                    AbilitySnapshotSourceType.PROJECT_REPORT,
                    report.getId(),
                    AbilityDimensionCodes.PROJECT_BACKGROUND,
                    "项目背景表达",
                    CATEGORY_PROJECT_INTERVIEW,
                    report.getTotalScore(),
                    difficulty,
                    report.getSummary(),
                    weaknessTags
            ));
            createSnapshotIfAbsent(new AbilitySnapshotCreateCommand(
                    report.getUserId(),
                    AbilitySnapshotSourceType.PROJECT_REPORT,
                    report.getId(),
                    AbilityDimensionCodes.TECH_SELECTION,
                    "技术选型解释",
                    CATEGORY_PROJECT_INTERVIEW,
                    report.getTotalScore(),
                    difficulty,
                    report.getSummary(),
                    weaknessTags
            ));
            createSnapshotIfAbsent(new AbilitySnapshotCreateCommand(
                    report.getUserId(),
                    AbilitySnapshotSourceType.PROJECT_REPORT,
                    report.getId(),
                    AbilityDimensionCodes.FOLLOW_UP_RESPONSE,
                    "追问应对能力",
                    CATEGORY_GENERAL_EXPRESSION,
                    report.getTotalScore(),
                    difficulty,
                    report.getSummary(),
                    weaknessTags
            ));
        } catch (Exception exception) {
            log.warn("Failed to create project ability snapshots, reportId={}", report == null ? null : report.getId(), exception);
        }
    }

    @Override
    public void createQuestionReportSnapshot(
            QuestionTrainingReport report,
            QuestionTrainingSession session,
            KnowledgeTopic topic
    ) {
        try {
            if (report == null || report.getId() == null) {
                return;
            }
            String category = topic == null ? null : topic.getCategory();
            String dimensionCode = mapQuestionDimensionCode(category);
            String dimensionName = StringUtils.hasText(category) ? category : "未知知识分类";
            String difficulty = session == null ? null : session.getDifficulty();
            String weaknessTags = toJsonString(mergeWeaknessTags(
                    parseStringListJson(report.getWeaknesses()),
                    parseStringListJson(report.getKnowledgeGaps())
            ));

            createSnapshotIfAbsent(new AbilitySnapshotCreateCommand(
                    report.getUserId(),
                    AbilitySnapshotSourceType.QUESTION_REPORT,
                    report.getId(),
                    dimensionCode,
                    dimensionName,
                    category,
                    report.getTotalScore(),
                    difficulty,
                    report.getSummary(),
                    weaknessTags
            ));
        } catch (Exception exception) {
            log.warn("Failed to create question ability snapshot, reportId={}", report == null ? null : report.getId(), exception);
        }
    }

    private void createSnapshotIfAbsent(AbilitySnapshotCreateCommand command) {
        if (command == null
                || command.userId() == null
                || !StringUtils.hasText(command.sourceType())
                || command.sourceId() == null
                || !StringUtils.hasText(command.dimensionCode())) {
            return;
        }

        Long count = userAbilitySnapshotMapper.selectCount(new LambdaQueryWrapper<UserAbilitySnapshot>()
                .eq(UserAbilitySnapshot::getSourceType, command.sourceType())
                .eq(UserAbilitySnapshot::getSourceId, command.sourceId())
                .eq(UserAbilitySnapshot::getDimensionCode, command.dimensionCode()));
        if (count != null && count > 0) {
            return;
        }

        UserAbilitySnapshot snapshot = new UserAbilitySnapshot();
        snapshot.setUserId(command.userId());
        snapshot.setSourceType(command.sourceType());
        snapshot.setSourceId(command.sourceId());
        snapshot.setDimensionCode(command.dimensionCode());
        snapshot.setDimensionName(command.dimensionName());
        snapshot.setCategory(command.category());
        snapshot.setScore(command.score());
        snapshot.setDifficulty(command.difficulty());
        snapshot.setEvidence(command.evidence());
        snapshot.setWeaknessTags(command.weaknessTags());
        snapshot.setCreatedAt(LocalDateTime.now());
        userAbilitySnapshotMapper.insert(snapshot);
    }

    private String mapQuestionDimensionCode(String category) {
        if (!StringUtils.hasText(category)) {
            return AbilityDimensionCodes.UNKNOWN;
        }
        return QUESTION_CATEGORY_DIMENSION_CODE_MAP.getOrDefault(category, AbilityDimensionCodes.UNKNOWN);
    }

    private List<String> parseStringListJson(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            List<String> values = objectMapper.readValue(json, STRING_LIST_TYPE);
            if (values == null) {
                return List.of();
            }
            return values.stream()
                    .filter(StringUtils::hasText)
                    .map(String::trim)
                    .toList();
        } catch (Exception exception) {
            log.warn("Failed to parse ability weakness json: {}", trimForLog(json), exception);
            return List.of();
        }
    }

    private List<String> mergeWeaknessTags(List<String> first, List<String> second) {
        Set<String> merged = new LinkedHashSet<>();
        if (first != null) {
            merged.addAll(first);
        }
        if (second != null) {
            merged.addAll(second);
        }
        return new ArrayList<>(merged).stream()
                .limit(MAX_WEAKNESS_TAGS)
                .toList();
    }

    private String toJsonString(List<String> values) {
        List<String> safeValues = values == null ? List.of() : values.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .limit(MAX_WEAKNESS_TAGS)
                .toList();
        try {
            return objectMapper.writeValueAsString(safeValues);
        } catch (JsonProcessingException exception) {
            log.warn("Failed to serialize ability weakness tags", exception);
            return "[]";
        }
    }

    private String trimForLog(String value) {
        if (value == null || value.length() <= 200) {
            return value;
        }
        return value.substring(0, 200);
    }

    private record AbilitySnapshotCreateCommand(
            Long userId,
            String sourceType,
            Long sourceId,
            String dimensionCode,
            String dimensionName,
            String category,
            Integer score,
            String difficulty,
            String evidence,
            String weaknessTags
    ) {
    }
}
