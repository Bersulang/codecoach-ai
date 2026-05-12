export interface QaReview {
  question: string;
  answer: string;
  feedback: string;
}

import type { InterviewDifficulty } from "./interview";

export interface InterviewReport {
  id: number;
  sessionId: number;
  projectId: number;
  projectName: string;
  targetRole: string;
  difficulty: InterviewDifficulty;
  totalScore: number;
  sampleSufficiency?: "INSUFFICIENT" | "LIMITED" | "ENOUGH";
  answerQuality?:
    | "NO_ANSWER"
    | "INVALID"
    | "VERY_WEAK"
    | "PARTIAL"
    | "BASIC"
    | "GOOD"
    | "EXCELLENT";
  answerCount?: number;
  validAnswerCount?: number;
  deductionReasons?: string[];
  nextActions?: string[];
  summary: string;
  strengths: string[];
  weaknesses: string[];
  suggestions: string[];
  qaReview: QaReview[];
  createdAt: string;
}
