import { Button, Progress, Space } from "antd";
import { useCallback, useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  getAbilityDimensions,
  getInsightOverview,
  getLearningRecommendations,
  getRecentTrend,
  getWeaknessInsights,
} from "../../api/insight";
import EmptyState from "../../components/EmptyState";
import PageHeader from "../../components/PageHeader";
import PageShell from "../../components/PageShell";
import SurfaceCard from "../../components/SurfaceCard";
import type {
  AbilityDimension,
  AbilityTrend,
  InsightOverview,
  LearningRecommendation,
  RecentTrend,
  WeaknessInsight,
} from "../../types/insight";
import "./index.css";

const DEFAULT_LIMIT = 10;

const TREND_LABELS: Record<AbilityTrend, string> = {
  UP: "提升",
  DOWN: "下降",
  FLAT: "稳定",
  UNKNOWN: "暂无趋势",
};

const TREND_CLASSES: Record<AbilityTrend, string> = {
  UP: "is-up",
  DOWN: "is-down",
  FLAT: "is-flat",
  UNKNOWN: "is-unknown",
};

const TRAINING_TYPE_LABELS: Record<string, string> = {
  QUESTION_REPORT: "八股训练",
  PROJECT_REPORT: "项目训练",
};

function formatScore(value?: number | null) {
  if (value === null || value === undefined || Number.isNaN(value)) {
    return "暂无";
  }
  return `${value} / 100`;
}

function formatDate(value?: string | null) {
  if (!value) {
    return "—";
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  return date.toLocaleDateString();
}

function formatDateTime(value?: string | null, emptyText = "—") {
  if (!value) {
    return emptyText;
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  return date.toLocaleString();
}

function clampPercent(value?: number | null) {
  if (value === null || value === undefined || Number.isNaN(value)) {
    return 0;
  }
  return Math.min(Math.max(value, 0), 100);
}

function formatRelevance(value?: number | null) {
  if (value === null || value === undefined || Number.isNaN(value)) {
    return "相关度暂无";
  }
  return `相关度 ${Math.round(value * 100)}%`;
}

function SkeletonCard({ lines = 2 }: { lines?: number }) {
  return (
    <SurfaceCard className="insights-skeleton-card">
      <div className="insights-skeleton insights-skeleton--short" />
      {Array.from({ length: lines }).map((_, index) => (
        <div key={index} className="insights-skeleton" />
      ))}
    </SurfaceCard>
  );
}

function OverviewSkeleton() {
  return (
    <div className="insights-overview-grid">
      {Array.from({ length: 6 }).map((_, index) => (
        <SkeletonCard key={index} lines={1} />
      ))}
    </div>
  );
}

function GridSkeleton({ count = 3 }: { count?: number }) {
  return (
    <div className="insights-dimension-grid">
      {Array.from({ length: count }).map((_, index) => (
        <SkeletonCard key={index} lines={3} />
      ))}
    </div>
  );
}

function InsightsPage() {
  const navigate = useNavigate();
  const [overview, setOverview] = useState<InsightOverview | null>(null);
  const [dimensions, setDimensions] = useState<AbilityDimension[]>([]);
  const [weaknesses, setWeaknesses] = useState<WeaknessInsight[]>([]);
  const [trends, setTrends] = useState<RecentTrend[]>([]);
  const [recommendations, setRecommendations] = useState<
    LearningRecommendation[]
  >([]);
  const [loading, setLoading] = useState(false);
  const [overviewError, setOverviewError] = useState(false);
  const [dimensionError, setDimensionError] = useState(false);
  const [weaknessError, setWeaknessError] = useState(false);
  const [trendError, setTrendError] = useState(false);
  const [recommendationError, setRecommendationError] = useState(false);
  const [pageError, setPageError] = useState(false);

  const loadInsights = useCallback(async () => {
    setLoading(true);
    setPageError(false);
    setOverviewError(false);
    setDimensionError(false);
    setWeaknessError(false);
    setTrendError(false);
    setRecommendationError(false);

    const results = await Promise.allSettled([
      getInsightOverview(),
      getAbilityDimensions(),
      getWeaknessInsights(DEFAULT_LIMIT),
      getRecentTrend(DEFAULT_LIMIT),
      getLearningRecommendations(),
    ]);

    const [
      overviewResult,
      dimensionResult,
      weaknessResult,
      trendResult,
      recommendationResult,
    ] = results;
    const coreResults = results.slice(0, 4);
    const successCount = coreResults.filter(
      (result) => result.status === "fulfilled",
    ).length;

    if (successCount === 0) {
      setOverview(null);
      setDimensions([]);
      setWeaknesses([]);
      setTrends([]);
      setRecommendations([]);
      setOverviewError(true);
      setDimensionError(true);
      setWeaknessError(true);
      setTrendError(true);
      setRecommendationError(true);
      setPageError(true);
      setLoading(false);
      return;
    }

    if (overviewResult.status === "fulfilled") {
      setOverview(overviewResult.value);
    } else {
      setOverview(null);
      setOverviewError(true);
    }

    if (dimensionResult.status === "fulfilled") {
      setDimensions(dimensionResult.value || []);
    } else {
      setDimensions([]);
      setDimensionError(true);
    }

    if (weaknessResult.status === "fulfilled") {
      setWeaknesses(weaknessResult.value || []);
    } else {
      setWeaknesses([]);
      setWeaknessError(true);
    }

    if (trendResult.status === "fulfilled") {
      setTrends(trendResult.value || []);
    } else {
      setTrends([]);
      setTrendError(true);
    }

    if (recommendationResult.status === "fulfilled") {
      setRecommendations(recommendationResult.value || []);
    } else {
      setRecommendations([]);
      setRecommendationError(true);
    }

    setLoading(false);
  }, []);

  useEffect(() => {
    void loadInsights();
  }, [loadInsights]);

  const overviewMetrics = useMemo(() => {
    if (!overview) {
      return [];
    }
    const items = [
      {
        label: "总训练次数",
        value: String(overview.totalTrainingCount),
      },
      {
        label: "项目训练",
        value: String(overview.projectTrainingCount),
      },
      {
        label: "八股训练",
        value: String(overview.questionTrainingCount),
      },
      {
        label: "平均分",
        value: formatScore(overview.averageScore),
        muted: overview.averageScore === null,
      },
      {
        label: "最近平均分",
        value: formatScore(overview.recentAverageScore),
        muted: overview.recentAverageScore === null,
      },
      {
        label: "最近训练时间",
        value: formatDateTime(overview.lastTrainingAt, "暂无训练"),
        muted: !overview.lastTrainingAt,
      },
    ];

    if (overview.bestDimension) {
      items.push({
        label: "最强维度",
        value: overview.bestDimension,
      });
    }

    if (overview.weakestDimension) {
      items.push({
        label: "待加强维度",
        value: overview.weakestDimension,
      });
    }

    return items;
  }, [overview]);

  const hasTrainingData = overview?.totalTrainingCount
    ? overview.totalTrainingCount > 0
    : false;
  const handleRetry = () => {
    void loadInsights();
  };

  return (
    <PageShell className="insights-page">
      <PageHeader
        title="成长洞察"
        description="从训练记录中沉淀能力画像，查看你的项目表达、八股知识和追问应对能力变化。"
        actions={
          <div className="insights-header-actions">
            <div className="insights-header-note">
              已接入 RAG 知识库，推荐会随训练数据持续更新。
            </div>
            <Button type="primary" onClick={() => navigate("/agent-review")}>
              生成复盘
            </Button>
          </div>
        }
      />

      {pageError && !loading ? (
          <EmptyState
            description="成长洞察加载失败，请稍后重试。"
            action={
              <Button type="primary" onClick={handleRetry}>
                重新加载
              </Button>
            }
          />
        ) : overview && !hasTrainingData && !overviewError && !loading ? (
          <EmptyState
            description="还没有足够的训练数据"
            action={
              <Space>
                <Button type="primary" onClick={() => navigate("/projects")}>
                  去项目档案
                </Button>
                <Button onClick={() => navigate("/questions")}>
                  开始八股问答
                </Button>
              </Space>
            }
          />
        ) : (
          <div className="insights-content">
            <section className="insights-section">
              <div className="cc-section-header">
                <div>
                  <h2 className="cc-section-title">总览</h2>
                  <p className="cc-section-description">
                    近期训练的数量、表现与核心趋势。
                  </p>
                </div>
              </div>
              {loading && !overview ? (
                <OverviewSkeleton />
              ) : overviewError ? (
                <EmptyState
                  description="成长洞察总览加载失败"
                  action={
                    <Button type="primary" onClick={handleRetry}>
                      重新加载
                    </Button>
                  }
                />
              ) : (
                <div className="insights-overview-grid">
                  {overviewMetrics.map((item) => (
                    <SurfaceCard key={item.label} className="insights-metric">
                      <span className="insights-metric__label">
                        {item.label}
                      </span>
                      <span
                        className={`insights-metric__value${
                          item.muted ? " is-muted" : ""
                        }`}
                      >
                        {item.value}
                      </span>
                    </SurfaceCard>
                  ))}
                </div>
              )}
            </section>

            <section className="insights-section">
              <div className="cc-section-header">
                <div>
                  <h2 className="cc-section-title">能力维度</h2>
                  <p className="cc-section-description">
                    根据训练报告沉淀的能力表现与趋势。
                  </p>
                </div>
              </div>
              {loading && dimensions.length === 0 ? (
                <GridSkeleton />
              ) : dimensionError ? (
                <EmptyState
                  description="能力维度加载失败"
                  action={
                    <Button type="primary" onClick={handleRetry}>
                      重新加载
                    </Button>
                  }
                />
              ) : dimensions.length === 0 ? (
                <EmptyState description="暂无能力维度数据" />
              ) : (
                <div className="insights-dimension-grid">
                  {dimensions.map((item) => {
                    const scoreText =
                      item.score === null
                        ? "暂无评分"
                        : formatScore(item.score);
                    const trendLabel =
                      TREND_LABELS[item.trend] || TREND_LABELS.UNKNOWN;
                    const trendClass =
                      TREND_CLASSES[item.trend] || TREND_CLASSES.UNKNOWN;

                    return (
                      <SurfaceCard
                        key={item.dimensionCode}
                        className="insights-dimension-card"
                      >
                        <div className="insights-dimension-header">
                          <div>
                            <div className="insights-dimension-name">
                              {item.dimensionName}
                            </div>
                            {item.category ? (
                              <div className="insights-dimension-category">
                                {item.category}
                              </div>
                            ) : null}
                          </div>
                          <span
                            className={`insights-trend insights-trend--${trendClass}`}
                          >
                            {trendLabel}
                          </span>
                        </div>
                        <div className="insights-dimension-score">
                          <span className="insights-dimension-score__value">
                            {scoreText}
                          </span>
                          <Progress
                            percent={clampPercent(item.score)}
                            showInfo={false}
                            strokeColor="var(--cc-accent)"
                            trailColor="var(--cc-surface-muted)"
                          />
                        </div>
                        <div className="insights-dimension-meta">
                          <span>近 {item.evidenceCount} 次训练记录</span>
                          <p>{item.latestEvidence || "暂无最新记录"}</p>
                        </div>
                      </SurfaceCard>
                    );
                  })}
                </div>
              )}
            </section>

            <section className="insights-section">
              <div className="cc-section-header">
                <div>
                  <h2 className="cc-section-title">高频薄弱点</h2>
                  <p className="cc-section-description">
                    反复出现的薄弱点，将用于后续智能学习推荐。
                  </p>
                </div>
              </div>
              {loading && weaknesses.length === 0 ? (
                <GridSkeleton />
              ) : weaknessError ? (
                <EmptyState
                  description="薄弱点加载失败"
                  action={
                    <Button type="primary" onClick={handleRetry}>
                      重新加载
                    </Button>
                  }
                />
              ) : weaknesses.length === 0 ? (
                <EmptyState description="暂无高频薄弱点" />
              ) : (
                <div className="insights-weakness-grid">
                  {weaknesses.map((item) => (
                    <SurfaceCard
                      key={`${item.keyword}-${item.latestAt}`}
                      className="insights-weakness-card"
                    >
                      <div className="insights-weakness-header">
                        <div className="insights-weakness-keyword">
                          {item.keyword}
                        </div>
                        <div className="insights-weakness-count">
                          {item.count} 次出现
                        </div>
                      </div>
                      {item.relatedDimension ? (
                        <div className="insights-weakness-dimension">
                          {item.relatedDimension}
                        </div>
                      ) : null}
                      <p className="insights-weakness-evidence">
                        {item.latestEvidence || "暂无最新记录"}
                      </p>
                      <div className="insights-weakness-meta">
                        最近记录 · {formatDateTime(item.latestAt)}
                      </div>
                    </SurfaceCard>
                  ))}
                </div>
              )}
            </section>

            <section className="insights-section">
              <div className="cc-section-header">
                <div>
                  <h2 className="cc-section-title">最近训练趋势</h2>
                  <p className="cc-section-description">
                    按训练报告记录的最近表现变化。
                  </p>
                </div>
              </div>
              {loading && trends.length === 0 ? (
                <GridSkeleton count={2} />
              ) : trendError ? (
                <EmptyState
                  description="最近训练趋势加载失败"
                  action={
                    <Button type="primary" onClick={handleRetry}>
                      重新加载
                    </Button>
                  }
                />
              ) : trends.length === 0 ? (
                <EmptyState description="暂无训练趋势记录" />
              ) : (
                <div className="insights-trend-list">
                  {trends.map((item) => (
                    <SurfaceCard
                      key={`${item.date}-${item.sourceId}`}
                      className="insights-trend-card"
                    >
                      <div className="insights-trend-header">
                        <div>
                          <div className="insights-trend-date">
                            {formatDate(item.date)}
                          </div>
                          <div className="insights-trend-type">
                            {TRAINING_TYPE_LABELS[item.trainingType] ||
                              "训练记录"}
                            {item.dimensionName
                              ? ` · ${item.dimensionName}`
                              : ""}
                          </div>
                        </div>
                        <div className="insights-trend-score">
                          {formatScore(item.score)}
                        </div>
                      </div>
                      <div className="insights-trend-meta">
                        记录时间 · {formatDateTime(item.createdAt)}
                      </div>
                    </SurfaceCard>
                  ))}
                </div>
              )}
            </section>

            <section className="insights-section">
              <div className="cc-section-header">
                <div>
                  <h2 className="cc-section-title">智能学习推荐</h2>
                  <p className="cc-section-description">
                    基于你的能力画像和知识库检索结果，为你推荐下一步优先学习的知识卡片。
                  </p>
                </div>
              </div>
              {loading && recommendations.length === 0 ? (
                <GridSkeleton count={2} />
              ) : recommendationError ? (
                <SurfaceCard className="insights-rag-card">
                  <div>
                    <div className="insights-rag-title">推荐暂时不可用</div>
                    <p className="insights-rag-desc">
                      成长洞察主体仍可使用，稍后可以重新加载推荐。
                    </p>
                  </div>
                  <Button onClick={handleRetry}>重新加载</Button>
                </SurfaceCard>
              ) : recommendations.length === 0 ? (
                <SurfaceCard className="insights-rag-card">
                  <div>
                    <div className="insights-rag-title">
                      还没有足够的推荐依据
                    </div>
                    <p className="insights-rag-desc">
                      完成更多训练后，CodeCoach AI
                      会根据你的薄弱点和知识库内容推荐学习卡片。
                    </p>
                  </div>
                  <Button type="primary" onClick={() => navigate("/learn")}>
                    前往知识学习
                  </Button>
                </SurfaceCard>
              ) : (
                <div className="insights-recommendation-grid">
                  {recommendations.map((item) => (
                    <SurfaceCard
                      key={`${item.articleId}-${item.section || "full"}`}
                      className="insights-recommendation-card"
                    >
                      <div className="insights-recommendation-tags">
                        {item.category ? <span>{item.category}</span> : null}
                        {item.topicName ? <span>{item.topicName}</span> : null}
                        {item.section ? <span>{item.section}</span> : null}
                      </div>
                      <div className="insights-recommendation-title">
                        {item.title}
                      </div>
                      <p className="insights-recommendation-reason">
                        {item.reason}
                      </p>
                      <div className="insights-recommendation-meta">
                        <span>{formatRelevance(item.score)}</span>
                        {item.evidence ? <span>{item.evidence}</span> : null}
                      </div>
                      <div className="insights-recommendation-actions">
                        <Button
                          type="primary"
                          onClick={() => navigate(item.targetPath)}
                        >
                          开始学习
                        </Button>
                        <Button onClick={() => navigate(item.targetPath)}>
                          开始专项训练
                        </Button>
                      </div>
                    </SurfaceCard>
                  ))}
                </div>
              )}
            </section>
          </div>
        )}
    </PageShell>
  );
}

export default InsightsPage;
