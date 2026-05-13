export type MockInterviewType =
  | "COMPREHENSIVE_TECHNICAL"
  | "RESUME_PROJECT_DEEP_DIVE"
  | "BA_GU_COMPREHENSIVE";

export type MockInterviewDifficulty = "EASY" | "NORMAL" | "HARD";

export type MockInterviewStage =
  | "OPENING"
  | "RESUME_PROJECT"
  | "TECHNICAL_FUNDAMENTAL"
  | "PROJECT_DEEP_DIVE"
  | "SCENARIO_DESIGN"
  | "WRAP_UP";

export interface MockInterviewCreateRequest {
  interviewType: MockInterviewType;
  targetRole: string;
  difficulty: MockInterviewDifficulty;
  maxRound: number;
  projectId?: number;
  resumeId?: number;
}

export interface MockInterviewMessage {
  messageId?: number | string;
  role?: "USER" | "ASSISTANT";
  messageType: "AI_QUESTION" | "USER_ANSWER";
  stage: MockInterviewStage;
  content: string;
  roundNo: number;
  score?: number;
  createdAt?: string;
}

export interface MockInterviewCreateResponse {
  sessionId: number;
  firstMessage: MockInterviewMessage;
  plan?: MockInterviewPlan;
}

export interface MockInterviewAnswerResponse {
  userAnswer: MockInterviewMessage;
  nextQuestion?: MockInterviewMessage | null;
  finished: boolean;
  reportId?: number | null;
  totalScore?: number | null;
  currentStage?: MockInterviewStage;
  currentStageGoal?: string;
  currentStageProgress?: number;
  currentStageSuggestedRounds?: number;
}

export interface MockInterviewFinishResponse {
  reportId: number;
  sessionId: number;
  totalScore: number;
}

export interface MockInterviewSessionDetail {
  sessionId: number;
  interviewType: MockInterviewType;
  targetRole: string;
  difficulty: MockInterviewDifficulty;
  projectId?: number;
  projectName?: string;
  resumeId?: number;
  resumeTitle?: string;
  status: "IN_PROGRESS" | "FINISHED" | "FAILED";
  currentRound: number;
  maxRound: number;
  currentStage: MockInterviewStage;
  currentStageGoal?: string;
  currentStageProgress?: number;
  currentStageSuggestedRounds?: number;
  plan?: MockInterviewPlan;
  reportId?: number;
  messages: MockInterviewMessage[];
}

export interface MockInterviewHistoryItem {
  sessionId: number;
  interviewType: MockInterviewType;
  targetRole: string;
  difficulty: MockInterviewDifficulty;
  status: string;
  currentRound: number;
  maxRound: number;
  currentStage: MockInterviewStage;
  totalScore?: number;
  reportId?: number;
  createdAt?: string;
}

export interface MockInterviewStagePerformance {
  stage: MockInterviewStage;
  stageName: string;
  score: number;
  comment: string;
  suggestedRounds?: number;
  completedRounds?: number;
  completionStatus?: "NOT_STARTED" | "EARLY_ENDED" | "COMPLETED" | "FOLLOWED_UP";
  followUpCount?: number;
  deductionReasons?: string[];
}

export interface MockInterviewPlanStage {
  stage: MockInterviewStage;
  stageName: string;
  objective: string;
  suggestedRounds: number;
  focusPoints: string[];
  ragSources: string[];
  scoringDimensions: string[];
}

export interface MockInterviewPlan {
  planId: string;
  totalRounds: number;
  coverageSummary: string;
  stages: MockInterviewPlanStage[];
}

export interface MockInterviewReport {
  reportId: number;
  sessionId: number;
  interviewType: MockInterviewType;
  targetRole: string;
  difficulty: MockInterviewDifficulty;
  totalScore: number;
  sampleSufficiency: string;
  summary: string;
  planSummary?: MockInterviewPlan;
  stagePerformances: MockInterviewStagePerformance[];
  strengths: string[];
  weaknesses: string[];
  highRiskAnswers: string[];
  followUpPressureScore: number;
  projectCredibilityScore: number;
  technicalFoundationScore: number;
  nextActions: string[];
  recommendedLearning: string[];
  recommendedTraining: string[];
  weaknessTags: string[];
  createdAt?: string;
}
