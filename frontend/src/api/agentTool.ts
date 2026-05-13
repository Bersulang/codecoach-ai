import request from "./request";
import type {
  GuideActionCard,
  ToolExecuteRequest,
  ToolExecuteResult,
} from "../types/guide";

export function executeAgentTool(data: ToolExecuteRequest) {
  return request.post<ToolExecuteResult, ToolExecuteRequest>(
    "/api/agent/tools/execute",
    data,
    { silentError: true },
  );
}

export function listAgentTools() {
  return request.get<GuideActionCard[]>("/api/agent/tools", {
    silentError: true,
  });
}
