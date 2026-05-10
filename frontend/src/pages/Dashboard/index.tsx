import { Button } from "antd";
import { Link } from "react-router-dom";
import "../Workspace/workspace.css";

const entries = [
  {
    title: "项目拷打",
    desc: "围绕项目档案进行多轮架构、异常场景和技术取舍追问。",
    to: "/projects",
  },
  {
    title: "八股问答",
    desc: "按 Java 后端知识点训练标准表达、参考答案和连续追问。",
    to: "/questions",
  },
  {
    title: "训练历史",
    desc: "回看训练记录、分数、报告和下一步改进建议。",
    to: "/history",
  },
];

function DashboardPage() {
  return (
    <div className="workspace-page">
      <section className="workspace-hero">
        <p className="workspace-kicker">工作台</p>
        <h1>欢迎回来，继续把技术表达磨到锋利。</h1>
        <p>
          从项目拷打、八股问答到训练报告，CodeCoach AI 帮你把“知道”
          变成面试里讲得清楚、讲得可信。
        </p>
      </section>

      <section className="workspace-entry-grid">
        {entries.map((entry) => (
          <Link key={entry.to} to={entry.to} className="workspace-entry-card">
            <span>{entry.title}</span>
            <p>{entry.desc}</p>
          </Link>
        ))}
      </section>

      <section className="workspace-panel">
        <div>
          <span className="workspace-kicker">训练建议</span>
          <h2>先从一个真实项目开始。</h2>
          <p>
            选择最近一次最熟悉的项目，把 Redis、MySQL、MQ 和异常补偿讲清楚。
            如果还没有项目档案，先创建一个。
          </p>
        </div>
        <Button type="primary" size="large" href="/projects/new">
          新建项目
        </Button>
      </section>
    </div>
  );
}

export default DashboardPage;
