package com.codecoach.module.rag.service;

import com.codecoach.module.rag.model.RagRetrievedChunk;
import java.util.List;

public interface RagRerankService {

    List<RagRetrievedChunk> rerank(String originalQuery, String rewrittenQuery, List<RagRetrievedChunk> chunks, int topK);
}
