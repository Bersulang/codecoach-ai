import {
  ArrowRightOutlined,
  BulbOutlined,
  CloseOutlined,
  CompassOutlined,
  LoadingOutlined,
  SendOutlined,
} from "@ant-design/icons";
import { Modal, message as antdMessage } from "antd";
import { useEffect, useMemo, useRef, useState } from "react";
import type { KeyboardEvent } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { executeAgentTool } from "../../api/agentTool";
import { sendGuideMessage } from "../../api/guide";
import { useChatAutoScroll } from "../../hooks/useChatAutoScroll";
import type { GuideActionCard, GuideActionType } from "../../types/guide";
import "./GuideWidget.css";

type GuideMessage = {
  id: string;
  role: "assistant" | "user";
  content: string;
  actions?: GuideActionCard[];
};

const quickQuestions = [
  "我下一步该做什么？",
  "我想练项目",
  "开始模拟面试",
  "我想练八股",
  "我想准备简历",
  "帮我看看最近弱点",
  "这个页面怎么用？",
];

const actionPathMap: Record<GuideActionType, string> = {
  GO_DASHBOARD: "/dashboard",
  GO_PROJECTS: "/projects",
  GO_QUESTIONS: "/questions",
  GO_LEARN: "/learn",
  GO_INSIGHTS: "/insights",
  GO_DOCUMENTS: "/documents",
  GO_RESUMES: "/resumes",
  GO_AGENT_REVIEW: "/agent-review",
  GO_HISTORY: "/history",
  GO_PROFILE: "/profile",
  GO_MOCK_INTERVIEWS: "/mock-interviews",
  START_PROJECT_TRAINING: "/projects",
  START_QUESTION_TRAINING: "/questions",
  START_MOCK_INTERVIEW: "/mock-interviews",
  GET_ABILITY_SUMMARY: "/insights",
  GET_RECENT_TRAINING_SUMMARY: "/history",
  GET_RESUME_RISK_SUMMARY: "/resumes",
  SEARCH_KNOWLEDGE: "/learn",
  SEARCH_USER_DOCUMENTS: "/documents",
  ANALYZE_RESUME: "/resumes",
  GENERATE_AGENT_REVIEW: "/agent-review",
  CREATE_PROJECT_FROM_RESUME: "/resumes",
  LOGIN: "/login",
};

function isAllowedAction(actionType: string): actionType is GuideActionType {
  return Object.prototype.hasOwnProperty.call(actionPathMap, actionType);
}

function resolveToolName(action: GuideActionCard) {
  return action.toolName || action.actionType;
}

function createId() {
  return `${Date.now()}-${Math.random().toString(16).slice(2)}`;
}

function GuideWidget() {
  const navigate = useNavigate();
  const location = useLocation();
  const inputRef = useRef<HTMLTextAreaElement | null>(null);
  const messagesEndRef = useRef<HTMLDivElement | null>(null);
  const [open, setOpen] = useState(false);
  const [input, setInput] = useState("");
  const [loading, setLoading] = useState(false);
  const [showThinking, setShowThinking] = useState(false);
  const [executingAction, setExecutingAction] = useState<string | null>(null);
  const [messages, setMessages] = useState<GuideMessage[]>([
    {
      id: "welcome",
      role: "assistant",
      content:
        "我是 CodeCoach Guide。你可以问我当前页面怎么用，或让我帮你找到下一步训练动作。",
    },
  ]);

  const currentPath = useMemo(
    () => `${location.pathname}${location.search}`,
    [location.pathname, location.search],
  );

  useChatAutoScroll(messagesEndRef, [messages, loading, showThinking, open]);

  useEffect(() => {
    if (!loading) {
      setShowThinking(false);
      return;
    }
    const timer = window.setTimeout(() => setShowThinking(true), 380);
    return () => window.clearTimeout(timer);
  }, [loading]);

  const submitMessage = async (rawText?: string) => {
    const text = (rawText ?? input).trim();
    if (!text || loading) {
      return;
    }
    setInput("");
    setLoading(true);
    setMessages((prev) => [
      ...prev,
      { id: createId(), role: "user", content: text },
    ]);

    try {
      const response = await sendGuideMessage({
        message: text,
        currentPath,
        pageTitle: document.title,
      });
      setMessages((prev) => [
        ...prev,
        {
          id: createId(),
          role: "assistant",
          content: response.answer,
          actions: response.actions,
        },
      ]);
    } catch {
      setMessages((prev) => [
        ...prev,
        {
          id: createId(),
          role: "assistant",
          content: "训练向导暂时没有响应，你可以稍后再试。",
        },
      ]);
    } finally {
      setLoading(false);
      window.setTimeout(() => inputRef.current?.focus(), 0);
    }
  };

  const executeToolAction = async (
    action: GuideActionCard,
    confirmed: boolean,
  ) => {
    const toolName = resolveToolName(action);
    if (!isAllowedAction(toolName)) {
      return;
    }
    setExecutingAction(toolName);
    try {
      const result = await executeAgentTool({
        toolName,
        agentType: "GUIDE",
        runId: action.runId,
        traceId: action.traceId,
        confirmed,
        params: action.params || {},
      });
      if (!result.success) {
        antdMessage.error(result.message || "Tool 执行失败，请稍后重试。");
        return;
      }
      if (result.message) {
        antdMessage.success(result.message);
      }
      if (result.displayType === "SUMMARY" && result.message) {
        const content = result.message;
        setMessages((prev) => [
          ...prev,
          {
            id: createId(),
            role: "assistant",
            content,
            actions: result.nextActions,
          },
        ]);
      }
      if (result.targetPath) {
        navigate(result.targetPath);
        setOpen(false);
      }
    } catch {
      antdMessage.error("训练入口暂时没有打开，请稍后重试或先进入对应页面。");
    } finally {
      setExecutingAction(null);
    }
  };

  const handleAction = (action: GuideActionCard) => {
    const toolName = resolveToolName(action);
    if (!isAllowedAction(toolName)) {
      return;
    }
    if (action.requiresConfirmation) {
      Modal.confirm({
        title: action.title,
        content: action.description || "确认后将执行这个训练动作。",
        okText: "确认执行",
        cancelText: "取消",
        onOk: () => executeToolAction(action, true),
      });
      return;
    }
    void executeToolAction(action, false);
  };

  const handleKeyDown = (event: KeyboardEvent<HTMLTextAreaElement>) => {
    if (event.key !== "Enter" || event.shiftKey) {
      return;
    }
    event.preventDefault();
    void submitMessage();
  };

  return (
    <div className="guide-widget" aria-live="polite">
      {open ? (
        <section className="guide-panel" aria-label="CodeCoach Guide">
          <header className="guide-panel__header">
            <div className="guide-panel__title-wrap">
              <span className="guide-panel__mark">
                <BulbOutlined />
              </span>
              <div>
                <h2>CodeCoach Guide</h2>
                <p>帮你找到下一步训练动作</p>
              </div>
            </div>
            <button
              type="button"
              className="guide-icon-button"
              onClick={() => setOpen(false)}
              aria-label="收起训练向导"
            >
              <CloseOutlined />
            </button>
          </header>

          <div className="guide-panel__messages">
            {messages.map((message) => (
              <div
                key={message.id}
                className={`guide-message guide-message--${message.role}`}
              >
                <div className="guide-message__bubble">{message.content}</div>
                {message.actions && message.actions.length > 0 ? (
                  <div className="guide-actions">
                    {message.actions
                      .filter((action) => isAllowedAction(resolveToolName(action)))
                      .map((action) => {
                        const toolName = resolveToolName(action);
                        return (
                          <button
                            type="button"
                            className="guide-action-card"
                            key={`${message.id}-${toolName}`}
                            disabled={Boolean(executingAction)}
                            onClick={() => handleAction(action)}
                          >
                            <span>
                              <strong>{action.title}</strong>
                              <small>{action.description}</small>
                            </span>
                            {executingAction === toolName ? (
                              <LoadingOutlined />
                            ) : (
                              <ArrowRightOutlined />
                            )}
                          </button>
                        );
                      })}
                  </div>
                ) : null}
              </div>
            ))}
            {loading ? (
              <div className="guide-thinking">
                <LoadingOutlined />
                <span>
                  {showThinking
                    ? "思考中，正在结合页面和训练状态判断下一步"
                    : "正在判断最合适的训练入口"}
                </span>
              </div>
            ) : null}
            <div ref={messagesEndRef} />
          </div>

          <div className="guide-quick">
            {quickQuestions.map((question) => (
              <button
                type="button"
                key={question}
                disabled={loading}
                onClick={() => submitMessage(question)}
              >
                {question}
              </button>
            ))}
          </div>

          <form
            className="guide-input"
            onSubmit={(event) => {
              event.preventDefault();
              void submitMessage();
            }}
          >
            <textarea
              ref={inputRef}
              value={input}
              rows={2}
              maxLength={240}
              disabled={loading}
              placeholder="问我下一步该练什么..."
              onChange={(event) => setInput(event.target.value)}
              onKeyDown={handleKeyDown}
            />
            <button
              type="submit"
              disabled={loading || !input.trim()}
              aria-label="发送给训练向导"
            >
              <SendOutlined />
            </button>
          </form>
        </section>
      ) : (
        <button
          type="button"
          className="guide-fab"
          onClick={() => setOpen(true)}
          aria-label="问问训练向导"
          title="问问训练向导"
        >
          <CompassOutlined />
          <span>Guide</span>
        </button>
      )}
    </div>
  );
}

export default GuideWidget;
