import {
  Button,
  Card,
  Empty,
  Pagination,
  Select,
  Space,
  Spin,
  Tabs,
  Tag,
  Typography,
  message,
} from "antd";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { getInterviewSessions } from "../../api/interview";
import {
  finishQuestionSession,
  getQuestionSessionHistory,
} from "../../api/question";
import type {
  InterviewDifficulty,
  InterviewSessionHistoryItem,
  InterviewStatus,
} from "../../types/interview";
import type {
  QuestionSessionHistoryItem,
  QuestionSessionStatus,
} from "../../types/question";
import "../../styles/history.css";

const DEFAULT_PAGE_SIZE = 10;

const STATUS_OPTIONS: Array<{ label: string; value: InterviewStatus }> = [
  { label: "进行中", value: "IN_PROGRESS" },
  { label: "已结束", value: "FINISHED" },
  { label: "失败", value: "FAILED" },
];

const QUESTION_STATUS_OPTIONS: Array<{
  label: string;
  value: "ALL" | QuestionSessionStatus;
}> = [
  { label: "全部", value: "ALL" },
  { label: "进行中", value: "IN_PROGRESS" },
  { label: "已结束", value: "FINISHED" },
];

const DIFFICULTY_LABELS: Record<InterviewDifficulty, string> = {
  EASY: "入门引导",
  NORMAL: "常规面试",
  HARD: "深度拷打",
};

function formatDifficulty(value?: InterviewDifficulty) {
  if (!value) {
    return "—";
  }
  return DIFFICULTY_LABELS[value] ?? value;
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

function statusColor(status: InterviewStatus) {
  if (status === "FINISHED") {
    return "default";
  }
  if (status === "FAILED") {
    return "red";
  }
  return "blue";
}

function statusLabel(status: InterviewStatus) {
  if (status === "FINISHED") {
    return "已结束";
  }
  if (status === "FAILED") {
    return "失败";
  }
  return "进行中";
}

function questionStatusColor(status: QuestionSessionStatus) {
  if (status === "FINISHED") {
    return "default";
  }
  if (status === "FAILED") {
    return "red";
  }
  return "blue";
}

function questionStatusLabel(status: QuestionSessionStatus) {
  if (status === "FINISHED") {
    return "已结束";
  }
  if (status === "FAILED") {
    return "失败";
  }
  return "进行中";
}

function HistoryPage() {
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState("project");
  const [items, setItems] = useState<InterviewSessionHistoryItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [status, setStatus] = useState<InterviewStatus | undefined>(undefined);
  const [pageNum, setPageNum] = useState(1);
  const [pageSize, setPageSize] = useState(DEFAULT_PAGE_SIZE);
  const [total, setTotal] = useState(0);
  const [questionItems, setQuestionItems] = useState<
    QuestionSessionHistoryItem[]
  >([]);
  const [questionLoading, setQuestionLoading] = useState(false);
  const [questionStatus, setQuestionStatus] = useState<
    "ALL" | QuestionSessionStatus
  >("ALL");
  const [questionPageNum, setQuestionPageNum] = useState(1);
  const [questionPageSize, setQuestionPageSize] = useState(DEFAULT_PAGE_SIZE);
  const [questionTotal, setQuestionTotal] = useState(0);
  const [questionRefreshKey, setQuestionRefreshKey] = useState(0);
  const [questionReportGeneratingIds, setQuestionReportGeneratingIds] =
    useState<Set<number>>(() => new Set());
  const questionReportGeneratingRef = useRef<Set<number>>(new Set());

  useEffect(() => {
    if (activeTab !== "project") {
      return undefined;
    }
    let active = true;
    setLoading(true);
    getInterviewSessions({
      pageNum,
      pageSize,
      status,
    })
      .then((data) => {
        if (!active) {
          return;
        }
        setItems(data.records || []);
        setTotal(data.total || 0);
      })
      .catch(() => {
        if (!active) {
          return;
        }
        message.error("训练历史加载失败");
      })
      .finally(() => {
        if (!active) {
          return;
        }
        setLoading(false);
      });

    return () => {
      active = false;
    };
  }, [activeTab, pageNum, pageSize, status]);

  const loadQuestionHistory = useCallback(async (active = true) => {
    setQuestionLoading(true);
    try {
      const data = await getQuestionSessionHistory({
      pageNum: questionPageNum,
      pageSize: questionPageSize,
      status: questionStatus === "ALL" ? undefined : questionStatus,
      });
      if (!active) {
        return;
      }
      setQuestionItems(data.records || []);
      setQuestionTotal(data.total || 0);
    } catch {
      if (!active) {
        return;
      }
      message.error("八股训练历史加载失败");
    } finally {
      if (active) {
        setQuestionLoading(false);
      }
    }
  }, [questionPageNum, questionPageSize, questionStatus]);

  useEffect(() => {
    if (activeTab !== "question") {
      return undefined;
    }
    let active = true;
    void loadQuestionHistory(active);

    return () => {
      active = false;
    };
  }, [activeTab, loadQuestionHistory, questionRefreshKey]);

  useEffect(() => {
    if (activeTab !== "question") {
      return undefined;
    }
    const pendingItems = questionItems.filter(
      (item) => item.status === "FINISHED" && !item.reportId,
    );
    if (pendingItems.length === 0) {
      return undefined;
    }

    for (const item of pendingItems) {
      if (questionReportGeneratingRef.current.has(item.id)) {
        continue;
      }
      questionReportGeneratingRef.current.add(item.id);
      setQuestionReportGeneratingIds(
        (prev) => new Set([...Array.from(prev), item.id]),
      );
      void finishQuestionSession(item.id, { silentError: true })
        .then((finished) => {
          if (typeof finished?.reportId === "number") {
            setQuestionItems((prev) =>
              prev.map((current) =>
                current.id === item.id
                  ? {
                      ...current,
                      reportId: finished.reportId,
                      totalScore: finished.totalScore ?? current.totalScore,
                    }
                  : current,
              ),
            );
            questionReportGeneratingRef.current.delete(item.id);
            setQuestionReportGeneratingIds((prev) => {
              const next = new Set(prev);
              next.delete(item.id);
              return next;
            });
          }
        })
        .catch(() => {
          questionReportGeneratingRef.current.delete(item.id);
          setQuestionReportGeneratingIds((prev) => {
            const next = new Set(prev);
            next.delete(item.id);
            return next;
          });
        });
    }

    const timer = window.setInterval(() => {
      setQuestionRefreshKey((prev) => prev + 1);
    }, 5000);
    return () => window.clearInterval(timer);
  }, [activeTab, questionItems]);

  const projectEmptyState = useMemo(
    () => (
      <div className="history-empty">
        <Empty description="暂无训练历史" />
      </div>
    ),
    [],
  );

  const questionEmptyState = useMemo(
    () => (
      <div className="history-empty">
        <Empty description="暂无八股训练记录" />
      </div>
    ),
    [],
  );

  const showPagination = total > pageSize;
  const showQuestionPagination = questionTotal > questionPageSize;

  const handlePageChange = (nextPage: number, nextSize: number) => {
    setPageNum(nextPage);
    setPageSize(nextSize);
  };

  const handleStatusChange = (value: InterviewStatus | undefined) => {
    setStatus(value);
    setPageNum(1);
  };

  const handleQuestionStatusChange = (value: "ALL" | QuestionSessionStatus) => {
    setQuestionStatus(value);
    setQuestionPageNum(1);
  };

  const statusFilter =
    activeTab === "project" ? (
      <Select
        placeholder="筛选状态"
        allowClear
        style={{ width: 160 }}
        value={status}
        onChange={handleStatusChange}
        options={STATUS_OPTIONS}
      />
    ) : (
      <Select
        placeholder="筛选状态"
        style={{ width: 160 }}
        value={questionStatus}
        onChange={handleQuestionStatusChange}
        options={QUESTION_STATUS_OPTIONS}
      />
    );

  return (
    <div className="history-page">
      <div className="history-header">
        <div>
          <Typography.Title level={3} className="history-title">
            训练历史
          </Typography.Title>
          <Typography.Text className="history-subtitle">
            查看项目拷打和八股问答的训练记录。
          </Typography.Text>
        </div>
      </div>
      <Tabs
        className="history-tabs"
        activeKey={activeTab}
        onChange={setActiveTab}
        tabBarExtraContent={
          <div className="history-tab-extra">{statusFilter}</div>
        }
        items={[
          {
            key: "project",
            label: "项目训练",
            children: (
              <div className="history-tab">
                <div className="history-list">
                  {loading ? (
                    <Card className="history-card" loading />
                  ) : items.length === 0 ? (
                    projectEmptyState
                  ) : (
                    items.map((item) => (
                      <Card key={item.id} className="history-card">
                        <div className="history-card__header">
                          <div>
                            <Typography.Title
                              level={4}
                              className="history-card__title"
                            >
                              {item.projectName}
                            </Typography.Title>
                            <Space size={8} wrap>
                              <Tag color={statusColor(item.status)}>
                                {statusLabel(item.status)}
                              </Tag>
                              <Typography.Text type="secondary">
                                {item.targetRole}
                              </Typography.Text>
                            </Space>
                          </div>
                          <Space className="history-actions-group" wrap>
                            {item.status === "FINISHED" && item.reportId ? (
                              <Button
                                type="primary"
                                onClick={() =>
                                  navigate(`/reports/${item.reportId}`)
                                }
                              >
                                查看报告
                              </Button>
                            ) : null}
                            {item.status === "IN_PROGRESS" ? (
                              <Button
                                onClick={() =>
                                  navigate(`/interviews/${item.id}`)
                                }
                              >
                                继续训练
                              </Button>
                            ) : null}
                          </Space>
                        </div>
                        <div className="history-card__meta">
                          <div>
                            <span className="history-card__label">难度</span>
                            <span className="history-card__value">
                              {formatDifficulty(item.difficulty)}
                            </span>
                          </div>
                          <div>
                            <span className="history-card__label">进度</span>
                            <span className="history-card__value">
                              {item.currentRound}/{item.maxRound}
                            </span>
                          </div>
                          <div>
                            <span className="history-card__label">总评分</span>
                            <span className="history-card__value">
                              {item.totalScore ?? "—"}
                            </span>
                          </div>
                          <div>
                            <span className="history-card__label">
                              开始时间
                            </span>
                            <span className="history-card__value">
                              {formatDate(item.createdAt)}
                            </span>
                          </div>
                          <div>
                            <span className="history-card__label">
                              结束时间
                            </span>
                            <span className="history-card__value">
                              {formatDate(item.endedAt)}
                            </span>
                          </div>
                        </div>
                      </Card>
                    ))
                  )}
                </div>
                {showPagination && (
                  <div className="history-footer">
                    <Pagination
                      current={pageNum}
                      pageSize={pageSize}
                      total={total}
                      showSizeChanger
                      onChange={handlePageChange}
                    />
                  </div>
                )}
              </div>
            ),
          },
          {
            key: "question",
            label: "八股训练",
            children: (
              <div className="history-tab">
                <div className="history-list">
                  {questionLoading ? (
                    <Card className="history-card" loading />
                  ) : questionItems.length === 0 ? (
                    questionEmptyState
                  ) : (
                    questionItems.map((item) => (
                      <Card
                        key={item.id}
                        className="history-card history-card--question"
                      >
                        <div className="history-card__header">
                          <div>
                            <div className="history-card__kicker">
                              {item.category}
                            </div>
                            <Typography.Title
                              level={4}
                              className="history-card__title"
                            >
                              {item.topicName}
                            </Typography.Title>
                            <Space size={8} wrap>
                              <Tag color={questionStatusColor(item.status)}>
                                {questionStatusLabel(item.status)}
                              </Tag>
                              <Typography.Text type="secondary">
                                {item.targetRole}
                              </Typography.Text>
                            </Space>
                          </div>
                          <Space className="history-actions-group" wrap>
                            {item.status === "FINISHED" && item.reportId ? (
                              <Button
                                type="primary"
                                onClick={() =>
                                  navigate(`/question-reports/${item.reportId}`)
                                }
                              >
                                查看报告
                              </Button>
                            ) : null}
                            {item.status === "FINISHED" && !item.reportId ? (
                              <Button
                                disabled
                                className="history-generating-btn"
                              >
                                <Spin size="small" />
                                {questionReportGeneratingIds.has(item.id)
                                  ? "报告生成中"
                                  : "等待生成报告"}
                              </Button>
                            ) : null}
                            {item.status === "IN_PROGRESS" ? (
                              <Button
                                onClick={() =>
                                  navigate(`/question-sessions/${item.id}`)
                                }
                              >
                                继续训练
                              </Button>
                            ) : null}
                          </Space>
                        </div>
                        <div className="history-card__meta">
                          <div>
                            <span className="history-card__label">难度</span>
                            <span className="history-card__value">
                              {formatDifficulty(item.difficulty)}
                            </span>
                          </div>
                          <div>
                            <span className="history-card__label">进度</span>
                            <span className="history-card__value">
                              {item.currentRound}/{item.maxRound}
                            </span>
                          </div>
                          <div>
                            <span className="history-card__label">总评分</span>
                            <span className="history-card__value">
                              {item.totalScore ?? "—"}
                            </span>
                          </div>
                          <div>
                            <span className="history-card__label">
                              开始时间
                            </span>
                            <span className="history-card__value">
                              {formatDate(item.createdAt)}
                            </span>
                          </div>
                          <div>
                            <span className="history-card__label">
                              结束时间
                            </span>
                            <span className="history-card__value">
                              {formatDate(item.endedAt)}
                            </span>
                          </div>
                        </div>
                      </Card>
                    ))
                  )}
                </div>
                {showQuestionPagination && (
                  <div className="history-footer">
                    <Pagination
                      current={questionPageNum}
                      pageSize={questionPageSize}
                      total={questionTotal}
                      showSizeChanger
                      onChange={(nextPage, nextSize) => {
                        setQuestionPageNum(nextPage);
                        setQuestionPageSize(nextSize);
                      }}
                    />
                  </div>
                )}
              </div>
            ),
          },
        ]}
      />
    </div>
  );
}

export default HistoryPage;
