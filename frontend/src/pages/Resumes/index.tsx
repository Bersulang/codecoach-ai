import {
  Button,
  Input,
  Modal,
  Popconfirm,
  Select,
  Spin,
  Tag,
  Typography,
  message,
} from "antd";
import {
  CopyOutlined,
  DeleteOutlined,
  FileSearchOutlined,
  FileAddOutlined,
  LinkOutlined,
  PlayCircleOutlined,
  ReloadOutlined,
} from "@ant-design/icons";
import { useEffect, useMemo, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import {
  analyzeResume,
  createResume,
  deleteResume,
  generateResumeProjectDraft,
  getResumeDetail,
  getResumes,
  linkResumeProject,
  saveResumeProjectFromDraft,
} from "../../api/resume";
import { getUserDocuments, type UserDocument } from "../../api/userDocument";
import { getProjects } from "../../api/project";
import { createInterviewSession } from "../../api/interview";
import type { ProjectVO } from "../../types/project";
import type {
  ResumeAnalysisStatus,
  ResumeListItem,
  ResumeProfile,
  ResumeProjectDraft,
  ResumeProjectSaveRequest,
  ResumeProjectExperience,
  ResumeRiskPoint,
  ResumeSkill,
} from "../../types/resume";
import type { InterviewDifficulty } from "../../types/interview";
import "../Workspace/workspace.css";
import "./index.css";

const DEFAULT_TARGET_ROLE = "Java 后端实习";
const DEFAULT_DIFFICULTY: InterviewDifficulty = "NORMAL";

const statusText: Record<ResumeAnalysisStatus, string> = {
  PENDING: "待分析",
  ANALYZING: "分析中",
  ANALYZED: "已分析",
  FAILED: "分析失败",
};

const riskText: Record<string, string> = {
  LOW: "低",
  MEDIUM: "中",
  HIGH: "高",
};

function friendlyError(error: unknown, fallback: string) {
  if (error instanceof Error && error.message) {
    return error.message;
  }
  return fallback;
}

function formatDate(value?: string | null) {
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

function riskLevelClass(level?: string) {
  return `resume-risk-level resume-risk-level--${(level || "MEDIUM").toLowerCase()}`;
}

function ResumesPage() {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const initialDocumentId = Number(searchParams.get("documentId")) || undefined;

  const [documents, setDocuments] = useState<UserDocument[]>([]);
  const [projects, setProjects] = useState<ProjectVO[]>([]);
  const [resumes, setResumes] = useState<ResumeListItem[]>([]);
  const [activeResume, setActiveResume] = useState<ResumeProfile | null>(null);
  const [selectedDocumentId, setSelectedDocumentId] = useState<number | undefined>(
    initialDocumentId,
  );
  const [title, setTitle] = useState("");
  const [targetRole, setTargetRole] = useState(DEFAULT_TARGET_ROLE);
  const [loading, setLoading] = useState(true);
  const [detailLoading, setDetailLoading] = useState(false);
  const [creating, setCreating] = useState(false);
  const [analyzingId, setAnalyzingId] = useState<number | null>(null);
  const [deletingId, setDeletingId] = useState<number | null>(null);
  const [startingProjectId, setStartingProjectId] = useState<number | null>(null);
  const [linkingProjectId, setLinkingProjectId] = useState<number | null>(null);
  const [linkTarget, setLinkTarget] = useState<ResumeProjectExperience | null>(
    null,
  );
  const [linkProjectId, setLinkProjectId] = useState<number | undefined>();
  const [draftTarget, setDraftTarget] = useState<ResumeProjectExperience | null>(
    null,
  );
  const [projectDraft, setProjectDraft] = useState<ResumeProjectDraft | null>(
    null,
  );
  const [draftForm, setDraftForm] = useState<ResumeProjectSaveRequest>({
    name: "",
    description: "",
    techStack: "",
    role: "",
    highlights: "",
    difficulties: "",
  });
  const [generatingProjectId, setGeneratingProjectId] = useState<number | null>(
    null,
  );
  const [savingDraft, setSavingDraft] = useState(false);
  const [createdProjectId, setCreatedProjectId] = useState<number | null>(null);

  const parsedDocuments = useMemo(
    () =>
      documents.filter(
        (item) =>
          item.parseStatus === "PARSED" &&
          ["TXT", "MARKDOWN", "PDF"].includes(item.fileType),
      ),
    [documents],
  );

  const projectNameById = useMemo(() => {
    const map = new Map<number, string>();
    projects.forEach((project) => map.set(project.id, project.name));
    return map;
  }, [projects]);

  const loadAll = async () => {
    setLoading(true);
    try {
      const [documentData, resumeData, projectData] = await Promise.all([
        getUserDocuments(),
        getResumes(),
        getProjects({ pageNum: 1, pageSize: 100 }, { silentError: true }),
      ]);
      setDocuments(documentData || []);
      setResumes(resumeData || []);
      setProjects(projectData.records || []);
      const nextActiveId = activeResume?.id || resumeData?.[0]?.id;
      if (nextActiveId) {
        await loadDetail(nextActiveId);
      }
    } catch (error) {
      message.error(friendlyError(error, "简历训练数据加载失败"));
    } finally {
      setLoading(false);
    }
  };

  const loadDetail = async (resumeId: number) => {
    setDetailLoading(true);
    try {
      const detail = await getResumeDetail(resumeId);
      setActiveResume(detail);
    } catch (error) {
      message.error(friendlyError(error, "简历详情加载失败"));
    } finally {
      setDetailLoading(false);
    }
  };

  useEffect(() => {
    loadAll();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    if (!initialDocumentId || !parsedDocuments.length) {
      return;
    }
    const document = parsedDocuments.find((item) => item.id === initialDocumentId);
    if (document && !title) {
      setSelectedDocumentId(document.id);
      setTitle(document.title);
      setSearchParams({}, { replace: true });
    }
  }, [initialDocumentId, parsedDocuments, setSearchParams, title]);

  const handleCreate = async () => {
    if (!selectedDocumentId) {
      message.warning("请选择一份已解析的简历文档");
      return;
    }
    setCreating(true);
    try {
      const created = await createResume({
        documentId: selectedDocumentId,
        title: title.trim() || undefined,
        targetRole: targetRole.trim() || DEFAULT_TARGET_ROLE,
      });
      message.success("简历档案已创建");
      setTitle("");
      setSelectedDocumentId(undefined);
      setActiveResume(created);
      await loadAll();
      await handleAnalyze(created.id);
    } catch (error) {
      message.error(friendlyError(error, "创建简历档案失败"));
    } finally {
      setCreating(false);
    }
  };

  const handleAnalyze = async (resumeId: number) => {
    setAnalyzingId(resumeId);
    try {
      const analyzed = await analyzeResume(resumeId);
      setActiveResume(analyzed);
      setResumes((items) =>
        items.map((item) =>
          item.id === analyzed.id
            ? {
                ...item,
                analysisStatus: analyzed.analysisStatus,
                summary: analyzed.summary,
                errorMessage: analyzed.errorMessage,
                analyzedAt: analyzed.analyzedAt,
                updatedAt: analyzed.updatedAt,
              }
            : item,
        ),
      );
      message.success("简历分析完成");
    } catch (error) {
      message.error(friendlyError(error, "简历分析失败，可以稍后重试"));
      await loadAll();
    } finally {
      setAnalyzingId(null);
    }
  };

  const handleDelete = async (resumeId: number) => {
    setDeletingId(resumeId);
    try {
      await deleteResume(resumeId);
      message.success("简历档案已删除，原文档不会被删除");
      setResumes((items) => items.filter((item) => item.id !== resumeId));
      if (activeResume?.id === resumeId) {
        setActiveResume(null);
      }
    } catch (error) {
      message.error(friendlyError(error, "删除失败"));
    } finally {
      setDeletingId(null);
    }
  };

  const copyQuestion = async (question: string) => {
    try {
      await navigator.clipboard.writeText(question);
      message.success("追问已复制");
    } catch {
      message.warning("复制失败，请手动选择文本");
    }
  };

  const openLinkModal = (project: ResumeProjectExperience) => {
    setLinkTarget(project);
    setLinkProjectId(project.projectId || undefined);
  };

  const confirmLinkProject = async () => {
    if (!activeResume || !linkTarget || !linkProjectId) {
      message.warning("请选择项目档案");
      return;
    }
    setLinkingProjectId(linkProjectId);
    try {
      const updated = await linkResumeProject(
        activeResume.id,
        linkTarget.id,
        linkProjectId,
      );
      setActiveResume(updated);
      message.success("项目档案已关联");
      setLinkTarget(null);
      setLinkProjectId(undefined);
    } catch (error) {
      message.error(friendlyError(error, "关联项目失败"));
    } finally {
      setLinkingProjectId(null);
    }
  };

  const startTraining = async (resumeProject: ResumeProjectExperience) => {
    if (!activeResume) {
      return;
    }
    if (!resumeProject.projectId) {
      openLinkModal(resumeProject);
      return;
    }
    setStartingProjectId(resumeProject.projectId);
    try {
      const data = await createInterviewSession({
        projectId: resumeProject.projectId,
        resumeId: activeResume.id,
        resumeProjectId: resumeProject.id,
        targetRole: activeResume.targetRole || DEFAULT_TARGET_ROLE,
        difficulty: DEFAULT_DIFFICULTY,
      });
      navigate(`/interviews/${data.sessionId}`);
    } catch (error) {
      message.error(friendlyError(error, "创建项目拷打训练失败"));
    } finally {
      setStartingProjectId(null);
    }
  };

  const startTrainingWithProject = async (projectId: number) => {
    if (!activeResume) {
      return;
    }
    setStartingProjectId(projectId);
    try {
      const data = await createInterviewSession({
        projectId,
        resumeId: activeResume.id,
        resumeProjectId: draftTarget?.id,
        targetRole: activeResume.targetRole || DEFAULT_TARGET_ROLE,
        difficulty: DEFAULT_DIFFICULTY,
      });
      setDraftTarget(null);
      setProjectDraft(null);
      setCreatedProjectId(null);
      navigate(`/interviews/${data.sessionId}`);
    } catch (error) {
      message.error(friendlyError(error, "创建项目拷打训练失败"));
    } finally {
      setStartingProjectId(null);
    }
  };

  const handleGenerateProjectDraft = async (
    resumeProject: ResumeProjectExperience,
  ) => {
    if (!activeResume) {
      return;
    }
    setGeneratingProjectId(resumeProject.id);
    setCreatedProjectId(null);
    try {
      const draft = await generateResumeProjectDraft(
        activeResume.id,
        resumeProject.id,
      );
      setDraftTarget(resumeProject);
      setProjectDraft(draft);
      setDraftForm({
        name: draft.name || resumeProject.projectName,
        description: draft.description || "",
        techStack: draft.techStack || "",
        role: draft.role || "",
        highlights: draft.highlights || "",
        difficulties: draft.difficulties || "",
      });
    } catch (error) {
      message.error(friendlyError(error, "生成项目档案草稿失败"));
    } finally {
      setGeneratingProjectId(null);
    }
  };

  const handleSaveProjectDraft = async () => {
    if (!activeResume || !draftTarget) {
      return;
    }
    if (
      !draftForm.name.trim() ||
      !draftForm.description.trim() ||
      !draftForm.techStack.trim()
    ) {
      message.warning("请至少确认项目名称、描述和技术栈");
      return;
    }
    setSavingDraft(true);
    try {
      const saved = await saveResumeProjectFromDraft(
        activeResume.id,
        draftTarget.id,
        draftForm,
      );
      setActiveResume(saved.resume);
      setCreatedProjectId(saved.projectId);
      message.success("项目档案已保存");
      const projectData = await getProjects(
        { pageNum: 1, pageSize: 100 },
        { silentError: true },
      );
      setProjects(projectData.records || []);
    } catch (error) {
      message.error(friendlyError(error, "保存项目档案失败"));
    } finally {
      setSavingDraft(false);
    }
  };

  const renderCreateArea = () => {
    if (!loading && documents.length === 0) {
      return (
        <section className="resume-create-card resume-create-card--empty">
          <div>
            <span className="workspace-kicker">创建简历档案</span>
            <h2>先上传一份简历文档。</h2>
            <p>支持 PDF / Markdown / TXT。上传后，简历训练会从真实内容里提取项目追问风险。</p>
          </div>
          <Button type="primary" size="large" onClick={() => navigate("/documents")}>
            去我的文档上传
          </Button>
        </section>
      );
    }

    return (
      <section className="resume-create-card">
        <div className="resume-create-card__copy">
          <span className="workspace-kicker">创建简历档案</span>
          <h2>选择一份已上传文档作为简历。</h2>
          <p>系统会找出简历中最可能被追问的项目、技术点和表达风险。</p>
        </div>
        <div className="resume-create-form">
          <Select
            size="large"
            placeholder="选择简历文档"
            value={selectedDocumentId}
            options={parsedDocuments.map((document) => ({
              label: `${document.title} · ${document.fileType}`,
              value: document.id,
            }))}
            onChange={(value) => {
              setSelectedDocumentId(value);
              const doc = parsedDocuments.find((item) => item.id === value);
              if (doc && !title.trim()) {
                setTitle(doc.title);
              }
            }}
          />
          <Input
            size="large"
            placeholder="简历标题，可选"
            value={title}
            onChange={(event) => setTitle(event.target.value)}
          />
          <Input
            size="large"
            placeholder="目标岗位"
            value={targetRole}
            onChange={(event) => setTargetRole(event.target.value)}
          />
          <Button type="primary" size="large" loading={creating} onClick={handleCreate}>
            创建并分析
          </Button>
        </div>
      </section>
    );
  };

  const renderResumeList = () => (
    <section className="resume-list-section">
      <div className="resume-section-heading">
        <div>
          <span className="workspace-kicker">RESUME PROFILES</span>
          <h2>简历档案</h2>
        </div>
        <Button onClick={loadAll} loading={loading}>
          刷新
        </Button>
      </div>

      {resumes.length === 0 ? (
        <div className="resume-empty-state">
          <FileSearchOutlined />
          <h3>还没有简历档案</h3>
          <p>从一份已上传简历开始，让系统帮你把“写在简历上”的内容变成训练问题。</p>
        </div>
      ) : (
        <div className="resume-card-list">
          {resumes.map((resume) => (
            <article
              key={resume.id}
              className={`resume-list-card${activeResume?.id === resume.id ? " is-active" : ""}`}
            >
              <button
                type="button"
                className="resume-list-card__main"
                onClick={() => loadDetail(resume.id)}
              >
                <span className="resume-status-row">
                  <Tag className={`resume-status resume-status--${resume.analysisStatus.toLowerCase()}`}>
                    {statusText[resume.analysisStatus]}
                  </Tag>
                  <span>{formatDate(resume.createdAt)}</span>
                </span>
                <strong>{resume.title}</strong>
                <em>{resume.targetRole || "未指定目标岗位"}</em>
                <p>{resume.summary || resume.errorMessage || "等待分析后生成项目追问风险。"}</p>
              </button>
              <div className="resume-list-card__actions">
                <Button size="small" onClick={() => loadDetail(resume.id)}>
                  查看分析
                </Button>
                <Button
                  size="small"
                  icon={<ReloadOutlined />}
                  loading={analyzingId === resume.id}
                  onClick={() => handleAnalyze(resume.id)}
                >
                  重新分析
                </Button>
                <Popconfirm
                  title="删除简历档案"
                  description="只删除训练档案，不会删除原始文档。"
                  okText="删除"
                  cancelText="取消"
                  okButtonProps={{ danger: true }}
                  onConfirm={() => handleDelete(resume.id)}
                >
                  <Button
                    size="small"
                    danger
                    icon={<DeleteOutlined />}
                    loading={deletingId === resume.id}
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
  );

  const renderAnalysis = () => {
    if (detailLoading) {
      return (
        <section className="resume-analysis-panel resume-loading">
          <Spin />
        </section>
      );
    }
    if (!activeResume) {
      return (
        <section className="resume-analysis-panel resume-empty-state">
          <FileSearchOutlined />
          <h3>选择一份简历查看分析</h3>
          <p>分析结果会把项目经历、风险点和推荐追问连接到项目拷打训练。</p>
        </section>
      );
    }
    const analysis = activeResume.analysisResult;
    const riskPoints = analysis?.riskPoints || [];
    const questions = analysis?.interviewQuestions || [];
    const suggestions = analysis?.optimizationSuggestions || [];
    const skills = analysis?.skills || [];
    const projects = activeResume.projectExperiences || [];

    return (
      <section className="resume-analysis-panel">
        <div className="resume-analysis-header">
          <div>
            <span className="workspace-kicker">ANALYSIS</span>
            <h2>{activeResume.title}</h2>
            <p>{activeResume.summary || "分析后会展示简历整体摘要。"}</p>
          </div>
          <Button
            type="primary"
            icon={<ReloadOutlined />}
            loading={analyzingId === activeResume.id}
            onClick={() => handleAnalyze(activeResume.id)}
          >
            {activeResume.analysisStatus === "FAILED" ? "重试分析" : "重新分析"}
          </Button>
        </div>

        {activeResume.analysisStatus === "ANALYZING" || analyzingId === activeResume.id ? (
          <div className="resume-analysis-loading">
            <Spin />
            <span>正在从简历中提取项目追问风险...</span>
          </div>
        ) : null}

        {activeResume.analysisStatus === "FAILED" ? (
          <div className="resume-analysis-error">
            <strong>分析失败</strong>
            <p>{activeResume.errorMessage || "AI 分析失败，可以重试。"}</p>
          </div>
        ) : null}

        {skills.length ? (
          <div className="resume-analysis-block">
            <h3>技能关键词</h3>
            <div className="resume-skill-grid">
              {skills.map((skill: ResumeSkill, index) => (
                <div key={`${skill.name}-${index}`} className="resume-skill-card">
                  <div>
                    <strong>{skill.name}</strong>
                    <span>{skill.category || "技术点"}</span>
                  </div>
                  <Tag className={riskLevelClass(skill.riskLevel)}>
                    {riskText[skill.riskLevel || ""] || skill.riskLevel || "中"}
                  </Tag>
                  <p>{skill.reason}</p>
                </div>
              ))}
            </div>
          </div>
        ) : null}

        <div className="resume-analysis-block">
          <h3>高频风险点</h3>
          {riskPoints.length ? (
            <div className="resume-risk-list">
              {riskPoints.map((risk: ResumeRiskPoint, index) => (
                <article key={`${risk.type}-${index}`} className="resume-risk-card">
                  <Tag className={riskLevelClass(risk.level)}>
                    {riskText[risk.level || ""] || risk.level || "中"}风险
                  </Tag>
                  <strong>{risk.type}</strong>
                  <p>{risk.evidence}</p>
                  <em>{risk.suggestion}</em>
                </article>
              ))}
            </div>
          ) : (
            <p className="resume-muted">分析后会展示具体风险点。</p>
          )}
        </div>

        <div className="resume-analysis-block">
          <h3>项目经历</h3>
          {projects.length ? (
            <div className="resume-project-list">
              {projects.map((project) => (
                <article key={project.id} className="resume-project-card">
                  <div className="resume-project-card__header">
                    <div>
                      <strong>{project.projectName}</strong>
                      <p>{project.description || "暂无项目描述"}</p>
                    </div>
                    <div className="resume-project-card__actions">
                      {!project.projectId ? (
                        <Button
                          icon={<FileAddOutlined />}
                          loading={generatingProjectId === project.id}
                          onClick={() => handleGenerateProjectDraft(project)}
                        >
                          生成项目档案
                        </Button>
                      ) : (
                        <Button onClick={() => navigate(`/projects/${project.projectId}/edit`)}>
                          查看项目
                        </Button>
                      )}
                      <Button
                        type="primary"
                        icon={<PlayCircleOutlined />}
                        loading={startingProjectId === project.projectId}
                        onClick={() => startTraining(project)}
                      >
                        开始项目拷打
                      </Button>
                    </div>
                  </div>
                  <div className="resume-project-tags">
                    {(project.techStack || []).map((tech) => (
                      <Tag key={tech}>{tech}</Tag>
                    ))}
                  </div>
                  <div className="resume-project-grid">
                    <div>
                      <span>个人职责</span>
                      <p>{project.role || "需要补充个人职责边界"}</p>
                    </div>
                    <div>
                      <span>关联项目档案</span>
                      <p>
                        {project.projectId
                          ? `${projectNameById.get(project.projectId) || `项目 #${project.projectId}`} · ${project.matchReason || "已关联"}`
                          : "未自动匹配，请手动选择项目档案"}
                      </p>
                      <Button
                        size="small"
                        icon={<LinkOutlined />}
                        onClick={() => openLinkModal(project)}
                      >
                        {project.projectId ? "更换关联" : "选择项目"}
                      </Button>
                    </div>
                  </div>
                  <div className="resume-project-columns">
                    <div>
                      <span>该项目风险点</span>
                      {(project.riskPoints || []).map((risk) => (
                        <p key={risk}>{risk}</p>
                      ))}
                    </div>
                    <div>
                      <span>推荐追问</span>
                      {(project.recommendedQuestions || []).map((question) => (
                        <button
                          key={question}
                          type="button"
                          className="resume-question-line"
                          onClick={() => copyQuestion(question)}
                        >
                          {question}
                          <CopyOutlined />
                        </button>
                      ))}
                    </div>
                  </div>
                </article>
              ))}
            </div>
          ) : (
            <p className="resume-muted">还没有识别到项目经历，可重新分析或补充简历内容。</p>
          )}
        </div>

        <div className="resume-analysis-two-col">
          <div className="resume-analysis-block">
            <h3>推荐面试追问</h3>
            {questions.map((question) => (
              <button
                key={question}
                type="button"
                className="resume-question-line"
                onClick={() => copyQuestion(question)}
              >
                {question}
                <CopyOutlined />
              </button>
            ))}
          </div>
          <div className="resume-analysis-block">
            <h3>表达优化建议</h3>
            {suggestions.map((suggestion) => (
              <p key={suggestion} className="resume-suggestion-line">
                {suggestion}
              </p>
            ))}
          </div>
        </div>
      </section>
    );
  };

  return (
    <div className="workspace-page resumes-page">
      <section className="workspace-hero resumes-hero">
        <p className="workspace-kicker">RESUME INTERVIEW TRAINING</p>
        <h1>简历训练</h1>
        <p>分析你的简历项目经历，提前发现面试官最可能追问的问题。</p>
        <strong>不是帮你包装简历，而是帮你验证简历上的内容是否经得起追问。</strong>
      </section>

      {renderCreateArea()}

      {loading ? (
        <section className="resume-loading">
          <Spin />
        </section>
      ) : (
        <div className="resume-workbench">
          {renderResumeList()}
          {renderAnalysis()}
        </div>
      )}

      <Modal
        title="关联项目档案"
        open={Boolean(linkTarget)}
        onCancel={() => setLinkTarget(null)}
        onOk={confirmLinkProject}
        okText="确认关联"
        cancelText="取消"
        okButtonProps={{ loading: Boolean(linkingProjectId) }}
      >
        <Typography.Paragraph type="secondary">
          选择一个已有项目档案后，就可以从这段简历项目经历直接进入项目拷打训练。
        </Typography.Paragraph>
        {projects.length ? (
          <Select
            value={linkProjectId}
            size="large"
            className="resume-link-select"
            placeholder="选择项目档案"
            options={projects.map((project) => ({
              label: project.name,
              value: project.id,
            }))}
            onChange={setLinkProjectId}
          />
        ) : (
          <div className="resume-link-empty">
            <p>还没有项目档案。</p>
            <Button type="primary" onClick={() => navigate("/projects/new")}>
              先创建项目档案
            </Button>
          </div>
        )}
      </Modal>

      <Modal
        title="确认项目档案草稿"
        open={Boolean(draftTarget)}
        onCancel={() => {
          setDraftTarget(null);
          setProjectDraft(null);
          setCreatedProjectId(null);
        }}
        width={760}
        footer={
          <div className="resume-draft-footer">
            <Button
              onClick={() => {
                setDraftTarget(null);
                setProjectDraft(null);
                setCreatedProjectId(null);
              }}
            >
              关闭
            </Button>
            {createdProjectId ? (
              <>
                <Button onClick={() => navigate(`/projects/${createdProjectId}/edit`)}>
                  查看项目
                </Button>
                <Button
                  type="primary"
                  icon={<PlayCircleOutlined />}
                  loading={startingProjectId === createdProjectId}
                  onClick={() => startTrainingWithProject(createdProjectId)}
                >
                  开始项目拷打
                </Button>
              </>
            ) : (
              <Button
                type="primary"
                loading={savingDraft}
                onClick={handleSaveProjectDraft}
              >
                确认保存为项目档案
              </Button>
            )}
          </div>
        }
      >
        <div className="resume-draft-modal">
          <Typography.Paragraph type="secondary">
            请确认这些内容真实准确，再保存为项目档案。系统不会自动编造指标，待补充内容需要你确认后填写。
          </Typography.Paragraph>
          {projectDraft?.pendingItems?.length ? (
            <div className="resume-draft-pending">
              <strong>待补充项</strong>
              {projectDraft.pendingItems.map((item) => (
                <Tag key={item}>{item}</Tag>
              ))}
            </div>
          ) : null}
          <div className="resume-draft-grid">
            <label>
              <span>项目名称</span>
              <Input
                value={draftForm.name}
                onChange={(event) =>
                  setDraftForm((prev) => ({ ...prev, name: event.target.value }))
                }
              />
            </label>
            <label>
              <span>技术栈</span>
              <Input
                value={draftForm.techStack}
                onChange={(event) =>
                  setDraftForm((prev) => ({
                    ...prev,
                    techStack: event.target.value,
                  }))
                }
              />
            </label>
          </div>
          <label className="resume-draft-field">
            <span>项目描述</span>
            <Input.TextArea
              autoSize={{ minRows: 3, maxRows: 6 }}
              value={draftForm.description}
              onChange={(event) =>
                setDraftForm((prev) => ({
                  ...prev,
                  description: event.target.value,
                }))
              }
            />
          </label>
          <label className="resume-draft-field">
            <span>个人职责 / 负责模块</span>
            <Input.TextArea
              autoSize={{ minRows: 2, maxRows: 5 }}
              value={draftForm.role}
              onChange={(event) =>
                setDraftForm((prev) => ({ ...prev, role: event.target.value }))
              }
            />
          </label>
          <label className="resume-draft-field">
            <span>项目亮点</span>
            <Input.TextArea
              autoSize={{ minRows: 2, maxRows: 5 }}
              value={draftForm.highlights}
              onChange={(event) =>
                setDraftForm((prev) => ({
                  ...prev,
                  highlights: event.target.value,
                }))
              }
            />
          </label>
          <label className="resume-draft-field">
            <span>项目难点 / 可追问风险点</span>
            <Input.TextArea
              autoSize={{ minRows: 3, maxRows: 7 }}
              value={draftForm.difficulties}
              onChange={(event) =>
                setDraftForm((prev) => ({
                  ...prev,
                  difficulties: event.target.value,
                }))
              }
            />
          </label>
          {projectDraft?.safetyNotice ? (
            <p className="resume-draft-notice">{projectDraft.safetyNotice}</p>
          ) : null}
        </div>
      </Modal>
    </div>
  );
}

export default ResumesPage;
