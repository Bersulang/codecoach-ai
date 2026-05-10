import { Button } from "antd";
import { Link } from "react-router-dom";
import "../Workspace/workspace.css";

const categories = [
  "Java 基础",
  "JVM",
  "JUC",
  "MySQL",
  "Redis",
  "Spring",
  "MQ",
  "分布式",
];

function QuestionsPage() {
  return (
    <div className="workspace-page">
      <section className="workspace-hero workspace-hero--compact">
        <p className="workspace-kicker">八股问答</p>
        <h1>八股问答训练</h1>
        <p>
          知识点选择页正在开发中。第一版将围绕 Java 后端高频主题生成追问、
          参考答案、评分和训练报告。
        </p>
      </section>

      <section className="question-category-grid">
        {categories.map((category) => (
          <article key={category} className="question-category-card">
            <span>{category}</span>
            <p>知识点选择页正在开发中</p>
          </article>
        ))}
      </section>

      <div className="workspace-actions">
        <Button type="primary">
          <Link to="/dashboard">返回工作台</Link>
        </Button>
      </div>
    </div>
  );
}

export default QuestionsPage;
