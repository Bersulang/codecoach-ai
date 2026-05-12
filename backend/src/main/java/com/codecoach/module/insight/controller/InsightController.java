package com.codecoach.module.insight.controller;

import com.codecoach.common.result.Result;
import com.codecoach.module.insight.service.InsightRecommendationService;
import com.codecoach.module.insight.service.InsightService;
import com.codecoach.module.insight.vo.AbilityDimensionVO;
import com.codecoach.module.insight.vo.InsightOverviewVO;
import com.codecoach.module.insight.vo.LearningRecommendationVO;
import com.codecoach.module.insight.vo.RecentTrendVO;
import com.codecoach.module.insight.vo.WeaknessInsightVO;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/insights")
public class InsightController {

    private final InsightService insightService;

    private final InsightRecommendationService insightRecommendationService;

    public InsightController(
            InsightService insightService,
            InsightRecommendationService insightRecommendationService
    ) {
        this.insightService = insightService;
        this.insightRecommendationService = insightRecommendationService;
    }

    @GetMapping("/overview")
    public Result<InsightOverviewVO> getOverview() {
        return Result.success(insightService.getOverview());
    }

    @GetMapping("/ability-dimensions")
    public Result<List<AbilityDimensionVO>> getAbilityDimensions() {
        return Result.success(insightService.getAbilityDimensions());
    }

    @GetMapping("/weaknesses")
    public Result<List<WeaknessInsightVO>> getWeaknesses(@RequestParam(required = false) Integer limit) {
        return Result.success(insightService.getWeaknesses(limit));
    }

    @GetMapping("/recent-trend")
    public Result<List<RecentTrendVO>> getRecentTrend(@RequestParam(required = false) Integer limit) {
        return Result.success(insightService.getRecentTrend(limit));
    }

    @GetMapping("/learning-recommendations")
    public Result<List<LearningRecommendationVO>> getLearningRecommendations() {
        return Result.success(insightRecommendationService.getLearningRecommendations());
    }
}
