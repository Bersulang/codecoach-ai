import { Button, Card, Form, Input, Space, Typography } from 'antd'
import type { FormInstance } from 'antd'
import type { ProjectCreateRequest } from '../types/project'
import '../styles/project-form.css'

export interface ProjectFormValues extends ProjectCreateRequest {}

interface ProjectFormProps {
  title: string
  subtitle?: string
  form: FormInstance<ProjectFormValues>
  submitText: string
  submitting: boolean
  loading?: boolean
  onSubmit: (values: ProjectFormValues) => void
  onCancel: () => void
}

function ProjectForm({
  title,
  subtitle,
  form,
  submitText,
  submitting,
  loading = false,
  onSubmit,
  onCancel,
}: ProjectFormProps) {
  return (
    <Card className="project-form-card" loading={loading} bordered={false}>
      <div className="project-form-header">
        <Typography.Title level={3} className="project-form-title">
          {title}
        </Typography.Title>
        {subtitle ? (
          <Typography.Text className="project-form-subtitle">
            {subtitle}
          </Typography.Text>
        ) : null}
      </div>
      <Form
        form={form}
        layout="vertical"
        onFinish={onSubmit}
        disabled={loading}
        className="project-form"
      >
        <Form.Item
          label="项目名称"
          name="name"
          rules={[{ required: true, message: '请输入项目名称' }]}
        >
          <Input placeholder="例如：电商订单系统" size="large" />
        </Form.Item>
        <Form.Item
          label="项目描述"
          name="description"
          rules={[{ required: true, message: '请输入项目描述' }]}
        >
          <Input.TextArea
            placeholder="简要描述项目背景与核心功能"
            rows={4}
          />
        </Form.Item>
        <Form.Item
          label="技术栈"
          name="techStack"
          rules={[{ required: true, message: '请输入技术栈' }]}
        >
          <Input.TextArea placeholder="例如：Java, Spring Boot, MySQL" rows={3} />
        </Form.Item>
        <Form.Item label="负责模块" name="role">
          <Input.TextArea placeholder="例如：订单服务、支付模块" rows={3} />
        </Form.Item>
        <Form.Item label="项目亮点" name="highlights">
          <Input.TextArea placeholder="突出你负责的亮点" rows={3} />
        </Form.Item>
        <Form.Item label="项目难点" name="difficulties">
          <Input.TextArea placeholder="描述项目中的挑战和解决方案" rows={3} />
        </Form.Item>
        <Space className="project-form-actions" size={12}>
          <Button type="primary" htmlType="submit" loading={submitting}>
            {submitText}
          </Button>
          <Button onClick={onCancel}>取消</Button>
        </Space>
      </Form>
    </Card>
  )
}

export default ProjectForm
