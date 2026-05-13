import { Button, Card, Col, Empty, List, Progress, Result, Row, Space, Statistic, Tag, Typography, message } from "antd";
import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { getMockInterviewReport } from "../../api/mockInterview";
import type { MockInterviewReport } from "../../types/mockInterview";
import "./index.css";

function renderList(items?: string[]) {
  if (!items || items.length === 0) {
    return <Empty description="暂无内容" />;
  }
  return <List size="small" dataSource={items} renderItem={(item) => <List.Item>{item}</List.Item>} />;
}

function MockInterviewReportPage() {
  const { sessionId } = useParams();
  const navigate = useNavigate();
  const [report, setReport] = useState<MockInterviewReport | null>(null);
  const [loadError, setLoadError] = useState(false);

  useEffect(() => {
    if (!sessionId) return;
    getMockInterviewReport(sessionId)
      .then((data) =>
        setReport({
          ...data,
          stagePerformances: data.stagePerformances || [],
          strengths: data.strengths || [],
          weaknesses: data.weaknesses || [],
          highRiskAnswers: data.highRiskAnswers || [],
          nextActions: data.nextActions || [],
          recommendedLearning: data.recommendedLearning || [],
          recommendedTraining: data.recommendedTraining || [],
          weaknessTags: data.weaknessTags || [],
        }),
      )
      .catch(() => {
        setLoadError(true);
        message.error("模拟面试报告加载失败");
      });
  }, [sessionId]);

  if (loadError || !sessionId) {
    return (
      <div className="mock-report-page">
        <Result
          status="error"
          title="报告不存在或无权限访问"
          extra={<Button onClick={() => navigate("/mock-interviews")}>返回模拟面试</Button>}
        />
      </div>
    );
  }

  return (
    <div className="mock-report-page">
      <header className="mock-report-header">
        <div>
          <Typography.Text className="mock-report-kicker">Mock Interview Report</Typography.Text>
          <Typography.Title level={3}>真实模拟面试综合报告</Typography.Title>
          <Typography.Paragraph>{report?.summary || "报告生成中..."}</Typography.Paragraph>
          <Space wrap>
            {report?.weaknessTags?.map((tag) => <Tag key={tag}>{tag}</Tag>)}
          </Space>
        </div>
        <Statistic title="总分" value={report?.totalScore ?? 0} suffix="/ 100" />
      </header>

      <Row gutter={[16, 16]} className="mock-report-metrics">
        <Col xs={24} md={6}>
          <Card><Statistic title="样本充分性" value={report?.sampleSufficiency || "—"} /></Card>
        </Col>
        <Col xs={24} md={6}>
          <Card><Statistic title="追问承压" value={report?.followUpPressureScore ?? 0} /></Card>
        </Col>
        <Col xs={24} md={6}>
          <Card><Statistic title="项目可信度" value={report?.projectCredibilityScore ?? 0} /></Card>
        </Col>
        <Col xs={24} md={6}>
          <Card><Statistic title="技术基础" value={report?.technicalFoundationScore ?? 0} /></Card>
        </Col>
      </Row>

      <Card title="阶段表现" className="mock-report-card">
        <div className="mock-stage-list">
          {report?.stagePerformances?.map((stage) => (
            <div key={stage.stage} className="mock-stage-item">
              <div>
                <strong>{stage.stageName}</strong>
                <p>{stage.comment}</p>
              </div>
              <Progress type="circle" percent={stage.score} size={64} />
            </div>
          ))}
        </div>
      </Card>

      <Row gutter={[16, 16]}>
        <Col xs={24} lg={12}>
          <Card title="核心优势" className="mock-report-card">{renderList(report?.strengths)}</Card>
        </Col>
        <Col xs={24} lg={12}>
          <Card title="主要问题" className="mock-report-card">{renderList(report?.weaknesses)}</Card>
        </Col>
        <Col xs={24} lg={12}>
          <Card title="高风险回答" className="mock-report-card">{renderList(report?.highRiskAnswers)}</Card>
        </Col>
        <Col xs={24} lg={12}>
          <Card title="下一步行动" className="mock-report-card">{renderList(report?.nextActions)}</Card>
        </Col>
        <Col xs={24} lg={12}>
          <Card title="推荐学习内容" className="mock-report-card">{renderList(report?.recommendedLearning)}</Card>
        </Col>
        <Col xs={24} lg={12}>
          <Card title="推荐专项训练" className="mock-report-card">{renderList(report?.recommendedTraining)}</Card>
        </Col>
      </Row>

      <div className="mock-report-actions">
        <Button type="primary" onClick={() => navigate("/questions")}>进入专项训练</Button>
        <Button onClick={() => navigate("/learn")}>知识学习</Button>
        <Button onClick={() => navigate("/resumes")}>简历训练</Button>
        <Button onClick={() => navigate("/agent-review")}>复盘 Agent</Button>
      </div>
    </div>
  );
}

export default MockInterviewReportPage;
