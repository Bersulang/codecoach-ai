package com.codecoach.module.rag.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.codecoach.common.exception.BusinessException;
import com.codecoach.common.result.ResultCode;
import com.codecoach.module.knowledge.entity.KnowledgeArticle;
import com.codecoach.module.knowledge.entity.KnowledgeTopic;
import com.codecoach.module.knowledge.mapper.KnowledgeArticleMapper;
import com.codecoach.module.knowledge.mapper.KnowledgeTopicMapper;
import com.codecoach.module.knowledge.support.KnowledgeMarkdownReader;
import com.codecoach.module.project.entity.Project;
import com.codecoach.module.project.mapper.ProjectMapper;
import com.codecoach.module.rag.constant.RagConstants;
import com.codecoach.module.rag.entity.RagChunk;
import com.codecoach.module.rag.entity.RagDocument;
import com.codecoach.module.rag.entity.RagEmbedding;
import com.codecoach.module.rag.mapper.RagChunkMapper;
import com.codecoach.module.rag.mapper.RagDocumentMapper;
import com.codecoach.module.rag.mapper.RagEmbeddingMapper;
import com.codecoach.module.rag.model.EmbeddingResult;
import com.codecoach.module.rag.model.KnowledgeArticleChunkCommand;
import com.codecoach.module.rag.model.RagBatchIndexResult;
import com.codecoach.module.rag.model.RagChunkCandidate;
import com.codecoach.module.rag.model.RagIndexResult;
import com.codecoach.module.rag.model.VectorUpsertRequest;
import com.codecoach.module.rag.service.EmbeddingService;
import com.codecoach.module.rag.service.MarkdownChunkService;
import com.codecoach.module.rag.service.ProjectChunkService;
import com.codecoach.module.rag.service.RagIndexService;
import com.codecoach.module.rag.service.VectorStoreService;
import com.codecoach.security.UserContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Service
public class RagIndexServiceImpl implements RagIndexService {

    private static final Logger log = LoggerFactory.getLogger(RagIndexServiceImpl.class);

    private static final String ARTICLE_STATUS_PUBLISHED = "PUBLISHED";

    private static final Integer NOT_DELETED = 0;

    private static final int MAX_ERROR_MESSAGE_LENGTH = 512;

    private final KnowledgeArticleMapper knowledgeArticleMapper;

    private final KnowledgeTopicMapper knowledgeTopicMapper;

    private final KnowledgeMarkdownReader knowledgeMarkdownReader;

    private final MarkdownChunkService markdownChunkService;

    private final ProjectChunkService projectChunkService;

    private final ProjectMapper projectMapper;

    private final EmbeddingService embeddingService;

    private final VectorStoreService vectorStoreService;

    private final RagDocumentMapper ragDocumentMapper;

    private final RagChunkMapper ragChunkMapper;

    private final RagEmbeddingMapper ragEmbeddingMapper;

    private final ObjectMapper objectMapper;

    public RagIndexServiceImpl(
            KnowledgeArticleMapper knowledgeArticleMapper,
            KnowledgeTopicMapper knowledgeTopicMapper,
            KnowledgeMarkdownReader knowledgeMarkdownReader,
            MarkdownChunkService markdownChunkService,
            ProjectChunkService projectChunkService,
            ProjectMapper projectMapper,
            EmbeddingService embeddingService,
            VectorStoreService vectorStoreService,
            RagDocumentMapper ragDocumentMapper,
            RagChunkMapper ragChunkMapper,
            RagEmbeddingMapper ragEmbeddingMapper,
            ObjectMapper objectMapper
    ) {
        this.knowledgeArticleMapper = knowledgeArticleMapper;
        this.knowledgeTopicMapper = knowledgeTopicMapper;
        this.knowledgeMarkdownReader = knowledgeMarkdownReader;
        this.markdownChunkService = markdownChunkService;
        this.projectChunkService = projectChunkService;
        this.projectMapper = projectMapper;
        this.embeddingService = embeddingService;
        this.vectorStoreService = vectorStoreService;
        this.ragDocumentMapper = ragDocumentMapper;
        this.ragChunkMapper = ragChunkMapper;
        this.ragEmbeddingMapper = ragEmbeddingMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public RagIndexResult indexKnowledgeArticle(Long articleId) {
        RagDocument document = null;
        try {
            KnowledgeArticle article = getPublishedArticle(articleId);
            KnowledgeTopic topic = article.getTopicId() == null ? null : knowledgeTopicMapper.selectById(article.getTopicId());
            String markdown = knowledgeMarkdownReader.readMarkdown(article.getContentPath());
            List<RagChunkCandidate> candidates = chunkArticle(article, topic, markdown);

            if (candidates.isEmpty()) {
                document = prepareDocument(article);
                updateDocumentStatus(document, RagConstants.DOCUMENT_STATUS_FAILED, 0, "知识文章没有可索引切片");
                return withSource(failedResult(articleId, document.getId(), "知识文章没有可索引切片"),
                        RagConstants.SOURCE_TYPE_KNOWLEDGE_ARTICLE,
                        articleId);
            }

            vectorStoreService.ensureCollection();
            document = prepareDocument(article);
            return withSource(indexChunks(article.getId(), document, candidates),
                    RagConstants.SOURCE_TYPE_KNOWLEDGE_ARTICLE,
                    article.getId());
        } catch (Exception ex) {
            String errorMessage = toErrorMessage(ex);
            log.warn("Knowledge article RAG indexing failed, articleId={}, error={}", articleId, errorMessage);
            if (document != null) {
                updateDocumentStatus(document, RagConstants.DOCUMENT_STATUS_FAILED, 0, errorMessage);
            }
            return withSource(new RagIndexResult(
                            articleId,
                            document == null ? null : document.getId(),
                            0,
                            0,
                            0,
                            RagConstants.DOCUMENT_STATUS_FAILED,
                            errorMessage
                    ),
                    RagConstants.SOURCE_TYPE_KNOWLEDGE_ARTICLE,
                    articleId);
        }
    }

    @Override
    public RagBatchIndexResult indexAllKnowledgeArticles() {
        List<KnowledgeArticle> articles = knowledgeArticleMapper.selectList(new LambdaQueryWrapper<KnowledgeArticle>()
                .eq(KnowledgeArticle::getStatus, ARTICLE_STATUS_PUBLISHED)
                .eq(KnowledgeArticle::getIsDeleted, NOT_DELETED)
                .orderByAsc(KnowledgeArticle::getSortOrder)
                .orderByAsc(KnowledgeArticle::getId));

        List<RagIndexResult> results = new ArrayList<>();
        int indexedCount = 0;
        int failedCount = 0;
        for (KnowledgeArticle article : articles) {
            RagIndexResult result = indexKnowledgeArticle(article.getId());
            results.add(result);
            if (RagConstants.DOCUMENT_STATUS_INDEXED.equals(result.getStatus())) {
                indexedCount++;
            } else {
                failedCount++;
            }
        }
        return new RagBatchIndexResult(articles.size(), indexedCount, failedCount, results);
    }

    @Override
    public RagIndexResult indexProject(Long projectId) {
        Long currentUserId = UserContext.getCurrentUserId();
        Project project = getCurrentUserProject(projectId, currentUserId);
        RagDocument document = null;
        try {
            List<RagChunkCandidate> candidates = projectChunkService.chunkProject(project);
            if (candidates.isEmpty()) {
                document = prepareProjectDocument(project);
                updateDocumentStatus(document, RagConstants.DOCUMENT_STATUS_FAILED, 0, "项目档案没有可索引内容");
                return withSource(failedResult(null, document.getId(), "项目档案没有可索引内容"),
                        RagConstants.SOURCE_TYPE_PROJECT,
                        project.getId());
            }

            vectorStoreService.ensureCollection();
            document = prepareProjectDocument(project);
            return withSource(indexChunks(project.getId(), document, candidates),
                    RagConstants.SOURCE_TYPE_PROJECT,
                    project.getId());
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception ex) {
            String errorMessage = toErrorMessage(ex);
            log.warn("Project RAG indexing failed, projectId={}, userId={}, error={}", projectId, currentUserId, errorMessage);
            if (document != null) {
                updateDocumentStatus(document, RagConstants.DOCUMENT_STATUS_FAILED, 0, errorMessage);
            }
            return withSource(new RagIndexResult(
                            null,
                            document == null ? null : document.getId(),
                            0,
                            0,
                            0,
                            RagConstants.DOCUMENT_STATUS_FAILED,
                            errorMessage
                    ),
                    RagConstants.SOURCE_TYPE_PROJECT,
                    projectId);
        }
    }

    @Override
    public RagBatchIndexResult indexCurrentUserProjects() {
        Long currentUserId = UserContext.getCurrentUserId();
        List<Project> projects = projectMapper.selectList(new LambdaQueryWrapper<Project>()
                .eq(Project::getUserId, currentUserId)
                .eq(Project::getIsDeleted, NOT_DELETED)
                .orderByDesc(Project::getUpdatedAt)
                .orderByDesc(Project::getId));

        List<RagIndexResult> results = new ArrayList<>();
        int indexedCount = 0;
        int failedCount = 0;
        for (Project project : projects) {
            RagIndexResult result = indexProject(project.getId());
            results.add(result);
            if (RagConstants.DOCUMENT_STATUS_INDEXED.equals(result.getStatus())) {
                indexedCount++;
            } else {
                failedCount++;
            }
        }
        return new RagBatchIndexResult(projects.size(), indexedCount, failedCount, results);
    }

    private KnowledgeArticle getPublishedArticle(Long articleId) {
        KnowledgeArticle article = articleId == null ? null : knowledgeArticleMapper.selectById(articleId);
        if (article == null
                || !ARTICLE_STATUS_PUBLISHED.equals(article.getStatus())
                || !NOT_DELETED.equals(article.getIsDeleted())) {
            throw new BusinessException(ResultCode.KNOWLEDGE_ARTICLE_NOT_FOUND);
        }
        return article;
    }

    private Project getCurrentUserProject(Long projectId, Long currentUserId) {
        Project project = projectId == null ? null : projectMapper.selectById(projectId);
        if (project == null || !NOT_DELETED.equals(project.getIsDeleted())) {
            throw new BusinessException(2001, "项目不存在");
        }
        if (!currentUserId.equals(project.getUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        return project;
    }

    private List<RagChunkCandidate> chunkArticle(KnowledgeArticle article, KnowledgeTopic topic, String markdown) {
        KnowledgeArticleChunkCommand command = new KnowledgeArticleChunkCommand();
        command.setArticleId(article.getId());
        command.setTopicId(article.getTopicId());
        command.setCategory(topic == null ? null : topic.getCategory());
        command.setTopicName(topic == null ? null : topic.getName());
        command.setTitle(article.getTitle());
        command.setMarkdown(markdown);
        return markdownChunkService.chunkKnowledgeArticle(command);
    }

    private RagDocument prepareDocument(KnowledgeArticle article) {
        RagDocument document = findKnowledgeArticleDocument(article.getId());
        if (document == null) {
            document = new RagDocument();
            document.setOwnerType(RagConstants.OWNER_TYPE_SYSTEM);
            document.setOwnerId(null);
            document.setUserId(null);
            document.setTitle(article.getTitle());
            document.setSourceType(RagConstants.SOURCE_TYPE_KNOWLEDGE_ARTICLE);
            document.setSourceId(article.getId());
            document.setSourcePath(article.getContentPath());
            document.setStatus(RagConstants.DOCUMENT_STATUS_PENDING);
            document.setChunkCount(0);
            document.setErrorMessage(null);
            document.setCreatedAt(LocalDateTime.now());
            document.setUpdatedAt(LocalDateTime.now());
            ragDocumentMapper.insert(document);
            return document;
        }

        cleanupExistingIndex(document);
        document.setTitle(article.getTitle());
        document.setSourcePath(article.getContentPath());
        document.setStatus(RagConstants.DOCUMENT_STATUS_PENDING);
        document.setChunkCount(0);
        document.setErrorMessage(null);
        document.setUpdatedAt(LocalDateTime.now());
        ragDocumentMapper.updateById(document);
        return document;
    }

    private RagDocument prepareProjectDocument(Project project) {
        RagDocument document = findProjectDocument(project.getId(), project.getUserId());
        if (document == null) {
            document = new RagDocument();
            document.setOwnerType(RagConstants.OWNER_TYPE_USER);
            document.setOwnerId(project.getUserId());
            document.setUserId(project.getUserId());
            document.setTitle(project.getName());
            document.setSourceType(RagConstants.SOURCE_TYPE_PROJECT);
            document.setSourceId(project.getId());
            document.setSourcePath(null);
            document.setStatus(RagConstants.DOCUMENT_STATUS_PENDING);
            document.setChunkCount(0);
            document.setErrorMessage(null);
            document.setCreatedAt(LocalDateTime.now());
            document.setUpdatedAt(LocalDateTime.now());
            ragDocumentMapper.insert(document);
            return document;
        }

        cleanupExistingIndex(document);
        document.setTitle(project.getName());
        document.setSourcePath(null);
        document.setStatus(RagConstants.DOCUMENT_STATUS_PENDING);
        document.setChunkCount(0);
        document.setErrorMessage(null);
        document.setUpdatedAt(LocalDateTime.now());
        ragDocumentMapper.updateById(document);
        return document;
    }

    private RagDocument findKnowledgeArticleDocument(Long articleId) {
        return ragDocumentMapper.selectOne(new LambdaQueryWrapper<RagDocument>()
                .eq(RagDocument::getSourceType, RagConstants.SOURCE_TYPE_KNOWLEDGE_ARTICLE)
                .eq(RagDocument::getSourceId, articleId)
                .eq(RagDocument::getOwnerType, RagConstants.OWNER_TYPE_SYSTEM)
                .isNull(RagDocument::getOwnerId)
                .last("LIMIT 1"));
    }

    private RagDocument findProjectDocument(Long projectId, Long userId) {
        return ragDocumentMapper.selectOne(new LambdaQueryWrapper<RagDocument>()
                .eq(RagDocument::getSourceType, RagConstants.SOURCE_TYPE_PROJECT)
                .eq(RagDocument::getSourceId, projectId)
                .eq(RagDocument::getOwnerType, RagConstants.OWNER_TYPE_USER)
                .eq(RagDocument::getOwnerId, userId)
                .eq(RagDocument::getUserId, userId)
                .last("LIMIT 1"));
    }

    private void cleanupExistingIndex(RagDocument document) {
        List<RagChunk> chunks = ragChunkMapper.selectList(new LambdaQueryWrapper<RagChunk>()
                .eq(RagChunk::getDocumentId, document.getId()));
        if (chunks.isEmpty()) {
            return;
        }

        List<Long> chunkIds = chunks.stream().map(RagChunk::getId).toList();
        List<RagEmbedding> embeddings = ragEmbeddingMapper.selectList(new LambdaQueryWrapper<RagEmbedding>()
                .in(RagEmbedding::getChunkId, chunkIds));
        for (RagEmbedding embedding : embeddings) {
            try {
                vectorStoreService.delete(embedding.getVectorId());
            } catch (Exception ex) {
                log.warn(
                        "Delete old RAG vector failed, documentId={}, vectorId={}, error={}",
                        document.getId(),
                        embedding.getVectorId(),
                        abbreviate(ex.getMessage())
                );
            }
        }

        deleteByIds(ragEmbeddingMapper, embeddings.stream().map(RagEmbedding::getId).toList());
        deleteByIds(ragChunkMapper, chunkIds);
    }

    private RagIndexResult indexChunks(Long sourceId, RagDocument document, List<RagChunkCandidate> candidates) {
        int embeddedCount = 0;
        int failedCount = 0;
        String firstError = null;

        for (RagChunkCandidate candidate : candidates) {
            RagChunk chunk = savePendingChunk(document, candidate);
            try {
                EmbeddingResult embeddingResult = embeddingService.embed(candidate.getContent());
                String vectorId = UUID.randomUUID().toString();
                vectorStoreService.upsert(buildUpsertRequest(vectorId, embeddingResult, document, chunk, candidate));
                saveEmbedding(chunk, vectorId, embeddingResult);
                updateChunkStatus(chunk, RagConstants.EMBEDDING_STATUS_EMBEDDED);
                embeddedCount++;
            } catch (Exception ex) {
                failedCount++;
                String errorMessage = toErrorMessage(ex);
                if (firstError == null) {
                    firstError = errorMessage;
                }
                updateChunkStatus(chunk, RagConstants.EMBEDDING_STATUS_FAILED);
                log.warn(
                        "RAG chunk indexing failed, sourceType={}, sourceId={}, documentId={}, chunkIndex={}, error={}",
                        document.getSourceType(),
                        sourceId,
                        document.getId(),
                        candidate.getChunkIndex(),
                        errorMessage
                );
            }
        }

        String status = embeddedCount > 0
                ? RagConstants.DOCUMENT_STATUS_INDEXED
                : RagConstants.DOCUMENT_STATUS_FAILED;
        String documentError = failedCount > 0
                ? abbreviate(embeddedCount > 0 ? "部分切片索引失败：" + firstError : firstError)
                : null;
        updateDocumentStatus(document, status, candidates.size(), documentError);

        return new RagIndexResult(
                RagConstants.SOURCE_TYPE_KNOWLEDGE_ARTICLE.equals(document.getSourceType()) ? sourceId : null,
                document.getId(),
                candidates.size(),
                embeddedCount,
                failedCount,
                status,
                documentError
        );
    }

    private RagChunk savePendingChunk(RagDocument document, RagChunkCandidate candidate) {
        RagChunk chunk = new RagChunk();
        chunk.setDocumentId(document.getId());
        chunk.setUserId(document.getUserId());
        chunk.setChunkIndex(candidate.getChunkIndex());
        chunk.setContent(candidate.getContent());
        chunk.setTokenCount(candidate.getTokenCount());
        chunk.setMetadata(toJson(candidate.getMetadata()));
        chunk.setEmbeddingStatus(RagConstants.EMBEDDING_STATUS_PENDING);
        chunk.setCreatedAt(LocalDateTime.now());
        ragChunkMapper.insert(chunk);
        return chunk;
    }

    private VectorUpsertRequest buildUpsertRequest(
            String vectorId,
            EmbeddingResult embeddingResult,
            RagDocument document,
            RagChunk chunk,
            RagChunkCandidate candidate
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        if (candidate.getMetadata() != null) {
            payload.putAll(candidate.getMetadata());
        }
        payload.put("chunkId", chunk.getId());
        payload.put("documentId", document.getId());
        payload.put("sourceType", document.getSourceType());
        payload.put("ownerType", document.getOwnerType());
        putIfNotNull(payload, "userId", document.getUserId());

        VectorUpsertRequest request = new VectorUpsertRequest();
        request.setVectorId(vectorId);
        request.setVector(embeddingResult.getVector());
        request.setPayload(payload);
        return request;
    }

    private void saveEmbedding(RagChunk chunk, String vectorId, EmbeddingResult embeddingResult) {
        RagEmbedding embedding = new RagEmbedding();
        embedding.setChunkId(chunk.getId());
        embedding.setVectorId(vectorId);
        embedding.setEmbeddingModel(embeddingResult.getModel());
        embedding.setVectorStore(RagConstants.VECTOR_STORE_QDRANT);
        embedding.setCreatedAt(LocalDateTime.now());
        ragEmbeddingMapper.insert(embedding);
    }

    private void updateChunkStatus(RagChunk chunk, String status) {
        chunk.setEmbeddingStatus(status);
        ragChunkMapper.updateById(chunk);
    }

    private void updateDocumentStatus(RagDocument document, String status, Integer chunkCount, String errorMessage) {
        document.setStatus(status);
        document.setChunkCount(chunkCount);
        document.setErrorMessage(abbreviate(errorMessage));
        document.setUpdatedAt(LocalDateTime.now());
        ragDocumentMapper.updateById(document);
    }

    private RagIndexResult failedResult(Long articleId, Long documentId, String errorMessage) {
        return new RagIndexResult(
                articleId,
                documentId,
                0,
                0,
                0,
                RagConstants.DOCUMENT_STATUS_FAILED,
                abbreviate(errorMessage)
        );
    }

    private RagIndexResult withSource(RagIndexResult result, String sourceType, Long sourceId) {
        result.setSourceType(sourceType);
        result.setSourceId(sourceId);
        return result;
    }

    private String toJson(Map<String, Object> metadata) {
        if (CollectionUtils.isEmpty(metadata)) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("RAG chunk metadata serialization failed", ex);
        }
    }

    private void putIfNotNull(Map<String, Object> payload, String key, Object value) {
        if (value != null) {
            payload.put(key, value);
        }
    }

    private void deleteByIds(com.baomidou.mybatisplus.core.mapper.BaseMapper<?> mapper, Collection<Long> ids) {
        if (!CollectionUtils.isEmpty(ids)) {
            mapper.deleteBatchIds(ids);
        }
    }

    private String toErrorMessage(Exception ex) {
        if (ex instanceof BusinessException businessException) {
            return abbreviate(businessException.getMessage());
        }
        String message = ex.getMessage();
        if (!StringUtils.hasText(message)) {
            message = ex.getClass().getSimpleName();
        }
        return abbreviate(message);
    }

    private String abbreviate(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        String trimmed = text.trim();
        if (trimmed.length() <= MAX_ERROR_MESSAGE_LENGTH) {
            return trimmed;
        }
        return trimmed.substring(0, MAX_ERROR_MESSAGE_LENGTH);
    }
}
