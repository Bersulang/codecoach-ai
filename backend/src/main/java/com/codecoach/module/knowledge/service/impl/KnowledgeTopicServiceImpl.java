package com.codecoach.module.knowledge.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.codecoach.common.result.PageResult;
import com.codecoach.module.knowledge.dto.KnowledgeTopicPageRequest;
import com.codecoach.module.knowledge.entity.KnowledgeTopic;
import com.codecoach.module.knowledge.mapper.KnowledgeTopicMapper;
import com.codecoach.module.knowledge.service.KnowledgeTopicService;
import com.codecoach.module.knowledge.vo.KnowledgeTopicVO;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class KnowledgeTopicServiceImpl implements KnowledgeTopicService {

    private static final String STATUS_ENABLED = "ENABLED";

    private static final int NOT_DELETED = 0;

    private static final long DEFAULT_PAGE_NUM = 1L;

    private static final long DEFAULT_PAGE_SIZE = 20L;

    private static final long MAX_PAGE_SIZE = 100L;

    private final KnowledgeTopicMapper knowledgeTopicMapper;

    public KnowledgeTopicServiceImpl(KnowledgeTopicMapper knowledgeTopicMapper) {
        this.knowledgeTopicMapper = knowledgeTopicMapper;
    }

    @Override
    public List<String> listCategories() {
        LambdaQueryWrapper<KnowledgeTopic> queryWrapper = new LambdaQueryWrapper<KnowledgeTopic>()
                .eq(KnowledgeTopic::getStatus, STATUS_ENABLED)
                .eq(KnowledgeTopic::getIsDeleted, NOT_DELETED)
                .isNotNull(KnowledgeTopic::getCategory)
                .ne(KnowledgeTopic::getCategory, "")
                .orderByAsc(KnowledgeTopic::getSortOrder)
                .orderByAsc(KnowledgeTopic::getId);

        return knowledgeTopicMapper.selectList(queryWrapper).stream()
                .map(KnowledgeTopic::getCategory)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
    }

    @Override
    public PageResult<KnowledgeTopicVO> pageKnowledgeTopics(KnowledgeTopicPageRequest request) {
        long pageNum = normalizePageNum(request.getPageNum());
        long pageSize = normalizePageSize(request.getPageSize());

        LambdaQueryWrapper<KnowledgeTopic> queryWrapper = new LambdaQueryWrapper<KnowledgeTopic>()
                .eq(KnowledgeTopic::getStatus, STATUS_ENABLED)
                .eq(KnowledgeTopic::getIsDeleted, NOT_DELETED)
                .eq(StringUtils.hasText(request.getCategory()), KnowledgeTopic::getCategory, request.getCategory())
                .eq(StringUtils.hasText(request.getDifficulty()), KnowledgeTopic::getDifficulty, request.getDifficulty())
                .and(StringUtils.hasText(request.getKeyword()), wrapper -> wrapper
                        .like(KnowledgeTopic::getName, request.getKeyword())
                        .or()
                        .like(KnowledgeTopic::getDescription, request.getKeyword())
                        .or()
                        .like(KnowledgeTopic::getInterviewFocus, request.getKeyword())
                        .or()
                        .like(KnowledgeTopic::getTags, request.getKeyword())
                )
                .orderByAsc(KnowledgeTopic::getCategory)
                .orderByAsc(KnowledgeTopic::getSortOrder)
                .orderByAsc(KnowledgeTopic::getId);

        Page<KnowledgeTopic> page = knowledgeTopicMapper.selectPage(new Page<>(pageNum, pageSize), queryWrapper);
        List<KnowledgeTopicVO> records = page.getRecords().stream()
                .map(this::toKnowledgeTopicVO)
                .toList();

        return new PageResult<>(records, page.getTotal(), page.getCurrent(), page.getSize(), page.getPages());
    }

    private long normalizePageNum(Long pageNum) {
        if (pageNum == null || pageNum < DEFAULT_PAGE_NUM) {
            return DEFAULT_PAGE_NUM;
        }
        return pageNum;
    }

    private long normalizePageSize(Long pageSize) {
        if (pageSize == null || pageSize < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    private KnowledgeTopicVO toKnowledgeTopicVO(KnowledgeTopic topic) {
        return new KnowledgeTopicVO(
                topic.getId(),
                topic.getCategory(),
                topic.getName(),
                topic.getDescription(),
                topic.getDifficulty(),
                topic.getInterviewFocus(),
                parseTags(topic.getTags()),
                topic.getSortOrder()
        );
    }

    private List<String> parseTags(String tags) {
        if (!StringUtils.hasText(tags)) {
            return List.of();
        }
        return Arrays.stream(tags.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }
}
