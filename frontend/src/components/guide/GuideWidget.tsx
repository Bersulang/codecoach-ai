import {
  ArrowRightOutlined,
  BulbOutlined,
  CloseOutlined,
  CompassOutlined,
  LoadingOutlined,
  SendOutlined,
} from "@ant-design/icons";
import { message as antdMessage } from "antd";
import { useEffect, useMemo, useRef, useState } from "react";
import type { KeyboardEvent } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { sendGuideMessage } from "../../api/guide";
import { createInterviewSession } from "../../api/interview";
import { getProjects } from "../../api/project";
import { createQuestionSession, getKnowledgeTopics } from "../../api/question";
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
  START_PROJECT_TRAINING: "/projects",
  START_QUESTION_TRAINING: "/questions",
  VIEW_LEARNING_ARTICLE: "/learn",
  UPLOAD_DOCUMENT: "/documents",
  ANALYZE_RESUME: "/resumes",
  GENERATE_REVIEW: "/agent-review",
  LOGIN: "/login",
};

const DEFAULT_TARGET_ROLE = "Java 后端实习";

function isAllowedAction(actionType: string): actionType is GuideActionType {
  return Object.prototype.hasOwnProperty.call(actionPathMap, actionType);
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
  const [executingAction, setExecutingAction] = useState<GuideActionType | null>(
    null,
  );
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

  useEffect(() => {
    if (!open) {
      return;
    }
    window.requestAnimationFrame(() => {
      messagesEndRef.current?.scrollIntoView({
        behavior: "smooth",
        block: "end",
      });
    });
  }, [messages, loading, showThinking, open]);

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

  const startProjectTraining = async () => {
    const data = await getProjects(
      { pageNum: 1, pageSize: 1 },
      { silentError: true },
    );
    const project = data.records?.[0];
    if (!project) {
      antdMessage.info("先创建一个项目档案，我再带你进入项目拷打。");
      navigate("/projects");
      return;
    }

    const session = await createInterviewSession({
      projectId: project.id,
      targetRole: DEFAULT_TARGET_ROLE,
      difficulty: "NORMAL",
    });
    if (!session?.sessionId) {
      throw new Error("Interview session creation failed");
    }
    navigate(`/interviews/${session.sessionId}`);
  };

  const startQuestionTraining = async () => {
    const data = await getKnowledgeTopics({
      pageNum: 1,
      pageSize: 1,
      difficulty: "NORMAL",
    });
    const topic = data.records?.[0];
    if (!topic) {
      antdMessage.info("暂时没有可用知识点，先进入八股问答页看看。");
      navigate("/questions");
      return;
    }

    const session = await createQuestionSession({
      topicId: topic.id,
      targetRole: DEFAULT_TARGET_ROLE,
      difficulty: "NORMAL",
    });
    if (!session?.sessionId) {
      throw new Error("Question session creation failed");
    }
    navigate(`/question-sessions/${session.sessionId}`);
  };

  const handleAction = async (action: GuideActionCard) => {
    if (!isAllowedAction(action.actionType)) {
      return;
    }
    const allowedPath = actionPathMap[action.actionType];
    if (action.targetPath !== allowedPath) {
      return;
    }
    setExecutingAction(action.actionType);
    try {
      if (action.actionType === "START_PROJECT_TRAINING") {
        await startProjectTraining();
      } else if (action.actionType === "START_QUESTION_TRAINING") {
        await startQuestionTraining();
      } else {
        navigate(allowedPath);
      }
      setOpen(false);
    } catch {
      antdMessage.error("训练入口暂时没有打开，请稍后重试或先进入对应页面。");
    } finally {
      setExecutingAction(null);
    }
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
                      .filter((action) => isAllowedAction(action.actionType))
                      .map((action) => (
                        <button
                          type="button"
                          className="guide-action-card"
                          key={`${message.id}-${action.actionType}`}
                          disabled={Boolean(executingAction)}
                          onClick={() => void handleAction(action)}
                        >
                          <span>
                            <strong>{action.title}</strong>
                            <small>{action.description}</small>
                          </span>
                          {executingAction === action.actionType ? (
                            <LoadingOutlined />
                          ) : (
                            <ArrowRightOutlined />
                          )}
                        </button>
                      ))}
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
