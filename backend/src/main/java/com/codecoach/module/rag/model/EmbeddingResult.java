package com.codecoach.module.rag.model;

import java.util.List;

public class EmbeddingResult {

    private List<Float> vector;

    private String model;

    private Integer dimensions;

    private Integer promptTokens;

    private Integer totalTokens;

    public EmbeddingResult() {
    }

    public EmbeddingResult(
            List<Float> vector,
            String model,
            Integer dimensions,
            Integer promptTokens,
            Integer totalTokens
    ) {
        this.vector = vector;
        this.model = model;
        this.dimensions = dimensions;
        this.promptTokens = promptTokens;
        this.totalTokens = totalTokens;
    }

    public List<Float> getVector() {
        return vector;
    }

    public void setVector(List<Float> vector) {
        this.vector = vector;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Integer getDimensions() {
        return dimensions;
    }

    public void setDimensions(Integer dimensions) {
        this.dimensions = dimensions;
    }

    public Integer getPromptTokens() {
        return promptTokens;
    }

    public void setPromptTokens(Integer promptTokens) {
        this.promptTokens = promptTokens;
    }

    public Integer getTotalTokens() {
        return totalTokens;
    }

    public void setTotalTokens(Integer totalTokens) {
        this.totalTokens = totalTokens;
    }
}
