package com.codecoach.module.rag.exception;

public class VectorStoreException extends RuntimeException {

    private final String errorCode;

    private final String errorMessage;

    private final Integer statusCode;

    private final String rawResponse;

    public VectorStoreException(String errorCode, String errorMessage) {
        this(errorCode, errorMessage, null, null, null);
    }

    public VectorStoreException(String errorCode, String errorMessage, Throwable cause) {
        this(errorCode, errorMessage, null, null, cause);
    }

    public VectorStoreException(
            String errorCode,
            String errorMessage,
            Integer statusCode,
            String rawResponse,
            Throwable cause
    ) {
        super(errorMessage, cause);
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.statusCode = statusCode;
        this.rawResponse = rawResponse;
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
}
