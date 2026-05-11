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
import { getQuestionReport } from "../../api/question";
import type { InterviewDifficulty } from "../../types/interview";
import type {
  QuestionReport,
  QuestionReportQaReview,
} from "../../types/question";
import "./index.css";

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

function QuestionReportPage() {
  const { reportId } = useParams();
  const navigate = useNavigate();
  const [report, setReport] = useState<QuestionReport | null>(null);
  const [loading, setLoading] = useState(false);
  const [loadError, setLoadError] = useState(false);

  useEffect(() => {
    if (!reportId) {
      return;
    }

    let active = true;
    setLoading(true);
    setLoadError(false);
    getQuestionReport(reportId)
      .then((data) => {
        if (!active) {
          return;
        }
        setReport({
          ...data,
          strengths: data.strengths || [],
          weaknesses: data.weaknesses || [],
          suggestions: data.suggestions || [],
          knowledgeGaps: data.knowledgeGaps || [],
          qaReview: data.qaReview || [],
        });
      })
      .catch(() => {
        if (!active) {
          return;
        }
        setLoadError(true);
        message.error("八股训练报告加载失败");
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
    navigate("/questions");
  };

  const handleRestart = () => {
    navigate("/questions");
  };

  const metaItems = useMemo(
    () =>
      report
        ? [
            { label: "知识分类", value: report.category },
            { label: "知识点", value: report.topicName },
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

  const qaItems: QuestionReportQaReview[] = report?.qaReview || [];

  if (!reportId) {
    return (
      <div className="question-report-page">
        <Result
          status="error"
          title="报告不存在"
          subTitle="请返回八股问答重新选择。"
          extra={
            <Button type="primary" onClick={handleBack}>
              返回八股问答
            </Button>
          }
        />
      </div>
    );
  }

  if (loadError) {
    return (
      <div className="question-report-page">
        <Result
          status="error"
          title="报告加载失败"
          subTitle="报告不存在或无权限访问。"
          extra={
            <Button type="primary" onClick={handleBack}>
              返回八股问答
            </Button>
          }
        />
      </div>
    );
  }

  return (
    <div className="question-report-page">
      <div className="question-report-header">
        <div>
          <Typography.Title level={3} className="question-report-title">
            八股训练报告
          </Typography.Title>
          <Typography.Text className="question-report-subtitle">
            聚焦知识点掌握情况，明确下一步训练方向
          </Typography.Text>
        </div>
        <div className="question-report-actions">
          <Button onClick={handleBack}>返回八股问答</Button>
          <Button type="primary" onClick={handleRestart} disabled={!report}>
            再练一次
          </Button>
        </div>
      </div>

      <Card className="question-report-card" loading={loading}>
        <div className="question-report-meta">
          {metaItems.map((item) => (
            <div key={item.label}>
              <span className="question-report-meta-label">{item.label}</span>
              <span className="question-report-meta-value">{item.value}</span>
            </div>
          ))}
          <div>
            <span className="question-report-meta-label">总评分</span>
            <Statistic value={report?.totalScore ?? 0} suffix="/ 100" />
          </div>
        </div>
      </Card>

      <Card className="question-report-card" loading={loading}>
        <Typography.Title level={4} className="question-report-section-title">
          总体评价
        </Typography.Title>
        <Typography.Paragraph>
          {report?.summary || "暂无总体评价"}
        </Typography.Paragraph>
      </Card>

      <Card className="question-report-card" loading={loading}>
        <Typography.Title level={4} className="question-report-section-title">
          优点
        </Typography.Title>
        {renderList(report?.strengths || [])}
      </Card>

      <Card className="question-report-card" loading={loading}>
        <Typography.Title level={4} className="question-report-section-title">
          薄弱点
        </Typography.Title>
        {renderList(report?.weaknesses || [])}
      </Card>

      <Card className="question-report-card" loading={loading}>
        <Typography.Title level={4} className="question-report-section-title">
          改进建议
        </Typography.Title>
        {renderList(report?.suggestions || [])}
      </Card>

      <Card className="question-report-card" loading={loading}>
        <Typography.Title level={4} className="question-report-section-title">
          知识盲区
        </Typography.Title>
        {renderList(report?.knowledgeGaps || [])}
      </Card>

      <Card className="question-report-card" loading={loading}>
        <Typography.Title level={4} className="question-report-section-title">
          问答复盘
        </Typography.Title>
        {qaItems.length === 0 ? (
          <div className="question-report-empty">
            <Empty description="暂无问答复盘" />
          </div>
        ) : (
          <Space direction="vertical" size={12} style={{ width: "100%" }}>
            {qaItems.map((item, index) => (
              <div
                key={`${item.question}-${index}`}
                className="question-report-qa-card"
              >
                <div>
                  <div className="question-report-qa-label">问题</div>
                  <div className="question-report-qa-content">
                    {item.question}
                  </div>
                </div>
                <div style={{ marginTop: 12 }}>
                  <div className="question-report-qa-label">我的回答</div>
                  <div className="question-report-qa-content">
                    {item.answer}
                  </div>
                </div>
                <div style={{ marginTop: 12 }}>
                  <div className="question-report-qa-label">参考答案</div>
                  <div className="question-report-qa-content">
                    {item.referenceAnswer}
                  </div>
                </div>
                <div style={{ marginTop: 12 }}>
                  <div className="question-report-qa-label">AI 反馈</div>
                  <div className="question-report-qa-content">
                    {item.feedback}
                  </div>
                </div>
              </div>
            ))}
          </Space>
        )}
      </Card>
    </div>
  );
}

export default QuestionReportPage;
