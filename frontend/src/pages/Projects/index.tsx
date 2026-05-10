import {
  Button,
  Form,
  Input,
  Modal,
  Pagination,
  Popconfirm,
  Radio,
  Space,
  Tag,
  Typography,
  message,
} from "antd";
import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { getProjects, deleteProject } from "../../api/project";
import { createInterviewSession } from "../../api/interview";
import EmptyState from "../../components/EmptyState";
import PageHeader from "../../components/PageHeader";
import PageShell from "../../components/PageShell";
import SurfaceCard from "../../components/SurfaceCard";
import type { InterviewDifficulty } from "../../types/interview";
import type { ProjectVO } from "../../types/project";
import "../../styles/projects.css";

const DEFAULT_PAGE_SIZE = 10;
const DEFAULT_TARGET_ROLE = "Java 后端实习";
const DEFAULT_DIFFICULTY: InterviewDifficulty = "NORMAL";

const DIFFICULTY_OPTIONS: Array<{
  value: InterviewDifficulty;
  label: string;
  description: string;
}> = [
  {
    value: "EASY",
    label: "入门引导",
    description:
      "适合第一次梳理项目，重点关注项目背景、业务流程和基础技术使用原因。",
  },
  {
    value: "NORMAL",
    label: "常规面试",
    description:
      "模拟常规 Java 后端实习 / 校招技术面，关注技术细节、异常场景和设计思路。",
  },
  {
    value: "HARD",
    label: "深度拷打",
    description:
      "偏大厂深挖，重点追问高并发、一致性、性能瓶颈和架构权衡。",
  },
];

type TrainingSettingsForm = {
  targetRole: string;
  difficulty: InterviewDifficulty;
};

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

function parseTechStack(value?: string) {
  if (!value) {
    return [];
  }
  return value
    .split(/[,，;/|]+/)
    .map((item) => item.trim())
    .filter(Boolean);
}

function ProjectsPage() {
  const navigate = useNavigate();
  const [projects, setProjects] = useState<ProjectVO[]>([]);
  const [loading, setLoading] = useState(false);
  const [keyword, setKeyword] = useState("");
  const [query, setQuery] = useState("");
  const [pageNum, setPageNum] = useState(1);
  const [pageSize, setPageSize] = useState(DEFAULT_PAGE_SIZE);
  const [total, setTotal] = useState(0);
  const [refreshKey, setRefreshKey] = useState(0);
  const [startingId, setStartingId] = useState<number | null>(null);
  const [deletingId, setDeletingId] = useState<number | null>(null);
  const [settingsOpen, setSettingsOpen] = useState(false);
  const [activeProject, setActiveProject] = useState<ProjectVO | null>(null);
  const [settingsForm] = Form.useForm<TrainingSettingsForm>();

  const hasData = projects.length > 0;
  const showPagination = total > pageSize;

  useEffect(() => {
    let active = true;
    setLoading(true);
    getProjects({
      pageNum,
      pageSize,
      keyword: query || undefined,
    })
      .then((data) => {
        if (!active) {
          return;
        }
        setProjects(data.records || []);
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
  }, [pageNum, pageSize, query, refreshKey]);

  const emptyState = useMemo(
    () => (
      <EmptyState
        description="暂无项目记录"
        action={
          <Button type="primary" onClick={() => navigate("/projects/new")}>
            新建项目
          </Button>
        }
      />
    ),
    [navigate],
  );

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

  const handlePageChange = (nextPage: number, nextSize: number) => {
    setPageNum(nextPage);
    setPageSize(nextSize);
  };

  const handleDelete = async (projectId: number) => {
    setDeletingId(projectId);
    try {
      await deleteProject(projectId);
      message.success("项目已删除");

      if (projects.length === 1 && pageNum > 1) {
        setPageNum(pageNum - 1);
      } else {
        setRefreshKey((prev) => prev + 1);
      }
    } catch {
      // Errors are handled by the request interceptor.
    } finally {
      setDeletingId(null);
    }
  };

  const openSettingsModal = (project: ProjectVO) => {
    setActiveProject(project);
    setSettingsOpen(true);
    settingsForm.setFieldsValue({
      targetRole: DEFAULT_TARGET_ROLE,
      difficulty: DEFAULT_DIFFICULTY,
    });
  };

  const closeSettingsModal = () => {
    if (startingId) {
      return;
    }
    setSettingsOpen(false);
    setActiveProject(null);
  };

  const handleConfirmStart = async () => {
    if (!activeProject) {
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

    setStartingId(activeProject.id);
    try {
      const data = await createInterviewSession({
        projectId: activeProject.id,
        targetRole,
        difficulty: values.difficulty,
      });

      if (!data?.sessionId) {
        message.error("创建训练失败，请稍后重试");
        return;
      }

      setSettingsOpen(false);
      setActiveProject(null);
      navigate(`/interviews/${data.sessionId}`);
    } catch {
      // Errors are handled by the request interceptor.
    } finally {
      setStartingId(null);
    }
  };

  return (
    <PageShell className="projects-page">
      <PageHeader
        title="项目档案库"
        description="沉淀你的项目经验，用于 AI 面试训练。"
        actions={
          <div className="projects-actions">
            <Input.Search
              className="projects-search"
              placeholder="搜索项目"
              allowClear
              value={keyword}
              size="large"
              onChange={(event) => handleKeywordChange(event.target.value)}
              onSearch={handleSearch}
              enterButton="搜索"
            />
            <Button
              type="primary"
              size="large"
              onClick={() => navigate("/projects/new")}
            >
              新建项目
            </Button>
          </div>
        }
      />

      <div className="projects-list">
        {loading ? (
          <SurfaceCard className="project-card" loading />
        ) : !hasData ? (
          emptyState
        ) : (
          projects.map((project) => {
            const tags = parseTechStack(project.techStack);
            return (
              <SurfaceCard key={project.id} className="project-card">
                <div className="project-card__header">
                  <div className="project-card__title-group">
                    <Typography.Title level={4} className="project-card__title">
                      {project.name}
                    </Typography.Title>
                    <div className="project-card__updated">
                      更新于 {formatDate(project.updatedAt)}
                    </div>
                  </div>
                  <Space className="project-card__actions" wrap>
                    <Button
                      type="primary"
                      loading={startingId === project.id}
                      onClick={() => openSettingsModal(project)}
                    >
                      开始拷打
                    </Button>
                    <Button
                      onClick={() => navigate(`/projects/${project.id}/edit`)}
                    >
                      编辑
                    </Button>
                    <Popconfirm
                      title="确认删除该项目吗？"
                      okText="删除"
                      cancelText="取消"
                      onConfirm={() => handleDelete(project.id)}
                    >
                      <Button
                        type="text"
                        danger
                        loading={deletingId === project.id}
                      >
                        删除
                      </Button>
                    </Popconfirm>
                  </Space>
                </div>
                <Typography.Paragraph
                  className="project-card__description"
                  ellipsis={{ rows: 2 }}
                >
                  {project.description || "暂无项目描述"}
                </Typography.Paragraph>
                <div className="project-card__meta">
                  <div>
                    <span className="project-card__meta-label">技术栈</span>
                    <div className="project-card__tags">
                      {tags.length ? (
                        tags.map((tag) => <Tag key={tag}>{tag}</Tag>)
                      ) : (
                        <span className="project-card__meta-value">—</span>
                      )}
                    </div>
                  </div>
                  <div>
                    <span className="project-card__meta-label">负责模块</span>
                    <span className="project-card__meta-value">
                      {project.role || "—"}
                    </span>
                  </div>
                </div>
              </SurfaceCard>
            );
          })
        )}
      </div>

      {showPagination && (
        <div className="projects-footer">
          <Pagination
            current={pageNum}
            pageSize={pageSize}
            total={total}
            showSizeChanger
            onChange={handlePageChange}
          />
        </div>
      )}

      <Modal
        title="训练设置"
        open={settingsOpen}
        onCancel={closeSettingsModal}
        onOk={handleConfirmStart}
        okText="开始训练"
        cancelText="取消"
        okButtonProps={{ loading: startingId === activeProject?.id }}
        cancelButtonProps={{ disabled: startingId === activeProject?.id }}
        maskClosable={!startingId}
        destroyOnClose
        className="training-modal"
      >
        <div className="training-modal__project">
          <Typography.Text type="secondary">当前项目</Typography.Text>
          <div className="training-modal__project-name">
            {activeProject?.name || "—"}
          </div>
        </div>
        <Form
          form={settingsForm}
          layout="vertical"
          initialValues={{
            targetRole: DEFAULT_TARGET_ROLE,
            difficulty: DEFAULT_DIFFICULTY,
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
            <Radio.Group className="difficulty-group">
              {DIFFICULTY_OPTIONS.map((option) => (
                <Radio
                  key={option.value}
                  value={option.value}
                  className="difficulty-option"
                >
                  <div className="difficulty-option__content">
                    <div className="difficulty-option__title">
                      {option.label}
                    </div>
                    <div className="difficulty-option__desc">
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

export default ProjectsPage;
