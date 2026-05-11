package com.codecoach.module.rag.controller;

import com.codecoach.common.result.Result;
import com.codecoach.module.rag.config.RagProperties;
import com.codecoach.module.rag.model.EmbeddingResult;
import com.codecoach.module.rag.model.VectorSearchRequest;
import com.codecoach.module.rag.model.VectorSearchResult;
import com.codecoach.module.rag.model.VectorUpsertRequest;
import com.codecoach.module.rag.service.EmbeddingService;
import com.codecoach.module.rag.service.VectorStoreService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rag/debug")
public class RagDebugController {

    private final EmbeddingService embeddingService;

    private final VectorStoreService vectorStoreService;

    private final RagProperties ragProperties;

    public RagDebugController(
            EmbeddingService embeddingService,
            VectorStoreService vectorStoreService,
            RagProperties ragProperties
    ) {
        this.embeddingService = embeddingService;
        this.vectorStoreService = vectorStoreService;
        this.ragProperties = ragProperties;
    }

    @PostMapping("/embed")
    public Result<EmbeddingDebugResponse> embed(@Valid @RequestBody EmbeddingDebugRequest request) {
        EmbeddingResult result = embeddingService.embed(request.getText());
        return Result.success(new EmbeddingDebugResponse(
                result.getModel(),
                result.getDimensions(),
                result.getVector() == null ? 0 : result.getVector().size(),
                result.getPromptTokens(),
                result.getTotalTokens()
        ));
    }

    @PostMapping("/vector-test")
    public Result<VectorTestResponse> vectorTest(@Valid @RequestBody EmbeddingDebugRequest request) {
        EmbeddingResult embedding = embeddingService.embed(request.getText());
        String vectorId = UUID.randomUUID().toString();

        vectorStoreService.ensureCollection();

        VectorUpsertRequest upsertRequest = new VectorUpsertRequest();
        upsertRequest.setVectorId(vectorId);
        upsertRequest.setVector(embedding.getVector());
        upsertRequest.setPayload(Map.of(
                "sourceType", "DEBUG",
                "debugVectorId", vectorId,
                "title", "RAG debug vector"
        ));
        vectorStoreService.upsert(upsertRequest);

        VectorSearchRequest searchRequest = new VectorSearchRequest();
        searchRequest.setVector(embedding.getVector());
        searchRequest.setTopK(3);
        searchRequest.setFilter(Map.of("debugVectorId", vectorId));
        List<VectorSearchResult> results = vectorStoreService.search(searchRequest);

        Double topScore = results.isEmpty() ? null : results.get(0).getScore();
        return Result.success(new VectorTestResponse(
                vectorId,
                embedding.getVector() == null ? 0 : embedding.getVector().size(),
                ragProperties.getVectorStore().getCollection(),
                results.size(),
                topScore
        ));
    }

    @PostMapping("/vector/delete")
    public Result<Boolean> deleteVector(@Valid @RequestBody VectorDeleteRequest request) {
        vectorStoreService.delete(request.getVectorId());
        return Result.success(Boolean.TRUE);
    }

    public static class EmbeddingDebugRequest {

        @NotBlank(message = "文本不能为空")
        private String text;

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }
    }

    public static class EmbeddingDebugResponse {

        private String model;

        private Integer dimensions;

        private Integer vectorSize;

        private Integer promptTokens;

        private Integer totalTokens;

        public EmbeddingDebugResponse(
                String model,
                Integer dimensions,
                Integer vectorSize,
                Integer promptTokens,
                Integer totalTokens
        ) {
            this.model = model;
            this.dimensions = dimensions;
            this.vectorSize = vectorSize;
            this.promptTokens = promptTokens;
            this.totalTokens = totalTokens;
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

        public Integer getVectorSize() {
            return vectorSize;
        }

        public void setVectorSize(Integer vectorSize) {
            this.vectorSize = vectorSize;
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

    public static class VectorTestResponse {

        private String vectorId;

        private Integer vectorSize;

        private String collection;

        private Integer resultCount;

        private Double topScore;

        public VectorTestResponse(
                String vectorId,
                Integer vectorSize,
                String collection,
                Integer resultCount,
                Double topScore
        ) {
            this.vectorId = vectorId;
            this.vectorSize = vectorSize;
            this.collection = collection;
            this.resultCount = resultCount;
            this.topScore = topScore;
        }

        public String getVectorId() {
            return vectorId;
        }

        public void setVectorId(String vectorId) {
            this.vectorId = vectorId;
        }

        public Integer getVectorSize() {
            return vectorSize;
        }

        public void setVectorSize(Integer vectorSize) {
            this.vectorSize = vectorSize;
        }

        public String getCollection() {
            return collection;
        }

        public void setCollection(String collection) {
            this.collection = collection;
        }

        public Integer getResultCount() {
            return resultCount;
        }

        public void setResultCount(Integer resultCount) {
            this.resultCount = resultCount;
        }

        public Double getTopScore() {
            return topScore;
        }

        public void setTopScore(Double topScore) {
            this.topScore = topScore;
        }
    }

    public static class VectorDeleteRequest {

        @NotBlank(message = "向量ID不能为空")
        private String vectorId;

        public String getVectorId() {
            return vectorId;
        }

        public void setVectorId(String vectorId) {
            this.vectorId = vectorId;
        }
    }
}
