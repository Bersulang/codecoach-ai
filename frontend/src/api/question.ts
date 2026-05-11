import request from "./request";
import type { PageResult } from "../types/api";
import type {
  CreateQuestionSessionRequest,
  CreateQuestionSessionResponse,
  KnowledgeTopic,
  KnowledgeTopicPageRequest,
} from "../types/question";

export const getKnowledgeTopicCategories = () =>
  request.get<string[]>("/api/knowledge-topics/categories");

export const getKnowledgeTopics = (params: KnowledgeTopicPageRequest) =>
  request.get<PageResult<KnowledgeTopic>>("/api/knowledge-topics", { params });

export const createQuestionSession = (payload: CreateQuestionSessionRequest) =>
  request.post<CreateQuestionSessionResponse>("/api/question-sessions", payload);
