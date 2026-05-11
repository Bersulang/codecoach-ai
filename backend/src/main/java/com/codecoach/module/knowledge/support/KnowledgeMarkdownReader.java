package com.codecoach.module.knowledge.support;

import com.codecoach.common.exception.BusinessException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

@Component
public class KnowledgeMarkdownReader {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeMarkdownReader.class);

    private static final int ARTICLE_CONTENT_NOT_FOUND_CODE = 5102;

    private static final String ARTICLE_CONTENT_NOT_FOUND_MESSAGE = "知识文章内容不存在";

    private static final String KNOWLEDGE_PREFIX = "knowledge/";

    public String readMarkdown(String contentPath) {
        if (!isValidKnowledgePath(contentPath)) {
            throw new BusinessException(ARTICLE_CONTENT_NOT_FOUND_CODE, ARTICLE_CONTENT_NOT_FOUND_MESSAGE);
        }

        ClassPathResource resource = new ClassPathResource(contentPath);
        if (!resource.exists() || !resource.isReadable()) {
            throw new BusinessException(ARTICLE_CONTENT_NOT_FOUND_CODE, ARTICLE_CONTENT_NOT_FOUND_MESSAGE);
        }

        try {
            return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            log.warn("Failed to read knowledge markdown: {}", contentPath, exception);
            throw new BusinessException(ARTICLE_CONTENT_NOT_FOUND_CODE, ARTICLE_CONTENT_NOT_FOUND_MESSAGE);
        }
    }

    private boolean isValidKnowledgePath(String contentPath) {
        if (!StringUtils.hasText(contentPath)) {
            return false;
        }
        String normalized = contentPath.trim();
        return normalized.startsWith(KNOWLEDGE_PREFIX)
                && !normalized.startsWith("/")
                && !normalized.contains("..")
                && normalized.endsWith(".md");
    }
}
