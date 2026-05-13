export type GuideActionType =
  | "GO_DASHBOARD"
  | "GO_PROJECTS"
  | "GO_QUESTIONS"
  | "GO_LEARN"
  | "GO_INSIGHTS"
  | "GO_DOCUMENTS"
  | "GO_RESUMES"
  | "GO_AGENT_REVIEW"
  | "GO_HISTORY"
  | "GO_PROFILE"
  | "GO_MOCK_INTERVIEWS"
  | "START_PROJECT_TRAINING"
  | "START_QUESTION_TRAINING"
  | "START_MOCK_INTERVIEW"
  | "GET_ABILITY_SUMMARY"
  | "GET_RECENT_TRAINING_SUMMARY"
  | "GET_RESUME_RISK_SUMMARY"
  | "SEARCH_KNOWLEDGE"
  | "SEARCH_USER_DOCUMENTS"
  | "ANALYZE_RESUME"
  | "GENERATE_AGENT_REVIEW"
  | "CREATE_PROJECT_FROM_RESUME"
  | "LOGIN";

export type ToolType = "NAVIGATION" | "QUERY" | "COMMAND";
export type ToolRiskLevel = "LOW" | "MEDIUM" | "HIGH";
export type ToolExecutionMode =
  | "SUGGEST_ONLY"
  | "EXECUTE_AFTER_CONFIRM"
  | "AUTO_EXECUTE";
export type ToolDisplayType =
  | "NAVIGATION"
  | "SUMMARY"
  | "SESSION_CREATED"
  | "REVIEW_CREATED"
  | "ERROR";

export interface GuideChatRequest {
  message: string;
  currentPath: string;
  pageTitle?: string;
}

export interface GuideActionCard {
  actionType: GuideActionType;
  toolName?: GuideActionType;
  toolType?: ToolType;
  riskLevel?: ToolRiskLevel;
  executionMode?: ToolExecutionMode;
  displayType?: ToolDisplayType;
  title: string;
  description: string;
  targetPath: string;
  requiresConfirmation?: boolean;
  params?: Record<string, unknown>;
}

export interface GuideChatResponse {
  answer: string;
  personalized: boolean;
  actions: GuideActionCard[];
}

export interface ToolExecuteRequest {
  toolName: string;
  agentType?: string;
  confirmed?: boolean;
  params?: Record<string, unknown>;
}

export interface ToolExecuteResult {
  success: boolean;
  message?: string;
  data?: Record<string, unknown>;
  targetPath?: string;
  errorCode?: string;
  displayType?: ToolDisplayType;
  nextActions?: GuideActionCard[];
  traceId?: string;
}
