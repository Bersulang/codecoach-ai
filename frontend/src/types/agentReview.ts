export type AgentReviewConfidence = "LOW" | "MEDIUM" | "HIGH";

export type AgentReviewActionType =
  | "LEARN"
  | "TRAIN_QUESTION"
  | "TRAIN_PROJECT"
  | "MOCK_INTERVIEW"
  | "REVIEW_RESUME"
  | "UPLOAD_DOCUMENT"
  | "VIEW_MEMORY"
  | "VIEW_REPORT_REPLAY";

export interface AgentReviewNextAction {
  type: AgentReviewActionType | string;
  title: string;
  reason: string;
  priority?: number | null;
  targetPath: string;
  toolName?: string | null;
  params?: Record<string, unknown> | null;
}

export interface ReviewRadarDimension {
  name: string;
  score?: number | null;
  evidence?: string | null;
}

export interface ReviewHighRiskAnswer {
  question?: string;
  answerSummary?: string;
  riskType?: string;
  riskLevel?: string;
  reason?: string;
  betterDirection?: string;
  relatedAction?: AgentReviewNextAction;
}

export interface ReviewStagePerformance {
  stage?: string;
  stageName?: string;
  score?: number | null;
  comment?: string;
  weaknessTags?: string[];
}

export interface ReviewQaReplay {
  sourceType?: string;
  sourceId?: number | null;
  question?: string;
  answerSummary?: string;
  aiFollowUp?: string;
  quality?: string;
  mainProblems?: string[];
  suggestedExpression?: string;
}

export interface ReviewRecommendation {
  id?: number | null;
  title?: string;
  reason?: string;
  targetPath?: string;
  sourceType?: string;
  metadata?: Record<string, unknown>;
}

export interface AgentReview {
  id: number;
  scopeType: string;
  summary: string;
  scoreOverview?: Record<string, unknown>;
  radarDimensions?: ReviewRadarDimension[];
  keyFindings: string[];
  recurringWeaknesses: string[];
  highRiskAnswers?: ReviewHighRiskAnswer[];
  stagePerformance?: ReviewStagePerformance[];
  qaReplay?: ReviewQaReplay[];
  causeAnalysis: string[];
  resumeRisks: string[];
  nextActions: AgentReviewNextAction[];
  recommendedArticles?: ReviewRecommendation[];
  recommendedTrainings?: ReviewRecommendation[];
  memoryUpdates?: string[];
  confidence: AgentReviewConfidence;
  sampleQuality?: "INSUFFICIENT" | "LIMITED" | "ENOUGH" | string;
  sourceSnapshot?: Record<string, unknown>;
  createdAt?: string;
}

export interface AgentReviewListItem {
  id: number;
  scopeType: string;
  summary: string;
  confidence: AgentReviewConfidence;
  createdAt?: string;
}
