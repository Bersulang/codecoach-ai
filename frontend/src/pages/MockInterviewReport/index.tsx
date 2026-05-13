import { Button, Card, Col, Collapse, Empty, List, Progress, Result, Row, Space, Statistic, Tag, Typography, message } from "antd";
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

function formatCompletionStatus(value?: string) {
  switch (value) {
    case "FOLLOWED_UP":
      return "追问较多";
    case "EARLY_ENDED":
      return "提前结束";
    case "COMPLETED":
      return "按计划完成";
    case "NOT_STARTED":
      return "样本不足";
    default:
      return "已记录";
  }
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
          qaReplay: data.qaReplay || [],
          planSummary: data.planSummary,
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

      <Card title="综合雷达维度" className="mock-report-card">
        <div className="mock-radar-grid">
          {[
            ["技术基础", report?.technicalFoundationScore ?? 0],
            ["项目表达", report?.projectCredibilityScore ?? 0],
            ["简历可信度", report?.projectCredibilityScore ?? 0],
            ["工程思维", report?.stagePerformances?.find((item) => item.stage === "SCENARIO_DESIGN")?.score ?? 0],
            ["追问应对", report?.followUpPressureScore ?? 0],
            ["表达结构", report?.stagePerformances?.find((item) => item.stage === "OPENING")?.score ?? 0],
            ["面试节奏", report?.totalScore ?? 0],
          ].map(([label, value]) => (
            <div key={label} className="mock-radar-item">
              <span>{label}</span>
              <Progress percent={Number(value)} showInfo={false} />
              <strong>{value}</strong>
            </div>
          ))}
        </div>
      </Card>

      <Card title="面试计划摘要" className="mock-report-card mock-report-plan">
        <Typography.Paragraph>
          本场面试按计划覆盖：{report?.planSummary?.coverageSummary || "多阶段技术面试"}
        </Typography.Paragraph>
        <Space wrap>
          {report?.planSummary?.stages
            ?.filter((stage) => stage.suggestedRounds > 0)
            .map((stage) => (
              <Tag key={stage.stage}>
                {stage.stageName} · {stage.suggestedRounds} 轮
              </Tag>
            ))}
        </Space>
      </Card>

      <Card title="阶段表现" className="mock-report-card">
        <div className="mock-stage-list">
          {report?.stagePerformances?.map((stage) => (
            <div key={stage.stage} className="mock-stage-item">
              <div>
                <Space wrap>
                  <strong>{stage.stageName}</strong>
                  <Tag>{formatCompletionStatus(stage.completionStatus)}</Tag>
                  {stage.followUpCount ? <Tag color="orange">追问 {stage.followUpCount} 次</Tag> : null}
                </Space>
                <p>{stage.comment}</p>
                <p>
                  完成 {stage.completedRounds ?? 0}/{stage.suggestedRounds ?? 0} 轮
                </p>
                {stage.deductionReasons?.length ? (
                  <ul className="mock-stage-deductions">
                    {stage.deductionReasons.map((reason) => (
                      <li key={reason}>{reason}</li>
                    ))}
                  </ul>
                ) : null}
              </div>
              <Progress type="circle" percent={stage.score} size={64} />
            </div>
          ))}
        </div>
      </Card>

      <Card title="问答回放" className="mock-report-card">
        {report?.qaReplay?.length ? (
          <Collapse
            items={report.qaReplay.map((item, index) => ({
              key: `${item.stage || "qa"}-${index}`,
              label: (
                <Space wrap>
                  <Tag>{item.stage || "UNKNOWN"}</Tag>
                  <span>{item.question || "问题摘要"}</span>
                  {item.qualityScore !== undefined ? <Tag color="blue">{item.qualityScore} 分</Tag> : null}
                </Space>
              ),
              children: (
                <div className="mock-qa-replay">
                  <p><strong>回答摘要：</strong>{item.answerSummary || "暂无摘要"}</p>
                  <p><strong>风险类型：</strong>{item.riskType || "未标记"}</p>
                  <p><strong>改进建议：</strong>{item.suggestion || "暂无建议"}</p>
                </div>
              ),
            }))}
          />
        ) : (
          <Empty description="暂无可回放问答摘要" />
        )}
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
