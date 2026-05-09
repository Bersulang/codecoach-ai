import request from './request'
import type {
  AnswerRequest,
  AnswerResponse,
  CreateInterviewSessionRequest,
  CreateInterviewSessionResponse,
  FinishResponse,
  InterviewSessionDetail,
} from '../types/interview'

export const createInterviewSession = (
  payload: CreateInterviewSessionRequest,
) => request.post<CreateInterviewSessionResponse>('/api/interview-sessions', payload)

export const getInterviewSession = (sessionId: number | string) =>
  request.get<InterviewSessionDetail>(`/api/interview-sessions/${sessionId}`)

export const submitInterviewAnswer = (
  sessionId: number | string,
  payload: AnswerRequest,
) =>
  request.post<AnswerResponse>(
    `/api/interview-sessions/${sessionId}/answer`,
    payload,
  )

export const finishInterview = (sessionId: number | string) =>
  request.post<FinishResponse>(`/api/interview-sessions/${sessionId}/finish`)
