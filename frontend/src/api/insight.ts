import request from "./request";
import type {
  AbilityDimension,
  InsightOverview,
  RecentTrend,
  WeaknessInsight,
} from "../types/insight";

export const getInsightOverview = () =>
  request.get<InsightOverview>("/api/insights/overview", {
    silentError: true,
  });

export const getAbilityDimensions = () =>
  request.get<AbilityDimension[]>("/api/insights/ability-dimensions", {
    silentError: true,
  });

export const getWeaknessInsights = (limit = 10) =>
  request.get<WeaknessInsight[]>("/api/insights/weaknesses", {
    params: { limit },
    silentError: true,
  });

export const getRecentTrend = (limit = 10) =>
  request.get<RecentTrend[]>("/api/insights/recent-trend", {
    params: { limit },
    silentError: true,
  });
