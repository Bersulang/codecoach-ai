package com.codecoach.module.rag.model;

import java.util.List;

public class RagBatchIndexResult {

    private Integer totalCount;

    private Integer indexedCount;

    private Integer failedCount;

    private List<RagIndexResult> results;

    public RagBatchIndexResult() {
    }

    public RagBatchIndexResult(
            Integer totalCount,
            Integer indexedCount,
            Integer failedCount,
            List<RagIndexResult> results
    ) {
        this.totalCount = totalCount;
        this.indexedCount = indexedCount;
        this.failedCount = failedCount;
        this.results = results;
    }

    public Integer getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Integer totalCount) {
        this.totalCount = totalCount;
    }

    public Integer getIndexedCount() {
        return indexedCount;
    }

    public void setIndexedCount(Integer indexedCount) {
        this.indexedCount = indexedCount;
    }

    public Integer getFailedCount() {
        return failedCount;
    }

    public void setFailedCount(Integer failedCount) {
        this.failedCount = failedCount;
    }

    public List<RagIndexResult> getResults() {
        return results;
    }

    public void setResults(List<RagIndexResult> results) {
        this.results = results;
    }
}
