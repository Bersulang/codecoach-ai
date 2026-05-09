export interface CreateInterviewSessionRequest {
  projectId: number
  targetRole: string
  difficulty: string
}

export interface CreateInterviewSessionResponse {
  sessionId: number
  firstQuestion?: string
}

export type InterviewMessageType =
  | 'AI_QUESTION'
  | 'USER_ANSWER'
  | 'AI_FEEDBACK'
  | 'AI_FOLLOW_UP'

export interface InterviewMessage {
  messageId?: number | string
  id?: number | string
  role?: string
  messageType: InterviewMessageType
  content: string
  roundNo?: number
  createdAt?: string
}

export interface InterviewSessionDetail {
  id: number
  projectId: number
  projectName: string
  targetRole: string
  difficulty: string
  status: string
  currentRound: number
  maxRound: number
  messages: InterviewMessage[]
}

export interface AnswerRequest {
  answer: string
}

export interface AnswerResponse {
  userAnswer: string
  aiFeedback: string
  nextQuestion?: string | null
  finished?: boolean
}

export interface FinishResponse {
  reportId: number
  sessionId: number
  totalScore?: number
}
