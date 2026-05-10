package com.codecoach.module.ai.service;

import java.util.Map;

public interface PromptTemplateService {

    String render(String templateName, Map<String, Object> variables);
}
