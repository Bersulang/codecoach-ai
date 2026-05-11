import { Button, Space } from "antd";
import { useEffect, useMemo, useState } from "react";
import ReactMarkdown from "react-markdown";
import { useNavigate, useParams } from "react-router-dom";
import { getKnowledgeArticleDetail } from "../../api/knowledge";
import EmptyState from "../../components/EmptyState";
import PageShell from "../../components/PageShell";
import SurfaceCard from "../../components/SurfaceCard";
import type { KnowledgeArticleDetail } from "../../types/knowledge";
import "./index.css";

type ArticleErrorState = "NOT_FOUND" | "CONTENT_MISSING" | "LOAD_ERROR" | null;

const LEARNING_PATH = [
  "一句话解释",
  "面试标准回答",
  "核心原理",
  "常见追问",
  "易错点",
  "项目表达模板",
  "推荐训练",
];

function formatDate(value?: string) {
  if (!value) {
    return "—";
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  return date.toLocaleDateString();
}

function LearnArticlePage() {
  const navigate = useNavigate();
  const { articleId } = useParams();
  const [article, setArticle] = useState<KnowledgeArticleDetail | null>(null);
  const [loading, setLoading] = useState(false);
  const [errorState, setErrorState] = useState<ArticleErrorState>(null);
  const [refreshKey, setRefreshKey] = useState(0);

  const parsedArticleId = useMemo(() => {
    const id = Number(articleId);
    return Number.isInteger(id) && id > 0 ? id : null;
  }, [articleId]);

  useEffect(() => {
    if (!parsedArticleId) {
      setErrorState("NOT_FOUND");
      setArticle(null);
      return;
    }

    let active = true;
    setLoading(true);
    setErrorState(null);
    setArticle(null);
    getKnowledgeArticleDetail(parsedArticleId)
      .then((data) => {
        if (!active) {
          return;
        }
        setArticle(data);
      })
      .catch((error: unknown) => {
        if (!active) {
          return;
        }
        const code = (error as { code?: number }).code;
        if (code === 5101) {
          setErrorState("NOT_FOUND");
        } else if (code === 5102) {
          setErrorState("CONTENT_MISSING");
        } else {
          setErrorState("LOAD_ERROR");
        }
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
  }, [parsedArticleId, refreshKey]);

  const metaText = useMemo(() => {
    if (!article) {
      return "—";
    }
    const version = article.version || "—";
    return `${version} · 更新于 ${formatDate(article.updatedAt)}`;
  }, [article]);

  const contentEmpty = !article?.content?.trim();

  const handleRetry = () => {
    setRefreshKey((prev) => prev + 1);
  };

  const handleStartTraining = () => {
    if (!article?.topicId) {
      return;
    }
    navigate(`/questions?topicId=${article.topicId}`);
  };

  const errorDescription =
    errorState === "NOT_FOUND"
      ? "知识文章不存在"
      : errorState === "CONTENT_MISSING"
        ? "知识文章内容暂不可用"
        : "文章加载失败，请稍后重试";

  return (
    <PageShell className="learn-article-page">
      <div className="learn-article-back">
        <Button type="text" onClick={() => navigate("/learn")}>
          返回知识学习
        </Button>
      </div>

      {loading ? (
        <div className="learn-article-layout">
          <div className="learn-article-main">
            <SurfaceCard className="learn-article-header-card" loading />
            <SurfaceCard className="learn-article-content-card" loading />
          </div>
          <aside className="learn-article-aside">
            <SurfaceCard className="learn-article-path" loading />
          </aside>
        </div>
      ) : errorState ? (
        <EmptyState
          description={errorDescription}
          action={
            <Space>
              {errorState === "LOAD_ERROR" ? (
                <Button type="primary" onClick={handleRetry}>
                  重新加载
                </Button>
              ) : null}
              <Button onClick={() => navigate("/learn")}>返回知识学习</Button>
            </Space>
          }
        />
      ) : article ? (
        <div className="learn-article-layout">
          <div className="learn-article-main">
            <SurfaceCard className="learn-article-header-card">
              <div className="learn-article-tags">
                <span className="learn-article-tag learn-article-tag--accent">
                  {article.category || "—"}
                </span>
                <span className="learn-article-tag">
                  {article.topicName || "—"}
                </span>
              </div>
              <h1 className="learn-article-title">{article.title}</h1>
              <p className="learn-article-summary">
                {article.summary || "暂无摘要"}
              </p>
              <div className="learn-article-meta">{metaText}</div>
              <div className="learn-article-actions">
                <Button type="primary" onClick={handleStartTraining}>
                  开始专项训练
                </Button>
                <Button onClick={() => navigate("/questions")}>
                  返回八股问答
                </Button>
              </div>
            </SurfaceCard>

            <SurfaceCard className="learn-article-content-card">
              {contentEmpty ? (
                <EmptyState description="这篇文章还在整理中" />
              ) : (
                <div className="learn-article-content">
                  <div className="learn-article-markdown">
                    <ReactMarkdown>{article.content}</ReactMarkdown>
                  </div>
                </div>
              )}
            </SurfaceCard>

            <div className="learn-article-bottom-actions">
              <Button onClick={() => navigate("/learn")}>返回知识学习</Button>
              <Button onClick={() => navigate("/questions")}>返回八股问答</Button>
              <Button type="primary" onClick={handleStartTraining}>
                开始专项训练
              </Button>
            </div>
          </div>
          <aside className="learn-article-aside">
            <SurfaceCard className="learn-article-path">
              <div className="learn-article-path__title">学习路径</div>
              <ol className="learn-article-path__list">
                {LEARNING_PATH.map((item, index) => (
                  <li key={item}>
                    <span>{index + 1}</span>
                    {item}
                  </li>
                ))}
              </ol>
            </SurfaceCard>
          </aside>
        </div>
      ) : (
        <EmptyState
          description="知识文章不存在。"
          action={
            <Button type="primary" onClick={() => navigate("/learn")}>
              返回知识学习
            </Button>
          }
        />
      )}
    </PageShell>
  );
}

export default LearnArticlePage;
