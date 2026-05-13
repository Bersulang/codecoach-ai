import {
  Alert,
  Button,
  Card,
  Form,
  Input,
  List,
  Select,
  Space,
  Tag,
  Typography,
  message,
} from "antd";
import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  createMockInterview,
  getMockInterviews,
} from "../../api/mockInterview";
import { getProjects } from "../../api/project";
import { getResumes } from "../../api/resume";
import PageShell from "../../components/PageShell";
import type { ProjectVO } from "../../types/project";
import type { ResumeListItem } from "../../types/resume";
import type {
  MockInterviewCreateRequest,
  MockInterviewHistoryItem,
} from "../../types/mockInterview";
import "./index.css";

const typeOptions = [
  { label: "综合技术一面", value: "COMPREHENSIVE_TECHNICAL" },
  { label: "简历项目深挖面", value: "RESUME_PROJECT_DEEP_DIVE" },
  { label: "八股综合面", value: "BA_GU_COMPREHENSIVE" },
];

const difficultyOptions = [
  { label: "入门引导", value: "EASY" },
  { label: "常规技术面", value: "NORMAL" },
  { label: "高压深挖", value: "HARD" },
];

function MockInterviewsPage() {
  const navigate = useNavigate();
  const [form] = Form.useForm<MockInterviewCreateRequest>();
  const [projects, setProjects] = useState<ProjectVO[]>([]);
  const [resumes, setResumes] = useState<ResumeListItem[]>([]);
  const [history, setHistory] = useState<MockInterviewHistoryItem[]>([]);
  const [creating, setCreating] = useState(false);

  const loadData = async () => {
    const [projectPage, resumeList, historyPage] = await Promise.all([
      getProjects({ pageNum: 1, pageSize: 50 }, { silentError: true }),
      getResumes(),
      getMockInterviews({ pageNum: 1, pageSize: 8 }),
    ]);
    setProjects(projectPage.records || []);
    setResumes(resumeList || []);
    setHistory(historyPage.records || []);
  };

  useEffect(() => {
    void loadData().catch(() => {
      message.warning("部分模拟面试配置数据加载失败，可先开始通用模拟面试");
    });
  }, []);

  const handleStart = async (values: MockInterviewCreateRequest) => {
    setCreating(true);
    try {
      const response = await createMockInterview({
        ...values,
        maxRound: Number(values.maxRound || 6),
      });
      navigate(`/mock-interviews/${response.sessionId}`);
    } finally {
      setCreating(false);
    }
  };

  return (
    <PageShell className="mock-page">
      <section className="mock-hero">
        <div>
          <span>Mock Interview</span>
          <h1>真实模拟面试</h1>
          <p>
            按 Java 后端技术面节奏推进，从开场、简历项目、技术基础到场景设计，结束后生成综合报告。
          </p>
        </div>
      </section>

      <div className="mock-grid">
        <Card className="mock-create-card" title="创建模拟面试">
          {projects.length === 0 || resumes.length === 0 ? (
            <Alert
              type="info"
              showIcon
              className="mock-hint"
              message="没有项目或简历也可以开始，系统会自动降级为通用 Java 后端模拟面试。"
            />
          ) : null}
          <Form
            form={form}
            layout="vertical"
            initialValues={{
              interviewType: "COMPREHENSIVE_TECHNICAL",
              targetRole: "Java 后端开发工程师",
              difficulty: "NORMAL",
              maxRound: 6,
            }}
            onFinish={handleStart}
          >
            <Form.Item label="面试类型" name="interviewType" required>
              <Select options={typeOptions} />
            </Form.Item>
            <Form.Item
              label="目标岗位"
              name="targetRole"
              rules={[{ required: true, message: "请输入目标岗位" }]}
            >
              <Input placeholder="例如：Java 后端实习 / Java 开发工程师" />
            </Form.Item>
            <div className="mock-form-row">
              <Form.Item label="难度" name="difficulty" required>
                <Select options={difficultyOptions} />
              </Form.Item>
              <Form.Item label="轮数" name="maxRound" required>
                <Select
                  options={[4, 5, 6, 8, 10, 12].map((value) => ({
                    label: `${value} 轮`,
                    value,
                  }))}
                />
              </Form.Item>
            </div>
            <Form.Item label="关联项目（可选）" name="projectId">
              <Select
                allowClear
                placeholder={
                  projects.length ? "选择一个项目档案" : "暂无项目档案"
                }
                options={projects.map((project) => ({
                  label: project.name,
                  value: project.id,
                }))}
              />
            </Form.Item>
            <Form.Item label="关联简历（可选）" name="resumeId">
              <Select
                allowClear
                placeholder={resumes.length ? "选择一份简历" : "暂无简历"}
                options={resumes.map((resume) => ({
                  label: resume.title,
                  value: resume.id,
                }))}
              />
            </Form.Item>
            <Button type="primary" htmlType="submit" size="large" loading={creating} block>
              开始模拟面试
            </Button>
          </Form>
        </Card>

        <Card className="mock-history-card" title="最近模拟面试">
          <List
            dataSource={history}
            locale={{ emptyText: "暂无模拟面试记录" }}
            renderItem={(item) => (
              <List.Item
                actions={[
                  <Button
                    key="open"
                    type="link"
                    onClick={() =>
                      navigate(
                        item.reportId
                          ? `/mock-interviews/${item.sessionId}/report`
                          : `/mock-interviews/${item.sessionId}`,
                      )
                    }
                  >
                    {item.reportId ? "看报告" : "继续"}
                  </Button>,
                ]}
              >
                <List.Item.Meta
                  title={
                    <Space>
                      <Typography.Text strong>{item.targetRole}</Typography.Text>
                      <Tag>{item.currentStage}</Tag>
                      {item.totalScore !== undefined ? (
                        <Tag color="blue">{item.totalScore} 分</Tag>
                      ) : null}
                    </Space>
                  }
                  description={`${item.currentRound}/${item.maxRound} 轮 · ${item.status}`}
                />
              </List.Item>
            )}
          />
        </Card>
      </div>
    </PageShell>
  );
}

export default MockInterviewsPage;
