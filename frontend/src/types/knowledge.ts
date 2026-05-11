export interface KnowledgeArticle {
  id: number;
  topicId: number;
  category: string;
  topicName: string;
  title: string;
  summary: string;
  version: string;
  status: string;
  sortOrder: number;
  updatedAt: string;
}

export interface KnowledgeArticlePageRequest {
  topicId?: number;
  category?: string;
  keyword?: string;
  pageNum?: number;
  pageSize?: number;
}
