import { Button } from "antd";
import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { getInsightOverview } from "../../api/insight";
import type { InsightOverview } from "../../types/insight";
import "../Workspace/workspace.css";

const entries = [
  {
    title: "项目拷打",
    desc: "从项目档案出发，训练架构设计、异常场景和技术取舍表达。",
    to: "/projects",
    action: "维护项目",
  },
  {
    title: "八股问答",
    desc: "围绕 Java 后端知识点进行连续追问、反馈和参考答案训练。",
    to: "/questions",
    action: "选择知识点",
  },
  {
    title: "知识学习",
    desc: "用面试表达型知识卡片补齐薄弱点，再回到专项训练。",
    to: "/learn",
    action: "开始学习",
  },
  {
    title: "训练历史",
    desc: "回看训练记录和报告，把每一次回答变成下一次的改进线索。",
    to: "/history",
    action: "查看记录",
  },
];

function formatScore(value?: number | null) {
  return value === null || value === undefined ? "暂无" : `${value}`;
}

function DashboardPage() {
  const [overview, setOverview] = useState<InsightOverview | null>(null);

  useEffect(() => {
    let active = true;
    getInsightOverview()
      .then((data) => {
        if (active) {
          setOverview(data);
        }
      })
      .catch(() => {
        if (active) {
          setOverview(null);
        }
      });
    return () => {
      active = false;
    };
  }, []);

  const hasTrainingData = Boolean(
    overview?.totalTrainingCount && overview.totalTrainingCount > 0,
  );

  return (
    <div className="workspace-page dashboard-page">
      <section className="workspace-hero dashboard-hero">
        <p className="workspace-kicker">训练工作台</p>
        <h1>欢迎回来，继续把技术表达磨到锋利。</h1>
        <p>
          从项目拷打、八股问答到训练报告，CodeCoach AI 帮你把“知道”
          变成面试里讲得清楚、讲得可信。
        </p>
      </section>

      <section className="dashboard-suggestion-card">
        <div>
          <span className="workspace-kicker">今日建议</span>
          <h2>
            {hasTrainingData
              ? `优先加强「${overview?.weakestDimension || "表达结构"}」`
              : "先完成一次训练，让系统认识你的表达状态。"}
          </h2>
          <p>
            {hasTrainingData
              ? "成长洞察已经沉淀了你的近期训练表现，可以先补齐薄弱维度，再回到专项训练。"
              : "完成一次项目拷打或八股问答后，这里会给出更具体的下一步训练建议。"}
          </p>
        </div>
        <Button type="primary" size="large" href="/insights">
          查看成长洞察
        </Button>
      </section>

      <section className="dashboard-stats-grid">
        <div className="dashboard-stat-card">
          <span>训练次数</span>
          <strong>{overview?.totalTrainingCount ?? 0}</strong>
        </div>
        <div className="dashboard-stat-card">
          <span>平均分</span>
          <strong>{formatScore(overview?.averageScore)}</strong>
        </div>
        <div className="dashboard-stat-card">
          <span>待加强维度</span>
          <strong>{overview?.weakestDimension || "暂无"}</strong>
        </div>
      </section>

      <section className="workspace-entry-grid dashboard-entry-grid">
        {entries.map((entry) => (
          <Link key={entry.to} to={entry.to} className="workspace-entry-card">
            <span>{entry.title}</span>
            <p>{entry.desc}</p>
            <em>{entry.action}</em>
          </Link>
        ))}
      </section>
    </div>
  );
}

export default DashboardPage;
