package com.codecoach.module.knowledge.controller;

import com.codecoach.common.result.PageResult;
import com.codecoach.common.result.Result;
import com.codecoach.module.knowledge.dto.KnowledgeTopicPageRequest;
import com.codecoach.module.knowledge.service.KnowledgeTopicService;
import com.codecoach.module.knowledge.vo.KnowledgeTopicVO;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/knowledge-topics")
public class KnowledgeTopicController {

    private final KnowledgeTopicService knowledgeTopicService;

    public KnowledgeTopicController(KnowledgeTopicService knowledgeTopicService) {
        this.knowledgeTopicService = knowledgeTopicService;
    }

    @GetMapping("/categories")
    public Result<List<String>> listCategories() {
        return Result.success(knowledgeTopicService.listCategories());
    }

    @GetMapping
    public Result<PageResult<KnowledgeTopicVO>> pageKnowledgeTopics(KnowledgeTopicPageRequest request) {
        return Result.success(knowledgeTopicService.pageKnowledgeTopics(request));
    }
}
