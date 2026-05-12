import { Alert, Button, Card, Empty, Select, Skeleton, Space, Tag, message } from "antd";
import { useCallback, useEffect, useState } from "react";
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
} from "../../types/agentReview";
import "./index.css";

const CONFIDENCE_LABELS = {
  LOW: "低置信度",
  MEDIUM: "中等置信度",
  HIGH: "高置信度",
};

const ACTION_LABELS: Record<string, string> = {
  LEARN: "知识学习",
  TRAIN_QUESTION: "八股训练",
  TRAIN_PROJECT: "项目拷打",
  REVIEW_RESUME: "简历训练",
  UPLOAD_DOCUMENT: "文档上传",
};

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

function listOrEmpty(items: string[] | undefined, empty = "暂无内容") {
  if (!items?.length) {
    return <Empty description={empty} />;
  }
  return (
    <ul>
      {items.map((item, index) => (
        <li key={`${item}-${index}`}>{item}</li>
      ))}
    </ul>
  );
}

function confidenceClass(confidence?: string) {
  return `agent-review-confidence agent-review-confidence--${(confidence || "LOW").toLowerCase()}`;
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
        const detail = await getAgentReviewDetail(data[0].id);
        setCurrentReview(detail);
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

  const handleGenerate = async () => {
    setGenerating(true);
    try {
      const review = await generateAgentReview(scopeType);
      setCurrentReview(review);
      message.success("复盘已生成");
      await loadHistory();
    } catch {
      message.error("复盘生成失败，可以稍后重试");
    } finally {
      setGenerating(false);
    }
  };

  const handleOpenHistory = async (reviewId: number) => {
    setDetailLoadingId(reviewId);
    try {
      const detail = await getAgentReviewDetail(reviewId);
      setCurrentReview(detail);
    } catch {
      message.error("复盘详情加载失败");
    } finally {
      setDetailLoadingId(null);
    }
  };

  const handleAction = (action: AgentReviewNextAction) => {
    if (action.targetPath) {
      navigate(action.targetPath);
    }
  };

  const source = currentReview?.sourceSnapshot || {};
  const hasNoData =
    Number(source.projectReportCount || 0) +
      Number(source.questionReportCount || 0) +
      Number(source.abilitySnapshotCount || 0) ===
    0;

  return (
    <PageShell className="agent-review-page">
      <section className="agent-review-hero">
        <p className="workspace-kicker">复盘 Agent</p>
        <h1>把最近训练变成下一步动作。</h1>
        <p>
          复盘 Agent 不只是总结一次训练，而是帮你从最近训练中找出反复出现的问题和下一步最该做的训练动作。
          它会结合训练报告、能力画像、简历风险点和知识库内容生成结构化复盘。
        </p>
      </section>

      <Card className="agent-review-generate-card">
        <div className="agent-review-generate">
          <div className="agent-review-generate__copy">
            <strong>生成复盘</strong>
            <span>第一版默认聚焦最近 10 次训练，并结合最近能力快照与简历风险。</span>
          </div>
          <Space wrap>
            <Select
              value={scopeType}
              onChange={setScopeType}
              style={{ width: 180 }}
              options={[
                { value: "RECENT_10", label: "最近 10 次训练" },
                { value: "RECENT_7_DAYS", label: "最近 7 天" },
              ]}
            />
            <Button type="primary" loading={generating} onClick={handleGenerate}>
              生成复盘
            </Button>
          </Space>
        </div>
        {currentReview?.confidence === "LOW" || hasNoData ? (
          <Alert
            type="warning"
            showIcon
            message="当前训练数据较少，建议先完成一次项目拷打和一次八股训练后再生成复盘。"
          />
        ) : null}
      </Card>

      {generating ? (
        <Card>
          <Skeleton active paragraph={{ rows: 5 }} />
        </Card>
      ) : currentReview ? (
        <>
          <Card className="agent-review-summary-card">
            <div className="agent-review-summary-head">
              <div>
                <h2>最新复盘</h2>
                <p>{formatDateTime(currentReview.createdAt)}</p>
              </div>
              <span className={confidenceClass(currentReview.confidence)}>
                {CONFIDENCE_LABELS[currentReview.confidence] || currentReview.confidence}
              </span>
            </div>
            <p className="agent-review-summary">{currentReview.summary}</p>
            <Space wrap>
              <Tag>项目报告 {String(source.projectReportCount ?? 0)}</Tag>
              <Tag>八股报告 {String(source.questionReportCount ?? 0)}</Tag>
              <Tag>能力快照 {String(source.abilitySnapshotCount ?? 0)}</Tag>
              <Tag>简历风险 {String(source.resumeRiskCount ?? 0)}</Tag>
              <Tag>知识文章 {String(source.ragArticleCount ?? 0)}</Tag>
            </Space>
          </Card>

          <section className="agent-review-grid">
            <Card className="agent-review-list-card">
              <div className="agent-review-section-title">关键发现</div>
              {listOrEmpty(currentReview.keyFindings)}
            </Card>
            <Card className="agent-review-list-card">
              <div className="agent-review-section-title">反复薄弱点</div>
              {listOrEmpty(currentReview.recurringWeaknesses)}
            </Card>
            <Card className="agent-review-list-card">
              <div className="agent-review-section-title">原因分析</div>
              {listOrEmpty(currentReview.causeAnalysis)}
            </Card>
            <Card className="agent-review-list-card">
              <div className="agent-review-section-title">简历风险提醒</div>
              {listOrEmpty(
                currentReview.resumeRisks,
                "上传并分析简历后，复盘会结合简历风险点",
              )}
            </Card>
          </section>

          <section className="agent-review-section">
            <div className="agent-review-section-title">下一步行动</div>
            {currentReview.nextActions?.length ? (
              <div className="agent-review-action-grid">
                {currentReview.nextActions.map((action, index) => (
                  <Card key={`${action.title}-${index}`} className="agent-review-action-card">
                    <div className="agent-review-action-meta">
                      <Tag>{ACTION_LABELS[action.type] || action.type}</Tag>
                      <Tag>优先级 {action.priority || index + 1}</Tag>
                    </div>
                    <h3>{action.title}</h3>
                    <p>{action.reason}</p>
                    <Button onClick={() => handleAction(action)}>去完成</Button>
                  </Card>
                ))}
              </div>
            ) : (
              <Empty description="暂无行动建议" />
            )}
          </section>
        </>
      ) : (
        <Card>
          <Empty description="还没有复盘记录，点击生成复盘开始第一次分析。" />
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
