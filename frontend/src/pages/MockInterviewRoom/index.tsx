import { Button, Popconfirm, Progress, Result, Space, Tag, Typography, message } from "antd";
import { useCallback, useEffect, useRef, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import {
  finishMockInterview,
  getMockInterview,
  submitMockInterviewAnswer,
  submitMockInterviewAnswerStream,
} from "../../api/mockInterview";
import AiThinkingIndicator from "../../components/training/AiThinkingIndicator";
import ChatInputBox from "../../components/training/ChatInputBox";
import { useChatAutoScroll } from "../../hooks/useChatAutoScroll";
import type {
  MockInterviewAnswerResponse,
  MockInterviewMessage,
  MockInterviewSessionDetail,
  MockInterviewStage,
} from "../../types/mockInterview";
import "./index.css";

const stageLabels: Record<MockInterviewStage, string> = {
  OPENING: "开场",
  RESUME_PROJECT: "简历项目",
  TECHNICAL_FUNDAMENTAL: "技术基础",
  PROJECT_DEEP_DIVE: "项目深挖",
  SCENARIO_DESIGN: "场景设计",
  WRAP_UP: "总结反问",
};

function createClientRequestId() {
  if (typeof crypto !== "undefined" && "randomUUID" in crypto) {
    return crypto.randomUUID();
  }
  return `${Date.now()}-${Math.random().toString(16).slice(2)}`;
}

function normalizeMessages(messages: MockInterviewMessage[]) {
  return messages.map((item, index) => ({
    ...item,
    messageId: item.messageId ?? `message-${index}`,
  }));
}

function MockInterviewRoomPage() {
  const { sessionId } = useParams();
  const navigate = useNavigate();
  const endRef = useRef<HTMLDivElement | null>(null);
  const [detail, setDetail] = useState<MockInterviewSessionDetail | null>(null);
  const [messages, setMessages] = useState<MockInterviewMessage[]>([]);
  const [answer, setAnswer] = useState("");
  const [loading, setLoading] = useState(false);
  const [sending, setSending] = useState(false);
  const [streamingId, setStreamingId] = useState<string | null>(null);
  const [streamingContent, setStreamingContent] = useState("");
  const [stageText, setStageText] = useState("");
  const [loadError, setLoadError] = useState(false);

  const refresh = useCallback(async () => {
    if (!sessionId) return null;
    const data = await getMockInterview(sessionId);
    setDetail(data);
    setMessages(normalizeMessages(data.messages || []));
    return data;
  }, [sessionId]);

  useEffect(() => {
    setLoading(true);
    refresh()
      .catch(() => {
        setLoadError(true);
        message.error("模拟面试加载失败");
      })
      .finally(() => setLoading(false));
  }, [refresh]);

  useChatAutoScroll(endRef, [messages, streamingContent, stageText, sending]);

  const appendResponse = (payload: MockInterviewAnswerResponse) => {
    setMessages((prev) => [
      ...prev,
      payload.userAnswer,
      ...(payload.nextQuestion ? [payload.nextQuestion] : []),
    ]);
    setDetail((prev) =>
      prev
        ? {
            ...prev,
            status: payload.finished ? "FINISHED" : prev.status,
            currentRound: payload.finished
              ? prev.maxRound
              : Math.min(prev.currentRound + 1, prev.maxRound),
            currentStage: payload.currentStage || prev.currentStage,
            currentStageGoal: payload.currentStageGoal || prev.currentStageGoal,
            currentStageProgress:
              payload.currentStageProgress ?? prev.currentStageProgress,
            currentStageSuggestedRounds:
              payload.currentStageSuggestedRounds ??
              prev.currentStageSuggestedRounds,
            reportId: payload.reportId || prev.reportId,
          }
        : prev,
    );
    if (payload.finished) {
      navigate(`/mock-interviews/${sessionId}/report`);
    }
  };

  const submit = async () => {
    const text = answer.trim();
    if (!sessionId || !text || sending || detail?.status === "FINISHED") return;
    setSending(true);
    setAnswer("");
    const requestId = createClientRequestId();
    const optimisticUser: MockInterviewMessage = {
      messageId: `user-${requestId}`,
      role: "USER",
      messageType: "USER_ANSWER",
      stage: detail?.currentStage || "OPENING",
      content: text,
      roundNo: detail?.currentRound || 1,
    };
    const optimisticAi: MockInterviewMessage = {
      messageId: `ai-${requestId}`,
      role: "ASSISTANT",
      messageType: "AI_QUESTION",
      stage: detail?.currentStage || "OPENING",
      content: "",
      roundNo: (detail?.currentRound || 1) + 1,
    };
    setMessages((prev) => [...prev, optimisticUser, optimisticAi]);
    setStreamingId(String(optimisticAi.messageId));
    setStreamingContent("");
    let started = false;
    try {
      await submitMockInterviewAnswerStream(sessionId, text, requestId, {
        onStart: (event) => {
          started = true;
          setStageText(event.message || "AI 面试官正在思考");
        },
        onStage: (event) => setStageText(event.message || "正在生成追问"),
        onDelta: (content) => setStreamingContent((prev) => prev + content),
        onDone: (payload) => {
          setMessages((prev) =>
            prev.filter(
              (item) =>
                item.messageId !== optimisticUser.messageId &&
                item.messageId !== optimisticAi.messageId,
            ),
          );
          setStreamingId(null);
          setStreamingContent("");
          setStageText("");
          appendResponse(payload);
        },
        onError: (event) => {
          throw new Error(event.message || "生成失败");
        },
      });
    } catch {
      try {
        if (!started) {
          const payload = await submitMockInterviewAnswer(sessionId, text);
          setMessages((prev) =>
            prev.filter(
              (item) =>
                item.messageId !== optimisticUser.messageId &&
                item.messageId !== optimisticAi.messageId,
            ),
          );
          appendResponse(payload);
        } else {
          await refresh();
          message.warning("流式连接中断，已刷新当前面试状态");
        }
      } finally {
        setStreamingId(null);
        setStreamingContent("");
        setStageText("");
      }
    } finally {
      setSending(false);
    }
  };

  const finish = async () => {
    if (!sessionId) return;
    const response = await finishMockInterview(sessionId);
    if (response.reportId) {
      navigate(`/mock-interviews/${sessionId}/report`);
    }
  };

  if (loadError || !sessionId) {
    return (
      <div className="mock-room-page">
        <Result
          status="error"
          title="模拟面试不存在或无权限访问"
          extra={<Button onClick={() => navigate("/mock-interviews")}>返回</Button>}
        />
      </div>
    );
  }

  return (
    <div className="mock-room-page">
      <header className="mock-room-header">
        <div>
          <Typography.Text className="mock-room-kicker">Mock Interview Room</Typography.Text>
          <Typography.Title level={3}>Java 后端真实模拟面试</Typography.Title>
          <Space wrap>
            <Tag color="blue">{stageLabels[detail?.currentStage || "OPENING"]}</Tag>
            <Tag>
              {detail?.currentRound || 1}/{detail?.maxRound || 6} 轮
            </Tag>
            <Tag>{detail?.targetRole}</Tag>
          </Space>
          <div className="mock-room-plan">
            <Typography.Text strong>
              {detail?.currentStageGoal || "正在加载阶段目标"}
            </Typography.Text>
            <Typography.Paragraph>
              本次模拟面试将覆盖：{detail?.plan?.coverageSummary || "自我介绍、简历项目、技术基础、项目深挖、场景设计、总结"}
            </Typography.Paragraph>
            <div className="mock-room-plan__progress">
              <span>
                阶段进度 {detail?.currentStageProgress ?? 0}/
                {detail?.currentStageSuggestedRounds ?? 1}
              </span>
              <Progress
                percent={Math.min(
                  100,
                  Math.round(
                    ((detail?.currentStageProgress ?? 0) /
                      Math.max(detail?.currentStageSuggestedRounds ?? 1, 1)) *
                      100,
                  ),
                )}
                showInfo={false}
              />
            </div>
          </div>
        </div>
        <Popconfirm title="结束后将生成综合报告" onConfirm={finish}>
          <Button danger disabled={!detail || detail.status === "FINISHED"}>
            主动结束
          </Button>
        </Popconfirm>
      </header>

      <main className="mock-room-thread" aria-busy={loading || sending}>
        {messages.map((item, index) => {
          const isStreaming = streamingId && item.messageId === streamingId;
          return (
            <article
              key={String(item.messageId ?? index)}
              className={`mock-room-message ${item.role === "USER" ? "is-user" : "is-ai"}`}
            >
              <div className="mock-room-message__meta">
                <span>{item.role === "USER" ? "我" : "AI 面试官"}</span>
                <Tag>{stageLabels[item.stage]}</Tag>
              </div>
              <p>{isStreaming ? streamingContent : item.content}</p>
              {isStreaming && stageText ? <AiThinkingIndicator stage={stageText} /> : null}
            </article>
          );
        })}
        {sending && !streamingId ? <AiThinkingIndicator stage="AI 面试官正在思考" /> : null}
        <div ref={endRef} />
      </main>

      <footer className="mock-room-input">
        <ChatInputBox
          value={answer}
          disabled={sending || detail?.status === "FINISHED"}
          loading={sending}
          placeholder="像真实面试一样回答这一问，不需要写标准答案..."
          onChange={setAnswer}
          onSend={submit}
        />
        {detail?.status === "FINISHED" ? (
          <Button type="primary" onClick={() => navigate(`/mock-interviews/${sessionId}/report`)}>
            查看综合报告
          </Button>
        ) : null}
      </footer>
    </div>
  );
}

export default MockInterviewRoomPage;
