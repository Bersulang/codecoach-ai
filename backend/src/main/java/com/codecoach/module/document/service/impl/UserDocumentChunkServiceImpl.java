package com.codecoach.module.document.service.impl;

import com.codecoach.module.document.model.UserDocumentChunkCommand;
import com.codecoach.module.document.service.UserDocumentChunkService;
import com.codecoach.module.rag.constant.RagConstants;
import com.codecoach.module.rag.model.RagChunkCandidate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class UserDocumentChunkServiceImpl implements UserDocumentChunkService {

    private static final int MAX_CHUNK_CHARS = 1200;
    private static final int MIN_CHUNK_CHARS = 120;
    private static final int OVERLAP_CHARS = 100;

    @Override
    public List<RagChunkCandidate> chunk(UserDocumentChunkCommand command) {
        if (command == null || !StringUtils.hasText(command.getText())) {
            return List.of();
        }
        List<String> sections = "MARKDOWN".equals(command.getFileType())
                ? splitByMarkdownHeading(command.getText())
                : List.of();
        if (sections.isEmpty()) {
            sections = splitParagraphs(command.getText());
        }
        List<RagChunkCandidate> chunks = new ArrayList<>();
        for (String section : sections) {
            for (String part : splitLongText(section)) {
                String normalized = part.trim();
                if (!StringUtils.hasText(normalized)) {
                    continue;
                }
                if (normalized.length() < MIN_CHUNK_CHARS && command.getText().length() >= MIN_CHUNK_CHARS) {
                    continue;
                }
                chunks.add(candidate(command, chunks.size(), normalized, sectionTitle(normalized, command.getFileType())));
            }
        }
        if (chunks.isEmpty() && StringUtils.hasText(command.getText())) {
            chunks.add(candidate(command, 0, truncate(command.getText().trim(), MAX_CHUNK_CHARS), "全文"));
        }
        return chunks;
    }

    private RagChunkCandidate candidate(UserDocumentChunkCommand command, int index, String content, String section) {
        RagChunkCandidate candidate = new RagChunkCandidate();
        candidate.setChunkIndex(index);
        candidate.setContent(content);
        candidate.setTokenCount(Math.max(1, content.length() / 2));
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("sourceType", RagConstants.SOURCE_TYPE_USER_UPLOAD);
        metadata.put("userDocumentId", command.getDocumentId());
        metadata.put("projectId", command.getProjectId());
        metadata.put("fileType", command.getFileType());
        metadata.put("originalFilename", command.getOriginalFilename());
        metadata.put("title", command.getTitle());
        metadata.put("section", section);
        metadata.put("ownerType", RagConstants.OWNER_TYPE_USER);
        metadata.put("userId", command.getUserId());
        candidate.setMetadata(metadata);
        return candidate;
    }

    private List<String> splitByMarkdownHeading(String text) {
        if (!text.startsWith("#") && !text.contains("\n#")) {
            return List.of();
        }
        List<String> sections = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String line : text.split("\n")) {
            if (line.matches("^#{1,6}\\s+.+") && !current.isEmpty()) {
                sections.add(current.toString());
                current.setLength(0);
            }
            current.append(line).append('\n');
        }
        if (!current.isEmpty()) {
            sections.add(current.toString());
        }
        return sections;
    }

    private List<String> splitParagraphs(String text) {
        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String paragraph : text.split("\\n\\s*\\n")) {
            String normalized = paragraph.trim();
            if (!StringUtils.hasText(normalized)) {
                continue;
            }
            if (current.length() + normalized.length() + 2 > MAX_CHUNK_CHARS && !current.isEmpty()) {
                chunks.add(current.toString());
                current.setLength(0);
            }
            if (!current.isEmpty()) {
                current.append("\n\n");
            }
            current.append(normalized);
        }
        if (!current.isEmpty()) {
            chunks.add(current.toString());
        }
        return chunks;
    }

    private List<String> splitLongText(String text) {
        if (text.length() <= MAX_CHUNK_CHARS) {
            return List.of(text);
        }
        List<String> parts = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + MAX_CHUNK_CHARS, text.length());
            parts.add(text.substring(start, end));
            if (end >= text.length()) {
                break;
            }
            start = Math.max(end - OVERLAP_CHARS, start + 1);
        }
        return parts;
    }

    private String sectionTitle(String content, String fileType) {
        for (String line : content.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.matches("^#{1,6}\\s+.+")) {
                return trimmed.replaceFirst("^#{1,6}\\s+", "");
            }
        }
        return "PDF".equals(fileType) ? "PDF 文档" : "全文";
    }

    private String truncate(String text, int maxLength) {
        return text.length() <= maxLength ? text : text.substring(0, maxLength);
    }
}
