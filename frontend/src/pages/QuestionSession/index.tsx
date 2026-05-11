import {
  Button,
  Input,
  Popconfirm,
  Result,
  Space,
  Tag,
  Typography,
  message,
} from "antd";
import { useEffect, useMemo, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import {
  finishQuestionSession,
  getQuestionSessionDetail,
  submitQuestionAnswer,
} from "../../api/question";
import EmptyState from "../../components/EmptyState";
import PageHeader from "../../components/PageHeader";
import PageShell from "../../components/PageShell";
import SurfaceCard from "../../components/SurfaceCard";
import type { InterviewDifficulty } from "../../types/interview";
import type {
  QuestionAnswerResponse,
  QuestionMessage,
  QuestionMessageType,
  QuestionSessionDetail,
} from "../../types/question";
import "./index.css";

const { TextArea } = Input;

const MESSAGE_TYPE_LABELS: Record<QuestionMessageType, string> = {
  AI_QUESTION: "AI 问题",
  USER_ANSWER: "我的回答",
  AI_FEEDBACK: "AI 反馈",
  AI_REFERENCE_ANSWER: "参考答案",
  AI_FOLLOW_UP: "AI 追问",
  SYSTEM_NOTICE: "系统提示",
};

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

function formatStatus(status?: string) {
  if (!status) {
    return "未知";
  }
  if (status === "FINISHED") {
    return "已结束";
  }
  if (status === "IN_PROGRESS") {
    return "进行中";
  }
  return status;
}

function normalizeMessages(messages: QuestionMessage[]) {
  return messages.map((messageItem, index) => ({
    ...messageItem,
    content:
      typeof messageItem.content === "string"
        ? messageItem.content
        : String(messageItem.content ?? ""),
    messageId: messageItem.messageId ?? messageItem.id ?? `msg-${index}`,
  }));
}

function buildMessage(
  raw: QuestionMessage | string | null | undefined,
  fallbackType: QuestionMessageType,
  fallbackId: string,
  roundNo?: number,
): QuestionMessage | null {
  if (!raw) {
    return null;
  }

  if (typeof raw === "string") {
    return {
      messageId: fallbackId,
      messageType: fallbackType,
      content: raw,
      roundNo,
    };
  }

  return {
    messageId: raw.messageId ?? raw.id ?? fallbackId,
    messageType: raw.messageType ?? fallbackType,
    content:
      typeof raw.content === "string" ? raw.content : String(raw.content ?? ""),
    roundNo: raw.roundNo ?? roundNo,
    createdAt: raw.createdAt,
    role: raw.role,
    score: raw.score,
  };
}

function getMessageClass(type: QuestionMessageType) {
  if (type === "USER_ANSWER") {
    return "question-message is-user";
  }
  if (type === "AI_FEEDBACK") {
    return "question-message is-feedback";
  }
  if (type === "AI_REFERENCE_ANSWER") {
    return "question-message is-reference";
  }
  if (type === "AI_FOLLOW_UP") {
    return "question-message is-followup";
  }
  if (type === "SYSTEM_NOTICE") {
    return "question-message is-notice";
  }
  return "question-message is-question";
}

function QuestionSessionPage() {
  const navigate = useNavigate();
  const { sessionId } = useParams();
  const [detail, setDetail] = useState<QuestionSessionDetail | null>(null);
  const [messages, setMessages] = useState<QuestionMessage[]>([]);
  const [loading, setLoading] = useState(false);
  const [sending, setSending] = useState(false);
  const [finishing, setFinishing] = useState(false);
  const [loadError, setLoadError] = useState(false);
  const [answer, setAnswer] = useState("");

  const isFinished = detail?.status === "FINISHED";

  useEffect(() => {
    if (!sessionId) {
      return;
    }

    let active = true;
    setLoading(true);
    setLoadError(false);
    getQuestionSessionDetail(sessionId)
      .then((data) => {
        if (!active) {
          return;
        }
        setDetail(data);
        setMessages(normalizeMessages(data.messages || []));
      })
      .catch(() => {
        if (!active) {
          return;
        }
        setLoadError(true);
        message.error("八股训练会话加载失败");
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
  }, [sessionId]);

  const handleAppend = (payload: QuestionAnswerResponse) => {
    setMessages((prev) => {
      const next = [...prev];
      const baseRound = detail?.currentRound ?? 0;

      const userMessage = buildMessage(
        payload.userAnswer,
        "USER_ANSWER",
        `user-${Date.now()}`,
        baseRound,
      );
      if (userMessage) {
        next.push(userMessage);
      }

      const feedbackMessage = buildMessage(
        payload.aiFeedback,
        "AI_FEEDBACK",
        `feedback-${Date.now()}`,
        baseRound,
      );
      if (feedbackMessage) {
        next.push(feedbackMessage);
      }

      const referenceMessage = buildMessage(
        payload.referenceAnswer,
        "AI_REFERENCE_ANSWER",
        `reference-${Date.now()}`,
        baseRound,
      );
      if (referenceMessage) {
        next.push(referenceMessage);
      }

      const followupMessage = buildMessage(
        payload.nextQuestion,
        "AI_FOLLOW_UP",
        `followup-${Date.now()}`,
        baseRound + 1,
      );
      if (followupMessage) {
        next.push(followupMessage);
      }
      return next;
    });

    setDetail((prev) => {
      if (!prev) {
        return prev;
      }
      if (payload.finished) {
        return { ...prev, status: "FINISHED", totalScore: payload.totalScore };
      }
      if (payload.nextQuestion) {
        return {
          ...prev,
          currentRound: Math.min(prev.currentRound + 1, prev.maxRound),
        };
      }
      return prev;
    });
  };

  const handleSubmitAnswer = async () => {
    const trimmed = answer.trim();
    if (!trimmed) {
      message.warning("请先输入回答");
      return;
    }
    if (!sessionId || sending || loading || isFinished) {
      return;
    }
    setSending(true);
    try {
      const data = await submitQuestionAnswer(sessionId, { answer: trimmed });
      handleAppend(data);
      setAnswer("");

      if (data.finished) {
        if (typeof data.reportId === "number") {
          message.success("训练已完成，正在生成报告");
          navigate(`/question-reports/${data.reportId}`);
        } else {
          message.info("训练已完成，请稍后查看报告");
        }
      }
    } catch {
      // Errors are handled by the request interceptor.
    } finally {
      setSending(false);
    }
  };

  const handleFinish = async () => {
    if (!sessionId) {
      return;
    }
    setFinishing(true);
    try {
      const data = await finishQuestionSession(sessionId);
      if (typeof data?.reportId === "number") {
        navigate(`/question-reports/${data.reportId}`);
      } else {
        message.error("结束训练失败，请稍后重试");
      }
    } catch {
      // Errors are handled by the request interceptor.
    } finally {
      setFinishing(false);
    }
  };

  const metaItems = useMemo(
    () =>
      detail
        ? [
            { label: "知识分类", value: detail.category },
            { label: "知识点", value: detail.topicName },
            { label: "目标岗位", value: detail.targetRole },
            { label: "难度", value: formatDifficulty(detail.difficulty) },
            {
              label: "进度",
              value: `${detail.currentRound}/${detail.maxRound}`,
            },
            { label: "状态", value: formatStatus(detail.status) },
          ]
        : [],
    [detail],
  );

  if (!sessionId || (Number.isNaN(Number(sessionId)) && !sessionId)) {
    return (
      <PageShell className="question-session-page">
        <Result
          status="error"
          title="训练会话不存在"
          subTitle="请从知识点选择页重新进入训练。"
          extra={
            <Button type="primary" onClick={() => navigate("/questions")}>
              返回知识点选择
            </Button>
          }
        />
      </PageShell>
    );
  }

  if (loadError) {
    return (
      <PageShell className="question-session-page">
        <Result
          status="error"
          title="训练会话加载失败"
          subTitle="训练会话不存在或无权限访问。"
          extra={
            <Button type="primary" onClick={() => navigate("/questions")}>
              返回知识点选择
            </Button>
          }
        />
      </PageShell>
    );
  }

  const trimmedAnswer = answer.trim();

  return (
    <PageShell className="question-session-page">
      <PageHeader
        title="八股训练"
        description="围绕核心知识点进行结构化追问与反馈。"
        actions={
          <Space>
            <Tag color={isFinished ? "default" : "blue"}>
              {formatStatus(detail?.status)}
            </Tag>
            <Popconfirm
              title="确认结束训练吗？"
              okText="结束"
              cancelText="取消"
              onConfirm={handleFinish}
              disabled={finishing}
            >
              <Button danger loading={finishing} disabled={!detail || isFinished}>
                结束训练
              </Button>
            </Popconfirm>
          </Space>
        }
      />

      <SurfaceCard className="question-summary" loading={loading}>
        <div className="question-summary__grid">
          {metaItems.map((item) => (
            <div key={item.label}>
              <span className="question-summary__label">{item.label}</span>
              <div className="question-summary__value">{item.value}</div>
            </div>
          ))}
        </div>
        {detail?.topicDescription ? (
          <div className="question-summary__desc">
            {detail.topicDescription}
          </div>
        ) : null}
      </SurfaceCard>

      <div className="question-thread">
        {loading ? (
          <SurfaceCard className="question-message" loading />
        ) : messages.length === 0 ? (
          <EmptyState description="暂无训练消息" />
        ) : (
          messages.map((messageItem, index) => (
            <div
              key={String(
                messageItem.messageId ?? messageItem.id ?? `msg-${index}`,
              )}
              className={getMessageClass(messageItem.messageType)}
            >
              <div className="question-message__meta">
                <span className="question-message__type">
                  {MESSAGE_TYPE_LABELS[messageItem.messageType] ||
                    messageItem.messageType}
                </span>
                {messageItem.roundNo ? (
                  <span className="question-message__round">
                    第 {messageItem.roundNo} 轮
                  </span>
                ) : null}
              </div>
              <div className="question-message__content">
                {messageItem.content}
              </div>
            </div>
          ))
        )}
      </div>

      {detail?.status === "IN_PROGRESS" ? (
        <SurfaceCard className="question-composer" bordered={false}>
          <div className="question-composer__header">
            <Typography.Text className="question-status">
              {sending
                ? "AI 正在分析你的回答..."
                : "请输入你的回答，AI 将生成反馈与下一道问题。"}
            </Typography.Text>
            <Typography.Text className="question-status-hint">
              内容越具体，反馈越准确。
            </Typography.Text>
          </div>
          <TextArea
            value={answer}
            onChange={(event) => setAnswer(event.target.value)}
            autoSize={{ minRows: 3, maxRows: 6 }}
            placeholder="输入你的回答..."
            disabled={loading || sending}
          />
          <div className="question-composer__footer">
            <Button
              type="primary"
              onClick={handleSubmitAnswer}
              loading={sending}
              disabled={!trimmedAnswer}
            >
              提交回答
            </Button>
          </div>
        </SurfaceCard>
      ) : (
        <SurfaceCard className="question-finished" bordered={false}>
          <Typography.Text className="question-status">
            训练已结束，你可以查看报告或返回知识点选择页。
          </Typography.Text>
          <div className="question-composer__footer">
            <Button onClick={() => navigate("/questions")}>返回选择</Button>
            {typeof detail?.totalScore === "number" ? (
              <Button type="primary" disabled>
                本次得分 {detail.totalScore}
              </Button>
            ) : null}
          </div>
        </SurfaceCard>
      )}
    </PageShell>
  );
}

export default QuestionSessionPage;
