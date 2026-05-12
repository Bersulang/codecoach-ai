package com.codecoach.module.rag.service;

import com.codecoach.module.rag.model.RagBatchIndexResult;
import com.codecoach.module.rag.model.RagIndexResult;

public interface RagIndexService {

    RagIndexResult indexKnowledgeArticle(Long articleId);

    RagBatchIndexResult indexAllKnowledgeArticles();

    RagIndexResult indexProject(Long projectId);

    RagBatchIndexResult indexCurrentUserProjects();
}
