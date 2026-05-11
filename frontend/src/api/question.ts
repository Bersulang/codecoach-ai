import request from "./request";
import type { PageResult } from "../types/api";
import type {
  CreateQuestionSessionRequest,
  CreateQuestionSessionResponse,
  KnowledgeTopic,
  KnowledgeTopicPageRequest,
  QuestionAnswerRequest,
  QuestionAnswerResponse,
  QuestionFinishResponse,
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

export const finishQuestionSession = (sessionId: number | string) =>
  request.post<QuestionFinishResponse>(
    `/api/question-sessions/${sessionId}/finish`,
  );
