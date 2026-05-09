export interface QaReview {
  question: string;
  answer: string;
  feedback: string;
}

export interface InterviewReport {
  id: number;
  sessionId: number;
  projectId: number;
  projectName: string;
  targetRole: string;
  difficulty: string;
  totalScore: number;
  summary: string;
  strengths: string[];
  weaknesses: string[];
  suggestions: string[];
  qaReview: QaReview[];
  createdAt: string;
}
