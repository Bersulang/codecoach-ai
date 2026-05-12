package com.codecoach.module.ai.support;

import java.util.List;
import java.util.regex.Pattern;
import org.springframework.util.StringUtils;

public final class AiTextSanitizer {

    private static final Pattern FENCED_CODE_MARKER = Pattern.compile("(?m)^\\s*```[a-zA-Z0-9_-]*\\s*$");

    private static final Pattern HEADING_MARKER = Pattern.compile("(?m)^\\s{0,3}#{1,6}\\s*");

    private static final Pattern BLOCKQUOTE_MARKER = Pattern.compile("(?m)^\\s*>\\s?");

    private static final Pattern UNORDERED_LIST_MARKER = Pattern.compile("(?m)^\\s*[-*+]\\s+");

    private static final Pattern ORDERED_LIST_MARKER = Pattern.compile("(?m)^\\s*\\d+[.)]\\s+");

    private static final Pattern LINK = Pattern.compile("\\[([^\\]]+)]\\(([^)]+)\\)");

    private static final Pattern BOLD = Pattern.compile("(\\*\\*|__)(.*?)\\1");

    private static final Pattern ITALIC = Pattern.compile("(?<!\\*)\\*(?!\\*)([^*]+)(?<!\\*)\\*(?!\\*)");

    private static final Pattern INLINE_CODE = Pattern.compile("`([^`]*)`");

    private AiTextSanitizer() {
    }

    public static String toPlainText(String value) {
        if (value == null) {
            return null;
        }
        String result = value.replace("\r\n", "\n").replace('\r', '\n');
        result = FENCED_CODE_MARKER.matcher(result).replaceAll("");
        result = HEADING_MARKER.matcher(result).replaceAll("");
        result = BLOCKQUOTE_MARKER.matcher(result).replaceAll("");
        result = UNORDERED_LIST_MARKER.matcher(result).replaceAll("");
        result = ORDERED_LIST_MARKER.matcher(result).replaceAll("");
        result = LINK.matcher(result).replaceAll("$1");
        result = BOLD.matcher(result).replaceAll("$2");
        result = ITALIC.matcher(result).replaceAll("$1");
        result = INLINE_CODE.matcher(result).replaceAll("$1");
        result = result.replace("```", "");
        result = result.replace("*", "");
        result = result.replace("`", "");
        result = result.replaceAll("[ \\t]+\\n", "\n");
        result = result.replaceAll("\\n{3,}", "\n\n");
        result = result.trim();
        return StringUtils.hasText(result) ? result : "";
    }

    public static List<String> toPlainTextList(List<String> values) {
        if (values == null) {
            return null;
        }
        return values.stream()
                .map(AiTextSanitizer::toPlainText)
                .filter(StringUtils::hasText)
                .toList();
    }
}
