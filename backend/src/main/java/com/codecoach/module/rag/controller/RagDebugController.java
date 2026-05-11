package com.codecoach.module.rag.controller;

import com.codecoach.common.result.Result;
import com.codecoach.module.rag.model.EmbeddingResult;
import com.codecoach.module.rag.service.EmbeddingService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rag/debug")
public class RagDebugController {

    private final EmbeddingService embeddingService;

    public RagDebugController(EmbeddingService embeddingService) {
        this.embeddingService = embeddingService;
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
}
