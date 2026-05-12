package com.codecoach.module.document.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.codecoach.common.exception.BusinessException;
import com.codecoach.common.result.ResultCode;
import com.codecoach.common.storage.AliyunOssService;
import com.codecoach.common.storage.OssUploadResult;
import com.codecoach.module.document.entity.UserDocument;
import com.codecoach.module.document.mapper.UserDocumentMapper;
import com.codecoach.module.document.model.ParsedDocument;
import com.codecoach.module.document.model.UserDocumentChunkCommand;
import com.codecoach.module.document.service.DocumentParseService;
import com.codecoach.module.document.service.UserDocumentChunkService;
import com.codecoach.module.document.service.UserDocumentService;
import com.codecoach.module.document.vo.UserDocumentVO;
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
import com.codecoach.module.rag.model.RagChunkCandidate;
import com.codecoach.module.rag.model.VectorUpsertRequest;
import com.codecoach.module.rag.service.EmbeddingService;
import com.codecoach.module.rag.service.VectorStoreService;
import com.codecoach.security.UserContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class UserDocumentServiceImpl implements UserDocumentService {

    private static final Logger log = LoggerFactory.getLogger(UserDocumentServiceImpl.class);

    private static final long MAX_FILE_SIZE = 10L * 1024L * 1024L;
    private static final int NOT_DELETED = 0;
    private static final int DELETED = 1;
    private static final int MAX_ERROR_MESSAGE_LENGTH = 512;

    private static final String PARSE_STATUS_PENDING = "PENDING";
    private static final String PARSE_STATUS_PARSED = "PARSED";
    private static final String PARSE_STATUS_FAILED = "FAILED";
    private static final String INDEX_STATUS_PENDING = "PENDING";
    private static final String INDEX_STATUS_INDEXED = "INDEXED";
    private static final String INDEX_STATUS_FAILED = "FAILED";
    private static final Duration DOCUMENT_LOCK_TTL = Duration.ofMinutes(10);
    private static final String DOCUMENT_LOCK_KEY_PREFIX = "user-document:lock:";
    private static final DefaultRedisScript<Long> RELEASE_LOCK_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then "
                    + "return redis.call('del', KEYS[1]) "
                    + "else return 0 end",
            Long.class
    );

    private static final Set<String> TEXT_MIME_TYPES = Set.of("text/plain", "application/octet-stream");
    private static final Set<String> MARKDOWN_MIME_TYPES = Set.of(
            "text/markdown",
            "text/x-markdown",
            "text/plain",
            "application/octet-stream"
    );
    private static final Set<String> PDF_MIME_TYPES = Set.of("application/pdf", "application/x-pdf");
    private static final Set<String> EXECUTABLE_EXTENSIONS = Set.of(
            "exe", "dll", "bat", "cmd", "sh", "js", "jar", "msi", "app", "dmg"
    );

    private final UserDocumentMapper userDocumentMapper;
    private final ProjectMapper projectMapper;
    private final AliyunOssService aliyunOssService;
    private final DocumentParseService documentParseService;
    private final UserDocumentChunkService userDocumentChunkService;
    private final EmbeddingService embeddingService;
    private final VectorStoreService vectorStoreService;
    private final RagDocumentMapper ragDocumentMapper;
    private final RagChunkMapper ragChunkMapper;
    private final RagEmbeddingMapper ragEmbeddingMapper;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate stringRedisTemplate;

    public UserDocumentServiceImpl(
            UserDocumentMapper userDocumentMapper,
            ProjectMapper projectMapper,
            AliyunOssService aliyunOssService,
            DocumentParseService documentParseService,
            UserDocumentChunkService userDocumentChunkService,
            EmbeddingService embeddingService,
            VectorStoreService vectorStoreService,
            RagDocumentMapper ragDocumentMapper,
            RagChunkMapper ragChunkMapper,
            RagEmbeddingMapper ragEmbeddingMapper,
            ObjectMapper objectMapper,
            StringRedisTemplate stringRedisTemplate
    ) {
        this.userDocumentMapper = userDocumentMapper;
        this.projectMapper = projectMapper;
        this.aliyunOssService = aliyunOssService;
        this.documentParseService = documentParseService;
        this.userDocumentChunkService = userDocumentChunkService;
        this.embeddingService = embeddingService;
        this.vectorStoreService = vectorStoreService;
        this.ragDocumentMapper = ragDocumentMapper;
        this.ragChunkMapper = ragChunkMapper;
        this.ragEmbeddingMapper = ragEmbeddingMapper;
        this.objectMapper = objectMapper;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public UserDocumentVO upload(MultipartFile file, Long projectId, String title) {
        Long userId = UserContext.getCurrentUserId();
        FileInfo fileInfo = validateFile(file);
        validateProject(projectId, userId);

        byte[] content = readFileBytes(file);
        String objectKey = "documents/" + userId + "/" + UUID.randomUUID() + "." + fileInfo.extension();
        OssUploadResult uploadResult;
        try {
            uploadResult = aliyunOssService.uploadWithKey(content, objectKey);
        } catch (BusinessException exception) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR.getCode(), "文件上传失败，请稍后重试");
        }

        UserDocument document = buildDocument(userId, projectId, title, fileInfo, uploadResult);
        try {
            userDocumentMapper.insert(document);
        } catch (Exception exception) {
            cleanupOssQuietly(uploadResult.objectKey());
            throw new BusinessException(ResultCode.INTERNAL_ERROR.getCode(), "文件保存失败，请稍后重试");
        }

        parseAndIndex(document, content);
        return toVO(userDocumentMapper.selectById(document.getId()));
    }

    @Override
    public List<UserDocumentVO> list(Long projectId, String fileType) {
        Long userId = UserContext.getCurrentUserId();
        String normalizedFileType = normalizeFileTypeFilter(fileType);
        return userDocumentMapper.selectList(new LambdaQueryWrapper<UserDocument>()
                        .eq(UserDocument::getUserId, userId)
                        .eq(UserDocument::getIsDeleted, NOT_DELETED)
                        .eq(projectId != null, UserDocument::getProjectId, projectId)
                        .eq(StringUtils.hasText(normalizedFileType), UserDocument::getFileType, normalizedFileType)
                        .orderByDesc(UserDocument::getCreatedAt)
                        .orderByDesc(UserDocument::getId))
                .stream()
                .map(this::toVO)
                .toList();
    }

    @Override
    public UserDocumentVO getDetail(Long id) {
        return toVO(getCurrentUserDocument(id));
    }

    @Override
    public void delete(Long id) {
        UserDocument document = getCurrentUserDocument(id);
        String lockKey = DOCUMENT_LOCK_KEY_PREFIX + document.getId();
        String lockValue = UUID.randomUUID().toString();
        acquireDocumentLock(lockKey, lockValue);
        try {
            cleanupRagIndex(document, true);

            LocalDateTime now = LocalDateTime.now();
            document.setIsDeleted(DELETED);
            document.setDeletedAt(now);
            document.setUpdatedAt(now);
            userDocumentMapper.updateById(document);
            cleanupOssQuietly(document.getOssKey());
        } finally {
            releaseDocumentLock(lockKey, lockValue);
        }
    }

    @Override
    public UserDocumentVO reindex(Long id) {
        UserDocument document = getCurrentUserDocument(id);
        String lockKey = DOCUMENT_LOCK_KEY_PREFIX + document.getId();
        String lockValue = UUID.randomUUID().toString();
        acquireDocumentLock(lockKey, lockValue);
        try {
            byte[] content;
            try {
                content = aliyunOssService.download(document.getOssKey());
            } catch (BusinessException exception) {
                updateStatus(document, PARSE_STATUS_FAILED, INDEX_STATUS_FAILED, "文件读取失败，请稍后重试");
                return toVO(userDocumentMapper.selectById(document.getId()));
            }
            cleanupRagIndex(document, true);
            parseAndIndex(document, content);
            return toVO(userDocumentMapper.selectById(document.getId()));
        } finally {
            releaseDocumentLock(lockKey, lockValue);
        }
    }

    private void parseAndIndex(UserDocument document, byte[] content) {
        long start = System.currentTimeMillis();
        try {
            updateStatus(document, PARSE_STATUS_PENDING, INDEX_STATUS_PENDING, null);
            ParsedDocument parsedDocument = documentParseService.parse(content, document.getFileType());
            updateStatus(document, PARSE_STATUS_PARSED, INDEX_STATUS_PENDING, null);

            List<RagChunkCandidate> candidates = userDocumentChunkService.chunk(buildChunkCommand(document, parsedDocument));
            if (candidates.isEmpty()) {
                updateStatus(document, PARSE_STATUS_PARSED, INDEX_STATUS_FAILED, "文档没有可索引内容");
                return;
            }

            IndexOutcome outcome = indexChunks(document, candidates);
            String indexStatus = outcome.embeddedCount() > 0 ? INDEX_STATUS_INDEXED : INDEX_STATUS_FAILED;
            updateStatus(document, PARSE_STATUS_PARSED, indexStatus, outcome.errorMessage());
            log.info(
                    "User document indexed, userId={}, documentId={}, fileType={}, fileSize={}, projectId={}, chunkCount={}, embeddedCount={}, failedCount={}, latencyMs={}",
                    document.getUserId(),
                    document.getId(),
                    document.getFileType(),
                    document.getFileSize(),
                    document.getProjectId(),
                    candidates.size(),
                    outcome.embeddedCount(),
                    outcome.failedCount(),
                    System.currentTimeMillis() - start
            );
        } catch (BusinessException exception) {
            updateStatus(document, PARSE_STATUS_FAILED, INDEX_STATUS_FAILED, toErrorMessage(exception));
        } catch (Exception exception) {
            updateStatus(document, PARSE_STATUS_PARSED, INDEX_STATUS_FAILED, toErrorMessage(exception));
            log.warn("User document indexing failed, userId={}, documentId={}, fileType={}, error={}",
                    document.getUserId(),
                    document.getId(),
                    document.getFileType(),
                    abbreviate(exception.getMessage()));
        }
    }

    private IndexOutcome indexChunks(UserDocument userDocument, List<RagChunkCandidate> candidates) {
        vectorStoreService.ensureCollection();
        RagDocument ragDocument = prepareRagDocument(userDocument);

        int embeddedCount = 0;
        int failedCount = 0;
        String firstError = null;
        for (RagChunkCandidate candidate : candidates) {
            RagChunk chunk = saveChunk(ragDocument, candidate);
            try {
                EmbeddingResult embeddingResult = embeddingService.embed(candidate.getContent());
                String vectorId = UUID.randomUUID().toString();
                vectorStoreService.upsert(buildUpsertRequest(vectorId, embeddingResult, ragDocument, chunk, candidate));
                saveEmbedding(chunk, vectorId, embeddingResult);
                updateChunkStatus(chunk, RagConstants.EMBEDDING_STATUS_EMBEDDED);
                embeddedCount++;
            } catch (Exception exception) {
                failedCount++;
                if (firstError == null) {
                    firstError = toErrorMessage(exception);
                }
                updateChunkStatus(chunk, RagConstants.EMBEDDING_STATUS_FAILED);
                log.warn("User document chunk indexing failed, userId={}, documentId={}, chunkIndex={}, error={}",
                        userDocument.getUserId(),
                        userDocument.getId(),
                        candidate.getChunkIndex(),
                        abbreviate(exception.getMessage()));
            }
        }

        String status = embeddedCount > 0
                ? RagConstants.DOCUMENT_STATUS_INDEXED
                : RagConstants.DOCUMENT_STATUS_FAILED;
        String errorMessage = failedCount > 0
                ? abbreviate(embeddedCount > 0 ? "部分切片索引失败：" + firstError : firstError)
                : null;
        ragDocument.setStatus(status);
        ragDocument.setChunkCount(candidates.size());
        ragDocument.setErrorMessage(errorMessage);
        ragDocument.setUpdatedAt(LocalDateTime.now());
        ragDocumentMapper.updateById(ragDocument);
        return new IndexOutcome(embeddedCount, failedCount, errorMessage);
    }

    private RagDocument prepareRagDocument(UserDocument userDocument) {
        RagDocument document = findRagDocument(userDocument);
        if (document == null) {
            document = new RagDocument();
            document.setOwnerType(RagConstants.OWNER_TYPE_USER);
            document.setOwnerId(userDocument.getUserId());
            document.setUserId(userDocument.getUserId());
            document.setSourceType(RagConstants.SOURCE_TYPE_USER_UPLOAD);
            document.setSourceId(userDocument.getId());
            document.setCreatedAt(LocalDateTime.now());
        } else {
            cleanupRagDocumentChildren(document);
        }
        document.setTitle(userDocument.getTitle());
        document.setSourcePath(userDocument.getOssKey());
        document.setStatus(RagConstants.DOCUMENT_STATUS_PENDING);
        document.setChunkCount(0);
        document.setErrorMessage(null);
        document.setUpdatedAt(LocalDateTime.now());
        if (document.getId() == null) {
            ragDocumentMapper.insert(document);
        } else {
            ragDocumentMapper.updateById(document);
        }
        return document;
    }

    private void cleanupRagIndex(UserDocument userDocument, boolean disableDocument) {
        RagDocument document = findRagDocument(userDocument);
        if (document == null) {
            return;
        }
        cleanupRagDocumentChildren(document);
        if (disableDocument) {
            document.setStatus(RagConstants.DOCUMENT_STATUS_DISABLED);
            document.setChunkCount(0);
            document.setErrorMessage(null);
            document.setUpdatedAt(LocalDateTime.now());
            ragDocumentMapper.updateById(document);
        }
    }

    private void cleanupRagDocumentChildren(RagDocument document) {
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
            } catch (Exception exception) {
                log.warn("Delete user document vector failed, documentId={}, vectorId={}, error={}",
                        document.getId(),
                        embedding.getVectorId(),
                        abbreviate(exception.getMessage()));
            }
        }
        deleteByIds(ragEmbeddingMapper, embeddings.stream().map(RagEmbedding::getId).toList());
        deleteByIds(ragChunkMapper, chunkIds);
    }

    private RagDocument findRagDocument(UserDocument userDocument) {
        return ragDocumentMapper.selectOne(new LambdaQueryWrapper<RagDocument>()
                .eq(RagDocument::getSourceType, RagConstants.SOURCE_TYPE_USER_UPLOAD)
                .eq(RagDocument::getSourceId, userDocument.getId())
                .eq(RagDocument::getOwnerType, RagConstants.OWNER_TYPE_USER)
                .eq(RagDocument::getOwnerId, userDocument.getUserId())
                .eq(RagDocument::getUserId, userDocument.getUserId())
                .last("LIMIT 1"));
    }

    private RagChunk saveChunk(RagDocument document, RagChunkCandidate candidate) {
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
        payload.put("ownerType", RagConstants.OWNER_TYPE_USER);
        payload.put("userId", document.getUserId());
        payload.put("sourceType", RagConstants.SOURCE_TYPE_USER_UPLOAD);

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

    private UserDocumentChunkCommand buildChunkCommand(UserDocument document, ParsedDocument parsedDocument) {
        UserDocumentChunkCommand command = new UserDocumentChunkCommand();
        command.setDocumentId(document.getId());
        command.setUserId(document.getUserId());
        command.setProjectId(document.getProjectId());
        command.setTitle(document.getTitle());
        command.setOriginalFilename(document.getOriginalFilename());
        command.setFileType(document.getFileType());
        command.setText(parsedDocument.getText());
        return command;
    }

    private UserDocument buildDocument(
            Long userId,
            Long projectId,
            String title,
            FileInfo fileInfo,
            OssUploadResult uploadResult
    ) {
        LocalDateTime now = LocalDateTime.now();
        UserDocument document = new UserDocument();
        document.setUserId(userId);
        document.setProjectId(projectId);
        document.setTitle(normalizeTitle(title, fileInfo.originalFilename()));
        document.setOriginalFilename(fileInfo.originalFilename());
        document.setFileType(fileInfo.fileType());
        document.setFileSize(fileInfo.fileSize());
        document.setOssKey(uploadResult.objectKey());
        document.setFileUrl(uploadResult.url());
        document.setParseStatus(PARSE_STATUS_PENDING);
        document.setIndexStatus(INDEX_STATUS_PENDING);
        document.setErrorMessage(null);
        document.setIsDeleted(NOT_DELETED);
        document.setCreatedAt(now);
        document.setUpdatedAt(now);
        return document;
    }

    private UserDocument getCurrentUserDocument(Long id) {
        Long userId = UserContext.getCurrentUserId();
        UserDocument document = id == null ? null : userDocumentMapper.selectById(id);
        if (document == null || !Integer.valueOf(NOT_DELETED).equals(document.getIsDeleted())) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "文档不存在");
        }
        if (!userId.equals(document.getUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        return document;
    }

    private void validateProject(Long projectId, Long userId) {
        if (projectId == null) {
            return;
        }
        Project project = projectMapper.selectById(projectId);
        if (project == null || Integer.valueOf(1).equals(project.getIsDeleted())) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "项目不存在");
        }
        if (!userId.equals(project.getUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
    }

    private FileInfo validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "请选择要上传的文档");
        }
        if (file.getSize() <= 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "文档内容不能为空");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "文档不能超过 10MB");
        }

        String originalFilename = StringUtils.cleanPath(
                StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename() : "document"
        );
        String extension = extensionOf(originalFilename);
        if (!StringUtils.hasText(extension) || EXECUTABLE_EXTENSIONS.contains(extension)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "文档类型不支持");
        }
        String fileType = toFileType(extension);
        if (!StringUtils.hasText(fileType)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "仅支持 TXT、Markdown、PDF 文档");
        }
        validateMimeType(fileType, file.getContentType());
        return new FileInfo(originalFilename, extension, fileType, file.getSize());
    }

    private void validateMimeType(String fileType, String mimeType) {
        if (!StringUtils.hasText(mimeType)) {
            return;
        }
        String normalized = mimeType.toLowerCase(Locale.ROOT);
        boolean allowed = switch (fileType) {
            case "TXT" -> TEXT_MIME_TYPES.contains(normalized);
            case "MARKDOWN" -> MARKDOWN_MIME_TYPES.contains(normalized);
            case "PDF" -> PDF_MIME_TYPES.contains(normalized);
            default -> false;
        };
        if (!allowed) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "文档 MIME 类型与后缀不匹配");
        }
    }

    private byte[] readFileBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (Exception exception) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "文件读取失败");
        }
    }

    private String normalizeTitle(String title, String originalFilename) {
        String value = StringUtils.hasText(title) ? title.trim() : stripExtension(originalFilename);
        if (!StringUtils.hasText(value)) {
            return "未命名文档";
        }
        return value.length() <= 255 ? value : value.substring(0, 255);
    }

    private String normalizeFileTypeFilter(String fileType) {
        if (!StringUtils.hasText(fileType)) {
            return null;
        }
        String normalized = fileType.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "TXT", "MARKDOWN", "PDF" -> normalized;
            default -> throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "文档类型不支持");
        };
    }

    private String extensionOf(String filename) {
        int index = filename.lastIndexOf('.');
        if (index < 0 || index == filename.length() - 1) {
            return "";
        }
        return filename.substring(index + 1).toLowerCase(Locale.ROOT);
    }

    private String stripExtension(String filename) {
        if (!StringUtils.hasText(filename)) {
            return "";
        }
        int index = filename.lastIndexOf('.');
        return index > 0 ? filename.substring(0, index) : filename;
    }

    private String toFileType(String extension) {
        return switch (extension) {
            case "txt" -> "TXT";
            case "md", "markdown" -> "MARKDOWN";
            case "pdf" -> "PDF";
            default -> null;
        };
    }

    private void updateStatus(UserDocument document, String parseStatus, String indexStatus, String errorMessage) {
        document.setParseStatus(parseStatus);
        document.setIndexStatus(indexStatus);
        document.setErrorMessage(abbreviate(errorMessage));
        document.setUpdatedAt(LocalDateTime.now());
        userDocumentMapper.updateById(document);
    }

    private UserDocumentVO toVO(UserDocument document) {
        UserDocumentVO vo = new UserDocumentVO();
        vo.setId(document.getId());
        vo.setProjectId(document.getProjectId());
        vo.setTitle(document.getTitle());
        vo.setOriginalFilename(document.getOriginalFilename());
        vo.setFileType(document.getFileType());
        vo.setFileSize(document.getFileSize());
        vo.setParseStatus(document.getParseStatus());
        vo.setIndexStatus(document.getIndexStatus());
        vo.setErrorMessage(document.getErrorMessage());
        vo.setCreatedAt(document.getCreatedAt());
        vo.setUpdatedAt(document.getUpdatedAt());
        return vo;
    }

    private String toJson(Map<String, Object> metadata) {
        if (CollectionUtils.isEmpty(metadata)) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("User document metadata serialization failed", exception);
        }
    }

    private void deleteByIds(com.baomidou.mybatisplus.core.mapper.BaseMapper<?> mapper, Collection<Long> ids) {
        if (!CollectionUtils.isEmpty(ids)) {
            mapper.deleteBatchIds(ids);
        }
    }

    private void cleanupOssQuietly(String objectKey) {
        try {
            aliyunOssService.delete(objectKey);
        } catch (Exception exception) {
            log.warn("User document OSS cleanup failed, objectKey={}, error={}", objectKey, abbreviate(exception.getMessage()));
        }
    }

    private void acquireDocumentLock(String lockKey, String lockValue) {
        try {
            Boolean locked = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, DOCUMENT_LOCK_TTL);
            if (Boolean.TRUE.equals(locked)) {
                return;
            }
        } catch (RuntimeException exception) {
            log.warn("Failed to acquire user document lock, error={}", abbreviate(exception.getMessage()));
        }
        throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "文档正在处理中，请稍后重试");
    }

    private void releaseDocumentLock(String lockKey, String lockValue) {
        try {
            stringRedisTemplate.execute(RELEASE_LOCK_SCRIPT, Collections.singletonList(lockKey), lockValue);
        } catch (RuntimeException exception) {
            log.warn("Failed to release user document lock, error={}", abbreviate(exception.getMessage()));
        }
    }

    private String toErrorMessage(Exception exception) {
        if (exception instanceof BusinessException businessException) {
            return abbreviate(businessException.getMessage());
        }
        String message = exception.getMessage();
        if (!StringUtils.hasText(message)) {
            message = exception.getClass().getSimpleName();
        }
        return abbreviate(message);
    }

    private String abbreviate(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        return normalized.length() <= MAX_ERROR_MESSAGE_LENGTH
                ? normalized
                : normalized.substring(0, MAX_ERROR_MESSAGE_LENGTH);
    }

    private record FileInfo(String originalFilename, String extension, String fileType, Long fileSize) {
    }

    private record IndexOutcome(int embeddedCount, int failedCount, String errorMessage) {
    }
}
