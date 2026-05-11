import { Button } from "antd";
import { useNavigate } from "react-router-dom";
import EmptyState from "../../components/EmptyState";
import PageHeader from "../../components/PageHeader";
import PageShell from "../../components/PageShell";
import "./index.css";

function LearnArticlePage() {
  const navigate = useNavigate();

  return (
    <PageShell className="learn-article-page">
      <PageHeader
        title="知识文章"
        description="文章详情正在准备中，先从知识学习首页开始。"
        actions={
          <Button type="primary" onClick={() => navigate("/learn")}>
            返回知识学习
          </Button>
        }
      />
      <EmptyState
        description="该学习文章的详细内容正在整理中。"
        action={
          <Button type="primary" onClick={() => navigate("/learn")}>
            返回知识学习
          </Button>
        }
      />
    </PageShell>
  );
}

export default LearnArticlePage;
