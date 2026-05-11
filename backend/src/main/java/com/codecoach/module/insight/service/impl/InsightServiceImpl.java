package com.codecoach.module.insight.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.codecoach.module.insight.constant.AbilitySnapshotSourceType;
import com.codecoach.module.insight.entity.UserAbilitySnapshot;
import com.codecoach.module.insight.mapper.UserAbilitySnapshotMapper;
import com.codecoach.module.insight.service.InsightService;
import com.codecoach.module.insight.vo.AbilityDimensionVO;
import com.codecoach.module.insight.vo.InsightOverviewVO;
import com.codecoach.module.insight.vo.RecentTrendVO;
import com.codecoach.module.insight.vo.WeaknessInsightVO;
import com.codecoach.security.UserContext;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class InsightServiceImpl implements InsightService {

    private static final Logger log = LoggerFactory.getLogger(InsightServiceImpl.class);

    private static final int DEFAULT_WEAKNESS_LIMIT = 10;

    private static final int DEFAULT_TREND_LIMIT = 10;

    private static final int MAX_LIMIT = 50;

    private static final int WEAKNESS_QUERY_LIMIT = 100;

    private static final int TREND_QUERY_LIMIT = 200;

    private static final int RECENT_SOURCE_LIMIT = 5;

    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };

    private final UserAbilitySnapshotMapper userAbilitySnapshotMapper;

    private final ObjectMapper objectMapper;

    public InsightServiceImpl(
            UserAbilitySnapshotMapper userAbilitySnapshotMapper,
            ObjectMapper objectMapper
    ) {
        this.userAbilitySnapshotMapper = userAbilitySnapshotMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public InsightOverviewVO getOverview() {
        List<UserAbilitySnapshot> snapshots = listUserSnapshotsAsc(UserContext.getCurrentUserId());
        if (snapshots.isEmpty()) {
            return new InsightOverviewVO(0L, 0L, 0L, null, null, null, null, null);
        }

        Set<SourceKey> allSources = snapshots.stream()
                .map(this::toSourceKey)
                .collect(Collectors.toSet());
        Set<SourceKey> projectSources = snapshots.stream()
                .filter(snapshot -> AbilitySnapshotSourceType.PROJECT_REPORT.equals(snapshot.getSourceType()))
                .map(this::toSourceKey)
                .collect(Collectors.toSet());
        Set<SourceKey> questionSources = snapshots.stream()
                .filter(snapshot -> AbilitySnapshotSourceType.QUESTION_REPORT.equals(snapshot.getSourceType()))
                .map(this::toSourceKey)
                .collect(Collectors.toSet());

        Integer averageScore = averageScore(snapshots);
        Integer recentAverageScore = averageScore(groupBySourceDesc(snapshots).values().stream()
                .limit(RECENT_SOURCE_LIMIT)
                .map(this::averageScore)
                .filter(Objects::nonNull)
                .toList());
        String bestDimension = findDimensionByAverageScore(snapshots, true);
        String weakestDimension = findDimensionByAverageScore(snapshots, false);
        LocalDateTime lastTrainingAt = snapshots.stream()
                .map(UserAbilitySnapshot::getCreatedAt)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        return new InsightOverviewVO(
                (long) allSources.size(),
                (long) projectSources.size(),
                (long) questionSources.size(),
                averageScore,
                recentAverageScore,
                bestDimension,
                weakestDimension,
                lastTrainingAt
        );
    }

    @Override
    public List<AbilityDimensionVO> getAbilityDimensions() {
        List<UserAbilitySnapshot> snapshots = listUserSnapshotsDesc(UserContext.getCurrentUserId());
        return snapshots.stream()
                .filter(snapshot -> StringUtils.hasText(snapshot.getDimensionCode()))
                .collect(Collectors.groupingBy(
                        UserAbilitySnapshot::getDimensionCode,
                        LinkedHashMap::new,
                        Collectors.toList()
                ))
                .entrySet()
                .stream()
                .map(entry -> toAbilityDimensionVO(entry.getKey(), entry.getValue()))
                .sorted(Comparator
                        .comparing(AbilityDimensionVO::getScore, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(AbilityDimensionVO::getDimensionCode, Comparator.nullsLast(String::compareTo)))
                .toList();
    }

    @Override
    public List<WeaknessInsightVO> getWeaknesses(Integer limit) {
        int normalizedLimit = normalizeLimit(limit, DEFAULT_WEAKNESS_LIMIT);
        List<UserAbilitySnapshot> snapshots = listRecentUserSnapshots(UserContext.getCurrentUserId(), WEAKNESS_QUERY_LIMIT);
        Map<String, WeaknessAccumulator> accumulatorMap = new LinkedHashMap<>();

        for (UserAbilitySnapshot snapshot : snapshots) {
            List<String> weaknessTags = parseWeaknessTags(snapshot.getWeaknessTags());
            for (String tag : weaknessTags) {
                String keyword = tag.trim();
                if (!StringUtils.hasText(keyword)) {
                    continue;
                }
                WeaknessAccumulator accumulator = accumulatorMap.computeIfAbsent(keyword, WeaknessAccumulator::new);
                accumulator.increase();
                if (accumulator.latestAt == null
                        || compareCreatedAt(snapshot.getCreatedAt(), accumulator.latestAt) > 0) {
                    accumulator.latestAt = snapshot.getCreatedAt();
                    accumulator.relatedDimension = snapshot.getDimensionName();
                    accumulator.latestEvidence = snapshot.getEvidence();
                    accumulator.latestSourceType = snapshot.getSourceType();
                    accumulator.latestSourceId = snapshot.getSourceId();
                }
            }
        }

        return accumulatorMap.values()
                .stream()
                .sorted(Comparator
                        .comparing(WeaknessAccumulator::count).reversed()
                        .thenComparing(WeaknessAccumulator::latestAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(normalizedLimit)
                .map(accumulator -> new WeaknessInsightVO(
                        accumulator.keyword,
                        accumulator.count,
                        accumulator.relatedDimension,
                        accumulator.latestEvidence,
                        accumulator.latestSourceType,
                        accumulator.latestSourceId,
                        accumulator.latestAt
                ))
                .toList();
    }

    @Override
    public List<RecentTrendVO> getRecentTrend(Integer limit) {
        int normalizedLimit = normalizeLimit(limit, DEFAULT_TREND_LIMIT);
        List<UserAbilitySnapshot> snapshots = listRecentUserSnapshots(UserContext.getCurrentUserId(), TREND_QUERY_LIMIT);
        return groupBySourceDesc(snapshots).values()
                .stream()
                .limit(normalizedLimit)
                .map(this::toRecentTrendVO)
                .toList();
    }

    private List<UserAbilitySnapshot> listUserSnapshotsAsc(Long userId) {
        LambdaQueryWrapper<UserAbilitySnapshot> queryWrapper = new LambdaQueryWrapper<UserAbilitySnapshot>()
                .eq(UserAbilitySnapshot::getUserId, userId)
                .orderByAsc(UserAbilitySnapshot::getCreatedAt)
                .orderByAsc(UserAbilitySnapshot::getId);
        return userAbilitySnapshotMapper.selectList(queryWrapper);
    }

    private List<UserAbilitySnapshot> listUserSnapshotsDesc(Long userId) {
        LambdaQueryWrapper<UserAbilitySnapshot> queryWrapper = new LambdaQueryWrapper<UserAbilitySnapshot>()
                .eq(UserAbilitySnapshot::getUserId, userId)
                .orderByDesc(UserAbilitySnapshot::getCreatedAt)
                .orderByDesc(UserAbilitySnapshot::getId);
        return userAbilitySnapshotMapper.selectList(queryWrapper);
    }

    private List<UserAbilitySnapshot> listRecentUserSnapshots(Long userId, int limit) {
        LambdaQueryWrapper<UserAbilitySnapshot> queryWrapper = new LambdaQueryWrapper<UserAbilitySnapshot>()
                .eq(UserAbilitySnapshot::getUserId, userId)
                .orderByDesc(UserAbilitySnapshot::getCreatedAt)
                .orderByDesc(UserAbilitySnapshot::getId)
                .last("LIMIT " + limit);
        return userAbilitySnapshotMapper.selectList(queryWrapper);
    }

    private AbilityDimensionVO toAbilityDimensionVO(String dimensionCode, List<UserAbilitySnapshot> snapshots) {
        List<UserAbilitySnapshot> sorted = sortDesc(snapshots);
        UserAbilitySnapshot latest = sorted.isEmpty() ? null : sorted.get(0);
        Integer score = averageScore(sorted.stream().limit(5).toList());
        return new AbilityDimensionVO(
                dimensionCode,
                latest == null ? null : latest.getDimensionName(),
                latest == null ? null : latest.getCategory(),
                score,
                calculateTrend(sorted),
                snapshots.size(),
                latest == null ? null : latest.getEvidence()
        );
    }

    private String calculateTrend(List<UserAbilitySnapshot> sortedSnapshots) {
        List<Integer> scores = sortedSnapshots.stream()
                .map(UserAbilitySnapshot::getScore)
                .filter(Objects::nonNull)
                .limit(2)
                .toList();
        if (scores.size() < 2) {
            return "UNKNOWN";
        }
        int diff = scores.get(0) - scores.get(1);
        if (diff > 3) {
            return "UP";
        }
        if (diff < -3) {
            return "DOWN";
        }
        return "FLAT";
    }

    private String findDimensionByAverageScore(List<UserAbilitySnapshot> snapshots, boolean best) {
        Comparator<Map.Entry<String, List<UserAbilitySnapshot>>> comparator = Comparator.comparing(
                entry -> averageScore(entry.getValue()),
                Comparator.nullsLast(Integer::compareTo)
        );
        if (best) {
            comparator = comparator.reversed();
        }
        return snapshots.stream()
                .filter(snapshot -> StringUtils.hasText(snapshot.getDimensionCode()))
                .collect(Collectors.groupingBy(UserAbilitySnapshot::getDimensionCode))
                .entrySet()
                .stream()
                .filter(entry -> averageScore(entry.getValue()) != null)
                .sorted(comparator)
                .map(entry -> latestSnapshot(entry.getValue()))
                .filter(Objects::nonNull)
                .map(UserAbilitySnapshot::getDimensionName)
                .findFirst()
                .orElse(null);
    }

    private Map<SourceKey, List<UserAbilitySnapshot>> groupBySourceDesc(List<UserAbilitySnapshot> snapshots) {
        Map<SourceKey, List<UserAbilitySnapshot>> grouped = new LinkedHashMap<>();
        for (UserAbilitySnapshot snapshot : sortDesc(snapshots)) {
            grouped.computeIfAbsent(toSourceKey(snapshot), key -> new ArrayList<>()).add(snapshot);
        }
        return grouped;
    }

    private RecentTrendVO toRecentTrendVO(List<UserAbilitySnapshot> snapshots) {
        List<UserAbilitySnapshot> sorted = sortDesc(snapshots);
        UserAbilitySnapshot latest = sorted.isEmpty() ? null : sorted.get(0);
        String trainingType = latest == null ? null : latest.getSourceType();
        String dimensionName = AbilitySnapshotSourceType.PROJECT_REPORT.equals(trainingType)
                ? "项目训练"
                : latest == null ? null : latest.getDimensionName();
        LocalDateTime createdAt = latest == null ? null : latest.getCreatedAt();
        return new RecentTrendVO(
                createdAt == null ? null : createdAt.toLocalDate().toString(),
                averageScore(sorted),
                trainingType,
                dimensionName,
                latest == null ? null : latest.getSourceId(),
                createdAt
        );
    }

    private List<UserAbilitySnapshot> sortDesc(List<UserAbilitySnapshot> snapshots) {
        return snapshots.stream()
                .sorted(Comparator
                        .comparing(UserAbilitySnapshot::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(UserAbilitySnapshot::getId, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private UserAbilitySnapshot latestSnapshot(List<UserAbilitySnapshot> snapshots) {
        List<UserAbilitySnapshot> sorted = sortDesc(snapshots);
        return sorted.isEmpty() ? null : sorted.get(0);
    }

    private Integer averageScore(List<?> values) {
        List<Integer> scores = values.stream()
                .map(value -> {
                    if (value instanceof UserAbilitySnapshot snapshot) {
                        return snapshot.getScore();
                    }
                    if (value instanceof Integer score) {
                        return score;
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .toList();
        if (scores.isEmpty()) {
            return null;
        }
        double average = scores.stream().mapToInt(Integer::intValue).average().orElse(0);
        return (int) Math.round(average);
    }

    private List<String> parseWeaknessTags(String json) {
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
            log.warn("Failed to parse snapshot weakness tags: {}", trimForLog(json), exception);
            return List.of();
        }
    }

    private int normalizeLimit(Integer limit, int defaultLimit) {
        if (limit == null || limit < 1) {
            return defaultLimit;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    private SourceKey toSourceKey(UserAbilitySnapshot snapshot) {
        return new SourceKey(snapshot.getSourceType(), snapshot.getSourceId());
    }

    private int compareCreatedAt(LocalDateTime left, LocalDateTime right) {
        if (left == null && right == null) {
            return 0;
        }
        if (left == null) {
            return -1;
        }
        if (right == null) {
            return 1;
        }
        return left.compareTo(right);
    }

    private String trimForLog(String value) {
        if (value == null || value.length() <= 200) {
            return value;
        }
        return value.substring(0, 200);
    }

    private record SourceKey(String sourceType, Long sourceId) {
    }

    private static class WeaknessAccumulator {

        private final String keyword;

        private int count;

        private String relatedDimension;

        private String latestEvidence;

        private String latestSourceType;

        private Long latestSourceId;

        private LocalDateTime latestAt;

        private WeaknessAccumulator(String keyword) {
            this.keyword = keyword;
        }

        private void increase() {
            this.count++;
        }

        private Integer count() {
            return count;
        }

        private LocalDateTime latestAt() {
            return latestAt;
        }
    }
}
