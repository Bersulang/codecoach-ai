package com.codecoach.module.insight.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.codecoach.module.insight.constant.AbilitySnapshotSourceType;
import com.codecoach.module.insight.entity.UserAbilitySnapshot;
import com.codecoach.module.insight.mapper.UserAbilitySnapshotMapper;
import com.codecoach.module.insight.service.InsightRecommendationService;
import com.codecoach.module.insight.vo.LearningRecommendationVO;
import com.codecoach.module.rag.constant.RagConstants;
import com.codecoach.module.rag.model.RagRetrievedChunk;
import com.codecoach.module.rag.model.RagSearchRequest;
import com.codecoach.module.rag.model.RagSearchResponse;
import com.codecoach.module.rag.service.RagRetrievalService;
import com.codecoach.security.UserContext;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.util.StringUtils;

@Service
public class InsightRecommendationServiceImpl implements InsightRecommendationService {

    private static final Logger log = LoggerFactory.getLogger(InsightRecommendationServiceImpl.class);

    private static final int SNAPSHOT_LIMIT = 100;

    private static final int RECOMMENDATION_LIMIT = 5;

    private static final int WEAKNESS_TAG_LIMIT = 10;

    private static final int LOW_SCORE_THRESHOLD = 80;

    private static final int EVIDENCE_QUERY_LIMIT = 160;

    private static final int EVIDENCE_REASON_LIMIT = 80;

    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };

    private final UserAbilitySnapshotMapper userAbilitySnapshotMapper;

    private final RagRetrievalService ragRetrievalService;

    private final ObjectMapper objectMapper;

    public InsightRecommendationServiceImpl(
            UserAbilitySnapshotMapper userAbilitySnapshotMapper,
            RagRetrievalService ragRetrievalService,
            ObjectMapper objectMapper
    ) {
        this.userAbilitySnapshotMapper = userAbilitySnapshotMapper;
        this.ragRetrievalService = ragRetrievalService;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<LearningRecommendationVO> getLearningRecommendations() {
        Long userId = UserContext.getCurrentUserId();
        List<UserAbilitySnapshot> snapshots = listRecentSnapshots(userId);
        if (snapshots.isEmpty()) {
            return List.of();
        }

        List<RecommendationSeed> seeds = buildSeeds(snapshots);
        if (seeds.isEmpty()) {
            return List.of();
        }

        Map<Long, LearningRecommendationVO> recommendationMap = new LinkedHashMap<>();
        for (RecommendationSeed seed : seeds) {
            List<RagRetrievedChunk> chunks = retrieveChunks(seed);
            mergeRecommendations(recommendationMap, seed, chunks);
            if (recommendationMap.size() >= RECOMMENDATION_LIMIT) {
                break;
            }
        }

        return recommendationMap.values()
                .stream()
                .sorted(Comparator.comparing(LearningRecommendationVO::getScore, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(RECOMMENDATION_LIMIT)
                .toList();
    }

    private List<UserAbilitySnapshot> listRecentSnapshots(Long userId) {
        LambdaQueryWrapper<UserAbilitySnapshot> queryWrapper = new LambdaQueryWrapper<UserAbilitySnapshot>()
                .eq(UserAbilitySnapshot::getUserId, userId)
                .orderByDesc(UserAbilitySnapshot::getCreatedAt)
                .orderByDesc(UserAbilitySnapshot::getId)
                .last("LIMIT " + SNAPSHOT_LIMIT);
        return userAbilitySnapshotMapper.selectList(queryWrapper);
    }

    private List<RecommendationSeed> buildSeeds(List<UserAbilitySnapshot> snapshots) {
        List<UserAbilitySnapshot> candidates = snapshots.stream()
                .filter(snapshot -> !parseWeaknessTags(snapshot.getWeaknessTags()).isEmpty()
                        || (snapshot.getScore() != null && snapshot.getScore() < LOW_SCORE_THRESHOLD))
                .sorted(Comparator
                        .comparing((UserAbilitySnapshot snapshot) -> AbilitySnapshotSourceType.QUESTION_REPORT.equals(snapshot.getSourceType()) ? 0 : 1)
                        .thenComparing(snapshot -> snapshot.getScore() == null ? 101 : snapshot.getScore())
                        .thenComparing(UserAbilitySnapshot::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        List<UserAbilitySnapshot> source = candidates.isEmpty() ? snapshots : candidates;
        Map<String, List<UserAbilitySnapshot>> grouped = new LinkedHashMap<>();
        for (UserAbilitySnapshot snapshot : source) {
            String key = StringUtils.hasText(snapshot.getCategory())
                    ? snapshot.getCategory()
                    : snapshot.getDimensionName();
            if (!StringUtils.hasText(key)) {
                key = snapshot.getDimensionCode();
            }
            if (!StringUtils.hasText(key)) {
                continue;
            }
            grouped.computeIfAbsent(key, ignored -> new ArrayList<>()).add(snapshot);
        }

        return grouped.values()
                .stream()
                .map(this::toSeed)
                .filter(Objects::nonNull)
                .limit(5)
                .toList();
    }

    private RecommendationSeed toSeed(List<UserAbilitySnapshot> snapshots) {
        if (snapshots.isEmpty()) {
            return null;
        }
        UserAbilitySnapshot latest = snapshots.stream()
                .max(Comparator
                        .comparing(UserAbilitySnapshot::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(UserAbilitySnapshot::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(snapshots.get(0));

        Set<String> tags = new LinkedHashSet<>();
        for (UserAbilitySnapshot snapshot : snapshots) {
            tags.addAll(parseWeaknessTags(snapshot.getWeaknessTags()));
            if (tags.size() >= WEAKNESS_TAG_LIMIT) {
                break;
            }
        }
        List<String> weaknessTags = tags.stream().limit(WEAKNESS_TAG_LIMIT).toList();
        String query = buildQuery(latest, weaknessTags);
        return new RecommendationSeed(latest, weaknessTags, query);
    }

    private String buildQuery(UserAbilitySnapshot snapshot, List<String> weaknessTags) {
        List<String> parts = new ArrayList<>();
        addIfText(parts, snapshot.getCategory());
        addIfText(parts, snapshot.getDimensionName());
        for (String tag : weaknessTags) {
            addIfText(parts, tag);
        }
        addIfText(parts, truncate(snapshot.getEvidence(), EVIDENCE_QUERY_LIMIT));
        return String.join(" ", parts);
    }

    private List<RagRetrievedChunk> retrieveChunks(RecommendationSeed seed) {
        try {
            if (StringUtils.hasText(seed.snapshot.getCategory())) {
                List<RagRetrievedChunk> chunks = search(seed.query, Map.of("category", seed.snapshot.getCategory()));
                if (!chunks.isEmpty()) {
                    return chunks;
                }
            }
            return search(seed.query, Map.of());
        } catch (Exception exception) {
            log.warn(
                    "RAG learning recommendation retrieval failed, userId={}, dimension={}, error={}",
                    seed.snapshot.getUserId(),
                    seed.snapshot.getDimensionName(),
                    abbreviate(exception.getMessage())
            );
            return List.of();
        }
    }

    private List<RagRetrievedChunk> search(String query, Map<String, Object> filter) {
        if (!StringUtils.hasText(query)) {
            return List.of();
        }
        RagSearchRequest request = new RagSearchRequest();
        request.setQuery(query);
        request.setSourceTypes(List.of(RagConstants.SOURCE_TYPE_KNOWLEDGE_ARTICLE));
        request.setTopK(RECOMMENDATION_LIMIT);
        request.setFilter(filter);
        RagSearchResponse response = ragRetrievalService.search(request);
        if (response == null || response.getChunks() == null) {
            return List.of();
        }
        return response.getChunks();
    }

    private void mergeRecommendations(
            Map<Long, LearningRecommendationVO> recommendationMap,
            RecommendationSeed seed,
            List<RagRetrievedChunk> chunks
    ) {
        for (RagRetrievedChunk chunk : chunks) {
            Long articleId = chunk.getArticleId();
            if (articleId == null) {
                continue;
            }
            LearningRecommendationVO current = recommendationMap.get(articleId);
            if (current != null && compareScore(current.getScore(), chunk.getScore()) >= 0) {
                continue;
            }
            recommendationMap.put(articleId, toRecommendation(seed, chunk));
        }
    }

    private LearningRecommendationVO toRecommendation(RecommendationSeed seed, RagRetrievedChunk chunk) {
        Long articleId = chunk.getArticleId();
        return new LearningRecommendationVO(
                StringUtils.hasText(chunk.getTitle()) ? chunk.getTitle() : "知识学习卡片",
                buildReason(seed),
                articleId,
                chunk.getTopicId(),
                chunk.getCategory(),
                chunk.getTopicName(),
                chunk.getSection(),
                chunk.getScore(),
                buildEvidence(seed),
                articleId == null ? "/learn" : "/learn/articles/" + articleId
        );
    }

    private String buildReason(RecommendationSeed seed) {
        String evidence = truncate(seed.snapshot.getEvidence(), EVIDENCE_REASON_LIMIT);
        StringBuilder builder = new StringBuilder();
        if (!seed.weaknessTags.isEmpty()) {
            builder.append("你最近在「")
                    .append(String.join("、", seed.weaknessTags))
                    .append("」相关问题上多次暴露薄弱点，建议学习该知识卡片，补齐面试表达和常见追问。");
        } else {
            builder.append("你在「")
                    .append(textOrDefault(seed.snapshot.getDimensionName(), "当前能力"))
                    .append("」维度的训练表现还有提升空间，建议学习该知识卡片后再进行专项训练。");
        }
        if (StringUtils.hasText(evidence)) {
            builder.append(" 最近一次表现：").append(evidence);
        }
        return builder.toString();
    }

    private String buildEvidence(RecommendationSeed seed) {
        if (!seed.weaknessTags.isEmpty()) {
            return String.join("、", seed.weaknessTags);
        }
        return truncate(seed.snapshot.getEvidence(), EVIDENCE_REASON_LIMIT);
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
                    .distinct()
                    .toList();
        } catch (Exception exception) {
            log.warn("Failed to parse recommendation weakness tags: {}", abbreviate(json));
            return List.of();
        }
    }

    private void addIfText(List<String> parts, String value) {
        if (StringUtils.hasText(value)) {
            parts.add(value.trim());
        }
    }

    private int compareScore(Double left, Double right) {
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

    private String textOrDefault(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }

    private String truncate(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength);
    }

    private String abbreviate(String value) {
        return truncate(value, 300);
    }

    private record RecommendationSeed(
            UserAbilitySnapshot snapshot,
            List<String> weaknessTags,
            String query
    ) {
    }
}
