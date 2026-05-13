export interface ObservabilityErrorItem {
  source: string;
  targetId?: string | null;
  name?: string | null;
  errorCode?: string | null;
  createdAt?: string;
}

export interface ObservabilityLatencyItem {
  source: string;
  targetId?: string | null;
  name?: string | null;
  latencyMs?: number | null;
}

export interface ObservabilitySummary {
  since: string;
  windowHours: number;
  agentRunCount: number;
  aiCallCount: number;
  toolCallCount: number;
  averageAgentLatencyMs?: number | null;
  averageLlmLatencyMs?: number | null;
  failureCount: number;
  recentErrors: ObservabilityErrorItem[];
  slowestItems: ObservabilityLatencyItem[];
}

export interface ObservabilityAgentRun {
  runId: string;
  traceId?: string | null;
  agentType?: string | null;
  status?: string | null;
  latencyMs?: number | null;
  inputSummary?: string | null;
  outputSummary?: string | null;
  errorCode?: string | null;
  errorMessage?: string | null;
  createdAt?: string;
}

export interface ObservabilityAgentStep {
  stepId: string;
  runId: string;
  stepType?: string | null;
  stepName?: string | null;
  toolName?: string | null;
  status?: string | null;
  latencyMs?: number | null;
  inputSummary?: string | null;
  outputSummary?: string | null;
  errorCode?: string | null;
  createdAt?: string;
}

export interface ObservabilityToolTrace {
  traceId: string;
  runId?: string | null;
  agentType?: string | null;
  toolName?: string | null;
  toolType?: string | null;
  success: boolean;
  latencyMs?: number | null;
  errorCode?: string | null;
  createdAt?: string;
  inputSummary?: string | null;
  outputSummary?: string | null;
}

export interface ObservabilityAiCall {
  provider?: string | null;
  modelName?: string | null;
  requestType?: string | null;
  promptVersion?: string | null;
  latencyMs?: number | null;
  success: boolean;
  errorCode?: string | null;
  createdAt?: string;
}
