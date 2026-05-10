package com.codecoach.module.knowledge.service;

import com.codecoach.common.result.PageResult;
import com.codecoach.module.knowledge.dto.KnowledgeTopicPageRequest;
import com.codecoach.module.knowledge.vo.KnowledgeTopicVO;
import java.util.List;

public interface KnowledgeTopicService {

    List<String> listCategories();

    PageResult<KnowledgeTopicVO> pageKnowledgeTopics(KnowledgeTopicPageRequest request);
}
