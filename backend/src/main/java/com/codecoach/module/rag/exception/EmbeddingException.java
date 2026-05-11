package com.codecoach.module.rag.exception;

public class EmbeddingException extends RuntimeException {

    private final String errorCode;

    private final String errorMessage;

    private final Integer statusCode;

    private final String rawResponse;

    private final String requestId;

    public EmbeddingException(String errorCode, String errorMessage) {
        this(errorCode, errorMessage, null, null, null, null);
    }

    public EmbeddingException(String errorCode, String errorMessage, Throwable cause) {
        this(errorCode, errorMessage, null, null, null, cause);
    }

    public EmbeddingException(
            String errorCode,
            String errorMessage,
            Integer statusCode,
            String rawResponse,
            String requestId,
            Throwable cause
    ) {
        super(errorMessage, cause);
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.statusCode = statusCode;
        this.rawResponse = rawResponse;
        this.requestId = requestId;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public String getRawResponse() {
        return rawResponse;
    }

    public String getRequestId() {
        return requestId;
    }
}
