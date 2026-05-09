export interface CreateInterviewSessionRequest {
  projectId: number;
  targetRole: string;
  difficulty: string;
}

export interface CreateInterviewSessionResponse {
  sessionId: number;
  firstQuestion?: string;
}

export type InterviewMessageType =
  | "AI_QUESTION"
  | "USER_ANSWER"
  | "AI_FEEDBACK"
  | "AI_FOLLOW_UP";

export interface InterviewMessage {
  messageId?: number | string;
  id?: number | string;
  role?: string;
  messageType: InterviewMessageType;
  content: string;
  roundNo?: number;
  createdAt?: string;
}

export interface InterviewSessionDetail {
  id: number;
  projectId: number;
  projectName: string;
  targetRole: string;
  difficulty: string;
  status: string;
  currentRound: number;
  maxRound: number;
  messages: InterviewMessage[];
}

export interface AnswerRequest {
  answer: string;
}

export interface AnswerResponse {
  userAnswer: InterviewMessage | string;
  aiFeedback: InterviewMessage | string;
  nextQuestion?: InterviewMessage | string | null;
  finished?: boolean;
}

export interface FinishResponse {
  reportId: number;
  sessionId: number;
  totalScore?: number;
}

export type InterviewStatus = "IN_PROGRESS" | "FINISHED" | "FAILED";

export interface InterviewSessionHistoryItem {
  id: number;
  projectId: number;
  projectName: string;
  targetRole: string;
  difficulty: string;
  status: InterviewStatus;
  currentRound: number;
  maxRound: number;
  totalScore?: number | null;
  reportId?: number | null;
  createdAt?: string;
  endedAt?: string | null;
}

export interface InterviewSessionPageParams {
  projectId?: number;
  status?: InterviewStatus;
  pageNum?: number;
  pageSize?: number;
}
