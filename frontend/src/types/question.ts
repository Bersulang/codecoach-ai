import type { InterviewDifficulty } from "./interview";

export type QuestionMessageType =
  | "AI_QUESTION"
  | "USER_ANSWER"
  | "AI_FEEDBACK"
  | "AI_REFERENCE_ANSWER"
  | "AI_FOLLOW_UP"
  | "SYSTEM_NOTICE";

export interface QuestionMessage {
  messageId?: number | string;
  id?: number | string;
  role?: string;
  messageType: QuestionMessageType;
  content: string;
  roundNo?: number;
  score?: number | null;
  createdAt?: string;
}

export interface KnowledgeTopic {
  id: number;
  category: string;
  name: string;
  description?: string;
  difficulty: InterviewDifficulty;
  interviewFocus?: string;
  tags?: string[];
  sortOrder?: number;
}

export interface KnowledgeTopicPageRequest {
  category?: string;
  keyword?: string;
  difficulty?: InterviewDifficulty;
  pageNum?: number;
  pageSize?: number;
}

export interface CreateQuestionSessionRequest {
  topicId: number;
  targetRole: string;
  difficulty: InterviewDifficulty;
}

export interface CreateQuestionSessionResponse {
  sessionId: number;
  topicId: number;
  category: string;
  topicName: string;
  targetRole: string;
  difficulty: InterviewDifficulty;
  currentRound: number;
  maxRound: number;
  firstQuestion?: QuestionMessage;
}

export interface QuestionSessionDetail {
  id: number;
  topicId: number;
  category: string;
  topicName: string;
  topicDescription?: string;
  targetRole: string;
  difficulty: InterviewDifficulty;
  status: string;
  currentRound: number;
  maxRound: number;
  totalScore?: number | null;
  createdAt?: string;
  endedAt?: string | null;
  messages: QuestionMessage[];
}

export interface QuestionAnswerRequest {
  answer: string;
}

export interface QuestionAnswerResponse {
  userAnswer?: QuestionMessage | string | null;
  aiFeedback?: QuestionMessage | string | null;
  referenceAnswer?: QuestionMessage | string | null;
  nextQuestion?: QuestionMessage | string | null;
  finished?: boolean;
  reportId?: number | null;
  totalScore?: number | null;
}

export interface QuestionFinishResponse {
  reportId: number;
  sessionId: number;
  totalScore?: number | null;
}
