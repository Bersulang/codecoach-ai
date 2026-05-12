import {
  Button,
  Input,
  Popconfirm,
  Select,
  Spin,
  Typography,
  message,
} from "antd";
import {
  DeleteOutlined,
  FileTextOutlined,
  InboxOutlined,
  ReloadOutlined,
  UploadOutlined,
} from "@ant-design/icons";
import { useEffect, useMemo, useRef, useState } from "react";
import { getProjects } from "../../api/project";
import {
  deleteUserDocument,
  getUserDocuments,
  reindexUserDocument,
  uploadUserDocument,
  type UserDocument,
} from "../../api/userDocument";
import type { ProjectVO } from "../../types/project";
import "../Workspace/workspace.css";
import "./index.css";

const MAX_FILE_SIZE = 10 * 1024 * 1024;
const ALLOWED_EXTENSIONS = [".txt", ".md", ".markdown", ".pdf"];

const parseStatusText: Record<UserDocument["parseStatus"], string> = {
  PENDING: "解析中",
  PARSED: "解析完成",
  FAILED: "解析失败",
};

const indexStatusText: Record<UserDocument["indexStatus"], string> = {
  PENDING: "等待索引",
  INDEXED: "已加入训练上下文",
  FAILED: "索引失败",
};

const fileTypeText: Record<UserDocument["fileType"], string> = {
  TXT: "TXT",
  MARKDOWN: "Markdown",
  PDF: "PDF",
};

function friendlyError(error: unknown, fallback: string) {
  if (error instanceof Error && error.message) {
    return error.message;
  }
  return fallback;
}

function formatFileSize(size?: number) {
  if (!size || size <= 0) {
    return "0 KB";
  }
  if (size >= 1024 * 1024) {
    return `${(size / 1024 / 1024).toFixed(size >= 10 * 1024 * 1024 ? 0 : 1)} MB`;
  }
  return `${Math.max(1, Math.round(size / 1024))} KB`;
}

function formatDate(value?: string) {
  if (!value) {
    return "—";
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  return date.toLocaleString("zh-CN", {
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  });
}

function getExtension(filename: string) {
  const index = filename.lastIndexOf(".");
  return index >= 0 ? filename.slice(index).toLowerCase() : "";
}

function validateFile(file: File) {
  if (file.size <= 0) {
    return "文档内容不能为空";
  }
  if (file.size > MAX_FILE_SIZE) {
    return "文档不能超过 10MB";
  }
  if (!ALLOWED_EXTENSIONS.includes(getExtension(file.name))) {
    return "仅支持 TXT、Markdown、PDF 文档";
  }
  return "";
}

function DocumentsPage() {
  const fileInputRef = useRef<HTMLInputElement | null>(null);
  const [documents, setDocuments] = useState<UserDocument[]>([]);
  const [projects, setProjects] = useState<ProjectVO[]>([]);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [title, setTitle] = useState("");
  const [projectId, setProjectId] = useState<number | undefined>();
  const [loading, setLoading] = useState(true);
  const [projectLoading, setProjectLoading] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [deletingId, setDeletingId] = useState<number | null>(null);
  const [reindexingId, setReindexingId] = useState<number | null>(null);
  const [listError, setListError] = useState("");

  const projectNameById = useMemo(() => {
    const map = new Map<number, string>();
    projects.forEach((project) => map.set(project.id, project.name));
    return map;
  }, [projects]);

  const loadDocuments = async () => {
    setListError("");
    setLoading(true);
    try {
      const data = await getUserDocuments();
      setDocuments(data || []);
    } catch (error) {
      setListError(friendlyError(error, "文档列表加载失败，请稍后重试。"));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadDocuments();
  }, []);

  useEffect(() => {
    let active = true;
    setProjectLoading(true);
    getProjects({ pageNum: 1, pageSize: 100 })
      .then((data) => {
        if (active) {
          setProjects(data.records || []);
        }
      })
      .catch(() => {
        if (active) {
          setProjects([]);
        }
      })
      .finally(() => {
        if (active) {
          setProjectLoading(false);
        }
      });
    return () => {
      active = false;
    };
  }, []);

  const handleFileChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    event.target.value = "";
    if (!file) {
      return;
    }
    const error = validateFile(file);
    if (error) {
      message.error(error);
      return;
    }
    setSelectedFile(file);
    if (!title.trim()) {
      setTitle(file.name.replace(/\.[^.]+$/, ""));
    }
  };

  const handleUpload = async () => {
    if (!selectedFile) {
      message.warning("请先选择一份文档");
      return;
    }
    const error = validateFile(selectedFile);
    if (error) {
      message.error(error);
      return;
    }

    setUploading(true);
    try {
      await uploadUserDocument(selectedFile, { projectId, title });
      message.success("文档已上传并加入处理队列");
      setSelectedFile(null);
      setTitle("");
      setProjectId(undefined);
      await loadDocuments();
    } catch (uploadError) {
      message.error(friendlyError(uploadError, "上传失败，请稍后重试"));
    } finally {
      setUploading(false);
    }
  };

  const handleDelete = async (documentId: number) => {
    setDeletingId(documentId);
    try {
      await deleteUserDocument(documentId);
      message.success("文档已删除");
      setDocuments((items) => items.filter((item) => item.id !== documentId));
    } catch (deleteError) {
      message.error(friendlyError(deleteError, "删除失败，请稍后重试"));
    } finally {
      setDeletingId(null);
    }
  };

  const handleReindex = async (documentId: number) => {
    setReindexingId(documentId);
    try {
      await reindexUserDocument(documentId);
      message.success("已重新解析并加入训练上下文");
      await loadDocuments();
    } catch (reindexError) {
      message.error(friendlyError(reindexError, "重新索引失败，请稍后重试"));
    } finally {
      setReindexingId(null);
    }
  };

  const openFilePicker = () => {
    fileInputRef.current?.click();
  };

  return (
    <div className="workspace-page documents-page">
      <section className="workspace-hero documents-hero">
        <p className="workspace-kicker">PRIVATE CONTEXT</p>
        <h1>我的文档</h1>
        <p>
          上传简历、README、项目文档和学习笔记，让 CodeCoach AI
          在训练时参考你的真实材料。
        </p>
      </section>

      <section className="documents-upload-card">
        <div className="documents-upload-card__copy">
          <span className="workspace-kicker">上传训练上下文</span>
          <h2>把真实材料交给训练场。</h2>
          <p>支持 TXT / Markdown / PDF，最大 10MB。</p>
        </div>

        <div className="documents-upload-form">
          <input
            ref={fileInputRef}
            type="file"
            accept=".txt,.md,.markdown,.pdf"
            className="documents-file-input"
            onChange={handleFileChange}
          />
          <button
            type="button"
            className="documents-file-picker"
            onClick={openFilePicker}
          >
            <span className="documents-file-picker__icon">
              <InboxOutlined />
            </span>
            <span>
              {selectedFile ? selectedFile.name : "选择文档"}
              <em>
                {selectedFile
                  ? formatFileSize(selectedFile.size)
                  : "TXT / Markdown / PDF，最大 10MB"}
              </em>
            </span>
          </button>

          <div className="documents-upload-fields">
            <Input
              value={title}
              placeholder="标题，可选"
              size="large"
              onChange={(event) => setTitle(event.target.value)}
            />
            <Select
              value={projectId}
              placeholder="不关联项目"
              size="large"
              loading={projectLoading}
              allowClear
              options={[
                { label: "不关联项目", value: 0 },
                ...projects.map((project) => ({
                  label: project.name,
                  value: project.id,
                })),
              ]}
              onChange={(value) => setProjectId(value || undefined)}
            />
          </div>

          <Button
            type="primary"
            size="large"
            icon={<UploadOutlined />}
            loading={uploading}
            onClick={handleUpload}
          >
            上传并索引
          </Button>
        </div>
      </section>

      <section className="documents-list-section">
        <div className="documents-section-heading">
          <div>
            <span className="workspace-kicker">DOCUMENTS</span>
            <h2>训练上下文材料</h2>
          </div>
          <Button onClick={loadDocuments} loading={loading}>
            刷新
          </Button>
        </div>

        {loading ? (
          <div className="documents-loading">
            <Spin />
          </div>
        ) : listError ? (
          <div className="documents-error">
            <Typography.Title level={4}>文档列表加载失败</Typography.Title>
            <p>{listError}</p>
            <Button type="primary" onClick={loadDocuments}>
              重新加载
            </Button>
          </div>
        ) : documents.length === 0 ? (
          <div className="documents-empty">
            <FileTextOutlined />
            <h3>还没有上传文档</h3>
            <p>
              上传你的简历、README 或项目设计文档，让 AI
              在项目拷打时更了解你的真实经历。
            </p>
            <Button type="primary" size="large" onClick={openFilePicker}>
              上传第一份文档
            </Button>
          </div>
        ) : (
          <div className="documents-card-list">
            {documents.map((document) => (
              <article key={document.id} className="document-card">
                <div className="document-card__main">
                  <div className="document-card__icon">
                    <FileTextOutlined />
                  </div>
                  <div>
                    <h3>{document.title}</h3>
                    <p>{document.originalFilename}</p>
                    <div className="document-card__meta">
                      <span>{fileTypeText[document.fileType]}</span>
                      <span>{formatFileSize(document.fileSize)}</span>
                      <span>
                        {document.projectId
                          ? projectNameById.get(document.projectId) ||
                            `项目 #${document.projectId}`
                          : "未关联项目"}
                      </span>
                      <span>上传于 {formatDate(document.createdAt)}</span>
                    </div>
                  </div>
                </div>

                <div className="document-card__status">
                  <span
                    className={`document-status-pill document-status-pill--${document.parseStatus.toLowerCase()}`}
                  >
                    {parseStatusText[document.parseStatus]}
                  </span>
                  <span
                    className={`document-status-pill document-status-pill--${document.indexStatus.toLowerCase()}`}
                  >
                    {indexStatusText[document.indexStatus]}
                  </span>
                </div>

                {document.errorMessage ? (
                  <p className="document-card__error">{document.errorMessage}</p>
                ) : null}

                <div className="document-card__actions">
                  <Button
                    icon={<ReloadOutlined />}
                    loading={reindexingId === document.id}
                    onClick={() => handleReindex(document.id)}
                  >
                    重新解析并加入训练上下文
                  </Button>
                  <Popconfirm
                    title="删除文档"
                    description="删除后，该文档将不再参与 AI 训练上下文检索。"
                    okText="删除"
                    cancelText="取消"
                    okButtonProps={{ danger: true }}
                    onConfirm={() => handleDelete(document.id)}
                  >
                    <Button
                      danger
                      icon={<DeleteOutlined />}
                      loading={deletingId === document.id}
                    >
                      删除
                    </Button>
                  </Popconfirm>
                </div>
              </article>
            ))}
          </div>
        )}
      </section>
    </div>
  );
}

export default DocumentsPage;
