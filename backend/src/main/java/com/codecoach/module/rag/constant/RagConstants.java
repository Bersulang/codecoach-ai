package com.codecoach.module.rag.constant;

public final class RagConstants {

    public static final String OWNER_TYPE_SYSTEM = "SYSTEM";

    public static final String OWNER_TYPE_USER = "USER";

    public static final String SOURCE_TYPE_KNOWLEDGE_ARTICLE = "KNOWLEDGE_ARTICLE";

    public static final String SOURCE_TYPE_PROJECT = "PROJECT";

    public static final String SOURCE_TYPE_INTERVIEW_REPORT = "INTERVIEW_REPORT";

    public static final String SOURCE_TYPE_QUESTION_REPORT = "QUESTION_REPORT";

    public static final String SOURCE_TYPE_USER_UPLOAD = "USER_UPLOAD";

    public static final String DOCUMENT_STATUS_PENDING = "PENDING";

    public static final String DOCUMENT_STATUS_INDEXED = "INDEXED";

    public static final String DOCUMENT_STATUS_FAILED = "FAILED";

    public static final String DOCUMENT_STATUS_DISABLED = "DISABLED";

    public static final String EMBEDDING_STATUS_PENDING = "PENDING";

    public static final String EMBEDDING_STATUS_EMBEDDED = "EMBEDDED";

    public static final String EMBEDDING_STATUS_FAILED = "FAILED";

    public static final String VECTOR_STORE_QDRANT = "QDRANT";

    private RagConstants() {
    }
}
