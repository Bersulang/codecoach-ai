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
  | "VIEW_LEARNING_ARTICLE"
  | "UPLOAD_DOCUMENT"
  | "ANALYZE_RESUME"
  | "GENERATE_REVIEW"
  | "LOGIN";

export interface GuideChatRequest {
  message: string;
  currentPath: string;
  pageTitle?: string;
}

export interface GuideActionCard {
  actionType: GuideActionType;
  title: string;
  description: string;
  targetPath: string;
}

export interface GuideChatResponse {
  answer: string;
  personalized: boolean;
  actions: GuideActionCard[];
}
