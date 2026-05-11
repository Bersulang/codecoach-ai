package com.codecoach.module.rag.model;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;

public class RagSearchRequest {

    @NotBlank(message = "检索内容不能为空")
    private String query;

    private List<String> sourceTypes;

    private Integer topK;

    private Map<String, Object> filter;

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public List<String> getSourceTypes() {
        return sourceTypes;
    }

    public void setSourceTypes(List<String> sourceTypes) {
        this.sourceTypes = sourceTypes;
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
