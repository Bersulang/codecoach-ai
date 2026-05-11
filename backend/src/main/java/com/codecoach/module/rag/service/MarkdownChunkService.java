package com.codecoach.module.rag.service;

import com.codecoach.module.rag.model.KnowledgeArticleChunkCommand;
import com.codecoach.module.rag.model.RagChunkCandidate;
import java.util.List;

public interface MarkdownChunkService {

    List<RagChunkCandidate> chunkKnowledgeArticle(KnowledgeArticleChunkCommand command);
}
