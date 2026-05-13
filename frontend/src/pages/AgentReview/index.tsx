import {
  Alert,
  Button,
  Card,
  Empty,
  Select,
  Skeleton,
  Space,
  Tag,
  message,
} from "antd";
import { ArrowRightOutlined, ReloadOutlined } from "@ant-design/icons";
import { useCallback, useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  generateAgentReview,
  getAgentReviewDetail,
  getAgentReviewHistory,
} from "../../api/agentReview";
import PageShell from "../../components/PageShell";
import type {
  AgentReview,
  AgentReviewListItem,
  AgentReviewNextAction,
  ReviewRecommendation,
} from "../../types/agentReview";
import "./index.css";

const CONFIDENCE_LABELS: Record<string, string> = {
  LOW: "低置信度",
  MEDIUM: "中等置信度",
  HIGH: "高置信度",
};

const SAMPLE_LABELS: Record<string, string> = {
  INSUFFICIENT: "样本不足",
  LIMITED: "样本有限",
  ENOUGH: "样本充足",
};

const ACTION_LABELS: Record<string, string> = {
  LEARN: "知识学习",
  TRAIN_QUESTION: "八股训练",
  TRAIN_PROJECT: "项目拷打",
  MOCK_INTERVIEW: "模拟面试",
  REVIEW_RESUME: "简历训练",
  UPLOAD_DOCUMENT: "文档上传",
  VIEW_MEMORY: "长期记忆",
  VIEW_REPORT_REPLAY: "问答回放",
};

function formatDateTime(value?: string) {
  if (!value) {
    return "--";
  }
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString();
}

function asNumber(value: unknown) {
  return typeof value === "number" && Number.isFinite(value) ? value : null;
}

function confidenceClass(confidence?: string) {
  return `agent-review-pill agent-review-pill--${(confidence || "LOW").toLowerCase()}`;
}

function RadarChart({ review }: { review: AgentReview }) {
  const dimensions = (review.radarDimensions || []).filter((item) =>
    Number.isFinite(item.score),
  );
  const size = 260;
  const center = size / 2;
  const radius = 92;

  if (dimensions.length < 3) {
    return (
      <div className="agent-review-empty-panel">
        <Empty description="雷达图数据不足，完成更多训练后会自动生成。" />
      </div>
    );
  }

  const points = dimensions.map((item, index) => {
    const angle = (Math.PI * 2 * index) / dimensions.length - Math.PI / 2;
    const value = Math.max(0, Math.min(100, Number(item.score || 0))) / 100;
    return {
      ...item,
      x: center + Math.cos(angle) * radius * value,
      y: center + Math.sin(angle) * radius * value,
      labelX: center + Math.cos(angle) * (radius + 30),
      labelY: center + Math.sin(angle) * (radius + 30),
      axisX: center + Math.cos(angle) * radius,
      axisY: center + Math.sin(angle) * radius,
    };
  });
  const polygon = points.map((point) => `${point.x},${point.y}`).join(" ");

  return (
    <div className="agent-review-radar-wrap">
      <svg className="agent-review-radar" viewBox={`0 0 ${size} ${size}`}>
        {[0.25, 0.5, 0.75, 1].map((scale) => (
          <circle
            key={scale}
            cx={center}
            cy={center}
            r={radius * scale}
            fill="none"
            stroke="rgba(120, 96, 72, 0.16)"
          />
        ))}
        {points.map((point) => (
          <line
            key={point.name}
            x1={center}
            y1={center}
            x2={point.axisX}
            y2={point.axisY}
            stroke="rgba(120, 96, 72, 0.18)"
          />
        ))}
        <polygon points={polygon} fill="rgba(205, 128, 76, 0.2)" stroke="#c97742" strokeWidth="2" />
        {points.map((point) => (
          <g key={point.name}>
            <circle cx={point.x} cy={point.y} r="4" fill="#c97742" />
            <text x={point.labelX} y={point.labelY} textAnchor="middle" dominantBaseline="middle">
              {point.name}
            </text>
          </g>
        ))}
      </svg>
      <div className="agent-review-radar-list">
        {dimensions.map((item) => (
          <div key={item.name}>
            <strong>{item.name}</strong>
            <span>{item.score ?? "--"} 分</span>
            <p>{item.evidence}</p>
          </div>
        ))}
      </div>
    </div>
  );
}

function ListPanel({
  title,
  items,
  empty,
}: {
  title: string;
  items?: string[];
  empty: string;
}) {
  return (
    <Card className="agent-review-section-card">
      <div className="agent-review-section-title">{title}</div>
      {items?.length ? (
        <ul className="agent-review-list">
          {items.map((item, index) => (
            <li key={`${item}-${index}`}>{item}</li>
          ))}
        </ul>
      ) : (
        <Empty description={empty} />
      )}
    </Card>
  );
}

function RecommendationList({
  title,
  items,
  onOpen,
}: {
  title: string;
  items?: ReviewRecommendation[];
  onOpen: (path?: string) => void;
}) {
  return (
    <Card className="agent-review-section-card">
      <div className="agent-review-section-title">{title}</div>
      {items?.length ? (
        <div className="agent-review-recommend-list">
          {items.map((item, index) => (
            <button
              key={`${item.title}-${index}`}
              type="button"
              onClick={() => onOpen(item.targetPath)}
            >
              <strong>{item.title}</strong>
              <span>{item.reason}</span>
            </button>
          ))}
        </div>
      ) : (
        <Empty description="暂无推荐内容" />
      )}
    </Card>
  );
}

function AgentReviewPage() {
  const navigate = useNavigate();
  const [scopeType, setScopeType] = useState("RECENT_10");
  const [currentReview, setCurrentReview] = useState<AgentReview | null>(null);
  const [history, setHistory] = useState<AgentReviewListItem[]>([]);
  const [loadingHistory, setLoadingHistory] = useState(false);
  const [generating, setGenerating] = useState(false);
  const [detailLoadingId, setDetailLoadingId] = useState<number | null>(null);

  const loadHistory = useCallback(async () => {
    setLoadingHistory(true);
    try {
      const data = await getAgentReviewHistory();
      setHistory(data || []);
      if (!currentReview && data?.[0]?.id) {
        setCurrentReview(await getAgentReviewDetail(data[0].id));
      }
    } catch {
      setHistory([]);
    } finally {
      setLoadingHistory(false);
    }
  }, [currentReview]);

  useEffect(() => {
    void loadHistory();
  }, []);

  const source = currentReview?.sourceSnapshot || {};
  const totalEvidence = useMemo(
    () =>
      Number(source.projectReportCount || 0) +
      Number(source.questionReportCount || 0) +
      Number(source.mockInterviewReportCount || 0) +
      Number(source.abilitySnapshotCount || 0) +
      Number(source.memoryItemCount || 0),
    [source],
  );

  const handleGenerate = async () => {
    setGenerating(true);
    try {
      const review = await generateAgentReview(scopeType);
      setCurrentReview(review);
      message.success("综合复盘已生成");
      await loadHistory();
    } catch {
      message.error("复盘生成失败或正在生成中，可以稍后重试");
    } finally {
      setGenerating(false);
    }
  };

  const handleOpenHistory = async (reviewId: number) => {
    setDetailLoadingId(reviewId);
    try {
      setCurrentReview(await getAgentReviewDetail(reviewId));
    } catch {
      message.error("复盘详情加载失败");
    } finally {
      setDetailLoadingId(null);
    }
  };

  const openPath = (path?: string) => {
    if (path) {
      navigate(path);
    }
  };

  const handleAction = (action: AgentReviewNextAction) => {
    openPath(action.targetPath);
  };

  const score = asNumber(currentReview?.scoreOverview?.score);

  return (
    <PageShell className="agent-review-page">
      <section className="agent-review-hero">
        <div>
          <p className="workspace-kicker">Review Center</p>
          <h1>综合复盘 Agent</h1>
          <p>
            聚合训练报告、能力画像、简历风险、长期记忆和 RAG 证据，用多角色 Agent 给出下一步训练路线。
          </p>
        </div>
        <div className="agent-review-hero-metrics">
          <span>AgentRun</span>
          <span>RagTrace</span>
          <span>Memory</span>
        </div>
      </section>

      <Card className="agent-review-generate-card">
        <div className="agent-review-generate">
          <div>
            <strong>生成综合复盘</strong>
            <span>高成本 AI 操作已做 Single-flight 防重复生成。</span>
          </div>
          <Space wrap>
            <Select
              value={scopeType}
              onChange={setScopeType}
              className="agent-review-scope"
              options={[
                { value: "RECENT_7_DAYS", label: "最近 7 天" },
                { value: "RECENT_30_DAYS", label: "最近 30 天" },
                { value: "RECENT_10", label: "最近 10 次训练" },
                { value: "ALL", label: "全部训练概览" },
              ]}
            />
            <Button
              type="primary"
              icon={<ReloadOutlined />}
              loading={generating}
              onClick={handleGenerate}
            >
              生成综合复盘
            </Button>
          </Space>
        </div>
        {currentReview?.confidence === "LOW" || totalEvidence === 0 ? (
          <Alert
            type="warning"
            showIcon
            message="当前证据较少，复盘会以低置信度生成。建议补一次项目拷打、八股训练或模拟面试。"
          />
        ) : null}
      </Card>

      {generating ? (
        <Card>
          <Skeleton active paragraph={{ rows: 6 }} />
        </Card>
      ) : currentReview ? (
        <>
          <Card className="agent-review-summary-card">
            <div className="agent-review-summary-head">
              <div>
                <h2>最新综合复盘</h2>
                <p>{formatDateTime(currentReview.createdAt)}</p>
              </div>
              <Space wrap>
                <span className={confidenceClass(currentReview.confidence)}>
                  {CONFIDENCE_LABELS[currentReview.confidence] || currentReview.confidence}
                </span>
                <span className="agent-review-pill">
                  {SAMPLE_LABELS[currentReview.sampleQuality || ""] || currentReview.sampleQuality}
                </span>
              </Space>
            </div>
            <div className="agent-review-score-row">
              <div className="agent-review-score">
                <strong>{score ?? "--"}</strong>
                <span>综合分</span>
              </div>
              <p>{currentReview.summary}</p>
            </div>
            <div className="agent-review-source-strip">
              <Tag>项目报告 {String(source.projectReportCount ?? 0)}</Tag>
              <Tag>八股报告 {String(source.questionReportCount ?? 0)}</Tag>
              <Tag>模拟面试 {String(source.mockInterviewReportCount ?? 0)}</Tag>
              <Tag>能力快照 {String(source.abilitySnapshotCount ?? 0)}</Tag>
              <Tag>Memory {String(source.memoryItemCount ?? 0)}</Tag>
              <Tag>RAG 文章 {String(source.ragArticleCount ?? 0)}</Tag>
              <Tag>用户文档 {String(source.ragDocumentCount ?? 0)}</Tag>
            </div>
          </Card>

          <section className="agent-review-two-col">
            <Card className="agent-review-section-card">
              <div className="agent-review-section-title">能力雷达图</div>
              <RadarChart review={currentReview} />
            </Card>
            <ListPanel title="关键发现" items={currentReview.keyFindings} empty="暂无关键发现" />
          </section>

          <section className="agent-review-grid">
            <ListPanel
              title="反复薄弱点"
              items={currentReview.recurringWeaknesses}
              empty="暂无稳定薄弱点"
            />
            <ListPanel title="问题归因" items={currentReview.causeAnalysis} empty="暂无归因" />
            <ListPanel
              title="简历风险"
              items={currentReview.resumeRisks}
              empty="上传并分析简历后会展示风险提醒"
            />
            <ListPanel
              title="Memory 更新"
              items={currentReview.memoryUpdates}
              empty="本次没有新的长期记忆沉淀"
            />
          </section>

          <Card className="agent-review-section-card">
            <div className="agent-review-section-title">高风险回答</div>
            {currentReview.highRiskAnswers?.length ? (
              <div className="agent-review-risk-list">
                {currentReview.highRiskAnswers.map((item, index) => (
                  <article key={`${item.question}-${index}`}>
                    <div>
                      <Tag color={item.riskLevel === "HIGH" ? "red" : "orange"}>
                        {item.riskLevel || "RISK"}
                      </Tag>
                      <Tag>{item.riskType || "回答风险"}</Tag>
                    </div>
                    <h3>{item.question}</h3>
                    <p>{item.answerSummary}</p>
                    <p>{item.reason}</p>
                    <strong>{item.betterDirection}</strong>
                    {item.relatedAction ? (
                      <Button type="link" onClick={() => handleAction(item.relatedAction!)}>
                        去处理 <ArrowRightOutlined />
                      </Button>
                    ) : null}
                  </article>
                ))}
              </div>
            ) : (
              <Empty description="暂未识别高风险回答" />
            )}
          </Card>

          <section className="agent-review-two-col">
            <Card className="agent-review-section-card">
              <div className="agent-review-section-title">模拟面试阶段表现</div>
              {currentReview.stagePerformance?.length ? (
                <div className="agent-review-stage-list">
                  {currentReview.stagePerformance.map((stage, index) => (
                    <div key={`${stage.stage}-${index}`}>
                      <strong>{stage.stageName || stage.stage}</strong>
                      <span>{stage.score ?? "--"} 分</span>
                      <p>{stage.comment}</p>
                    </div>
                  ))}
                </div>
              ) : (
                <Empty description="暂无模拟面试阶段数据" />
              )}
            </Card>

            <Card className="agent-review-section-card">
              <div className="agent-review-section-title">问答回放</div>
              {currentReview.qaReplay?.length ? (
                <div className="agent-review-replay-list">
                  {currentReview.qaReplay.map((item, index) => (
                    <details key={`${item.sourceType}-${index}`}>
                      <summary>
                        <span>{item.sourceType}</span>
                        {item.question}
                      </summary>
                      <p>{item.answerSummary}</p>
                      {item.aiFollowUp ? <p>追问：{item.aiFollowUp}</p> : null}
                      {item.mainProblems?.length ? (
                        <div>
                          {item.mainProblems.map((problem) => (
                            <Tag key={problem}>{problem}</Tag>
                          ))}
                        </div>
                      ) : null}
                      <strong>{item.suggestedExpression}</strong>
                    </details>
                  ))}
                </div>
              ) : (
                <Empty description="暂无可回放问答摘要" />
              )}
            </Card>
          </section>

          <Card className="agent-review-section-card">
            <div className="agent-review-section-title">下一步行动</div>
            {currentReview.nextActions?.length ? (
              <div className="agent-review-action-grid">
                {currentReview.nextActions.map((action, index) => (
                  <article key={`${action.title}-${index}`} className="agent-review-action-card">
                    <div>
                      <Tag>{ACTION_LABELS[action.type] || action.type}</Tag>
                      <Tag>优先级 {action.priority || index + 1}</Tag>
                    </div>
                    <h3>{action.title}</h3>
                    <p>{action.reason}</p>
                    <Button onClick={() => handleAction(action)}>
                      进入 <ArrowRightOutlined />
                    </Button>
                  </article>
                ))}
              </div>
            ) : (
              <Empty description="暂无行动建议" />
            )}
          </Card>

          <section className="agent-review-two-col">
            <RecommendationList
              title="推荐学习"
              items={currentReview.recommendedArticles}
              onOpen={openPath}
            />
            <RecommendationList
              title="推荐训练"
              items={currentReview.recommendedTrainings}
              onOpen={openPath}
            />
          </section>
        </>
      ) : (
        <Card>
          <Empty description="还没有复盘记录，点击生成复盘开始第一次综合分析。" />
        </Card>
      )}

      <section className="agent-review-section">
        <div className="agent-review-section-title">复盘历史</div>
        {loadingHistory ? (
          <Card>
            <Skeleton active paragraph={{ rows: 3 }} />
          </Card>
        ) : history.length ? (
          <div className="agent-review-history">
            {history.map((item) => (
              <button
                key={item.id}
                type="button"
                className="agent-review-history-card"
                onClick={() => handleOpenHistory(item.id)}
                disabled={detailLoadingId === item.id}
              >
                <strong>{formatDateTime(item.createdAt)}</strong>
                <span>{CONFIDENCE_LABELS[item.confidence] || item.confidence}</span>
                <p>{item.summary || "查看这次复盘详情"}</p>
              </button>
            ))}
          </div>
        ) : (
          <Card>
            <Empty description="暂无历史复盘" />
          </Card>
        )}
      </section>
    </PageShell>
  );
}

export default AgentReviewPage;
