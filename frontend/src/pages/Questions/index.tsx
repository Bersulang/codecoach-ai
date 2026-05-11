import {
  Button,
  Form,
  Input,
  Modal,
  Pagination,
  Radio,
  Select,
  Spin,
  Tag,
  Typography,
  message,
} from "antd";
import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  createQuestionSession,
  getKnowledgeTopicCategories,
  getKnowledgeTopics,
} from "../../api/question";
import EmptyState from "../../components/EmptyState";
import PageHeader from "../../components/PageHeader";
import PageShell from "../../components/PageShell";
import SurfaceCard from "../../components/SurfaceCard";
import type { InterviewDifficulty } from "../../types/interview";
import type { KnowledgeTopic } from "../../types/question";
import "./index.css";

const DEFAULT_TARGET_ROLE = "Java 后端实习";
const DEFAULT_PAGE_SIZE = 20;
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

const DIFFICULTY_LABELS: Record<InterviewDifficulty, string> = {
  EASY: "入门引导",
  NORMAL: "常规面试",
  HARD: "深度拷打",
};

const DIFFICULTY_OPTIONS: Array<{
  value: InterviewDifficulty;
  title: string;
  description: string;
}> = [
  {
    value: "EASY",
    title: "入门引导",
    description: "适合刚学完知识点，重点确认基础概念和表达。",
  },
  {
    value: "NORMAL",
    title: "常规面试",
    description: "模拟常规实习 / 校招技术面，关注原理、场景和常见追问。",
  },
  {
    value: "HARD",
    title: "深度拷打",
    description: "偏深度拷打，关注底层原理、边界条件、性能和工程权衡。",
  },
];

type DifficultyFilter = "ALL" | InterviewDifficulty;

type TrainingSettingsForm = {
  targetRole: string;
  difficulty: InterviewDifficulty;
};

function mergeCategories(base: string[], incoming: string[]) {
  return Array.from(new Set([...base, ...incoming]));
}

function trimText(value?: string, maxLength = 72) {
  if (!value) {
    return "暂无描述";
  }
  if (value.length <= maxLength) {
    return value;
  }
  return `${value.slice(0, maxLength)}...`;
}

function QuestionsPage() {
  const navigate = useNavigate();
  const [categories, setCategories] = useState<string[]>(DEFAULT_CATEGORIES);
  const [activeCategory, setActiveCategory] = useState("全部");
  const [difficulty, setDifficulty] = useState<DifficultyFilter>("ALL");
  const [keyword, setKeyword] = useState("");
  const [query, setQuery] = useState("");
  const [topics, setTopics] = useState<KnowledgeTopic[]>([]);
  const [loading, setLoading] = useState(false);
  const [pageNum, setPageNum] = useState(1);
  const [pageSize, setPageSize] = useState(DEFAULT_PAGE_SIZE);
  const [total, setTotal] = useState(0);
  const [settingsOpen, setSettingsOpen] = useState(false);
  const [activeTopic, setActiveTopic] = useState<KnowledgeTopic | null>(null);
  const [startingId, setStartingId] = useState<number | null>(null);
  const [settingsForm] = Form.useForm<TrainingSettingsForm>();

  const categoryOptions = useMemo(
    () => ["全部", ...categories],
    [categories],
  );

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
        message.error("知识点分类加载失败");
      });

    return () => {
      active = false;
    };
  }, []);

  useEffect(() => {
    let active = true;
    setLoading(true);
    getKnowledgeTopics({
      category: activeCategory === "全部" ? undefined : activeCategory,
      keyword: query || undefined,
      difficulty: difficulty === "ALL" ? undefined : difficulty,
      pageNum,
      pageSize,
    })
      .then((data) => {
        if (!active) {
          return;
        }
        setTopics(data.records || []);
        setTotal(data.total || 0);
      })
      .catch(() => {
        if (!active) {
          return;
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
  }, [activeCategory, difficulty, query, pageNum, pageSize]);

  const showPagination = total > pageSize;

  const handleCategoryChange = (value: string) => {
    setActiveCategory(value);
    setPageNum(1);
  };

  const handleDifficultyChange = (value: DifficultyFilter) => {
    setDifficulty(value);
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

  const handleClearFilters = () => {
    setActiveCategory("全部");
    setDifficulty("ALL");
    setKeyword("");
    setQuery("");
    setPageNum(1);
  };

  const openSettingsModal = (topic: KnowledgeTopic) => {
    setActiveTopic(topic);
    setSettingsOpen(true);
    settingsForm.setFieldsValue({
      targetRole: DEFAULT_TARGET_ROLE,
      difficulty: "NORMAL",
    });
  };

  const closeSettingsModal = () => {
    if (startingId) {
      return;
    }
    setSettingsOpen(false);
    setActiveTopic(null);
  };

  const handleStartTraining = async () => {
    if (!activeTopic) {
      return;
    }
    let values: TrainingSettingsForm;
    try {
      values = await settingsForm.validateFields();
    } catch {
      return;
    }

    const targetRole = values.targetRole.trim();
    if (!targetRole) {
      settingsForm.setFields([
        { name: "targetRole", errors: ["请填写目标岗位"] },
      ]);
      return;
    }

    setStartingId(activeTopic.id);
    try {
      const data = await createQuestionSession({
        topicId: activeTopic.id,
        targetRole,
        difficulty: values.difficulty,
      });

      if (!data?.sessionId) {
        message.error("创建训练失败，请稍后重试");
        return;
      }

      setSettingsOpen(false);
      setActiveTopic(null);
      navigate(`/question-sessions/${data.sessionId}`);
    } catch {
      // Errors are handled by the request interceptor.
    } finally {
      setStartingId(null);
    }
  };

  return (
    <PageShell className="questions-page">
      <PageHeader
        title="八股问答训练"
        description="围绕 Java 后端高频知识点进行 AI 追问训练，提升标准表达、参考答案理解和连续追问应对能力。"
      />

      <div className="questions-filters">
        <div className="questions-categories">
          {categoryOptions.map((category) => (
            <button
              key={category}
              type="button"
              className={`questions-category${
                category === activeCategory ? " is-active" : ""
              }`}
              onClick={() => handleCategoryChange(category)}
            >
              {category}
            </button>
          ))}
        </div>
        <div className="questions-toolbar">
          <Input.Search
            className="questions-search"
            placeholder="搜索知识点"
            allowClear
            value={keyword}
            onChange={(event) => handleKeywordChange(event.target.value)}
            onSearch={handleSearch}
            enterButton
          />
          <Select<DifficultyFilter>
            className="questions-select"
            value={difficulty}
            onChange={handleDifficultyChange}
            options={[
              { label: "全部难度", value: "ALL" },
              { label: "EASY", value: "EASY" },
              { label: "NORMAL", value: "NORMAL" },
              { label: "HARD", value: "HARD" },
            ]}
          />
        </div>
      </div>

      <Spin spinning={loading}>
        {topics.length === 0 && !loading ? (
          <EmptyState
            description="没有找到相关知识点"
            action={
              <Button type="primary" onClick={handleClearFilters}>
                清空筛选
              </Button>
            }
          />
        ) : (
          <div className="questions-list">
            {topics.map((topic) => (
              <SurfaceCard key={topic.id} className="questions-card">
                <div className="questions-card__header">
                  <div>
                    <div className="questions-card__category">
                      {topic.category}
                    </div>
                    <div className="questions-card__title">{topic.name}</div>
                  </div>
                  <div className="questions-card__difficulty">
                    {DIFFICULTY_LABELS[topic.difficulty]}
                  </div>
                </div>
                <Typography.Paragraph className="questions-card__desc">
                  {trimText(topic.description, 80)}
                </Typography.Paragraph>
                <div className="questions-card__focus">
                  <span>训练重点</span>
                  <p>{trimText(topic.interviewFocus, 72)}</p>
                </div>
                <div className="questions-card__tags">
                  {(topic.tags || []).slice(0, 4).map((tag) => (
                    <Tag key={tag}>{tag}</Tag>
                  ))}
                </div>
                <div className="questions-card__actions">
                  <Button
                    type="primary"
                    onClick={() => openSettingsModal(topic)}
                    loading={startingId === topic.id}
                  >
                    开始训练
                  </Button>
                </div>
              </SurfaceCard>
            ))}
          </div>
        )}
      </Spin>

      {showPagination && (
        <div className="questions-footer">
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
      )}

      <Modal
        title="训练设置"
        open={settingsOpen}
        onCancel={closeSettingsModal}
        onOk={handleStartTraining}
        okText="开始训练"
        cancelText="取消"
        okButtonProps={{ loading: startingId === activeTopic?.id }}
        cancelButtonProps={{ disabled: startingId === activeTopic?.id }}
        maskClosable={!startingId}
        destroyOnClose
        className="questions-modal"
      >
        <div className="questions-modal__topic">
          <Typography.Text type="secondary">当前知识点</Typography.Text>
          <div className="questions-modal__title">{activeTopic?.name || "—"}</div>
          <div className="questions-modal__meta">{activeTopic?.category}</div>
        </div>
        <Form
          form={settingsForm}
          layout="vertical"
          initialValues={{
            targetRole: DEFAULT_TARGET_ROLE,
            difficulty: "NORMAL",
          }}
        >
          <Form.Item
            label="目标岗位"
            name="targetRole"
            rules={[{ required: true, message: "请填写目标岗位" }]}
          >
            <Input placeholder="例如：Java 后端实习" />
          </Form.Item>
          <Form.Item
            label="训练难度"
            name="difficulty"
            rules={[{ required: true, message: "请选择训练难度" }]}
          >
            <Radio.Group className="questions-difficulty-group">
              {DIFFICULTY_OPTIONS.map((option) => (
                <Radio
                  key={option.value}
                  value={option.value}
                  className="questions-difficulty-option"
                >
                  <div className="questions-difficulty-option__content">
                    <div className="questions-difficulty-option__title">
                      {option.title}
                    </div>
                    <div className="questions-difficulty-option__desc">
                      {option.description}
                    </div>
                  </div>
                </Radio>
              ))}
            </Radio.Group>
          </Form.Item>
        </Form>
      </Modal>
    </PageShell>
  );
}

export default QuestionsPage;
