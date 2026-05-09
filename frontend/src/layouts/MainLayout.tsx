import { Layout } from "antd";
import { NavLink, Outlet } from "react-router-dom";
import "./MainLayout.css";

const { Header, Content } = Layout;

function MainLayout() {
  return (
    <Layout className="main-layout">
      <Header className="main-layout__header">
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
      </Header>
      <Content className="main-layout__content">
        <Outlet />
      </Content>
    </Layout>
  );
}

export default MainLayout;
