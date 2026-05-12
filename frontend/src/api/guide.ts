import request from "./request";
import type { GuideChatRequest, GuideChatResponse } from "../types/guide";

export function sendGuideMessage(data: GuideChatRequest) {
  return request.post<GuideChatResponse, GuideChatRequest>(
    "/api/guide/chat",
    data,
    { silentError: true },
  );
}
