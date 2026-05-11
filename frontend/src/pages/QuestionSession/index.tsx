import { Button, Result } from "antd";
import { useNavigate, useParams } from "react-router-dom";
import PageShell from "../../components/PageShell";
import "./index.css";

function QuestionSessionPage() {
  const navigate = useNavigate();
  const { sessionId } = useParams();

  return (
    <PageShell className="question-session-page">
      <Result
        status="info"
        title="八股训练页开发中"
        subTitle={`训练会话 ${sessionId || ""} 已创建，可返回继续选择知识点。`}
        extra={
          <Button type="primary" onClick={() => navigate("/questions")}>
            返回知识点选择
          </Button>
        }
      />
    </PageShell>
  );
}

export default QuestionSessionPage;
