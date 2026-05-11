import { Dropdown } from "antd";
import type { MenuProps } from "antd";
import { Link, NavLink, useNavigate } from "react-router-dom";
import type { CurrentUser } from "../types/auth";
import "./ProductHeader.css";

interface ProductHeaderProps {
  user?: CurrentUser | null;
  onLogout?: () => void;
}

const productNavItems = [
  { label: "项目档案", to: "/projects" },
  { label: "八股问答", to: "/questions" },
  { label: "知识学习", to: "/learn" },
  { label: "训练历史", to: "/history" },
];

function getDisplayName(user?: CurrentUser | null) {
  return user?.nickname || user?.username || "用户";
}

function getInitial(user?: CurrentUser | null) {
  return getDisplayName(user).slice(0, 1).toUpperCase();
}

function ProductHeader({ user, onLogout }: ProductHeaderProps) {
  const navigate = useNavigate();
  const isAuthed = Boolean(localStorage.getItem("token"));

  const handleLogout = () => {
    localStorage.removeItem("token");
    localStorage.removeItem("user");
    onLogout?.();
    navigate("/", { replace: true });
  };

  const menuItems: MenuProps["items"] = [
    {
      key: "profile",
      label: <Link to="/profile">个人中心</Link>,
    },
    {
      key: "dashboard",
      label: <Link to="/dashboard">工作台</Link>,
    },
    {
      key: "insights",
      label: <Link to="/insights">成长洞察</Link>,
    },
    {
      type: "divider",
    },
    {
      key: "logout",
      label: "退出登录",
      onClick: handleLogout,
    },
  ];

  return (
    <header className="product-header">
      <Link to="/" className="product-brand" aria-label="CodeCoach AI 首页">
        <span className="product-brand__mark">
          <span />
        </span>
        <span className="product-brand__text">CodeCoach AI</span>
      </Link>

      {isAuthed ? (
        <nav className="glass-product-nav" aria-label="产品导航">
          {productNavItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) =>
                `glass-product-nav__item${isActive ? " is-active" : ""}`
              }
            >
              <span>{item.label}</span>
            </NavLink>
          ))}
        </nav>
      ) : null}

      <div className="product-header__right">
        {isAuthed ? (
          <Dropdown
            menu={{ items: menuItems }}
            trigger={["click"]}
            placement="bottomRight"
          >
            <button type="button" className="user-avatar-menu">
              <span className="user-avatar-menu__avatar">
                {user?.avatarUrl ? (
                  <img src={user.avatarUrl} alt={getDisplayName(user)} />
                ) : (
                  getInitial(user)
                )}
              </span>
            </button>
          </Dropdown>
        ) : (
          <button
            type="button"
            className="product-header__cta"
            onClick={() => navigate("/login")}
          >
            开始训练
          </button>
        )}
      </div>
    </header>
  );
}

export default ProductHeader;
