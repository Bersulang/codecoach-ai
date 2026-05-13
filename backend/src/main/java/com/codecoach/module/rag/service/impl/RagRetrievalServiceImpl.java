package com.codecoach.module.rag.service.impl;

import com.codecoach.common.exception.BusinessException;
import com.codecoach.common.result.ResultCode;
import com.codecoach.module.rag.config.RagProperties;
import com.codecoach.module.rag.constant.RagConstants;
import com.codecoach.module.rag.entity.RagChunk;
import com.codecoach.module.rag.entity.RagDocument;
import com.codecoach.module.rag.mapper.RagChunkMapper;
import com.codecoach.module.rag.mapper.RagDocumentMapper;
import com.codecoach.module.rag.model.EmbeddingResult;
import com.codecoach.module.rag.model.RagEvaluationResult;
import com.codecoach.module.rag.model.RagRetrievedChunk;
import com.codecoach.module.rag.model.RagSearchRequest;
import com.codecoach.module.rag.model.RagSearchResponse;
import com.codecoach.module.rag.model.VectorSearchRequest;
import com.codecoach.module.rag.model.VectorSearchResult;
import com.codecoach.module.rag.service.EmbeddingService;
import com.codecoach.module.rag.service.RagQueryRewriteService;
import com.codecoach.module.rag.service.RagRerankService;
import com.codecoach.module.rag.service.RagRetrievalService;
import com.codecoach.module.rag.service.RagTraceService;
import com.codecoach.module.rag.service.VectorStoreService;
import com.codecoach.module.observability.trace.TraceContextHolder;
import com.codecoach.security.UserContext;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Service
public class RagRetrievalServiceImpl implements RagRetrievalService {

    private static final Logger log = LoggerFactory.getLogger(RagRetrievalServiceImpl.class);

    private static final int DEFAULT_TOP_K = 5;

    private static final int MAX_TOP_K = 20;

    private final EmbeddingService embeddingService;

    private final VectorStoreService vectorStoreService;

    private final RagChunkMapper ragChunkMapper;

    private final RagDocumentMapper ragDocumentMapper;

    private final RagProperties ragProperties;

    private final ObjectMapper objectMapper;

    private final RagQueryRewriteService ragQueryRewriteService;

    private final RagRerankService ragRerankService;

    private final RagTraceService ragTraceService;

    public RagRetrievalServiceImpl(
            EmbeddingService embeddingService,
            VectorStoreService vectorStoreService,
            RagChunkMapper ragChunkMapper,
            RagDocumentMapper ragDocumentMapper,
            RagProperties ragProperties,
            ObjectMapper objectMapper,
            RagQueryRewriteService ragQueryRewriteService,
            RagRerankService ragRerankService,
            RagTraceService ragTraceService
    ) {
        this.embeddingService = embeddingService;
        this.vectorStoreService = vectorStoreService;
        this.ragChunkMapper = ragChunkMapper;
        this.ragDocumentMapper = ragDocumentMapper;
        this.ragProperties = ragProperties;
        this.objectMapper = objectMapper;
        this.ragQueryRewriteService = ragQueryRewriteService;
        this.ragRerankService = ragRerankService;
        this.ragTraceService = ragTraceService;
    }

    @Override
    public RagSearchResponse search(RagSearchRequest request) {
        long start = System.currentTimeMillis();
        String traceId = TraceContextHolder.getOrCreateTraceId();
        Long currentUserId = UserContext.getCurrentUserId();
        String query = request == null ? null : request.getQuery();
        if (!StringUtils.hasText(query)) {
            throw new BusinessException(ResultCode.RAG_PARAM_ERROR);
        }

        int topK = normalizeTopK(request.getTopK());
        List<String> sourceTypes = normalizeSourceTypes(request.getSourceTypes());
        String rewrittenQuery = query.trim();
        try {
            rewrittenQuery = ragQueryRewriteService.rewrite(query.trim(), sourceTypes);
            EmbeddingResult embedding = embeddingService.embed(StringUtils.hasText(rewrittenQuery) ? rewrittenQuery : query.trim());
            List<VectorSearchResult> vectorResults = searchVectors(embedding, request, Math.min(topK * 2, MAX_TOP_K), currentUserId, sourceTypes);
            List<RagRetrievedChunk> candidates = toRetrievedChunks(vectorResults, currentUserId, Math.min(topK * 2, MAX_TOP_K));
            List<RagRetrievedChunk> chunks = ragRerankService.rerank(query.trim(), rewrittenQuery, candidates, topK);
            RagEvaluationResult evaluation = evaluate(chunks);
            ragTraceService.record(traceId, currentUserId, query.trim(), rewrittenQuery, sourceTypes, topK, chunks, evaluation, true, null, System.currentTimeMillis() - start);
            return new RagSearchResponse(query.trim(), rewrittenQuery, traceId, topK, chunks.size(), chunks, evaluation);
        } catch (BusinessException exception) {
            ragTraceService.record(traceId, currentUserId, query.trim(), rewrittenQuery, sourceTypes, topK, List.of(), null, false, exception.getMessage(), System.currentTimeMillis() - start);
            throw exception;
        } catch (Exception exception) {
            log.warn("RAG retrieval failed, queryLength={}, error={}", query.trim().length(), abbreviate(exception.getMessage()), exception);
            ragTraceService.record(traceId, currentUserId, query.trim(), rewrittenQuery, sourceTypes, topK, List.of(), null, false, "RAG_RETRIEVAL_FAILED", System.currentTimeMillis() - start);
            throw new BusinessException(ResultCode.RAG_RETRIEVAL_FAILED);
        }
    }

    @Override
    public String buildContextBlock(List<RagRetrievedChunk> chunks, int maxChars) {
        if (CollectionUtils.isEmpty(chunks)) {
            return "";
        }
        int limit = maxChars > 0 ? maxChars : defaultMaxContextChars();
        StringBuilder builder = new StringBuilder("""
                【检索到的相关知识片段】
                """);
        int index = 1;
        for (RagRetrievedChunk chunk : chunks) {
            String source = safeText(chunk.getTitle());
            String section = StringUtils.hasText(chunk.getSection()) ? " / " + chunk.getSection() : "";
            String item = index + ". 来源：" + source + section + "\n内容：" + safeText(chunk.getContent()) + "\n\n";
            if (builder.length() + item.length() > limit) {
                int remaining = limit - builder.length();
                if (remaining > 80) {
                    builder.append(item, 0, remaining);
                }
                break;
            }
            builder.append(item);
            index++;
        }
        String usage = """

                【使用要求】
                - 上述内容仅作为参考知识背景。
                - 请优先结合用户回答进行反馈。
                - 如果知识片段与当前问题无关，可以忽略。
                - 不要编造用户没有提到的项目经历。
                - 不要直接照抄知识片段，应转化为面试反馈和参考答案。
                """;
        if (builder.length() + usage.length() <= limit) {
            builder.append(usage);
        }
        return builder.toString().trim();
    }

    private List<VectorSearchResult> searchVectors(EmbeddingResult embedding, RagSearchRequest request, int topK, Long currentUserId, List<String> sourceTypes) {
        if (sourceTypes.isEmpty()) {
            VectorSearchRequest vectorSearchRequest = new VectorSearchRequest();
            vectorSearchRequest.setVector(embedding.getVector());
            vectorSearchRequest.setTopK(topK);
            vectorSearchRequest.setFilter(buildFilter(request.getFilter(), null, currentUserId));
            return vectorStoreService.search(vectorSearchRequest);
        }

        List<VectorSearchResult> merged = new ArrayList<>();
        for (String sourceType : sourceTypes) {
            VectorSearchRequest vectorSearchRequest = new VectorSearchRequest();
            vectorSearchRequest.setVector(embedding.getVector());
            vectorSearchRequest.setTopK(topK);
            vectorSearchRequest.setFilter(buildFilter(request.getFilter(), sourceType, currentUserId));
            merged.addAll(vectorStoreService.search(vectorSearchRequest));
        }
        return mergeAndLimit(merged, topK);
    }

    private Map<String, Object> buildFilter(Map<String, Object> requestFilter, String sourceType, Long currentUserId) {
        Map<String, Object> filter = new LinkedHashMap<>();
        if (requestFilter != null) {
            filter.putAll(requestFilter);
        }
        filter.remove("userId");
        filter.remove("ownerType");
        if (StringUtils.hasText(sourceType)) {
            filter.put("sourceType", sourceType);
        }
        String effectiveSourceType = toStringValue(filter.get("sourceType"));
        if (RagConstants.SOURCE_TYPE_PROJECT.equals(effectiveSourceType)
                || RagConstants.SOURCE_TYPE_USER_UPLOAD.equals(effectiveSourceType)) {
            filter.put("ownerType", RagConstants.OWNER_TYPE_USER);
            filter.put("userId", currentUserId);
        } else {
            filter.put("ownerType", RagConstants.OWNER_TYPE_SYSTEM);
        }
        return filter;
    }

    private List<RagRetrievedChunk> toRetrievedChunks(List<VectorSearchResult> vectorResults, Long currentUserId, int topK) {
        List<RagRetrievedChunk> chunks = new ArrayList<>();
        for (VectorSearchResult vectorResult : vectorResults) {
            Long chunkId = toLong(vectorResult.getPayload() == null ? null : vectorResult.getPayload().get("chunkId"));
            if (chunkId == null) {
                log.warn("RAG search result skipped because chunkId is missing, vectorId={}", vectorResult.getVectorId());
                continue;
            }

            RagChunk chunk = ragChunkMapper.selectById(chunkId);
            if (chunk == null) {
                log.warn("RAG search result skipped because chunk does not exist, chunkId={}, vectorId={}", chunkId, vectorResult.getVectorId());
                continue;
            }

            RagDocument document = ragDocumentMapper.selectById(chunk.getDocumentId());
            if (document == null) {
                log.warn("RAG search result skipped because document does not exist, chunkId={}, documentId={}", chunkId, chunk.getDocumentId());
                continue;
            }
            if (!isReadableDocument(document, currentUserId)) {
                log.warn("RAG search result skipped because document is not readable, chunkId={}, documentId={}", chunkId, document.getId());
                continue;
            }

            Map<String, Object> metadata = parseMetadata(chunk);
            RagRetrievedChunk retrievedChunk = new RagRetrievedChunk();
            retrievedChunk.setChunkId(chunk.getId());
            retrievedChunk.setDocumentId(document.getId());
            retrievedChunk.setVectorId(vectorResult.getVectorId());
            retrievedChunk.setScore(vectorResult.getScore());
            retrievedChunk.setContent(chunk.getContent());
            retrievedChunk.setSourceType(document.getSourceType());
            retrievedChunk.setTitle(document.getTitle());
            retrievedChunk.setMetadata(metadata);
            retrievedChunk.setSection(toStringValue(metadata.get("section")));
            retrievedChunk.setArticleId(toLong(metadata.get("articleId")));
            retrievedChunk.setTopicId(toLong(metadata.get("topicId")));
            retrievedChunk.setCategory(toStringValue(metadata.get("category")));
            retrievedChunk.setTopicName(toStringValue(metadata.get("topicName")));
            chunks.add(retrievedChunk);
        }
        return chunks.stream()
                .sorted(Comparator.comparing(RagRetrievedChunk::getScore, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(topK)
                .toList();
    }

    private RagEvaluationResult evaluate(List<RagRetrievedChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return new RagEvaluationResult(0, null, true, Map.of(), 0);
        }
        double avgScore = chunks.stream()
                .map(RagRetrievedChunk::getScore)
                .filter(score -> score != null)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0D);
        Map<String, Long> distribution = chunks.stream()
                .collect(Collectors.groupingBy(chunk -> safeText(chunk.getSourceType()), LinkedHashMap::new, Collectors.counting()));
        int contextChars = chunks.stream()
                .map(RagRetrievedChunk::getContent)
                .filter(StringUtils::hasText)
                .mapToInt(String::length)
                .sum();
        return new RagEvaluationResult(chunks.size(), avgScore, false, distribution, contextChars);
    }

    private boolean isReadableDocument(RagDocument document, Long currentUserId) {
        if (!RagConstants.DOCUMENT_STATUS_INDEXED.equals(document.getStatus())) {
            return false;
        }
        if (RagConstants.OWNER_TYPE_SYSTEM.equals(document.getOwnerType())) {
            return true;
        }
        return RagConstants.OWNER_TYPE_USER.equals(document.getOwnerType())
                && document.getUserId() != null
                && document.getUserId().equals(currentUserId);
    }

    private Map<String, Object> parseMetadata(RagChunk chunk) {
        if (!StringUtils.hasText(chunk.getMetadata())) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(chunk.getMetadata(), new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception exception) {
            log.warn("RAG chunk metadata parse failed, chunkId={}, error={}", chunk.getId(), abbreviate(exception.getMessage()));
            return Map.of();
        }
    }

    private List<VectorSearchResult> mergeAndLimit(List<VectorSearchResult> results, int topK) {
        Map<String, VectorSearchResult> resultMap = new LinkedHashMap<>();
        for (VectorSearchResult result : results) {
            if (!StringUtils.hasText(result.getVectorId())) {
                continue;
            }
            VectorSearchResult existing = resultMap.get(result.getVectorId());
            if (existing == null || compareScore(result.getScore(), existing.getScore()) > 0) {
                resultMap.put(result.getVectorId(), result);
            }
        }
        return resultMap.values().stream()
                .sorted((left, right) -> compareScore(right.getScore(), left.getScore()))
                .limit(topK)
                .toList();
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

    private List<String> normalizeSourceTypes(List<String> sourceTypes) {
        if (sourceTypes == null) {
            return List.of();
        }
        return sourceTypes.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();
    }

    private int normalizeTopK(Integer topK) {
        int result = topK == null || topK <= 0 ? defaultTopK() : topK;
        return Math.min(result, MAX_TOP_K);
    }

    private int defaultTopK() {
        return ragProperties.getTopK() == null || ragProperties.getTopK() <= 0 ? DEFAULT_TOP_K : ragProperties.getTopK();
    }

    private int defaultMaxContextChars() {
        return ragProperties.getMaxContextChars() == null || ragProperties.getMaxContextChars() <= 0
                ? 4000
                : ragProperties.getMaxContextChars();
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String string && StringUtils.hasText(string)) {
            try {
                return Long.parseLong(string.trim());
            } catch (NumberFormatException exception) {
                return null;
            }
        }
        return null;
    }

    private String toStringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }

    private String abbreviate(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= 500) {
            return normalized;
        }
        return normalized.substring(0, 500);
    }
}
