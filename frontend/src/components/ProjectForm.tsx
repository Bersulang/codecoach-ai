import { Button, Form, Input, Space, Typography } from "antd";
import type { FormInstance } from "antd";
import type { ProjectCreateRequest } from "../types/project";
import SectionHeader from "./SectionHeader";
import "../styles/project-form.css";

export interface ProjectFormValues extends ProjectCreateRequest {}

interface ProjectFormProps {
  form: FormInstance<ProjectFormValues>;
  submitText: string;
  submitting: boolean;
  loading?: boolean;
  onSubmit: (values: ProjectFormValues) => void;
  onCancel: () => void;
}

function ProjectForm({
  form,
  submitText,
  submitting,
  loading = false,
  onSubmit,
  onCancel,
}: ProjectFormProps) {
  return (
    <Form
      form={form}
      layout="vertical"
      onFinish={onSubmit}
      disabled={loading}
      className="project-form"
    >
      <div className="project-form-section">
        <SectionHeader
          title="基本信息"
          description="AI 会基于项目信息生成整体背景与核心追问。"
        />
        <Form.Item
          label="项目名称"
          name="name"
          rules={[{ required: true, message: "请输入项目名称" }]}
        >
          <Input placeholder="例如：电商订单系统" size="large" />
        </Form.Item>
        <Form.Item
          label="项目描述"
          name="description"
          rules={[{ required: true, message: "请输入项目描述" }]}
        >
          <Input.TextArea
            placeholder="简要描述项目背景与核心功能"
            autoSize={{ minRows: 3, maxRows: 6 }}
          />
        </Form.Item>
      </div>

      <div className="project-form-section">
        <SectionHeader
          title="技术信息"
          description="技术栈与负责模块会影响追问深度与方向。"
        />
        <Form.Item
          label="技术栈"
          name="techStack"
          rules={[{ required: true, message: "请输入技术栈" }]}
        >
          <Input.TextArea
            placeholder="例如：Java, Spring Boot, MySQL"
            autoSize={{ minRows: 2, maxRows: 5 }}
          />
        </Form.Item>
        <Form.Item label="负责模块" name="role">
          <Input.TextArea
            placeholder="例如：订单服务、支付模块"
            autoSize={{ minRows: 2, maxRows: 5 }}
          />
        </Form.Item>
      </div>

      <div className="project-form-section">
        <SectionHeader
          title="面试素材"
          description="补充亮点与难点，AI 会更精准地模拟追问。"
        />
        <Form.Item label="项目亮点" name="highlights">
          <Input.TextArea
            placeholder="突出你负责的亮点"
            autoSize={{ minRows: 2, maxRows: 5 }}
          />
        </Form.Item>
        <Form.Item label="项目难点" name="difficulties">
          <Input.TextArea
            placeholder="描述项目中的挑战和解决方案"
            autoSize={{ minRows: 2, maxRows: 5 }}
          />
        </Form.Item>
        <Typography.Text className="project-form-hint">
          提示：描述越具体，AI 追问越贴近真实面试场景。
        </Typography.Text>
      </div>

      <div className="project-form-actions">
        <Space size={12}>
          <Button type="primary" htmlType="submit" loading={submitting}>
            {submitText}
          </Button>
          <Button onClick={onCancel}>取消</Button>
        </Space>
      </div>
    </Form>
  );
}

export default ProjectForm;
