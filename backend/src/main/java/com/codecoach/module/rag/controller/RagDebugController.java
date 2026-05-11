package com.codecoach.module.rag.controller;

import com.codecoach.common.result.Result;
import com.codecoach.module.rag.config.RagProperties;
import com.codecoach.module.rag.model.EmbeddingResult;
import com.codecoach.module.rag.model.KnowledgeArticleChunkCommand;
import com.codecoach.module.rag.model.RagChunkCandidate;
import com.codecoach.module.rag.model.VectorSearchRequest;
import com.codecoach.module.rag.model.VectorSearchResult;
import com.codecoach.module.rag.model.VectorUpsertRequest;
import com.codecoach.module.rag.service.EmbeddingService;
import com.codecoach.module.rag.service.MarkdownChunkService;
import com.codecoach.module.rag.service.VectorStoreService;
import com.codecoach.module.knowledge.entity.KnowledgeArticle;
import com.codecoach.module.knowledge.entity.KnowledgeTopic;
import com.codecoach.module.knowledge.mapper.KnowledgeArticleMapper;
import com.codecoach.module.knowledge.mapper.KnowledgeTopicMapper;
import com.codecoach.module.knowledge.support.KnowledgeMarkdownReader;
import com.codecoach.common.exception.BusinessException;
import com.codecoach.common.result.ResultCode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rag/debug")
public class RagDebugController {

    private final EmbeddingService embeddingService;

    private final VectorStoreService vectorStoreService;

    private final RagProperties ragProperties;

    private final KnowledgeArticleMapper knowledgeArticleMapper;

    private final KnowledgeTopicMapper knowledgeTopicMapper;

    private final KnowledgeMarkdownReader knowledgeMarkdownReader;

    private final MarkdownChunkService markdownChunkService;

    public RagDebugController(
            EmbeddingService embeddingService,
            VectorStoreService vectorStoreService,
            RagProperties ragProperties,
            KnowledgeArticleMapper knowledgeArticleMapper,
            KnowledgeTopicMapper knowledgeTopicMapper,
            KnowledgeMarkdownReader knowledgeMarkdownReader,
            MarkdownChunkService markdownChunkService
    ) {
        this.embeddingService = embeddingService;
        this.vectorStoreService = vectorStoreService;
        this.ragProperties = ragProperties;
        this.knowledgeArticleMapper = knowledgeArticleMapper;
        this.knowledgeTopicMapper = knowledgeTopicMapper;
        this.knowledgeMarkdownReader = knowledgeMarkdownReader;
        this.markdownChunkService = markdownChunkService;
    }

    @PostMapping("/embed")
    public Result<EmbeddingDebugResponse> embed(@Valid @RequestBody EmbeddingDebugRequest request) {
        EmbeddingResult result = embeddingService.embed(request.getText());
        return Result.success(new EmbeddingDebugResponse(
                result.getModel(),
                result.getDimensions(),
                result.getVector() == null ? 0 : result.getVector().size(),
                result.getPromptTokens(),
                result.getTotalTokens()
        ));
    }

    @PostMapping("/vector-test")
    public Result<VectorTestResponse> vectorTest(@Valid @RequestBody EmbeddingDebugRequest request) {
        EmbeddingResult embedding = embeddingService.embed(request.getText());
        String vectorId = UUID.randomUUID().toString();

        vectorStoreService.ensureCollection();

        VectorUpsertRequest upsertRequest = new VectorUpsertRequest();
        upsertRequest.setVectorId(vectorId);
        upsertRequest.setVector(embedding.getVector());
        upsertRequest.setPayload(Map.of(
                "sourceType", "DEBUG",
                "debugVectorId", vectorId,
                "title", "RAG debug vector"
        ));
        vectorStoreService.upsert(upsertRequest);

        VectorSearchRequest searchRequest = new VectorSearchRequest();
        searchRequest.setVector(embedding.getVector());
        searchRequest.setTopK(3);
        searchRequest.setFilter(Map.of("debugVectorId", vectorId));
        List<VectorSearchResult> results = vectorStoreService.search(searchRequest);

        Double topScore = results.isEmpty() ? null : results.get(0).getScore();
        return Result.success(new VectorTestResponse(
                vectorId,
                embedding.getVector() == null ? 0 : embedding.getVector().size(),
                ragProperties.getVectorStore().getCollection(),
                results.size(),
                topScore
        ));
    }

    @PostMapping("/vector/delete")
    public Result<Boolean> deleteVector(@Valid @RequestBody VectorDeleteRequest request) {
        vectorStoreService.delete(request.getVectorId());
        return Result.success(Boolean.TRUE);
    }

    @PostMapping("/chunk-markdown")
    public Result<MarkdownChunkDebugResponse> chunkMarkdown(@Valid @RequestBody MarkdownChunkDebugRequest request) {
        KnowledgeArticle article = knowledgeArticleMapper.selectById(request.getArticleId());
        if (article == null) {
            throw new BusinessException(ResultCode.KNOWLEDGE_ARTICLE_NOT_FOUND);
        }
        KnowledgeTopic topic = article.getTopicId() == null ? null : knowledgeTopicMapper.selectById(article.getTopicId());
        String markdown = knowledgeMarkdownReader.readMarkdown(article.getContentPath());

        KnowledgeArticleChunkCommand command = new KnowledgeArticleChunkCommand();
        command.setArticleId(article.getId());
        command.setTopicId(article.getTopicId());
        command.setCategory(topic == null ? null : topic.getCategory());
        command.setTopicName(topic == null ? null : topic.getName());
        command.setTitle(article.getTitle());
        command.setMarkdown(markdown);

        List<RagChunkCandidate> chunks = markdownChunkService.chunkKnowledgeArticle(command);
        List<MarkdownChunkDebugItem> chunkItems = chunks.stream()
                .map(chunk -> new MarkdownChunkDebugItem(
                        chunk.getChunkIndex(),
                        chunk.getMetadata() == null ? null : String.valueOf(chunk.getMetadata().get("section")),
                        preview(chunk.getContent()),
                        chunk.getContent() == null ? 0 : chunk.getContent().length(),
                        chunk.getTokenCount()
                ))
                .toList();
        return Result.success(new MarkdownChunkDebugResponse(
                article.getId(),
                article.getTitle(),
                chunks.size(),
                chunkItems
        ));
    }

    public static class EmbeddingDebugRequest {

        @NotBlank(message = "文本不能为空")
        private String text;

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }
    }

    public static class EmbeddingDebugResponse {

        private String model;

        private Integer dimensions;

        private Integer vectorSize;

        private Integer promptTokens;

        private Integer totalTokens;

        public EmbeddingDebugResponse(
                String model,
                Integer dimensions,
                Integer vectorSize,
                Integer promptTokens,
                Integer totalTokens
        ) {
            this.model = model;
            this.dimensions = dimensions;
            this.vectorSize = vectorSize;
            this.promptTokens = promptTokens;
            this.totalTokens = totalTokens;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public Integer getDimensions() {
            return dimensions;
        }

        public void setDimensions(Integer dimensions) {
            this.dimensions = dimensions;
        }

        public Integer getVectorSize() {
            return vectorSize;
        }

        public void setVectorSize(Integer vectorSize) {
            this.vectorSize = vectorSize;
        }

        public Integer getPromptTokens() {
            return promptTokens;
        }

        public void setPromptTokens(Integer promptTokens) {
            this.promptTokens = promptTokens;
        }

        public Integer getTotalTokens() {
            return totalTokens;
        }

        public void setTotalTokens(Integer totalTokens) {
            this.totalTokens = totalTokens;
        }
    }

    public static class VectorTestResponse {

        private String vectorId;

        private Integer vectorSize;

        private String collection;

        private Integer resultCount;

        private Double topScore;

        public VectorTestResponse(
                String vectorId,
                Integer vectorSize,
                String collection,
                Integer resultCount,
                Double topScore
        ) {
            this.vectorId = vectorId;
            this.vectorSize = vectorSize;
            this.collection = collection;
            this.resultCount = resultCount;
            this.topScore = topScore;
        }

        public String getVectorId() {
            return vectorId;
        }

        public void setVectorId(String vectorId) {
            this.vectorId = vectorId;
        }

        public Integer getVectorSize() {
            return vectorSize;
        }

        public void setVectorSize(Integer vectorSize) {
            this.vectorSize = vectorSize;
        }

        public String getCollection() {
            return collection;
        }

        public void setCollection(String collection) {
            this.collection = collection;
        }

        public Integer getResultCount() {
            return resultCount;
        }

        public void setResultCount(Integer resultCount) {
            this.resultCount = resultCount;
        }

        public Double getTopScore() {
            return topScore;
        }

        public void setTopScore(Double topScore) {
            this.topScore = topScore;
        }
    }

    public static class VectorDeleteRequest {

        @NotBlank(message = "向量ID不能为空")
        private String vectorId;

        public String getVectorId() {
            return vectorId;
        }

        public void setVectorId(String vectorId) {
            this.vectorId = vectorId;
        }
    }

    public static class MarkdownChunkDebugRequest {

        @NotNull(message = "文章ID不能为空")
        private Long articleId;

        public Long getArticleId() {
            return articleId;
        }

        public void setArticleId(Long articleId) {
            this.articleId = articleId;
        }
    }

    public static class MarkdownChunkDebugResponse {

        private Long articleId;

        private String title;

        private Integer chunkCount;

        private List<MarkdownChunkDebugItem> chunks;

        public MarkdownChunkDebugResponse(
                Long articleId,
                String title,
                Integer chunkCount,
                List<MarkdownChunkDebugItem> chunks
        ) {
            this.articleId = articleId;
            this.title = title;
            this.chunkCount = chunkCount;
            this.chunks = chunks;
        }

        public Long getArticleId() {
            return articleId;
        }

        public void setArticleId(Long articleId) {
            this.articleId = articleId;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public Integer getChunkCount() {
            return chunkCount;
        }

        public void setChunkCount(Integer chunkCount) {
            this.chunkCount = chunkCount;
        }

        public List<MarkdownChunkDebugItem> getChunks() {
            return chunks;
        }

        public void setChunks(List<MarkdownChunkDebugItem> chunks) {
            this.chunks = chunks;
        }
    }

    public static class MarkdownChunkDebugItem {

        private Integer chunkIndex;

        private String section;

        private String contentPreview;

        private Integer contentLength;

        private Integer tokenCount;

        public MarkdownChunkDebugItem(
                Integer chunkIndex,
                String section,
                String contentPreview,
                Integer contentLength,
                Integer tokenCount
        ) {
            this.chunkIndex = chunkIndex;
            this.section = section;
            this.contentPreview = contentPreview;
            this.contentLength = contentLength;
            this.tokenCount = tokenCount;
        }

        public Integer getChunkIndex() {
            return chunkIndex;
        }

        public void setChunkIndex(Integer chunkIndex) {
            this.chunkIndex = chunkIndex;
        }

        public String getSection() {
            return section;
        }

        public void setSection(String section) {
            this.section = section;
        }

        public String getContentPreview() {
            return contentPreview;
        }

        public void setContentPreview(String contentPreview) {
            this.contentPreview = contentPreview;
        }

        public Integer getContentLength() {
            return contentLength;
        }

        public void setContentLength(Integer contentLength) {
            this.contentLength = contentLength;
        }

        public Integer getTokenCount() {
            return tokenCount;
        }

        public void setTokenCount(Integer tokenCount) {
            this.tokenCount = tokenCount;
        }
    }

    private String preview(String content) {
        if (content == null || content.length() <= 200) {
            return content;
        }
        return content.substring(0, 200);
    }
}
