import request from "./request";
import { postNdjsonStream } from "./streamRequest";
import type { PageResult } from "../types/api";
import type { NdjsonStreamHandlers } from "../utils/stream";
import type {
  MockInterviewAnswerResponse,
  MockInterviewCreateRequest,
  MockInterviewCreateResponse,
  MockInterviewFinishResponse,
  MockInterviewHistoryItem,
  MockInterviewReport,
  MockInterviewSessionDetail,
} from "../types/mockInterview";

export const createMockInterview = (payload: MockInterviewCreateRequest) =>
  request.post<MockInterviewCreateResponse>("/api/mock-interviews", payload);

export const getMockInterviews = (params: {
  pageNum?: number;
  pageSize?: number;
  status?: string;
}) =>
  request.get<PageResult<MockInterviewHistoryItem>>("/api/mock-interviews", {
    params,
  });

export const getMockInterview = (sessionId: number | string) =>
  request.get<MockInterviewSessionDetail>(`/api/mock-interviews/${sessionId}`);

export const submitMockInterviewAnswer = (
  sessionId: number | string,
  answer: string,
) =>
  request.post<MockInterviewAnswerResponse>(
    `/api/mock-interviews/${sessionId}/answer`,
    { answer },
  );

export const submitMockInterviewAnswerStream = (
  sessionId: number | string,
  content: string,
  clientRequestId: string,
  handlers: NdjsonStreamHandlers<MockInterviewAnswerResponse>,
) =>
  postNdjsonStream<MockInterviewAnswerResponse>(
    `/api/mock-interviews/${sessionId}/answers/stream`,
    { content, clientRequestId },
    handlers,
  );

export const finishMockInterview = (sessionId: number | string) =>
  request.post<MockInterviewFinishResponse>(
    `/api/mock-interviews/${sessionId}/finish`,
  );

export const getMockInterviewReport = (sessionId: number | string) =>
  request.get<MockInterviewReport>(`/api/mock-interviews/${sessionId}/report`);
