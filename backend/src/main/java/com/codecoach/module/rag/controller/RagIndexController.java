package com.codecoach.module.rag.controller;

import com.codecoach.common.result.Result;
import com.codecoach.module.rag.model.RagBatchIndexResult;
import com.codecoach.module.rag.model.RagIndexResult;
import com.codecoach.module.rag.service.RagIndexService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rag/index")
public class RagIndexController {

    private final RagIndexService ragIndexService;

    public RagIndexController(RagIndexService ragIndexService) {
        this.ragIndexService = ragIndexService;
    }

    @PostMapping("/knowledge-articles/{articleId}")
    public Result<RagIndexResult> indexKnowledgeArticle(@PathVariable Long articleId) {
        return Result.success(ragIndexService.indexKnowledgeArticle(articleId));
    }

    @PostMapping("/knowledge-articles")
    public Result<RagBatchIndexResult> indexAllKnowledgeArticles() {
        return Result.success(ragIndexService.indexAllKnowledgeArticles());
    }
}
