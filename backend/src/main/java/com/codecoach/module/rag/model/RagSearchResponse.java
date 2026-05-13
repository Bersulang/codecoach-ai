package com.codecoach.module.rag.model;

import java.util.List;

public class RagSearchResponse {

    private String query;

    private String rewrittenQuery;

    private String traceId;

    private Integer topK;

    private Integer resultCount;

    private List<RagRetrievedChunk> chunks;

    private RagEvaluationResult evaluation;

    public RagSearchResponse() {
    }

    public RagSearchResponse(String query, Integer topK, Integer resultCount, List<RagRetrievedChunk> chunks) {
        this.query = query;
        this.topK = topK;
        this.resultCount = resultCount;
        this.chunks = chunks;
    }

    public RagSearchResponse(String query, String rewrittenQuery, String traceId, Integer topK, Integer resultCount, List<RagRetrievedChunk> chunks, RagEvaluationResult evaluation) {
        this.query = query;
        this.rewrittenQuery = rewrittenQuery;
        this.traceId = traceId;
        this.topK = topK;
        this.resultCount = resultCount;
        this.chunks = chunks;
        this.evaluation = evaluation;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getRewrittenQuery() {
        return rewrittenQuery;
    }

    public void setRewrittenQuery(String rewrittenQuery) {
        this.rewrittenQuery = rewrittenQuery;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public Integer getTopK() {
        return topK;
    }

    public void setTopK(Integer topK) {
        this.topK = topK;
    }

    public Integer getResultCount() {
        return resultCount;
    }

    public void setResultCount(Integer resultCount) {
        this.resultCount = resultCount;
    }

    public List<RagRetrievedChunk> getChunks() {
        return chunks;
    }

    public void setChunks(List<RagRetrievedChunk> chunks) {
        this.chunks = chunks;
    }

    public RagEvaluationResult getEvaluation() {
        return evaluation;
    }

    public void setEvaluation(RagEvaluationResult evaluation) {
        this.evaluation = evaluation;
    }
}
