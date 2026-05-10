import { Avatar, Button, Layout, Space, Typography, message } from "antd";
import { useEffect, useMemo, useState } from "react";
import { NavLink, Outlet, useNavigate } from "react-router-dom";
import { getCurrentUser } from "../api/auth";
import type { CurrentUser } from "../types/auth";
import "./MainLayout.css";

const { Header, Content } = Layout;

function MainLayout() {
  const navigate = useNavigate();
  const [user, setUser] = useState<CurrentUser | null>(null);

  const displayName = useMemo(() => {
    if (!user) {
      return "";
    }
    return user.nickname || user.username || "";
  }, [user]);

  useEffect(() => {
    const token = localStorage.getItem("token");
    const storedUser = localStorage.getItem("user");

    if (storedUser) {
      try {
        const parsed = JSON.parse(storedUser) as CurrentUser;
        setUser(parsed);
        return;
      } catch {
        localStorage.removeItem("user");
      }
    }

    if (!token) {
      return;
    }

    let active = true;
    getCurrentUser()
      .then((data) => {
        if (!active) {
          return;
        }
        setUser(data);
        localStorage.setItem("user", JSON.stringify(data));
      })
      .catch(() => {
        if (!active) {
          return;
        }
        const hadToken = Boolean(localStorage.getItem("token"));
        localStorage.removeItem("token");
        localStorage.removeItem("user");
        if (hadToken) {
          message.error("登录状态失效，请重新登录");
        }
        navigate("/login", { replace: true });
      });

    return () => {
      active = false;
    };
  }, [navigate]);

  const handleLogout = () => {
    localStorage.removeItem("token");
    localStorage.removeItem("user");
    message.success("已退出登录");
    navigate("/login", { replace: true });
  };

  return (
    <Layout className="main-layout">
      <Header className="main-layout__header">
        <div className="main-layout__inner">
          <div className="main-layout__left">
            <div className="main-layout__brand">CodeCoach AI</div>
            <nav className="main-layout__nav">
              <NavLink
                to="/projects"
                className={({ isActive }) =>
                  `main-layout__nav-item${isActive ? " is-active" : ""}`
                }
              >
                我的项目
              </NavLink>
              <NavLink
                to="/history"
                className={({ isActive }) =>
                  `main-layout__nav-item${isActive ? " is-active" : ""}`
                }
              >
                训练历史
              </NavLink>
            </nav>
          </div>
          <div className="main-layout__user">
            <Space size={12} align="center">
              <Avatar
                size={28}
                src={user?.avatarUrl}
                className="main-layout__avatar"
              >
                {displayName ? displayName.slice(0, 1).toUpperCase() : "?"}
              </Avatar>
              <Typography.Text className="main-layout__username">
                {displayName || "—"}
              </Typography.Text>
              <Button type="text" onClick={handleLogout}>
                退出登录
              </Button>
            </Space>
          </div>
        </div>
      </Header>
      <Content className="main-layout__content">
        <Outlet />
      </Content>
    </Layout>
  );
}

export default MainLayout;
