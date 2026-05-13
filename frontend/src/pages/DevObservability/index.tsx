import {
  ApiOutlined,
  BranchesOutlined,
  ClockCircleOutlined,
  ExclamationCircleOutlined,
  ReloadOutlined,
  RobotOutlined,
  ThunderboltOutlined,
  ToolOutlined,
} from "@ant-design/icons";
import { Button, Card, Empty, Input, Select, Skeleton, Space, Tabs, Tag, message } from "antd";
import type { ReactNode } from "react";
import { useCallback, useEffect, useMemo, useState } from "react";
import {
  getObservabilityAgentRun,
  getObservabilityAgentRuns,
  getObservabilityAgentSteps,
  getObservabilityAiCalls,
  getObservabilityRagTraces,
  getObservabilitySingleFlightTraces,
  getObservabilitySummary,
  getObservabilityToolTraces,
} from "../../api/observability";
import PageShell from "../../components/PageShell";
import type {
  ObservabilityAgentRun,
  ObservabilityAgentStep,
  ObservabilityAiCall,
  ObservabilityRagTrace,
  ObservabilitySingleFlightTrace,
  ObservabilitySummary,
  ObservabilityToolTrace,
} from "../../types/observability";
import "./index.css";

const STATUS_OPTIONS = [
  { value: "", label: "全部状态" },
  { value: "RUNNING", label: "RUNNING" },
  { value: "SUCCEEDED", label: "SUCCEEDED" },
  { value: "FAILED", label: "FAILED" },
  { value: "CANCELLED", label: "CANCELLED" },
];

const SUCCESS_OPTIONS = [
  { value: "all", label: "全部结果" },
  { value: "true", label: "成功" },
  { value: "false", label: "失败" },
];

function formatDateTime(value?: string) {
  if (!value) {
    return "—";
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  return date.toLocaleString();
}

function formatDuration(value?: number | null) {
  if (value === null || value === undefined) {
    return "—";
  }
  if (value >= 1000) {
    return `${(value / 1000).toFixed(value >= 10000 ? 1 : 2)}s`;
  }
  return `${value}ms`;
}

function compactId(value?: string | null) {
  if (!value) {
    return "—";
  }
  if (value.length <= 14) {
    return value;
  }
  return `${value.slice(0, 8)}...${value.slice(-4)}`;
}

function textOrEmpty(value?: string | null) {
  return value || "暂无摘要";
}

function statusTone(status?: string | null) {
  const normalized = (status || "").toUpperCase();
  if (normalized === "SUCCEEDED" || normalized === "SUCCESS") {
    return "success";
  }
  if (normalized === "FAILED" || normalized === "ERROR") {
    return "error";
  }
  if (normalized === "RUNNING") {
    return "processing";
  }
  return "default";
}

function successTag(success?: boolean) {
  return success ? <Tag color="success">成功</Tag> : <Tag color="error">失败</Tag>;
}

function toSuccess(value: string) {
  if (value === "true") {
    return true;
  }
  if (value === "false") {
    return false;
  }
  return undefined;
}

function StatCard({
  icon,
  label,
  value,
  hint,
}: {
  icon: ReactNode;
  label: string;
  value: string | number;
  hint?: string;
}) {
  return (
    <Card className="observability-stat-card">
      <div className="observability-stat-card__icon">{icon}</div>
      <div>
        <span>{label}</span>
        <strong>{value}</strong>
        {hint ? <small>{hint}</small> : null}
      </div>
    </Card>
  );
}

function SummaryLists({ summary }: { summary: ObservabilitySummary | null }) {
  return (
    <section className="observability-summary-grid">
      <Card title="错误摘要">
        {summary?.recentErrors?.length ? (
          <div className="observability-compact-list">
            {summary.recentErrors.map((item, index) => (
              <div key={`${item.source}-${item.targetId || index}`} className="observability-compact-row">
                <Tag color="error">{item.source}</Tag>
                <div>
                  <strong>{item.name || item.targetId || "未知调用"}</strong>
                  <span>{item.errorCode || "未记录错误码"}</span>
                </div>
                <time>{formatDateTime(item.createdAt)}</time>
              </div>
            ))}
          </div>
        ) : (
          <Empty description="最近没有失败调用" />
        )}
      </Card>

      <Card title="耗时摘要">
        {summary?.slowestItems?.length ? (
          <div className="observability-compact-list">
            {summary.slowestItems.map((item, index) => (
              <div key={`${item.source}-${item.targetId || index}`} className="observability-compact-row">
                <Tag>{item.source}</Tag>
                <div>
                  <strong>{item.name || item.targetId || "未知调用"}</strong>
                  <span>{compactId(item.targetId)}</span>
                </div>
                <b>{formatDuration(item.latencyMs)}</b>
              </div>
            ))}
          </div>
        ) : (
          <Empty description="暂无耗时记录" />
        )}
      </Card>
    </section>
  );
}

function AgentTimeline({ steps }: { steps: ObservabilityAgentStep[] }) {
  if (!steps.length) {
    return <Empty description="这次 AgentRun 暂无步骤记录" />;
  }
  return (
    <div className="observability-timeline">
      {steps.map((step) => (
        <article key={step.stepId} className="observability-step">
          <div className={`observability-step__dot observability-step__dot--${statusTone(step.status)}`} />
          <div className="observability-step__content">
            <div className="observability-step__head">
              <Space wrap>
                <Tag>{step.stepType || "STEP"}</Tag>
                {step.toolName ? <Tag icon={<ToolOutlined />}>{step.toolName}</Tag> : null}
                <Tag color={statusTone(step.status)}>{step.status || "UNKNOWN"}</Tag>
              </Space>
              <span>{formatDuration(step.latencyMs)}</span>
            </div>
            <h3>{step.stepName || step.stepType || "Agent Step"}</h3>
            <p>{textOrEmpty(step.outputSummary || step.inputSummary)}</p>
            <div className="observability-step__meta">
              <span>{formatDateTime(step.createdAt)}</span>
              {step.errorCode ? <strong>{step.errorCode}</strong> : null}
            </div>
          </div>
        </article>
      ))}
    </div>
  );
}

function ToolTraceList({ traces, emptyText }: { traces: ObservabilityToolTrace[]; emptyText: string }) {
  if (!traces.length) {
    return <Empty description={emptyText} />;
  }
  return (
    <div className="observability-card-list">
      {traces.map((trace) => (
        <article key={trace.traceId} className="observability-data-card">
          <div className="observability-data-card__head">
            <Space wrap>
              <Tag icon={<ToolOutlined />}>{trace.toolName || "UNKNOWN_TOOL"}</Tag>
              {trace.toolType ? <Tag>{trace.toolType}</Tag> : null}
              {successTag(trace.success)}
            </Space>
            <strong>{formatDuration(trace.latencyMs)}</strong>
          </div>
          <p>{textOrEmpty(trace.outputSummary || trace.inputSummary)}</p>
          <div className="observability-data-card__meta">
            <span>trace {compactId(trace.traceId)}</span>
            <span>run {compactId(trace.runId)}</span>
            <span>{trace.agentType || "UNKNOWN_AGENT"}</span>
            <time>{formatDateTime(trace.createdAt)}</time>
          </div>
          {trace.errorCode ? <div className="observability-error-line">{trace.errorCode}</div> : null}
        </article>
      ))}
    </div>
  );
}

function AiCallList({ calls }: { calls: ObservabilityAiCall[] }) {
  if (!calls.length) {
    return <Empty description="暂无 AI Call 记录" />;
  }
  return (
    <div className="observability-card-list">
      {calls.map((call, index) => (
        <article
          key={`${call.createdAt || "ai"}-${call.requestType || "call"}-${index}`}
          className="observability-data-card"
        >
          <div className="observability-data-card__head">
            <Space wrap>
              <Tag icon={<ApiOutlined />}>{call.requestType || "UNKNOWN_REQUEST"}</Tag>
              <Tag>{call.provider || "UNKNOWN_PROVIDER"}</Tag>
              {successTag(call.success)}
            </Space>
            <strong>{formatDuration(call.latencyMs)}</strong>
          </div>
          <p>
            {call.modelName || "未记录模型"}
            {call.promptVersion ? ` · ${call.promptVersion}` : ""}
          </p>
          <div className="observability-data-card__meta">
            <time>{formatDateTime(call.createdAt)}</time>
            <span>trace {compactId(call.traceId)}</span>
            {call.errorCode ? <span>{call.errorCode}</span> : null}
          </div>
        </article>
      ))}
    </div>
  );
}

function RagTraceList({ traces }: { traces: ObservabilityRagTrace[] }) {
  if (!traces.length) {
    return <Empty description="暂无 RagTrace 记录" />;
  }
  return (
    <div className="observability-card-list">
      {traces.map((trace, index) => (
        <article
          key={`${trace.traceId || "rag"}-${trace.createdAt || index}`}
          className="observability-data-card"
        >
          <div className="observability-data-card__head">
            <Space wrap>
              <Tag icon={<BranchesOutlined />}>RAG</Tag>
              <Tag>{trace.sourceTypes || "ALL_SOURCES"}</Tag>
              {successTag(trace.success)}
            </Space>
            <strong>{formatDuration(trace.latencyMs)}</strong>
          </div>
          <p>{trace.query || "未记录查询"}</p>
          {trace.rewrittenQuery && trace.rewrittenQuery !== trace.query ? (
            <p>改写：{trace.rewrittenQuery}</p>
          ) : null}
          <div className="observability-data-card__meta">
            <span>hit {trace.hitCount ?? 0}</span>
            <span>topK {trace.topK ?? "—"}</span>
            <span>ctx {trace.contextChars ?? 0}</span>
            <span>score {trace.avgScore?.toFixed(3) ?? "—"}</span>
            <span>trace {compactId(trace.traceId)}</span>
            <time>{formatDateTime(trace.createdAt)}</time>
          </div>
          {trace.fallbackReason ? <div className="observability-error-line">{trace.fallbackReason}</div> : null}
        </article>
      ))}
    </div>
  );
}

function SingleFlightTraceList({ traces }: { traces: ObservabilitySingleFlightTrace[] }) {
  if (!traces.length) {
    return <Empty description="暂无 Single-flight 记录" />;
  }
  return (
    <div className="observability-card-list">
      {traces.map((trace, index) => (
        <article
          key={`${trace.traceId || "singleflight"}-${trace.createdAt || index}`}
          className="observability-data-card"
        >
          <div className="observability-data-card__head">
            <Space wrap>
              <Tag icon={<ThunderboltOutlined />}>Single-flight</Tag>
              <Tag>{trace.action || "UNKNOWN_ACTION"}</Tag>
              {successTag(trace.success)}
            </Space>
            <strong>{formatDuration(trace.latencyMs)}</strong>
          </div>
          <p>{trace.requestKey || "未记录请求Key"}</p>
          <div className="observability-data-card__meta">
            <span>trace {compactId(trace.traceId)}</span>
            <time>{formatDateTime(trace.createdAt)}</time>
          </div>
          {trace.fallbackReason ? <div className="observability-error-line">{trace.fallbackReason}</div> : null}
        </article>
      ))}
    </div>
  );
}

function DevObservabilityPage() {
  const [summary, setSummary] = useState<ObservabilitySummary | null>(null);
  const [agentRuns, setAgentRuns] = useState<ObservabilityAgentRun[]>([]);
  const [selectedRun, setSelectedRun] = useState<ObservabilityAgentRun | null>(null);
  const [steps, setSteps] = useState<ObservabilityAgentStep[]>([]);
  const [runTools, setRunTools] = useState<ObservabilityToolTrace[]>([]);
  const [toolTraces, setToolTraces] = useState<ObservabilityToolTrace[]>([]);
  const [aiCalls, setAiCalls] = useState<ObservabilityAiCall[]>([]);
  const [ragTraces, setRagTraces] = useState<ObservabilityRagTrace[]>([]);
  const [singleFlightTraces, setSingleFlightTraces] = useState<ObservabilitySingleFlightTrace[]>([]);
  const [agentTypeFilter, setAgentTypeFilter] = useState("");
  const [statusFilter, setStatusFilter] = useState("");
  const [toolNameFilter, setToolNameFilter] = useState("");
  const [toolAgentFilter, setToolAgentFilter] = useState("");
  const [toolSuccessFilter, setToolSuccessFilter] = useState("all");
  const [aiRequestFilter, setAiRequestFilter] = useState("");
  const [aiSuccessFilter, setAiSuccessFilter] = useState("all");
  const [ragSuccessFilter, setRagSuccessFilter] = useState("all");
  const [singleFlightSuccessFilter, setSingleFlightSuccessFilter] = useState("all");
  const [loadingOverview, setLoadingOverview] = useState(false);
  const [loadingRun, setLoadingRun] = useState(false);
  const [loadingTools, setLoadingTools] = useState(false);
  const [loadingAi, setLoadingAi] = useState(false);
  const [loadingRag, setLoadingRag] = useState(false);
  const [loadingSingleFlight, setLoadingSingleFlight] = useState(false);

  const loadRunDetail = useCallback(async (runId: string) => {
    setLoadingRun(true);
    try {
      const [run, stepList, relatedTools] = await Promise.all([
        getObservabilityAgentRun(runId),
        getObservabilityAgentSteps(runId),
        getObservabilityToolTraces({ runId, limit: 50 }),
      ]);
      setSelectedRun(run);
      setSteps(stepList || []);
      setRunTools(relatedTools || []);
    } catch {
      message.error("AgentRun 详情加载失败");
      setSteps([]);
      setRunTools([]);
    } finally {
      setLoadingRun(false);
    }
  }, []);

  const loadAgentRuns = useCallback(async () => {
    const data = await getObservabilityAgentRuns({
      agentType: agentTypeFilter || undefined,
      status: statusFilter || undefined,
      limit: 100,
    });
    const runs = data || [];
    setAgentRuns(runs);
    const stillSelected = selectedRun && runs.some((run) => run.runId === selectedRun.runId);
    const nextRunId = stillSelected ? selectedRun.runId : runs[0]?.runId;
    if (nextRunId) {
      await loadRunDetail(nextRunId);
    } else {
      setSelectedRun(null);
      setSteps([]);
      setRunTools([]);
    }
  }, [agentTypeFilter, loadRunDetail, selectedRun, statusFilter]);

  const loadToolTraces = useCallback(async () => {
    setLoadingTools(true);
    try {
      const data = await getObservabilityToolTraces({
        agentType: toolAgentFilter || undefined,
        toolName: toolNameFilter || undefined,
        success: toSuccess(toolSuccessFilter),
        limit: 100,
      });
      setToolTraces(data || []);
    } catch {
      setToolTraces([]);
    } finally {
      setLoadingTools(false);
    }
  }, [toolAgentFilter, toolNameFilter, toolSuccessFilter]);

  const loadAiCalls = useCallback(async () => {
    setLoadingAi(true);
    try {
      const data = await getObservabilityAiCalls({
        requestType: aiRequestFilter || undefined,
        success: toSuccess(aiSuccessFilter),
        limit: 100,
      });
      setAiCalls(data || []);
    } catch {
      setAiCalls([]);
    } finally {
      setLoadingAi(false);
    }
  }, [aiRequestFilter, aiSuccessFilter]);

  const loadRagTraces = useCallback(async () => {
    setLoadingRag(true);
    try {
      const data = await getObservabilityRagTraces({
        success: toSuccess(ragSuccessFilter),
        limit: 100,
      });
      setRagTraces(data || []);
    } catch {
      setRagTraces([]);
    } finally {
      setLoadingRag(false);
    }
  }, [ragSuccessFilter]);

  const loadSingleFlightTraces = useCallback(async () => {
    setLoadingSingleFlight(true);
    try {
      const data = await getObservabilitySingleFlightTraces({
        success: toSuccess(singleFlightSuccessFilter),
        limit: 100,
      });
      setSingleFlightTraces(data || []);
    } catch {
      setSingleFlightTraces([]);
    } finally {
      setLoadingSingleFlight(false);
    }
  }, [singleFlightSuccessFilter]);

  const loadOverview = useCallback(async () => {
    setLoadingOverview(true);
    try {
      const [summaryData] = await Promise.all([getObservabilitySummary(), loadAgentRuns()]);
      setSummary(summaryData);
    } catch {
      message.error("可观测性数据加载失败");
    } finally {
      setLoadingOverview(false);
    }
  }, [loadAgentRuns]);

  useEffect(() => {
    void loadOverview();
    void loadToolTraces();
    void loadAiCalls();
    void loadRagTraces();
    void loadSingleFlightTraces();
  }, []);

  const stats = useMemo(
    () => [
      {
        label: "AgentRun",
        value: summary?.agentRunCount ?? "—",
        icon: <RobotOutlined />,
        hint: `最近 ${summary?.windowHours || 24} 小时`,
      },
      {
        label: "AI Call",
        value: summary?.aiCallCount ?? "—",
        icon: <ApiOutlined />,
      },
      {
        label: "ToolCall",
        value: summary?.toolCallCount ?? "—",
        icon: <ToolOutlined />,
      },
      {
        label: "Agent 平均耗时",
        value: formatDuration(summary?.averageAgentLatencyMs),
        icon: <ClockCircleOutlined />,
      },
      {
        label: "LLM 平均耗时",
        value: formatDuration(summary?.averageLlmLatencyMs),
        icon: <ThunderboltOutlined />,
      },
      {
        label: "失败次数",
        value: summary?.failureCount ?? "—",
        icon: <ExclamationCircleOutlined />,
      },
    ],
    [summary],
  );

  return (
    <PageShell className="observability-page" maxWidth={1240}>
      <section className="observability-hero">
        <div>
          <p className="workspace-kicker">Dev Observability</p>
          <h1>AI 可观测性</h1>
          <p>查看 AI 调用、RAG 检索、Tool 调用和 Agent 执行链路。</p>
        </div>
        <Button
          icon={<ReloadOutlined />}
          loading={loadingOverview}
          onClick={() => {
            void loadOverview();
            void loadToolTraces();
            void loadAiCalls();
            void loadRagTraces();
            void loadSingleFlightTraces();
          }}
        >
          刷新
        </Button>
      </section>

      <section className="observability-stat-grid">
        {stats.map((item) => (
          <StatCard key={item.label} {...item} />
        ))}
      </section>

      <section className="observability-run-grid">
        <Card
          title="AgentRun 列表"
          extra={
            <Button size="small" onClick={() => void loadAgentRuns()}>
              应用过滤
            </Button>
          }
        >
          <div className="observability-filters">
            <Input
              allowClear
              placeholder="agentType"
              value={agentTypeFilter}
              onChange={(event) => setAgentTypeFilter(event.target.value)}
            />
            <Select value={statusFilter} options={STATUS_OPTIONS} onChange={setStatusFilter} />
          </div>
          {loadingOverview ? (
            <Skeleton active paragraph={{ rows: 5 }} />
          ) : agentRuns.length ? (
            <div className="observability-run-list">
              {agentRuns.map((run) => (
                <button
                  key={run.runId}
                  type="button"
                  className={`observability-run-item${
                    selectedRun?.runId === run.runId ? " observability-run-item--active" : ""
                  }`}
                  onClick={() => void loadRunDetail(run.runId)}
                >
                  <div>
                    <strong>{run.agentType || "UNKNOWN_AGENT"}</strong>
                    <span>{compactId(run.runId)}</span>
                  </div>
                  <Tag color={statusTone(run.status)}>{run.status || "UNKNOWN"}</Tag>
                  <p>{textOrEmpty(run.inputSummary || run.outputSummary)}</p>
                  <small>
                    {formatDuration(run.latencyMs)} · {formatDateTime(run.createdAt)}
                  </small>
                </button>
              ))}
            </div>
          ) : (
            <Empty description="暂无 AgentRun 记录" />
          )}
        </Card>

        <Card title="AgentRun 详情" className="observability-detail-card">
          {loadingRun ? (
            <Skeleton active paragraph={{ rows: 8 }} />
          ) : selectedRun ? (
            <>
              <div className="observability-detail-head">
                <div>
                  <Tag icon={<BranchesOutlined />}>{selectedRun.agentType || "UNKNOWN_AGENT"}</Tag>
                  <Tag color={statusTone(selectedRun.status)}>{selectedRun.status || "UNKNOWN"}</Tag>
                </div>
                <strong>{formatDuration(selectedRun.latencyMs)}</strong>
              </div>
              <div className="observability-detail-grid">
                <span>runId</span>
                <b>{compactId(selectedRun.runId)}</b>
                <span>traceId</span>
                <b>{compactId(selectedRun.traceId)}</b>
                <span>createdAt</span>
                <b>{formatDateTime(selectedRun.createdAt)}</b>
                <span>errorCode</span>
                <b>{selectedRun.errorCode || "—"}</b>
              </div>
              {selectedRun.errorMessage ? (
                <div className="observability-error-box">{selectedRun.errorMessage}</div>
              ) : null}
              <div className="observability-summary-copy">
                <strong>输入摘要</strong>
                <p>{textOrEmpty(selectedRun.inputSummary)}</p>
                <strong>输出摘要</strong>
                <p>{textOrEmpty(selectedRun.outputSummary)}</p>
              </div>
              <div className="observability-section-title">AgentStep 时间线</div>
              <AgentTimeline steps={steps} />
              <div className="observability-section-title">本次 ToolTrace</div>
              <ToolTraceList traces={runTools} emptyText="这次 AgentRun 暂无 Tool 调用" />
            </>
          ) : (
            <Empty description="选择一条 AgentRun 查看详情" />
          )}
        </Card>
      </section>

      <Tabs
        className="observability-tabs"
        items={[
          {
            key: "tools",
            label: "ToolTrace",
            children: (
              <Card
                title="ToolTrace 列表"
                extra={
                  <Button size="small" onClick={() => void loadToolTraces()}>
                    应用过滤
                  </Button>
                }
              >
                <div className="observability-filters observability-filters--wide">
                  <Input
                    allowClear
                    placeholder="toolName"
                    value={toolNameFilter}
                    onChange={(event) => setToolNameFilter(event.target.value)}
                  />
                  <Input
                    allowClear
                    placeholder="agentType"
                    value={toolAgentFilter}
                    onChange={(event) => setToolAgentFilter(event.target.value)}
                  />
                  <Select
                    value={toolSuccessFilter}
                    options={SUCCESS_OPTIONS}
                    onChange={setToolSuccessFilter}
                  />
                </div>
                {loadingTools ? (
                  <Skeleton active paragraph={{ rows: 4 }} />
                ) : (
                  <ToolTraceList traces={toolTraces} emptyText="暂无 ToolTrace 记录" />
                )}
              </Card>
            ),
          },
          {
            key: "rag",
            label: "RagTrace",
            children: (
              <Card
                title="RagTrace 列表"
                extra={
                  <Button size="small" onClick={() => void loadRagTraces()}>
                    应用过滤
                  </Button>
                }
              >
                <div className="observability-filters observability-filters--wide">
                  <Select
                    value={ragSuccessFilter}
                    options={SUCCESS_OPTIONS}
                    onChange={setRagSuccessFilter}
                  />
                </div>
                {loadingRag ? <Skeleton active paragraph={{ rows: 4 }} /> : <RagTraceList traces={ragTraces} />}
              </Card>
            ),
          },
          {
            key: "singleflight",
            label: "Single-flight",
            children: (
              <Card
                title="Single-flight 列表"
                extra={
                  <Button size="small" onClick={() => void loadSingleFlightTraces()}>
                    应用过滤
                  </Button>
                }
              >
                <div className="observability-filters observability-filters--wide">
                  <Select
                    value={singleFlightSuccessFilter}
                    options={SUCCESS_OPTIONS}
                    onChange={setSingleFlightSuccessFilter}
                  />
                </div>
                {loadingSingleFlight ? (
                  <Skeleton active paragraph={{ rows: 4 }} />
                ) : (
                  <SingleFlightTraceList traces={singleFlightTraces} />
                )}
              </Card>
            ),
          },
          {
            key: "ai",
            label: "AI Call",
            children: (
              <Card
                title="AI Call 列表"
                extra={
                  <Button size="small" onClick={() => void loadAiCalls()}>
                    应用过滤
                  </Button>
                }
              >
                <div className="observability-filters observability-filters--wide">
                  <Input
                    allowClear
                    placeholder="requestType"
                    value={aiRequestFilter}
                    onChange={(event) => setAiRequestFilter(event.target.value)}
                  />
                  <Select
                    value={aiSuccessFilter}
                    options={SUCCESS_OPTIONS}
                    onChange={setAiSuccessFilter}
                  />
                </div>
                {loadingAi ? <Skeleton active paragraph={{ rows: 4 }} /> : <AiCallList calls={aiCalls} />}
              </Card>
            ),
          },
          {
            key: "summary",
            label: "错误与耗时",
            children: <SummaryLists summary={summary} />,
          },
        ]}
      />
    </PageShell>
  );
}

export default DevObservabilityPage;
