package com.codecoach.module.memory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.codecoach.module.memory.entity.UserMemory;
import com.codecoach.module.memory.mapper.UserMemoryMapper;
import com.codecoach.module.memory.model.MemorySemanticHit;
import com.codecoach.module.memory.service.UserSemanticMemoryService;
import com.codecoach.module.observability.trace.TraceContextHolder;
import com.codecoach.module.rag.model.EmbeddingResult;
import com.codecoach.module.rag.model.VectorSearchRequest;
import com.codecoach.module.rag.model.VectorSearchResult;
import com.codecoach.module.rag.model.VectorUpsertRequest;
import com.codecoach.module.rag.service.EmbeddingService;
import com.codecoach.module.rag.service.VectorStoreService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class UserSemanticMemoryServiceImpl implements UserSemanticMemoryService {

    private static final Logger log = LoggerFactory.getLogger(UserSemanticMemoryServiceImpl.class);
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String SOURCE_TYPE = "USER_MEMORY";
    private static final String OWNER_TYPE_USER = "USER";

    private final UserMemoryMapper userMemoryMapper;
    private final EmbeddingService embeddingService;
    private final VectorStoreService vectorStoreService;

    public UserSemanticMemoryServiceImpl(
            UserMemoryMapper userMemoryMapper,
            EmbeddingService embeddingService,
            VectorStoreService vectorStoreService
    ) {
        this.userMemoryMapper = userMemoryMapper;
        this.embeddingService = embeddingService;
        this.vectorStoreService = vectorStoreService;
    }

    @Override
    public int indexActiveMemories(Long userId) {
        if (userId == null) {
            return 0;
        }
        List<UserMemory> memories = userMemoryMapper.selectList(new LambdaQueryWrapper<UserMemory>()
                .eq(UserMemory::getUserId, userId)
                .eq(UserMemory::getStatus, STATUS_ACTIVE)
                .orderByDesc(UserMemory::getWeight)
                .orderByDesc(UserMemory::getLastReinforcedAt)
                .last("LIMIT 100"));
        int count = 0;
        for (UserMemory memory : memories) {
            try {
                String summary = semanticText(memory);
                if (!StringUtils.hasText(summary)) {
                    continue;
                }
                EmbeddingResult embedding = embeddingService.embed(summary);
                VectorUpsertRequest request = new VectorUpsertRequest();
                request.setVectorId(vectorId(memory.getId()));
                request.setVector(embedding.getVector());
                request.setPayload(payload(memory, summary));
                vectorStoreService.upsert(request);
                count++;
            } catch (RuntimeException exception) {
                log.debug("Semantic memory index skipped, userId={}, memoryId={}, traceId={}", userId, memory.getId(), TraceContextHolder.getTraceId());
            }
        }
        return count;
    }

    @Override
    public List<MemorySemanticHit> search(Long userId, String query, int topK) {
        if (userId == null || !StringUtils.hasText(query)) {
            return List.of();
        }
        try {
            EmbeddingResult embedding = embeddingService.embed(query.trim());
            VectorSearchRequest request = new VectorSearchRequest();
            request.setVector(embedding.getVector());
            request.setTopK(Math.min(Math.max(topK, 1), 8));
            request.setFilter(Map.of("sourceType", SOURCE_TYPE, "ownerType", OWNER_TYPE_USER, "userId", userId));
            List<VectorSearchResult> results = vectorStoreService.search(request);
            List<MemorySemanticHit> hits = new ArrayList<>();
            for (VectorSearchResult result : results) {
                Long memoryId = toLong(result.getPayload() == null ? null : result.getPayload().get("memoryId"));
                if (memoryId == null) {
                    continue;
                }
                UserMemory memory = userMemoryMapper.selectById(memoryId);
                if (memory == null || !userId.equals(memory.getUserId()) || !STATUS_ACTIVE.equals(memory.getStatus())) {
                    continue;
                }
                hits.add(new MemorySemanticHit(
                        memory.getId(),
                        memory.getMemoryType(),
                        memory.getMemoryValue(),
                        memory.getConfidence(),
                        memory.getWeight(),
                        result.getScore()
                ));
            }
            return hits.stream()
                    .sorted(Comparator.comparing(MemorySemanticHit::score, Comparator.nullsLast(Comparator.reverseOrder())))
                    .limit(Math.min(Math.max(topK, 1), 8))
                    .toList();
        } catch (RuntimeException exception) {
            log.debug("Semantic memory search fallback, userId={}, queryLength={}, traceId={}", userId, query.trim().length(), TraceContextHolder.getTraceId());
            return List.of();
        }
    }

    private Map<String, Object> payload(UserMemory memory, String summary) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sourceType", SOURCE_TYPE);
        payload.put("ownerType", OWNER_TYPE_USER);
        payload.put("userId", memory.getUserId());
        payload.put("memoryId", memory.getId());
        payload.put("memoryType", memory.getMemoryType());
        payload.put("summary", summary);
        return payload;
    }

    private String semanticText(UserMemory memory) {
        if (memory == null || !StringUtils.hasText(memory.getMemoryValue())) {
            return null;
        }
        return abbreviate(memory.getMemoryType() + "：" + memory.getMemoryValue(), 260);
    }

    private String vectorId(Long memoryId) {
        return "memory-" + memoryId;
    }

    private Long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String string && StringUtils.hasText(string)) {
            try {
                return Long.parseLong(string.trim());
            } catch (NumberFormatException ignored) {
                return null;
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
