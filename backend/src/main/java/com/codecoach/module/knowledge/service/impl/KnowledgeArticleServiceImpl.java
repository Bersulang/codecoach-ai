package com.codecoach.module.knowledge.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.codecoach.common.exception.BusinessException;
import com.codecoach.common.result.PageResult;
import com.codecoach.module.knowledge.dto.KnowledgeArticlePageRequest;
import com.codecoach.module.knowledge.entity.KnowledgeArticle;
import com.codecoach.module.knowledge.entity.KnowledgeTopic;
import com.codecoach.module.knowledge.mapper.KnowledgeArticleMapper;
import com.codecoach.module.knowledge.mapper.KnowledgeTopicMapper;
import com.codecoach.module.knowledge.service.KnowledgeArticleService;
import com.codecoach.module.knowledge.support.KnowledgeMarkdownReader;
import com.codecoach.module.knowledge.vo.KnowledgeArticleDetailVO;
import com.codecoach.module.knowledge.vo.KnowledgeArticleListVO;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Service
public class KnowledgeArticleServiceImpl implements KnowledgeArticleService {

    private static final int ARTICLE_NOT_FOUND_CODE = 5101;

    private static final String ARTICLE_NOT_FOUND_MESSAGE = "知识文章不存在";

    private static final String STATUS_PUBLISHED = "PUBLISHED";

    private static final int NOT_DELETED = 0;

    private static final long DEFAULT_PAGE_NUM = 1L;

    private static final long DEFAULT_PAGE_SIZE = 10L;

    private static final long MAX_PAGE_SIZE = 100L;

    private final KnowledgeArticleMapper knowledgeArticleMapper;

    private final KnowledgeTopicMapper knowledgeTopicMapper;

    private final KnowledgeMarkdownReader knowledgeMarkdownReader;

    public KnowledgeArticleServiceImpl(
            KnowledgeArticleMapper knowledgeArticleMapper,
            KnowledgeTopicMapper knowledgeTopicMapper,
            KnowledgeMarkdownReader knowledgeMarkdownReader
    ) {
        this.knowledgeArticleMapper = knowledgeArticleMapper;
        this.knowledgeTopicMapper = knowledgeTopicMapper;
        this.knowledgeMarkdownReader = knowledgeMarkdownReader;
    }

    @Override
    public PageResult<KnowledgeArticleListVO> pageArticles(KnowledgeArticlePageRequest request) {
        long pageNum = normalizePageNum(request.getPageNum());
        long pageSize = normalizePageSize(request.getPageSize());
        List<Long> categoryTopicIds = getTopicIdsByCategory(request.getCategory());
        List<Long> keywordTopicIds = getTopicIdsByKeyword(request.getKeyword());

        if (StringUtils.hasText(request.getCategory()) && categoryTopicIds.isEmpty()) {
            return emptyPageResult(pageNum, pageSize);
        }

        LambdaQueryWrapper<KnowledgeArticle> queryWrapper = new LambdaQueryWrapper<KnowledgeArticle>()
                .eq(KnowledgeArticle::getStatus, STATUS_PUBLISHED)
                .eq(KnowledgeArticle::getIsDeleted, NOT_DELETED)
                .eq(request.getTopicId() != null, KnowledgeArticle::getTopicId, request.getTopicId())
                .in(!categoryTopicIds.isEmpty(), KnowledgeArticle::getTopicId, categoryTopicIds)
                .and(StringUtils.hasText(request.getKeyword()), wrapper -> {
                    wrapper.like(KnowledgeArticle::getTitle, request.getKeyword())
                            .or()
                            .like(KnowledgeArticle::getSummary, request.getKeyword());
                    if (!keywordTopicIds.isEmpty()) {
                        wrapper.or().in(KnowledgeArticle::getTopicId, keywordTopicIds);
                    }
                })
                .orderByAsc(KnowledgeArticle::getSortOrder)
                .orderByDesc(KnowledgeArticle::getUpdatedAt)
                .orderByDesc(KnowledgeArticle::getId);

        Page<KnowledgeArticle> page = knowledgeArticleMapper.selectPage(new Page<>(pageNum, pageSize), queryWrapper);
        return toPageResult(page);
    }

    @Override
    public KnowledgeArticleDetailVO getArticleDetail(Long articleId) {
        KnowledgeArticle article = knowledgeArticleMapper.selectById(articleId);
        validateArticle(article);
        KnowledgeTopic topic = article.getTopicId() == null ? null : knowledgeTopicMapper.selectById(article.getTopicId());
        String content = knowledgeMarkdownReader.readMarkdown(article.getContentPath());
        return toDetailVO(article, topic, content);
    }

    @Override
    public KnowledgeArticleDetailVO getArticleByTopicId(Long topicId) {
        KnowledgeArticle article = knowledgeArticleMapper.selectOne(new LambdaQueryWrapper<KnowledgeArticle>()
                .eq(KnowledgeArticle::getTopicId, topicId)
                .eq(KnowledgeArticle::getStatus, STATUS_PUBLISHED)
                .eq(KnowledgeArticle::getIsDeleted, NOT_DELETED)
                .orderByAsc(KnowledgeArticle::getSortOrder)
                .orderByDesc(KnowledgeArticle::getUpdatedAt)
                .orderByDesc(KnowledgeArticle::getId)
                .last("LIMIT 1"));
        validateArticle(article);
        KnowledgeTopic topic = topicId == null ? null : knowledgeTopicMapper.selectById(topicId);
        String content = knowledgeMarkdownReader.readMarkdown(article.getContentPath());
        return toDetailVO(article, topic, content);
    }

    private PageResult<KnowledgeArticleListVO> toPageResult(Page<KnowledgeArticle> page) {
        Map<Long, KnowledgeTopic> topicMap = getTopicMap(page.getRecords());
        List<KnowledgeArticleListVO> records = page.getRecords().stream()
                .map(article -> toListVO(article, topicMap.get(article.getTopicId())))
                .toList();
        return new PageResult<>(records, page.getTotal(), page.getCurrent(), page.getSize(), page.getPages());
    }

    private PageResult<KnowledgeArticleListVO> emptyPageResult(long pageNum, long pageSize) {
        return new PageResult<>(Collections.emptyList(), 0L, pageNum, pageSize, 0L);
    }

    private List<Long> getTopicIdsByCategory(String category) {
        if (!StringUtils.hasText(category)) {
            return Collections.emptyList();
        }
        return knowledgeTopicMapper.selectList(new LambdaQueryWrapper<KnowledgeTopic>()
                        .eq(KnowledgeTopic::getCategory, category))
                .stream()
                .map(KnowledgeTopic::getId)
                .toList();
    }

    private List<Long> getTopicIdsByKeyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return Collections.emptyList();
        }
        return knowledgeTopicMapper.selectList(new LambdaQueryWrapper<KnowledgeTopic>()
                        .like(KnowledgeTopic::getName, keyword))
                .stream()
                .map(KnowledgeTopic::getId)
                .toList();
    }

    private Map<Long, KnowledgeTopic> getTopicMap(List<KnowledgeArticle> articles) {
        Set<Long> topicIds = articles.stream()
                .map(KnowledgeArticle::getTopicId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        if (CollectionUtils.isEmpty(topicIds)) {
            return Collections.emptyMap();
        }
        return knowledgeTopicMapper.selectByIds(topicIds).stream()
                .collect(Collectors.toMap(KnowledgeTopic::getId, Function.identity(), (left, right) -> left));
    }

    private void validateArticle(KnowledgeArticle article) {
        if (article == null
                || !STATUS_PUBLISHED.equals(article.getStatus())
                || article.getIsDeleted() == null
                || article.getIsDeleted() != NOT_DELETED) {
            throw new BusinessException(ARTICLE_NOT_FOUND_CODE, ARTICLE_NOT_FOUND_MESSAGE);
        }
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

    private KnowledgeArticleListVO toListVO(KnowledgeArticle article, KnowledgeTopic topic) {
        return new KnowledgeArticleListVO(
                article.getId(),
                article.getTopicId(),
                topic == null ? null : topic.getCategory(),
                topic == null ? null : topic.getName(),
                article.getTitle(),
                article.getSummary(),
                article.getVersion(),
                article.getStatus(),
                article.getSortOrder(),
                article.getUpdatedAt()
        );
    }

    private KnowledgeArticleDetailVO toDetailVO(KnowledgeArticle article, KnowledgeTopic topic, String content) {
        return new KnowledgeArticleDetailVO(
                article.getId(),
                article.getTopicId(),
                topic == null ? null : topic.getCategory(),
                topic == null ? null : topic.getName(),
                article.getTitle(),
                article.getSummary(),
                content,
                article.getVersion(),
                article.getUpdatedAt()
        );
    }
}
