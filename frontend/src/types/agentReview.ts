export type AgentReviewConfidence = "LOW" | "MEDIUM" | "HIGH";

export type AgentReviewActionType =
  | "LEARN"
  | "TRAIN_QUESTION"
  | "TRAIN_PROJECT"
  | "REVIEW_RESUME"
  | "UPLOAD_DOCUMENT";

export interface AgentReviewNextAction {
  type: AgentReviewActionType | string;
  title: string;
  reason: string;
  priority?: number | null;
  targetPath: string;
}

export interface AgentReview {
  id: number;
  scopeType: string;
  summary: string;
  keyFindings: string[];
  recurringWeaknesses: string[];
  causeAnalysis: string[];
  resumeRisks: string[];
  nextActions: AgentReviewNextAction[];
  confidence: AgentReviewConfidence;
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
