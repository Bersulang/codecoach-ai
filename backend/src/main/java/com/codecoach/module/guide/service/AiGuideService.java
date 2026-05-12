package com.codecoach.module.guide.service;

import com.codecoach.module.guide.model.GuideAiSuggestion;

public interface AiGuideService {

    GuideAiSuggestion suggest(String prompt);
}
