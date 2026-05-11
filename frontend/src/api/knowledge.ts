import request from "./request";
import type { PageResult } from "../types/api";
import type {
  KnowledgeArticle,
  KnowledgeArticleDetail,
  KnowledgeArticlePageRequest,
} from "../types/knowledge";

export const getKnowledgeArticleList = (params: KnowledgeArticlePageRequest) =>
  request.get<PageResult<KnowledgeArticle>>("/api/knowledge-articles", {
    params,
  });

export const getKnowledgeTopicCategories = () =>
  request.get<string[]>("/api/knowledge-topics/categories");

export const getKnowledgeArticleDetail = (articleId: number | string) =>
  request.get<KnowledgeArticleDetail>(`/api/knowledge-articles/${articleId}`);
