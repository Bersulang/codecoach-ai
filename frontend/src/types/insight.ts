export type AbilityTrend = "UP" | "DOWN" | "FLAT" | "UNKNOWN";

export interface InsightOverview {
  totalTrainingCount: number;
  projectTrainingCount: number;
  questionTrainingCount: number;
  averageScore: number | null;
  recentAverageScore: number | null;
  bestDimension: string | null;
  weakestDimension: string | null;
  lastTrainingAt: string | null;
}

export interface AbilityDimension {
  dimensionCode: string;
  dimensionName: string;
  category: string | null;
  score: number | null;
  trend: AbilityTrend;
  evidenceCount: number;
  latestEvidence: string | null;
}

export interface WeaknessInsight {
  keyword: string;
  count: number;
  relatedDimension: string | null;
  latestEvidence: string | null;
  latestSourceType: string | null;
  latestSourceId: number | null;
  latestAt: string | null;
}

export interface RecentTrend {
  date: string;
  score: number | null;
  trainingType: string;
  dimensionName: string | null;
  sourceId: number;
  createdAt: string;
}
