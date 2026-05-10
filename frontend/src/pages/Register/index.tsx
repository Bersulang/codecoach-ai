import { Button, Form, Input, Typography, message } from "antd";
import { Link, useNavigate } from "react-router-dom";
import { useState } from "react";
import { register } from "../../api/auth";
import type { RegisterRequest } from "../../types/auth";
import "../../styles/auth.css";

function RegisterPage() {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);

  const handleFinish = async (values: RegisterRequest) => {
    setLoading(true);
    try {
      await register(values);
      message.success("注册成功，请登录后开始训练");
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
          创建账号
        </Typography.Title>
        <Typography.Text className="auth-subtitle">
          注册后即可开始项目拷打训练
        </Typography.Text>

        <Form layout="vertical" onFinish={handleFinish} className="auth-form">
          <Form.Item
            label="用户名"
            name="username"
            rules={[
              { required: true, message: "请输入用户名" },
              { min: 4, max: 20, message: "用户名长度为 4-20" },
            ]}
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
            rules={[
              { required: true, message: "请输入密码" },
              { min: 6, max: 32, message: "密码长度为 6-32" },
            ]}
          >
            <Input.Password
              placeholder="请输入密码"
              size="large"
              autoComplete="new-password"
            />
          </Form.Item>
          <Form.Item
            label="确认密码"
            name="confirmPassword"
            dependencies={["password"]}
            rules={[
              { required: true, message: "请再次输入密码" },
              ({ getFieldValue }) => ({
                validator(_, value) {
                  if (!value || getFieldValue("password") === value) {
                    return Promise.resolve();
                  }
                  return Promise.reject(new Error("两次密码不一致"));
                },
              }),
            ]}
          >
            <Input.Password
              placeholder="请再次输入密码"
              size="large"
              autoComplete="new-password"
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
              注册
            </Button>
          </Form.Item>
        </Form>

        <div className="auth-footer">
          已有账号？<Link to="/login">去登录</Link>
        </div>
      </div>
    </div>
  );
}

export default RegisterPage;
