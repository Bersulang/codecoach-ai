package com.codecoach.module.rag.service;

import com.codecoach.module.rag.model.EmbeddingResult;

public interface EmbeddingService {

    EmbeddingResult embed(String text);
}
