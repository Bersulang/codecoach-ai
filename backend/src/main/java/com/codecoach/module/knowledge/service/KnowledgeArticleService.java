package com.codecoach.module.knowledge.service;

import com.codecoach.common.result.PageResult;
import com.codecoach.module.knowledge.dto.KnowledgeArticlePageRequest;
import com.codecoach.module.knowledge.vo.KnowledgeArticleDetailVO;
import com.codecoach.module.knowledge.vo.KnowledgeArticleListVO;

public interface KnowledgeArticleService {

    PageResult<KnowledgeArticleListVO> pageArticles(KnowledgeArticlePageRequest request);

    KnowledgeArticleDetailVO getArticleDetail(Long articleId);

    KnowledgeArticleDetailVO getArticleByTopicId(Long topicId);
}
