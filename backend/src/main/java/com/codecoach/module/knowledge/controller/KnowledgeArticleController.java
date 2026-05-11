package com.codecoach.module.knowledge.controller;

import com.codecoach.common.result.PageResult;
import com.codecoach.common.result.Result;
import com.codecoach.module.knowledge.dto.KnowledgeArticlePageRequest;
import com.codecoach.module.knowledge.service.KnowledgeArticleService;
import com.codecoach.module.knowledge.vo.KnowledgeArticleDetailVO;
import com.codecoach.module.knowledge.vo.KnowledgeArticleListVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class KnowledgeArticleController {

    private final KnowledgeArticleService knowledgeArticleService;

    public KnowledgeArticleController(KnowledgeArticleService knowledgeArticleService) {
        this.knowledgeArticleService = knowledgeArticleService;
    }

    @GetMapping("/api/knowledge-articles")
    public Result<PageResult<KnowledgeArticleListVO>> pageArticles(KnowledgeArticlePageRequest request) {
        return Result.success(knowledgeArticleService.pageArticles(request));
    }

    @GetMapping("/api/knowledge-articles/{articleId}")
    public Result<KnowledgeArticleDetailVO> getArticleDetail(@PathVariable Long articleId) {
        return Result.success(knowledgeArticleService.getArticleDetail(articleId));
    }

    @GetMapping("/api/knowledge-topics/{topicId}/article")
    public Result<KnowledgeArticleDetailVO> getArticleByTopicId(@PathVariable Long topicId) {
        return Result.success(knowledgeArticleService.getArticleByTopicId(topicId));
    }
}
