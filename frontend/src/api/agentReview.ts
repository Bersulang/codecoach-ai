import request from "./request";
import type { AgentReview, AgentReviewListItem } from "../types/agentReview";

export const generateAgentReview = (scopeType = "RECENT_10") =>
  request.post<AgentReview>("/api/agent-reviews", undefined, {
    params: { scopeType },
  });

export const getAgentReviewHistory = (limit = 20) =>
  request.get<AgentReviewListItem[]>("/api/agent-reviews", {
    params: { limit },
    silentError: true,
  });

export const getAgentReviewDetail = (reviewId: number | string) =>
  request.get<AgentReview>(`/api/agent-reviews/${reviewId}`, {
    silentError: true,
  });
