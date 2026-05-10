import {
  Button,
  Card,
  Empty,
  List,
  Result,
  Space,
  Statistic,
  Typography,
  message,
} from "antd";
import { useEffect, useMemo, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { getReportDetail } from "../../api/report";
import { createInterviewSession } from "../../api/interview";
import type { InterviewDifficulty } from "../../types/interview";
import type { InterviewReport, QaReview } from "../../types/report";
import "../../styles/report.css";

const DEFAULT_TARGET_ROLE = "Java 后端实习";
const DEFAULT_DIFFICULTY: InterviewDifficulty = "NORMAL";

const DIFFICULTY_LABELS: Record<InterviewDifficulty, string> = {
  EASY: "入门引导",
  NORMAL: "常规面试",
  HARD: "深度拷打",
};

function formatDifficulty(value?: InterviewDifficulty) {
  if (!value) {
    return "—";
  }
  return DIFFICULTY_LABELS[value] ?? value;
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

function ReportPage() {
  const { reportId } = useParams();
  const navigate = useNavigate();
  const [report, setReport] = useState<InterviewReport | null>(null);
  const [loading, setLoading] = useState(false);
  const [loadError, setLoadError] = useState(false);
  const [restarting, setRestarting] = useState(false);

  useEffect(() => {
    if (!reportId) {
      return;
    }

    let active = true;
    setLoading(true);
    setLoadError(false);
    getReportDetail(reportId)
      .then((data) => {
        if (!active) {
          return;
        }
        setReport({
          ...data,
          strengths: data.strengths || [],
          weaknesses: data.weaknesses || [],
          suggestions: data.suggestions || [],
          qaReview: data.qaReview || [],
        });
      })
      .catch(() => {
        if (!active) {
          return;
        }
        setLoadError(true);
        message.error("训练报告加载失败");
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
  }, [reportId]);

  const handleBack = () => {
    navigate("/projects");
  };

  const handleRestart = async () => {
    if (!report) {
      return;
    }
    const projectId = report.projectId;
    if (!projectId) {
      message.error("无法发起新训练");
      return;
    }
    setRestarting(true);
    try {
      const data = await createInterviewSession({
        projectId,
        targetRole: report.targetRole || DEFAULT_TARGET_ROLE,
        difficulty: report.difficulty || DEFAULT_DIFFICULTY,
      });

      if (!data?.sessionId) {
        message.error("创建训练失败，请稍后重试");
        return;
      }

      navigate(`/interviews/${data.sessionId}`);
    } catch {
      // Errors are handled by the request interceptor.
    } finally {
      setRestarting(false);
    }
  };

  const metaItems = useMemo(
    () =>
      report
        ? [
            { label: "项目名称", value: report.projectName },
            { label: "目标岗位", value: report.targetRole },
            { label: "难度", value: formatDifficulty(report.difficulty) },
            { label: "报告生成", value: formatDate(report.createdAt) },
          ]
        : [],
    [report],
  );

  const renderList = (items: string[]) => {
    if (!items.length) {
      return <Empty description="暂无内容" />;
    }
    return (
      <List
        dataSource={items}
        renderItem={(item) => <List.Item>{item}</List.Item>}
      />
    );
  };

  const qaItems: QaReview[] = report?.qaReview || [];

  if (!reportId) {
    return (
      <div className="report-page">
        <Result
          status="error"
          title="报告不存在"
          subTitle="请返回项目列表重新选择。"
          extra={
            <Button type="primary" onClick={handleBack}>
              返回项目列表
            </Button>
          }
        />
      </div>
    );
  }

  if (loadError) {
    return (
      <div className="report-page">
        <Result
          status="error"
          title="报告加载失败"
          subTitle="报告不存在或无权限访问。"
          extra={
            <Button type="primary" onClick={handleBack}>
              返回项目列表
            </Button>
          }
        />
      </div>
    );
  }

  return (
    <div className="report-page">
      <div className="report-header">
        <div>
          <Typography.Title level={3} className="report-title">
            训练报告
          </Typography.Title>
          <Typography.Text className="report-subtitle">
            聚焦核心反馈，帮助你优化项目表达
          </Typography.Text>
        </div>
        <div className="report-actions">
          <Button onClick={handleBack}>返回项目列表</Button>
          <Button
            type="primary"
            onClick={handleRestart}
            loading={restarting}
            disabled={!report}
          >
            再来一次
          </Button>
        </div>
      </div>

      <Card className="report-card" loading={loading}>
        <div className="report-meta">
          {metaItems.map((item) => (
            <div key={item.label}>
              <span className="report-meta-label">{item.label}</span>
              <span className="report-meta-value">{item.value}</span>
            </div>
          ))}
          <div>
            <span className="report-meta-label">总评分</span>
            <Statistic value={report?.totalScore ?? 0} suffix="/ 100" />
          </div>
        </div>
      </Card>

      <Card className="report-card" loading={loading}>
        <Typography.Title level={4} className="report-section-title">
          总体评价
        </Typography.Title>
        <Typography.Paragraph>
          {report?.summary || "暂无总体评价"}
        </Typography.Paragraph>
      </Card>

      <Card className="report-card" loading={loading}>
        <Typography.Title level={4} className="report-section-title">
          优点
        </Typography.Title>
        {renderList(report?.strengths || [])}
      </Card>

      <Card className="report-card" loading={loading}>
        <Typography.Title level={4} className="report-section-title">
          薄弱点
        </Typography.Title>
        {renderList(report?.weaknesses || [])}
      </Card>

      <Card className="report-card" loading={loading}>
        <Typography.Title level={4} className="report-section-title">
          改进建议
        </Typography.Title>
        {renderList(report?.suggestions || [])}
      </Card>

      <Card className="report-card" loading={loading}>
        <Typography.Title level={4} className="report-section-title">
          问答复盘
        </Typography.Title>
        {qaItems.length === 0 ? (
          <div className="report-empty">
            <Empty description="暂无问答复盘" />
          </div>
        ) : (
          <Space direction="vertical" size={12} style={{ width: "100%" }}>
            {qaItems.map((item, index) => (
              <div key={`${item.question}-${index}`} className="report-qa-card">
                <div>
                  <div className="report-qa-label">问题</div>
                  <div className="report-qa-content">{item.question}</div>
                </div>
                <div style={{ marginTop: 12 }}>
                  <div className="report-qa-label">回答</div>
                  <div className="report-qa-content">{item.answer}</div>
                </div>
                <div style={{ marginTop: 12 }}>
                  <div className="report-qa-label">反馈</div>
                  <div className="report-qa-content">{item.feedback}</div>
                </div>
              </div>
            ))}
          </Space>
        )}
      </Card>
    </div>
  );
}

export default ReportPage;
