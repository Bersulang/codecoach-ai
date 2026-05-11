import type { InterviewDifficulty, InterviewMessage } from "./interview";

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
  firstQuestion?: InterviewMessage;
}
