import request from "./request";
import type {
  ObservabilityAgentRun,
  ObservabilityAgentStep,
  ObservabilityAiCall,
  ObservabilityRagTrace,
  ObservabilitySingleFlightTrace,
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

export const getObservabilityRagTraces = (params?: {
  success?: boolean;
  limit?: number;
}) =>
  request.get<ObservabilityRagTrace[]>("/api/dev/observability/rag-traces", {
    params,
    silentError: true,
  });

export const getObservabilitySingleFlightTraces = (params?: {
  success?: boolean;
  limit?: number;
}) =>
  request.get<ObservabilitySingleFlightTrace[]>(
    "/api/dev/observability/single-flight-traces",
    {
      params,
      silentError: true,
    },
  );
