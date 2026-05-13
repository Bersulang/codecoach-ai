import { Button, Form, Result, message } from "antd";
import { FileAddOutlined } from "@ant-design/icons";
import { useEffect, useMemo, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import PageHeader from "../../components/PageHeader";
import PageShell from "../../components/PageShell";
import ProjectForm, {
  type ProjectFormValues,
} from "../../components/ProjectForm";
import SurfaceCard from "../../components/SurfaceCard";
import {
  createProject,
  getProjectDetail,
  updateProject,
} from "../../api/project";
import "../../styles/project-form.css";

function ProjectFormPage() {
  const navigate = useNavigate();
  const { id } = useParams();
  const [form] = Form.useForm<ProjectFormValues>();
  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [loadError, setLoadError] = useState(false);

  const isEdit = Boolean(id);
  const projectId = Number(id);

  const subtitle = useMemo(
    () => (isEdit ? "更新你的项目档案信息" : "填写核心信息以创建项目档案"),
    [isEdit],
  );

  useEffect(() => {
    if (!isEdit || Number.isNaN(projectId)) {
      return;
    }

    let active = true;
    setLoading(true);
    setLoadError(false);
    getProjectDetail(projectId)
      .then((data) => {
        if (!active) {
          return;
        }
        form.setFieldsValue({
          name: data.name,
          description: data.description,
          techStack: data.techStack,
          role: data.role,
          highlights: data.highlights,
          difficulties: data.difficulties,
        });
      })
      .catch(() => {
        if (!active) {
          return;
        }
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
  }, [form, isEdit, projectId]);

  const handleSubmit = async (values: ProjectFormValues) => {
    setSubmitting(true);
    try {
      if (isEdit) {
        await updateProject(projectId, values);
        message.success("项目已更新");
      } else {
        await createProject(values);
        message.success("项目已创建");
      }
      navigate("/projects");
    } catch {
      // Errors are handled by the request interceptor.
    } finally {
      setSubmitting(false);
    }
  };

  const handleCancel = () => {
    navigate("/projects");
  };

  if (isEdit && Number.isNaN(projectId)) {
    return (
      <PageShell className="project-form-page" maxWidth={860}>
        <Result
          status="error"
          title="项目不存在"
          subTitle="请返回项目列表重新选择。"
          extra={
            <Button type="primary" onClick={() => navigate("/projects")}>
              返回项目列表
            </Button>
          }
        />
      </PageShell>
    );
  }

  if (isEdit && loadError) {
    return (
      <PageShell className="project-form-page" maxWidth={860}>
        <Result
          status="error"
          title="项目加载失败"
          subTitle="项目不存在或无权限访问。"
          extra={
            <Button type="primary" onClick={() => navigate("/projects")}>
              返回项目列表
            </Button>
          }
        />
      </PageShell>
    );
  }

  return (
    <PageShell className="project-form-page" maxWidth={860}>
      <PageHeader
        title={isEdit ? "编辑项目" : "新建项目"}
        description={subtitle}
      />
      {!isEdit ? (
        <section className="project-form-resume-entry">
          <div>
            <strong>简历里已经写过项目？</strong>
            <p>可以从简历分析结果一键生成项目档案草稿，确认编辑后再保存。</p>
          </div>
          <Button
            icon={<FileAddOutlined />}
            onClick={() => navigate("/resumes")}
          >
            从简历生成项目档案
          </Button>
        </section>
      ) : null}
      <SurfaceCard className="project-form-card" loading={loading}>
        <ProjectForm
          form={form}
          submitText={isEdit ? "保存修改" : "保存项目"}
          submitting={submitting}
          loading={loading}
          onSubmit={handleSubmit}
          onCancel={handleCancel}
        />
      </SurfaceCard>
    </PageShell>
  );
}

export default ProjectFormPage;
