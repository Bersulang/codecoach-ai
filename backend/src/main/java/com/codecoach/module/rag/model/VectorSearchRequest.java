package com.codecoach.module.rag.model;

import java.util.List;
import java.util.Map;

public class VectorSearchRequest {

    private List<Float> vector;

    private Integer topK;

    private Map<String, Object> filter;

    public List<Float> getVector() {
        return vector;
    }

    public void setVector(List<Float> vector) {
        this.vector = vector;
    }

    public Integer getTopK() {
        return topK;
    }

    public void setTopK(Integer topK) {
        this.topK = topK;
    }

    public Map<String, Object> getFilter() {
        return filter;
    }

    public void setFilter(Map<String, Object> filter) {
        this.filter = filter;
    }
}
