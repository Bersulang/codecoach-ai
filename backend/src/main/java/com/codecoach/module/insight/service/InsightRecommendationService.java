package com.codecoach.module.insight.service;

import com.codecoach.module.insight.vo.LearningRecommendationVO;
import java.util.List;

public interface InsightRecommendationService {

    List<LearningRecommendationVO> getLearningRecommendations();
}
