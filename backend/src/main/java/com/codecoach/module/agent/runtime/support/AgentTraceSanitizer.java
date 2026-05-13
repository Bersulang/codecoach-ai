package com.codecoach.module.agent.runtime.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AgentTraceSanitizer {

    public static final int SUMMARY_LIMIT = 500;
    public static final int ERROR_LIMIT = 512;

    private static final Pattern AUTHORIZATION_PATTERN = Pattern.compile(
            "(?i)(authorization\\s*[:=]\\s*)(bearer\\s+)?[A-Za-z0-9._~+/=-]{12,}");
    private static final Pattern SECRET_PATTERN = Pattern.compile(
            "(?i)((api[-_ ]?key|access[-_ ]?key[-_ ]?secret|oss[-_ ]?secret|token|secret)\\s*[:=]\\s*)[^\\s,;]{6,}");
    private static final Pattern VECTOR_PATTERN = Pattern.compile("\\[(\\s*-?\\d+(\\.\\d+)?\\s*,){8,}.*?]");

    private final ObjectMapper objectMapper;

    public AgentTraceSanitizer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String summarizeText(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        return truncate(redact(text.replaceAll("[\\r\\n]+", " ").trim()), SUMMARY_LIMIT);
    }

    public String summarizeObject(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String text) {
            return summarizeText(text);
        }
        try {
            return summarizeText(objectMapper.writeValueAsString(value));
        } catch (Exception exception) {
            return "{}";
        }
    }

    public String errorMessage(Throwable throwable) {
        if (throwable == null || !StringUtils.hasText(throwable.getMessage())) {
            return null;
        }
        return truncate(redact(throwable.getMessage()), ERROR_LIMIT);
    }

    public String errorMessage(String message) {
        if (!StringUtils.hasText(message)) {
            return null;
        }
        return truncate(redact(message), ERROR_LIMIT);
    }

    public Map<String, Object> safeMap(Map<String, Object> source) {
        Map<String, Object> safe = new LinkedHashMap<>();
        if (source == null || source.isEmpty()) {
            return safe;
        }
        source.forEach((key, value) -> {
            String normalizedKey = key == null ? "" : key.toLowerCase(Locale.ROOT);
            if (isSensitiveKey(normalizedKey)) {
                safe.put(key, "[REDACTED]");
            } else {
                safe.put(key, compactValue(value));
            }
        });
        return safe;
    }

    private boolean isSensitiveKey(String normalizedKey) {
        return normalizedKey.contains("authorization")
                || normalizedKey.contains("api")
                || normalizedKey.contains("key")
                || normalizedKey.contains("token")
                || normalizedKey.contains("secret")
                || normalizedKey.contains("content")
                || normalizedKey.contains("document")
                || normalizedKey.contains("resume")
                || normalizedKey.contains("answer")
                || normalizedKey.contains("prompt")
                || normalizedKey.contains("embedding")
                || normalizedKey.contains("vector");
    }

    private Object compactValue(Object value) {
        if (!(value instanceof String text)) {
            return value;
        }
        return summarizeText(text);
    }

    private String redact(String text) {
        String redacted = AUTHORIZATION_PATTERN.matcher(text).replaceAll("$1[REDACTED]");
        redacted = SECRET_PATTERN.matcher(redacted).replaceAll("$1[REDACTED]");
        redacted = VECTOR_PATTERN.matcher(redacted).replaceAll("[VECTOR_REDACTED]");
        return redacted;
    }

    private String truncate(String text, int limit) {
        if (text == null || text.length() <= limit) {
            return text;
        }
        return text.substring(0, limit) + "...";
    }
}
