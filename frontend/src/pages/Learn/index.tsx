import { Button, Input, Pagination, Spin, Typography, message } from "antd";
import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  getKnowledgeArticleList,
  getKnowledgeTopicCategories,
} from "../../api/knowledge";
import EmptyState from "../../components/EmptyState";
import PageShell from "../../components/PageShell";
import SurfaceCard from "../../components/SurfaceCard";
import type { KnowledgeArticle } from "../../types/knowledge";
import "./index.css";

const DEFAULT_PAGE_SIZE = 10;
const DEFAULT_CATEGORIES = [
  "Java 基础",
  "JVM",
  "JUC",
  "MySQL",
  "Redis",
  "Spring",
  "MQ",
  "计算机网络",
  "操作系统",
  "分布式",
];

function mergeCategories(base: string[], incoming: string[]) {
  return Array.from(new Set([...base, ...incoming]));
}

function trimText(value?: string, maxLength = 96) {
  if (!value) {
    return "暂无摘要";
  }
  if (value.length <= maxLength) {
    return value;
  }
  return `${value.slice(0, maxLength)}...`;
}

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

function LearnPage() {
  const navigate = useNavigate();
  const [categories, setCategories] = useState<string[]>(DEFAULT_CATEGORIES);
  const [activeCategory, setActiveCategory] = useState("全部");
  const [keyword, setKeyword] = useState("");
  const [query, setQuery] = useState("");
  const [articles, setArticles] = useState<KnowledgeArticle[]>([]);
  const [loading, setLoading] = useState(false);
  const [loadError, setLoadError] = useState(false);
  const [pageNum, setPageNum] = useState(1);
  const [pageSize, setPageSize] = useState(DEFAULT_PAGE_SIZE);
  const [total, setTotal] = useState(0);
  const [refreshKey, setRefreshKey] = useState(0);

  const categoryOptions = useMemo(() => ["全部", ...categories], [categories]);

  useEffect(() => {
    let active = true;
    getKnowledgeTopicCategories()
      .then((data) => {
        if (!active) {
          return;
        }
        setCategories((prev) => mergeCategories(prev, data || []));
      })
      .catch(() => {
        if (!active) {
          return;
        }
        message.error("知识分类加载失败");
      });

    return () => {
      active = false;
    };
  }, []);

  useEffect(() => {
    let active = true;
    setLoading(true);
    setLoadError(false);
    getKnowledgeArticleList({
      category: activeCategory === "全部" ? undefined : activeCategory,
      keyword: query || undefined,
      pageNum,
      pageSize,
    })
      .then((data) => {
        if (!active) {
          return;
        }
        setArticles(data.records || []);
        setTotal(data.total || 0);
      })
      .catch(() => {
        if (!active) {
          return;
        }
        setArticles([]);
        setTotal(0);
        setLoadError(true);
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
  }, [activeCategory, query, pageNum, pageSize, refreshKey]);

  const showPagination = total > pageSize;

  const handleCategoryChange = (value: string) => {
    setActiveCategory(value);
    setPageNum(1);
  };

  const handleSearch = (value: string) => {
    const nextQuery = value.trim();
    setKeyword(value);
    setQuery(nextQuery);
    setPageNum(1);
  };

  const handleKeywordChange = (value: string) => {
    setKeyword(value);
    if (!value.trim() && query) {
      setQuery("");
      setPageNum(1);
    }
  };

  const handleRetry = () => {
    setRefreshKey((prev) => prev + 1);
  };

  return (
    <PageShell className="learn-page">
      <section className="learn-hero">
        <div className="learn-hero__text">
          <div className="learn-hero__kicker">面试表达型学习</div>
          <h1 className="learn-hero__title">知识学习</h1>
          <p className="learn-hero__subtitle">
            面向 Java 后端面试表达的知识卡片，帮助你从“知道概念”到“讲清楚场景”。
          </p>
          <div className="learn-hero__loop">训练 → 报告 → 学习 → 再训练</div>
          <p className="learn-hero__note">
            训练报告指出薄弱点后，可以回到这里补齐知识，再发起专项训练。
          </p>
        </div>
        <SurfaceCard className="learn-loop-card">
          <div className="learn-loop__title">训练闭环</div>
          <ol className="learn-loop__list">
            <li>
              <span>1</span>
              完成训练
            </li>
            <li>
              <span>2</span>
              查看报告
            </li>
            <li>
              <span>3</span>
              学习薄弱知识点
            </li>
            <li>
              <span>4</span>
              再次专项训练
            </li>
          </ol>
          <Typography.Text className="learn-loop__hint">
            不是普通知识库，而是面向面试表达的复盘入口。
          </Typography.Text>
        </SurfaceCard>
      </section>

      <section className="learn-filters">
        <div className="learn-categories">
          {categoryOptions.length === 0 ? (
            <div className="learn-categories__empty">暂无分类</div>
          ) : (
            categoryOptions.map((category) => (
              <button
                key={category}
                type="button"
                className={`learn-category${
                  category === activeCategory ? " is-active" : ""
                }`}
                onClick={() => handleCategoryChange(category)}
              >
                {category}
              </button>
            ))
          )}
        </div>
        <div className="learn-toolbar">
          <Input.Search
            className="learn-search"
            placeholder="搜索知识点、文章或面试表达"
            allowClear
            value={keyword}
            onChange={(event) => handleKeywordChange(event.target.value)}
            onSearch={handleSearch}
            enterButton
          />
        </div>
      </section>

      <Spin spinning={loading}>
        {loadError ? (
          <EmptyState
            description="知识文章加载失败，请稍后重试。"
            action={
              <Button type="primary" onClick={handleRetry}>
                重新加载
              </Button>
            }
          />
        ) : articles.length === 0 && !loading ? (
          <EmptyState
            description="暂未找到相关知识文章"
            action={
              <Button type="primary" onClick={() => navigate("/questions")}>
                去八股问答
              </Button>
            }
          />
        ) : (
          <div className="learn-list">
            {articles.map((article) => (
              <SurfaceCard key={article.id} className="learn-card">
                <div className="learn-card__header">
                  <div>
                    <div className="learn-card__kicker">
                      {article.category || "—"}
                    </div>
                    <div className="learn-card__topic">
                      {article.topicName || "—"}
                    </div>
                  </div>
                  <div className="learn-card__version">
                    {article.version || "—"}
                  </div>
                </div>
                <Typography.Title level={4} className="learn-card__title">
                  {article.title}
                </Typography.Title>
                <Typography.Paragraph className="learn-card__summary">
                  {trimText(article.summary, 120)}
                </Typography.Paragraph>
                <div className="learn-card__footer">
                  <span className="learn-card__date">
                    更新于 {formatDate(article.updatedAt)}
                  </span>
                  <Button
                    type="primary"
                    onClick={() => navigate(`/learn/articles/${article.id}`)}
                  >
                    开始学习
                  </Button>
                </div>
              </SurfaceCard>
            ))}
          </div>
        )}
      </Spin>

      {showPagination && !loadError ? (
        <div className="learn-footer">
          <Pagination
            current={pageNum}
            pageSize={pageSize}
            total={total}
            showSizeChanger
            onChange={(nextPage, nextSize) => {
              setPageNum(nextPage);
              setPageSize(nextSize);
            }}
          />
        </div>
      ) : null}
    </PageShell>
  );
}

export default LearnPage;
