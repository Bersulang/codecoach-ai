import {
  Button,
  Card,
  Empty,
  Input,
  Pagination,
  Popconfirm,
  Space,
  Typography,
  message,
} from "antd";
import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { getProjects, deleteProject } from "../../api/project";
import { createInterviewSession } from "../../api/interview";
import type { ProjectVO } from "../../types/project";
import "../../styles/projects.css";

const DEFAULT_PAGE_SIZE = 10;
const DEFAULT_TARGET_ROLE = "Java 后端实习";
const DEFAULT_DIFFICULTY = "NORMAL";

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
      <div className="projects-empty">
        <Empty description="暂无项目记录" />
      </div>
    ),
    [],
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

  const handleStartInterview = async (projectId: number) => {
    setStartingId(projectId);
    try {
      const data = await createInterviewSession({
        projectId,
        targetRole: DEFAULT_TARGET_ROLE,
        difficulty: DEFAULT_DIFFICULTY,
      });

      if (!data?.sessionId) {
        message.error("创建训练失败，请稍后重试");
        return;
      }

      navigate(`/interviews/${data.sessionId}`);
    } catch {
      // Errors are handled by the request interceptor.
    } finally {
      setStartingId(null);
    }
  };

  return (
    <div className="projects-page">
      <div className="projects-header">
        <div>
          <Typography.Title level={3} className="projects-title">
            我的项目
          </Typography.Title>
          <Typography.Text className="projects-subtitle">
            管理你的项目档案，并发起拷打训练
          </Typography.Text>
        </div>
        <div className="projects-actions">
          <Input.Search
            className="projects-search"
            placeholder="搜索项目"
            allowClear
            value={keyword}
            onChange={(event) => handleKeywordChange(event.target.value)}
            onSearch={handleSearch}
            enterButton="搜索"
          />
          <Button type="primary" onClick={() => navigate("/projects/new")}>
            新建项目
          </Button>
        </div>
      </div>

      <div className="projects-list">
        {loading ? (
          <Card className="project-card" loading />
        ) : !hasData ? (
          emptyState
        ) : (
          projects.map((project) => (
            <Card key={project.id} className="project-card">
              <div className="project-card__header">
                <div>
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
                    onClick={() => handleStartInterview(project.id)}
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
                    <Button danger loading={deletingId === project.id}>
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
                  <span className="project-card__meta-value">
                    {project.techStack || "—"}
                  </span>
                </div>
                <div>
                  <span className="project-card__meta-label">负责模块</span>
                  <span className="project-card__meta-value">
                    {project.role || "—"}
                  </span>
                </div>
              </div>
            </Card>
          ))
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
    </div>
  );
}

export default ProjectsPage;
