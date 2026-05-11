package com.codecoach.module.rag.model;

import java.util.List;

public class RagSearchResponse {

    private String query;

    private Integer topK;

    private Integer resultCount;

    private List<RagRetrievedChunk> chunks;

    public RagSearchResponse() {
    }

    public RagSearchResponse(String query, Integer topK, Integer resultCount, List<RagRetrievedChunk> chunks) {
        this.query = query;
        this.topK = topK;
        this.resultCount = resultCount;
        this.chunks = chunks;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
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
}
