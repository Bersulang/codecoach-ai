import { Button } from "antd";
import { Link, useNavigate } from "react-router-dom";
import type { CurrentUser } from "../../types/auth";
import "../Workspace/workspace.css";

function readStoredUser(): CurrentUser | null {
  const stored = localStorage.getItem("user");
  if (!stored) {
    return null;
  }
  try {
    return JSON.parse(stored) as CurrentUser;
  } catch {
    return null;
  }
}

function ProfilePage() {
  const navigate = useNavigate();
  const user = readStoredUser();
  const displayName = user?.nickname || user?.username || "CodeCoach User";
  const initial = displayName.slice(0, 1).toUpperCase();

  const handleLogout = () => {
    localStorage.removeItem("token");
    localStorage.removeItem("user");
    navigate("/", { replace: true });
  };

  return (
    <div className="workspace-page">
      <section className="profile-card">
        <div className="profile-card__avatar">{initial}</div>
        <div>
          <p className="workspace-kicker">个人中心</p>
          <h1>{displayName}</h1>
          <p>登录状态：已登录</p>
        </div>
      </section>

      <section className="workspace-entry-grid">
        <Link to="/dashboard" className="workspace-entry-card">
          <span>工作台</span>
          <p>回到训练入口和近期建议。</p>
        </Link>
        <Link to="/projects" className="workspace-entry-card">
          <span>项目档案</span>
          <p>维护项目经历，开始项目拷打训练。</p>
        </Link>
        <Link to="/history" className="workspace-entry-card">
          <span>训练历史</span>
          <p>查看报告和训练复盘。</p>
        </Link>
      </section>

      <div className="workspace-actions">
        <Button danger onClick={handleLogout}>
          退出登录
        </Button>
      </div>
    </div>
  );
}

export default ProfilePage;
