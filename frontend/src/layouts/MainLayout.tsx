import { Layout, message } from "antd";
import { useEffect, useState } from "react";
import { Outlet, useNavigate } from "react-router-dom";
import { getCurrentUser } from "../api/auth";
import ProductHeader from "../components/ProductHeader";
import type { CurrentUser } from "../types/auth";
import "./MainLayout.css";

const { Content } = Layout;

function readStoredUser(): CurrentUser | null {
  const storedUser = localStorage.getItem("user");
  if (!storedUser) {
    return null;
  }
  try {
    return JSON.parse(storedUser) as CurrentUser;
  } catch {
    localStorage.removeItem("user");
    return null;
  }
}

function MainLayout() {
  const navigate = useNavigate();
  const [user, setUser] = useState<CurrentUser | null>(() => readStoredUser());

  useEffect(() => {
    const token = localStorage.getItem("token");

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
        localStorage.removeItem("token");
        localStorage.removeItem("user");
        message.error("登录状态失效，请重新登录");
        navigate("/login", { replace: true });
      });

    return () => {
      active = false;
    };
  }, [navigate]);

  return (
    <Layout className="main-layout">
      <ProductHeader user={user} onLogout={() => setUser(null)} />
      <Content className="main-layout__content">
        <Outlet />
      </Content>
    </Layout>
  );
}

export default MainLayout;
