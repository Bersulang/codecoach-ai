package com.codecoach.module.rag.model;

import java.util.List;
import java.util.Map;

public class VectorUpsertRequest {

    private String vectorId;

    private List<Float> vector;

    private Map<String, Object> payload;

    public String getVectorId() {
        return vectorId;
    }

    public void setVectorId(String vectorId) {
        this.vectorId = vectorId;
    }

    public List<Float> getVector() {
        return vector;
    }

    public void setVector(List<Float> vector) {
        this.vector = vector;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }
}
