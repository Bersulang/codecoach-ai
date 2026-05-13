package com.codecoach.module.rag.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.codecoach.module.rag.entity.RagTrace;
import com.codecoach.module.rag.mapper.RagTraceMapper;
import com.codecoach.module.rag.model.RagEvaluationResult;
import com.codecoach.module.rag.model.RagRetrievedChunk;
import com.codecoach.module.rag.service.RagTraceService;
import com.codecoach.security.UserContext;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class RagTraceServiceImpl implements RagTraceService {

    private static final Logger log = LoggerFactory.getLogger(RagTraceServiceImpl.class);
    private static final int DEFAULT_LIMIT = 50;

    private final RagTraceMapper ragTraceMapper;

    public RagTraceServiceImpl(RagTraceMapper ragTraceMapper) {
        this.ragTraceMapper = ragTraceMapper;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(
            String traceId,
            Long userId,
            String query,
            String rewrittenQuery,
            List<String> sourceTypes,
            int topK,
            List<RagRetrievedChunk> chunks,
            RagEvaluationResult evaluation,
            boolean success,
            String fallbackReason,
            long latencyMs
    ) {
        try {
            RagTrace trace = new RagTrace();
            trace.setTraceId(traceId);
            trace.setUserId(userId);
            trace.setQuery(abbreviate(query, 512));
            trace.setRewrittenQuery(abbreviate(rewrittenQuery, 512));
            trace.setSourceTypes(sourceTypes == null ? null : String.join(",", sourceTypes));
            trace.setTopK(topK);
            trace.setHitCount(chunks == null ? 0 : chunks.size());
            trace.setSelectedChunkIds(chunkIds(chunks));
            trace.setAvgScore(evaluation == null ? null : evaluation.avgScore());
            trace.setContextChars(evaluation == null ? null : evaluation.contextChars());
            trace.setSuccess(success ? 1 : 0);
            trace.setFallbackReason(abbreviate(fallbackReason, 256));
            trace.setLatencyMs(latencyMs);
            trace.setCreatedAt(LocalDateTime.now());
            ragTraceMapper.insert(trace);
        } catch (RuntimeException exception) {
            log.debug("Failed to persist RAG trace, traceId={}", traceId);
        }
    }

    @Override
    public List<RagTrace> listRecentForCurrentUser(Integer limit) {
        Long userId = UserContext.getCurrentUserId();
        int normalized = limit == null || limit <= 0 ? DEFAULT_LIMIT : Math.min(limit, 200);
        return ragTraceMapper.selectList(new LambdaQueryWrapper<RagTrace>()
                .eq(RagTrace::getUserId, userId)
                .orderByDesc(RagTrace::getCreatedAt)
                .orderByDesc(RagTrace::getId)
                .last("LIMIT " + normalized));
    }

    private String chunkIds(List<RagRetrievedChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return null;
        }
        return chunks.stream()
                .map(RagRetrievedChunk::getChunkId)
                .filter(id -> id != null)
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    private String abbreviate(String value, int limit) {
        if (!StringUtils.hasText(value) || value.length() <= limit) {
            return value;
        }
        return value.substring(0, limit);
    }
}
