package com.codecoach.module.ai.service.impl;

import com.codecoach.common.exception.BusinessException;
import com.codecoach.module.ai.service.PromptTemplateService;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

@Service
public class PromptTemplateServiceImpl implements PromptTemplateService {

    private static final int AI_CALL_FAILED_CODE = 3003;

    private static final String AI_CALL_FAILED_MESSAGE = "AI 调用失败，请稍后重试";

    private static final String PROMPT_BASE_PATH = "prompts/";

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{\\s*([A-Za-z0-9_]+)\\s*}}");

    private final Map<String, String> templateCache = new ConcurrentHashMap<>();

    @Override
    public String render(String templateName, Map<String, Object> variables) {
        String template = templateCache.computeIfAbsent(templateName, this::loadTemplate);
        Matcher matcher = VARIABLE_PATTERN.matcher(template);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String variableName = matcher.group(1);
            Object value = variables == null ? null : variables.get(variableName);
            matcher.appendReplacement(result, Matcher.quoteReplacement(value == null ? "" : String.valueOf(value)));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private String loadTemplate(String templateName) {
        ClassPathResource resource = new ClassPathResource(PROMPT_BASE_PATH + templateName);
        if (!resource.exists()) {
            throw new BusinessException(AI_CALL_FAILED_CODE, AI_CALL_FAILED_MESSAGE);
        }
        try {
            return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new BusinessException(AI_CALL_FAILED_CODE, AI_CALL_FAILED_MESSAGE);
        }
    }
}
