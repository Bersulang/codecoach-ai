import { Button, Result } from "antd";
import { useNavigate, useParams } from "react-router-dom";
import PageShell from "../../components/PageShell";
import "./index.css";

function QuestionReportPage() {
  const navigate = useNavigate();
  const { reportId } = useParams();

  return (
    <PageShell className="question-report-page">
      <Result
        status="info"
        title="八股训练报告开发中"
        subTitle={`报告 ${reportId || ""} 已生成，可返回继续训练。`}
        extra={
          <Button type="primary" onClick={() => navigate("/questions")}>
            返回知识点选择
          </Button>
        }
      />
    </PageShell>
  );
}

export default QuestionReportPage;
