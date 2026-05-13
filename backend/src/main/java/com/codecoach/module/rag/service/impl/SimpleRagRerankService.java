package com.codecoach.module.rag.service.impl;

import com.codecoach.module.rag.constant.RagConstants;
import com.codecoach.module.rag.model.RagRetrievedChunk;
import com.codecoach.module.rag.service.RagRerankService;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SimpleRagRerankService implements RagRerankService {

    @Override
    public List<RagRetrievedChunk> rerank(String originalQuery, String rewrittenQuery, List<RagRetrievedChunk> chunks, int topK) {
        if (chunks == null || chunks.isEmpty()) {
            return List.of();
        }
        Set<String> terms = queryTerms(originalQuery + " " + rewrittenQuery);
        return chunks.stream()
                .sorted(Comparator.comparing((RagRetrievedChunk chunk) -> rerankScore(chunk, terms)).reversed())
                .limit(topK)
                .toList();
    }

    private double rerankScore(RagRetrievedChunk chunk, Set<String> terms) {
        double vectorScore = chunk.getScore() == null ? 0D : chunk.getScore();
        double overlapScore = keywordOverlap(chunk, terms) * 0.08D;
        double sourceBoost = sourceBoost(chunk.getSourceType());
        return vectorScore + overlapScore + sourceBoost;
    }

    private int keywordOverlap(RagRetrievedChunk chunk, Set<String> terms) {
        if (terms.isEmpty()) {
            return 0;
        }
        String text = (safe(chunk.getTitle()) + " " + safe(chunk.getSection()) + " " + safe(chunk.getCategory()) + " " + safe(chunk.getTopicName()) + " " + safe(chunk.getContent()))
                .toLowerCase(Locale.ROOT);
        int count = 0;
        for (String term : terms) {
            if (StringUtils.hasText(term) && text.contains(term.toLowerCase(Locale.ROOT))) {
                count++;
            }
        }
        return count;
    }

    private double sourceBoost(String sourceType) {
        if (RagConstants.SOURCE_TYPE_PROJECT.equals(sourceType) || RagConstants.SOURCE_TYPE_USER_UPLOAD.equals(sourceType)) {
            return 0.04D;
        }
        if (RagConstants.SOURCE_TYPE_KNOWLEDGE_ARTICLE.equals(sourceType)) {
            return 0.03D;
        }
        return 0D;
    }

    private Set<String> queryTerms(String query) {
        if (!StringUtils.hasText(query)) {
            return Set.of();
        }
        Set<String> terms = new HashSet<>(Arrays.asList(query.replaceAll("[，。！？、；：,.!?;:()（）\\[\\]{}]", " ").split("\\s+")));
        terms.removeIf(term -> !StringUtils.hasText(term) || term.length() < 2);
        return terms;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
