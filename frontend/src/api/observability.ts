import request from "./request";
import type {
  ObservabilityAgentRun,
  ObservabilityAgentStep,
  ObservabilityAiCall,
  ObservabilitySummary,
  ObservabilityToolTrace,
} from "../types/observability";

export const getObservabilitySummary = () =>
  request.get<ObservabilitySummary>("/api/dev/observability/summary", {
    silentError: true,
  });

export const getObservabilityAgentRuns = (params?: {
  agentType?: string;
  status?: string;
  limit?: number;
}) =>
  request.get<ObservabilityAgentRun[]>("/api/dev/observability/agent-runs", {
    params,
    silentError: true,
  });

export const getObservabilityAgentRun = (runId: string) =>
  request.get<ObservabilityAgentRun>(
    `/api/dev/observability/agent-runs/${runId}`,
    { silentError: true },
  );

export const getObservabilityAgentSteps = (runId: string) =>
  request.get<ObservabilityAgentStep[]>(
    `/api/dev/observability/agent-runs/${runId}/steps`,
    { silentError: true },
  );

export const getObservabilityToolTraces = (params?: {
  runId?: string;
  agentType?: string;
  toolName?: string;
  success?: boolean;
  limit?: number;
}) =>
  request.get<ObservabilityToolTrace[]>("/api/dev/observability/tool-traces", {
    params,
    silentError: true,
  });

export const getObservabilityAiCalls = (params?: {
  requestType?: string;
  success?: boolean;
  limit?: number;
}) =>
  request.get<ObservabilityAiCall[]>("/api/dev/observability/ai-calls", {
    params,
    silentError: true,
  });
