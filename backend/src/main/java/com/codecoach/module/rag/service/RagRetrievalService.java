package com.codecoach.module.rag.service;

import com.codecoach.module.rag.model.RagRetrievedChunk;
import com.codecoach.module.rag.model.RagSearchRequest;
import com.codecoach.module.rag.model.RagSearchResponse;
import java.util.List;

public interface RagRetrievalService {

    RagSearchResponse search(RagSearchRequest request);

    String buildContextBlock(List<RagRetrievedChunk> chunks, int maxChars);
}
