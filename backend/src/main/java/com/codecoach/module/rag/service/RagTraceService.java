package com.codecoach.module.rag.service;

import com.codecoach.module.rag.entity.RagTrace;
import com.codecoach.module.rag.model.RagEvaluationResult;
import com.codecoach.module.rag.model.RagRetrievedChunk;
import java.util.List;

public interface RagTraceService {

    void record(
            String traceId,
            Long userId,
            String query,
            String rewrittenQuery,
            List<String> sourceTypes,
            int topK,
            List<RagRetrievedChunk> chunks,
            RagEvaluationResult evaluation,
            boolean success,
            String fallbackReason,
            long latencyMs
    );

    List<RagTrace> listRecentForCurrentUser(Integer limit);
}
