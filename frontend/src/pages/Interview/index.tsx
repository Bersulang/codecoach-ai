import {
  Button,
  Popconfirm,
  Result,
  Space,
  Tag,
  Typography,
  message,
} from "antd";
import { useEffect, useMemo, useRef, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import {
  finishInterview,
  getInterviewSession,
  submitInterviewAnswer,
  submitInterviewAnswerStream,
} from "../../api/interview";
import EmptyState from "../../components/EmptyState";
import PageHeader from "../../components/PageHeader";
import PageShell from "../../components/PageShell";
import SurfaceCard from "../../components/SurfaceCard";
import AiThinkingIndicator from "../../components/training/AiThinkingIndicator";
import ChatInputBox from "../../components/training/ChatInputBox";
import type {
  AnswerResponse,
  InterviewDifficulty,
  InterviewMessage,
  InterviewMessageType,
  InterviewSessionDetail,
} from "../../types/interview";
import "../../styles/interview.css";

const MESSAGE_TYPE_LABELS: Record<InterviewMessageType, string> = {
  AI_QUESTION: "AI 问题",
  USER_ANSWER: "我的回答",
  AI_FEEDBACK: "AI 反馈",
  AI_FOLLOW_UP: "AI 追问",
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

function normalizeMessages(messages: InterviewMessage[]) {
  return messages.map((message, index) => ({
    ...message,
    content:
      typeof message.content === "string"
        ? message.content
        : String(message.content ?? ""),
    messageId: message.messageId ?? message.id ?? `msg-${index}`,
  }));
}

function buildMessage(
  raw: InterviewMessage | string | null | undefined,
  fallbackType: InterviewMessageType,
  fallbackId: string,
  roundNo?: number,
): InterviewMessage | null {
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
  };
}

function formatStatus(status?: string) {
  if (!status) {
    return "未知";
  }
  if (status === "FINISHED") {
    return "已结束";
  }
  if (status === "RUNNING") {
    return "进行中";
  }
  return status;
}

function getMessageClass(type: InterviewMessageType) {
  if (type === "USER_ANSWER") {
    return "interview-message is-user";
  }
  if (type === "AI_FEEDBACK") {
    return "interview-message is-feedback";
  }
  if (type === "AI_FOLLOW_UP") {
    return "interview-message is-followup";
  }
  return "interview-message is-question";
}

function createClientRequestId() {
  if (typeof crypto !== "undefined" && "randomUUID" in crypto) {
    return crypto.randomUUID();
  }
  return `${Date.now()}-${Math.random().toString(16).slice(2)}`;
}

function InterviewPage() {
  const navigate = useNavigate();
  const { sessionId } = useParams();
  const [detail, setDetail] = useState<InterviewSessionDetail | null>(null);
  const [messages, setMessages] = useState<InterviewMessage[]>([]);
  const [loading, setLoading] = useState(false);
  const [sending, setSending] = useState(false);
  const [finishing, setFinishing] = useState(false);
  const [loadError, setLoadError] = useState(false);
  const [answer, setAnswer] = useState("");
  const [streamingMessageId, setStreamingMessageId] = useState<string | null>(
    null,
  );
  const [streamingStage, setStreamingStage] = useState("");
  const [streamingContent, setStreamingContent] = useState("");
  const threadEndRef = useRef<HTMLDivElement | null>(null);

  const isFinished = detail?.status === "FINISHED";

  useEffect(() => {
    if (!sessionId) {
      return;
    }

    let active = true;
    setLoading(true);
    setLoadError(false);
    getInterviewSession(sessionId)
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
        message.error("训练会话加载失败");
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

  useEffect(() => {
    threadEndRef.current?.scrollIntoView({ behavior: "smooth", block: "end" });
  }, [messages, streamingStage, streamingContent]);

  const handleAppend = (payload: AnswerResponse) => {
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

      const questionMessage = buildMessage(
        payload.nextQuestion,
        "AI_FOLLOW_UP",
        `question-${Date.now()}`,
        baseRound + 1,
      );
      if (questionMessage) {
        next.push(questionMessage);
      }
      return next;
    });

    setDetail((prev) => {
      if (!prev) {
        return prev;
      }
      if (payload.finished) {
        return { ...prev, status: "FINISHED" };
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
    if (!trimmed || !sessionId || sending || loading || isFinished) {
      return;
    }
    setSending(true);
    setAnswer("");
    const currentRound = detail?.currentRound ?? 0;
    const clientRequestId = createClientRequestId();
    const optimisticUserId = `stream-user-${clientRequestId}`;
    const optimisticAiId = `stream-ai-${clientRequestId}`;
    let receivedStart = false;
    setStreamingMessageId(optimisticAiId);
    setStreamingStage("正在连接 AI...");
    setStreamingContent("");
    setMessages((prev) => [
      ...prev,
      {
        messageId: optimisticUserId,
        messageType: "USER_ANSWER",
        content: trimmed,
        roundNo: currentRound,
        role: "USER",
      },
      {
        messageId: optimisticAiId,
        messageType: "AI_FEEDBACK",
        content: "",
        roundNo: currentRound,
        role: "ASSISTANT",
      },
    ]);
    try {
      await submitInterviewAnswerStream(sessionId, trimmed, clientRequestId, {
        onStart: (event) => {
          receivedStart = true;
          setStreamingStage(event.message || "AI 正在分析你的项目表达");
        },
        onStage: (event) => {
          setStreamingStage(event.message || "AI 正在生成回复");
        },
        onDelta: (content) => {
          setStreamingContent((prev) => prev + content);
        },
        onDone: (payload) => {
          setMessages((prev) =>
            prev.filter(
              (item) =>
                item.messageId !== optimisticUserId &&
                item.messageId !== optimisticAiId,
            ),
          );
          setStreamingMessageId(null);
          setStreamingStage("");
          setStreamingContent("");
          handleAppend(payload);
          if (payload.finished) {
            if (typeof payload.reportId === "number") {
              message.success("训练已完成，正在生成报告");
              navigate(`/reports/${payload.reportId}`);
            } else {
              message.info("训练已完成，请稍后在历史记录中查看报告");
            }
          }
        },
        onError: (event) => {
          throw new Error(event.message || "生成失败，请稍后重试");
        },
      });
    } catch {
      if (!receivedStart) {
        message.info("网络不稳定，正在切换为普通加载模式...");
        try {
          const data = await submitInterviewAnswer(sessionId, { answer: trimmed });
          setMessages((prev) =>
            prev.filter(
              (item) =>
                item.messageId !== optimisticUserId &&
                item.messageId !== optimisticAiId,
            ),
          );
          setStreamingMessageId(null);
          setStreamingStage("");
          setStreamingContent("");
          handleAppend(data);
          if (data.finished) {
            if (typeof data.reportId === "number") {
              message.success("训练已完成，正在生成报告");
              navigate(`/reports/${data.reportId}`);
            } else {
              message.info("训练已完成，请稍后在历史记录中查看报告");
            }
          }
        } catch {
          setMessages((prev) =>
            prev.filter(
              (item) =>
                item.messageId !== optimisticUserId &&
                item.messageId !== optimisticAiId,
            ),
          );
          setStreamingMessageId(null);
        }
      } else {
        setStreamingStage("连接中断，AI 可能仍在生成。请稍后刷新训练记录查看结果。");
        setStreamingContent("");
        message.warning("连接中断，请稍后刷新训练记录查看结果");
      }
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
      const data = await finishInterview(sessionId);
      if (typeof data?.reportId === "number") {
        navigate(`/reports/${data.reportId}`);
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
            { label: "项目名称", value: detail.projectName },
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
      <PageShell className="interview-page">
        <Result
          status="error"
          title="训练会话不存在"
          subTitle="请从项目列表重新进入训练。"
          extra={
            <Button type="primary" onClick={() => navigate("/projects")}>
              返回项目列表
            </Button>
          }
        />
      </PageShell>
    );
  }

  if (loadError) {
    return (
      <PageShell className="interview-page">
        <Result
          status="error"
          title="训练会话加载失败"
          subTitle="训练会话不存在或无权限访问。"
          extra={
            <Button type="primary" onClick={() => navigate("/projects")}>
              返回项目列表
            </Button>
          }
        />
      </PageShell>
    );
  }

  return (
    <PageShell className="interview-page">
      <PageHeader
        title="Interview Studio"
        description="沉浸式 AI 项目训练室，逐轮提升表达与思维。"
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
              <Button
                danger
                loading={finishing}
                disabled={!detail || isFinished}
              >
                结束训练
              </Button>
            </Popconfirm>
          </Space>
        }
      />

      <SurfaceCard className="interview-summary" loading={loading}>
        <div className="interview-summary__grid">
          {metaItems.map((item) => (
            <div key={item.label}>
              <span className="interview-summary__label">{item.label}</span>
              <div className="interview-summary__value">{item.value}</div>
            </div>
          ))}
        </div>
      </SurfaceCard>

      <div className="interview-thread">
        {loading ? (
          <SurfaceCard className="interview-message" loading />
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
              <div className="interview-message__meta">
                <span className="interview-message__type">
                  {MESSAGE_TYPE_LABELS[messageItem.messageType]}
                </span>
                {messageItem.roundNo ? (
                  <span className="interview-message__round">
                    第 {messageItem.roundNo} 轮
                  </span>
                ) : null}
              </div>
              <div className="interview-message__content">
                {messageItem.messageId === streamingMessageId ? (
                  <AiThinkingIndicator
                    stage={streamingStage}
                    content={streamingContent}
                  />
                ) : (
                  messageItem.content
                )}
              </div>
            </div>
          ))
        )}
        <div ref={threadEndRef} />
      </div>

      <SurfaceCard className="interview-composer" bordered={false}>
        <div className="interview-composer__header">
          <Typography.Text className="interview-status">
            {isFinished
              ? "训练已结束，你可以查看报告或返回项目列表。"
              : "请输入你的回答，AI 将生成反馈与下一道问题。"}
          </Typography.Text>
          {!isFinished ? (
            <Typography.Text className="interview-status-hint">
              AI 回复可能需要数秒，请勿关闭页面
            </Typography.Text>
          ) : null}
        </div>
        <ChatInputBox
          value={answer}
          onChange={setAnswer}
          onSend={handleSubmitAnswer}
          placeholder="请填写你的回答"
          disabled={isFinished || loading || sending}
          loading={sending}
        />
        <div className="interview-composer__footer">
          {detail ? (
            <span className="interview-status">
              当前进度 {detail.currentRound}/{detail.maxRound}
            </span>
          ) : null}
        </div>
      </SurfaceCard>
    </PageShell>
  );
}

export default InterviewPage;
