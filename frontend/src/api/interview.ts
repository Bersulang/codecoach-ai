import request from './request'
import type {
  CreateInterviewSessionRequest,
  CreateInterviewSessionResponse,
} from '../types/interview'

export const createInterviewSession = (
  payload: CreateInterviewSessionRequest,
) => request.post<CreateInterviewSessionResponse>('/api/interview-sessions', payload)
