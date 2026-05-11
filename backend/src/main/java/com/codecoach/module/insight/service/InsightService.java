package com.codecoach.module.insight.service;

import com.codecoach.module.insight.vo.AbilityDimensionVO;
import com.codecoach.module.insight.vo.InsightOverviewVO;
import com.codecoach.module.insight.vo.RecentTrendVO;
import com.codecoach.module.insight.vo.WeaknessInsightVO;

import java.util.List;

public interface InsightService {

    InsightOverviewVO getOverview();

    List<AbilityDimensionVO> getAbilityDimensions();

    List<WeaknessInsightVO> getWeaknesses(Integer limit);

    List<RecentTrendVO> getRecentTrend(Integer limit);
}
