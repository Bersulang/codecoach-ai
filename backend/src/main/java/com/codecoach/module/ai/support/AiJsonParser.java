package com.codecoach.module.ai.support;

import com.codecoach.common.exception.BusinessException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AiJsonParser {

    private static final Logger log = LoggerFactory.getLogger(AiJsonParser.class);

    private static final int AI_CALL_FAILED_CODE = 3003;

    private static final String AI_CALL_FAILED_MESSAGE = "AI 调用失败，请稍后重试";

    private static final int RAW_CONTENT_LOG_LIMIT = 500;

    private final ObjectMapper objectMapper;

    public AiJsonParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public <T> T parseObject(String rawContent, Class<T> clazz) {
        String jsonContent = extractJsonObject(rawContent);
        try {
            return objectMapper.readValue(jsonContent, clazz);
        } catch (JsonProcessingException exception) {
            log.warn("AI JSON parse failed, rawContent={}", abbreviate(rawContent), exception);
            throw aiCallFailed();
        }
    }

    private String extractJsonObject(String rawContent) {
        if (!StringUtils.hasText(rawContent)) {
            log.warn("AI JSON content is empty");
            throw aiCallFailed();
        }

        String content = stripMarkdownFence(rawContent.trim());
        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');
        if (start < 0 || end < 0 || start >= end) {
            log.warn("AI JSON object not found, rawContent={}", abbreviate(rawContent));
            throw aiCallFailed();
        }

        String jsonContent = content.substring(start, end + 1).trim();
        if (!StringUtils.hasText(jsonContent)) {
            log.warn("AI JSON object is empty, rawContent={}", abbreviate(rawContent));
            throw aiCallFailed();
        }
        return jsonContent;
    }

    private String stripMarkdownFence(String content) {
        String result = content;
        if (result.startsWith("```")) {
            int firstLineEnd = result.indexOf('\n');
            if (firstLineEnd >= 0) {
                result = result.substring(firstLineEnd + 1).trim();
            }
        }
        if (result.endsWith("```")) {
            result = result.substring(0, result.length() - 3).trim();
        }
        return result;
    }

    private String abbreviate(String content) {
        if (content == null) {
            return "";
        }
        String normalized = content.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= RAW_CONTENT_LOG_LIMIT) {
            return normalized;
        }
        return normalized.substring(0, RAW_CONTENT_LOG_LIMIT);
    }

    private BusinessException aiCallFailed() {
        return new BusinessException(AI_CALL_FAILED_CODE, AI_CALL_FAILED_MESSAGE);
    }
}
