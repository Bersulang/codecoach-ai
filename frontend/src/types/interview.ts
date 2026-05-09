export interface CreateInterviewSessionRequest {
  projectId: number
  targetRole: string
  difficulty: string
}

export interface CreateInterviewSessionResponse {
  sessionId: number
  firstQuestion?: string
}
