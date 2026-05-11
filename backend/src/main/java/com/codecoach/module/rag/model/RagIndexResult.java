package com.codecoach.module.rag.model;

public class RagIndexResult {

    private Long articleId;

    private Long documentId;

    private Integer chunkCount;

    private Integer embeddedCount;

    private Integer failedCount;

    private String status;

    private String errorMessage;

    public RagIndexResult() {
    }

    public RagIndexResult(
            Long articleId,
            Long documentId,
            Integer chunkCount,
            Integer embeddedCount,
            Integer failedCount,
            String status,
            String errorMessage
    ) {
        this.articleId = articleId;
        this.documentId = documentId;
        this.chunkCount = chunkCount;
        this.embeddedCount = embeddedCount;
        this.failedCount = failedCount;
        this.status = status;
        this.errorMessage = errorMessage;
    }

    public Long getArticleId() {
        return articleId;
    }

    public void setArticleId(Long articleId) {
        this.articleId = articleId;
    }

    public Long getDocumentId() {
        return documentId;
    }

    public void setDocumentId(Long documentId) {
        this.documentId = documentId;
    }

    public Integer getChunkCount() {
        return chunkCount;
    }

    public void setChunkCount(Integer chunkCount) {
        this.chunkCount = chunkCount;
    }

    public Integer getEmbeddedCount() {
        return embeddedCount;
    }

    public void setEmbeddedCount(Integer embeddedCount) {
        this.embeddedCount = embeddedCount;
    }

    public Integer getFailedCount() {
        return failedCount;
    }

    public void setFailedCount(Integer failedCount) {
        this.failedCount = failedCount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
