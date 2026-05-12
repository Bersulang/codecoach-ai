import request from "./request";
import { postNdjsonStream } from "./streamRequest";
import type { PageResult } from "../types/api";
import type { NdjsonStreamHandlers } from "../utils/stream";
import type {
  CreateQuestionSessionRequest,
  CreateQuestionSessionResponse,
  KnowledgeTopic,
  KnowledgeTopicPageRequest,
  QuestionAnswerRequest,
  QuestionAnswerResponse,
  QuestionFinishResponse,
  QuestionReport,
  QuestionSessionHistoryItem,
  QuestionSessionHistoryParams,
  QuestionSessionDetail,
} from "../types/question";

export const getKnowledgeTopicCategories = () =>
  request.get<string[]>("/api/knowledge-topics/categories");

export const getKnowledgeTopics = (params: KnowledgeTopicPageRequest) =>
  request.get<PageResult<KnowledgeTopic>>("/api/knowledge-topics", { params });

export const createQuestionSession = (payload: CreateQuestionSessionRequest) =>
  request.post<CreateQuestionSessionResponse>(
    "/api/question-sessions",
    payload,
  );

export const getQuestionSessionDetail = (sessionId: number | string) =>
  request.get<QuestionSessionDetail>(`/api/question-sessions/${sessionId}`);

export const submitQuestionAnswer = (
  sessionId: number | string,
  payload: QuestionAnswerRequest,
) =>
  request.post<QuestionAnswerResponse>(
    `/api/question-sessions/${sessionId}/answer`,
    payload,
  );

export const submitQuestionAnswerStream = (
  sessionId: number | string,
  content: string,
  clientRequestId: string,
  handlers: NdjsonStreamHandlers<QuestionAnswerResponse>,
) =>
  postNdjsonStream<QuestionAnswerResponse>(
    `/api/question-sessions/${sessionId}/answers/stream`,
    { content, clientRequestId },
    handlers,
  );

export const finishQuestionSession = (
  sessionId: number | string,
  options?: { silentError?: boolean },
) =>
  request.post<QuestionFinishResponse>(
    `/api/question-sessions/${sessionId}/finish`,
    undefined,
    { silentError: options?.silentError },
  );

export const getQuestionReport = (reportId: number | string) =>
  request.get<QuestionReport>(`/api/question-reports/${reportId}`);

export const getQuestionSessionHistory = (
  params: QuestionSessionHistoryParams,
) =>
  request.get<PageResult<QuestionSessionHistoryItem>>(
    "/api/question-sessions",
    { params },
  );
