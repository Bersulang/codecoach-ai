package com.codecoach.module.rag.service.impl;

import com.codecoach.common.exception.BusinessException;
import com.codecoach.common.result.ResultCode;
import com.codecoach.module.rag.model.KnowledgeArticleChunkCommand;
import com.codecoach.module.rag.model.RagChunkCandidate;
import com.codecoach.module.rag.service.MarkdownChunkService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class MarkdownChunkServiceImpl implements MarkdownChunkService {

    private static final int MAX_CHUNK_CHARS = 1200;

    private static final int MIN_CHUNK_CHARS = 120;

    private static final int OVERLAP_CHARS = 100;

    private static final String SOURCE_TYPE_KNOWLEDGE_ARTICLE = "KNOWLEDGE_ARTICLE";

    private static final String DEFAULT_SECTION = "全文";

    @Override
    public List<RagChunkCandidate> chunkKnowledgeArticle(KnowledgeArticleChunkCommand command) {
        validateCommand(command);

        String markdown = normalizeLineEndings(command.getMarkdown()).trim();
        String articleTitle = resolveArticleTitle(command.getTitle(), markdown);
        List<MarkdownSection> sections = parseSections(markdown);
        List<RagChunkCandidate> chunks = new ArrayList<>();
        Set<String> seenContents = new LinkedHashSet<>();

        for (MarkdownSection section : sections) {
            List<String> sectionChunks = splitSection(articleTitle, section);
            int partNo = 1;
            for (String content : sectionChunks) {
                String normalizedContent = normalizeContent(content);
                if (!StringUtils.hasText(normalizedContent)) {
                    continue;
                }
                String sectionName = sectionChunks.size() > 1
                        ? section.title() + " - part " + partNo
                        : section.title();
                partNo++;
                if (shouldSkipChunk(normalizedContent, chunks.isEmpty() && sections.size() == 1)) {
                    continue;
                }
                if (!seenContents.add(normalizedContent)) {
                    continue;
                }
                chunks.add(toCandidate(command, chunks.size(), normalizedContent, sectionName));
            }
        }

        if (chunks.isEmpty() && StringUtils.hasText(markdown)) {
            String fallbackContent = buildChunkContent(articleTitle, DEFAULT_SECTION, stripPrimaryTitle(markdown));
            chunks.add(toCandidate(command, 0, normalizeContent(fallbackContent), DEFAULT_SECTION));
        }

        return chunks;
    }

    private void validateCommand(KnowledgeArticleChunkCommand command) {
        if (command == null
                || command.getArticleId() == null
                || !StringUtils.hasText(command.getTitle())
                || !StringUtils.hasText(command.getMarkdown())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "知识文章切片参数错误");
        }
    }

    private List<MarkdownSection> parseSections(String markdown) {
        List<MarkdownSection> sections = new ArrayList<>();
        String[] lines = markdown.split("\\n");
        String currentTitle = DEFAULT_SECTION;
        StringBuilder currentContent = new StringBuilder();

        for (String line : lines) {
            if (isPrimaryTitle(line)) {
                continue;
            }
            if (isSecondaryTitle(line)) {
                addSection(sections, currentTitle, currentContent);
                currentTitle = stripHeadingMarker(line);
                currentContent = new StringBuilder();
                currentContent.append(line).append('\n');
                continue;
            }
            currentContent.append(line).append('\n');
        }
        addSection(sections, currentTitle, currentContent);

        if (sections.isEmpty()) {
            sections.add(new MarkdownSection(DEFAULT_SECTION, stripPrimaryTitle(markdown)));
        }
        return sections;
    }

    private void addSection(List<MarkdownSection> sections, String title, StringBuilder content) {
        String value = content.toString().trim();
        if (!StringUtils.hasText(value)) {
            return;
        }
        if (DEFAULT_SECTION.equals(title) && value.lines().allMatch(this::isHeadingLine)) {
            return;
        }
        sections.add(new MarkdownSection(title, value));
    }

    private List<String> splitSection(String articleTitle, MarkdownSection section) {
        String content = buildChunkContent(articleTitle, section.title(), stripSectionTitle(section.content()));
        if (content.length() <= MAX_CHUNK_CHARS) {
            return List.of(content);
        }

        List<String> chunks = new ArrayList<>();
        List<String> paragraphs = splitParagraphs(stripSectionTitle(section.content()));
        StringBuilder current = new StringBuilder();

        for (String paragraph : paragraphs) {
            if (!StringUtils.hasText(paragraph)) {
                continue;
            }
            if (paragraph.length() > maxBodyChars(articleTitle, section.title())) {
                flushParagraphChunk(chunks, articleTitle, section.title(), current);
                chunks.addAll(splitLongParagraph(articleTitle, section.title(), paragraph));
                continue;
            }
            if (current.length() + paragraph.length() + 2 > maxBodyChars(articleTitle, section.title())) {
                flushParagraphChunk(chunks, articleTitle, section.title(), current);
            }
            if (current.length() > 0) {
                current.append("\n\n");
            }
            current.append(paragraph);
        }
        flushParagraphChunk(chunks, articleTitle, section.title(), current);
        return chunks;
    }

    private void flushParagraphChunk(
            List<String> chunks,
            String articleTitle,
            String sectionTitle,
            StringBuilder current
    ) {
        if (current.length() == 0) {
            return;
        }
        chunks.add(buildChunkContent(articleTitle, sectionTitle, current.toString()));
        current.setLength(0);
    }

    private List<String> splitLongParagraph(String articleTitle, String sectionTitle, String paragraph) {
        List<String> chunks = new ArrayList<>();
        int bodyLimit = maxBodyChars(articleTitle, sectionTitle);
        int start = 0;
        while (start < paragraph.length()) {
            int end = Math.min(start + bodyLimit, paragraph.length());
            String body = paragraph.substring(start, end);
            chunks.add(buildChunkContent(articleTitle, sectionTitle, body));
            if (end >= paragraph.length()) {
                break;
            }
            start = Math.max(end - OVERLAP_CHARS, start + 1);
        }
        return chunks;
    }

    private int maxBodyChars(String articleTitle, String sectionTitle) {
        int headingLength = buildHeadingPrefix(articleTitle, sectionTitle).length();
        return Math.max(300, MAX_CHUNK_CHARS - headingLength);
    }

    private String buildChunkContent(String articleTitle, String sectionTitle, String body) {
        StringBuilder builder = new StringBuilder(buildHeadingPrefix(articleTitle, sectionTitle));
        if (StringUtils.hasText(body)) {
            builder.append(body.trim());
        }
        return builder.toString();
    }

    private String buildHeadingPrefix(String articleTitle, String sectionTitle) {
        StringBuilder builder = new StringBuilder();
        builder.append("# ").append(articleTitle).append("\n\n");
        if (StringUtils.hasText(sectionTitle) && !DEFAULT_SECTION.equals(sectionTitle)) {
            builder.append("## ").append(sectionTitle).append("\n\n");
        }
        return builder.toString();
    }

    private RagChunkCandidate toCandidate(
            KnowledgeArticleChunkCommand command,
            Integer chunkIndex,
            String content,
            String section
    ) {
        RagChunkCandidate candidate = new RagChunkCandidate();
        candidate.setChunkIndex(chunkIndex);
        candidate.setContent(content);
        candidate.setTokenCount(estimateTokenCount(content));
        candidate.setMetadata(buildMetadata(command, chunkIndex, section));
        return candidate;
    }

    private Map<String, Object> buildMetadata(
            KnowledgeArticleChunkCommand command,
            Integer chunkIndex,
            String section
    ) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("sourceType", SOURCE_TYPE_KNOWLEDGE_ARTICLE);
        metadata.put("articleId", command.getArticleId());
        metadata.put("topicId", command.getTopicId());
        metadata.put("category", command.getCategory());
        metadata.put("topicName", command.getTopicName());
        metadata.put("title", command.getTitle());
        metadata.put("section", section);
        metadata.put("chunkIndex", chunkIndex);
        return metadata;
    }

    private Integer estimateTokenCount(String content) {
        if (!StringUtils.hasText(content)) {
            return 0;
        }
        return Math.max(1, content.length() / 2);
    }

    private boolean shouldSkipChunk(String content, boolean onlySection) {
        if (!StringUtils.hasText(content)) {
            return true;
        }
        if (onlySection) {
            return false;
        }
        if (content.trim().length() < MIN_CHUNK_CHARS) {
            return true;
        }
        return content.lines().filter(line -> !isHeadingLine(line)).noneMatch(StringUtils::hasText);
    }

    private List<String> splitParagraphs(String value) {
        String[] parts = value.split("\\n\\s*\\n");
        List<String> paragraphs = new ArrayList<>();
        for (String part : parts) {
            String paragraph = part.trim();
            if (StringUtils.hasText(paragraph)) {
                paragraphs.add(paragraph);
            }
        }
        return paragraphs;
    }

    private String resolveArticleTitle(String commandTitle, String markdown) {
        for (String line : markdown.split("\\n")) {
            if (isPrimaryTitle(line)) {
                return StringUtils.hasText(commandTitle) ? commandTitle.trim() : stripHeadingMarker(line);
            }
        }
        return commandTitle.trim();
    }

    private String stripPrimaryTitle(String markdown) {
        StringBuilder builder = new StringBuilder();
        for (String line : markdown.split("\\n")) {
            if (isPrimaryTitle(line)) {
                continue;
            }
            builder.append(line).append('\n');
        }
        return builder.toString().trim();
    }

    private String stripSectionTitle(String content) {
        StringBuilder builder = new StringBuilder();
        for (String line : content.split("\\n")) {
            if (isSecondaryTitle(line)) {
                continue;
            }
            builder.append(line).append('\n');
        }
        return builder.toString().trim();
    }

    private boolean isPrimaryTitle(String line) {
        return line != null && line.matches("^#\\s+.+");
    }

    private boolean isSecondaryTitle(String line) {
        return line != null && line.matches("^##\\s+[^#].*");
    }

    private boolean isHeadingLine(String line) {
        return line != null && line.matches("^#{1,6}\\s+.+");
    }

    private String stripHeadingMarker(String line) {
        return line == null ? "" : line.replaceFirst("^#{1,6}\\s+", "").trim();
    }

    private String normalizeLineEndings(String value) {
        return value == null ? "" : value.replace("\r\n", "\n").replace('\r', '\n');
    }

    private String normalizeContent(String value) {
        return value == null ? "" : value.replaceAll("\\n{3,}", "\n\n").trim();
    }

    private record MarkdownSection(String title, String content) {
    }
}
