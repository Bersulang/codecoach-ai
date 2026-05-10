import { Button, Form, Input, Typography } from "antd";
import { Link, useNavigate } from "react-router-dom";
import { useState } from "react";
import { login } from "../../api/auth";
import type { LoginRequest } from "../../types/auth";
import "../../styles/auth.css";

function LoginPage() {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);

  const handleFinish = async (values: LoginRequest) => {
    setLoading(true);
    try {
      const data = await login(values);
      localStorage.setItem("token", data.token);
      localStorage.setItem("user", JSON.stringify(data.user));
      navigate("/");
    } catch {
      // Errors are handled by the request interceptor.
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-page">
      <div className="auth-card">
        <div className="auth-brand">CodeCoach AI</div>
        <Typography.Title level={3} className="auth-title">
          欢迎回来
        </Typography.Title>
        <Typography.Text className="auth-subtitle">
          登录后开始你的项目训练
        </Typography.Text>

        <Form layout="vertical" onFinish={handleFinish} className="auth-form">
          <Form.Item
            label="用户名"
            name="username"
            rules={[{ required: true, message: "请输入用户名" }]}
          >
            <Input
              placeholder="请输入用户名"
              size="large"
              autoComplete="username"
            />
          </Form.Item>
          <Form.Item
            label="密码"
            name="password"
            rules={[{ required: true, message: "请输入密码" }]}
          >
            <Input.Password
              placeholder="请输入密码"
              size="large"
              autoComplete="current-password"
            />
          </Form.Item>
          <Form.Item>
            <Button
              type="primary"
              htmlType="submit"
              block
              size="large"
              loading={loading}
              className="auth-primary-button"
            >
              登录
            </Button>
          </Form.Item>
        </Form>

        <div className="auth-footer">
          还没有账号？<Link to="/register">去注册</Link>
        </div>
      </div>
    </div>
  );
}

export default LoginPage;
