import { useState } from 'react';
import { Alert, Button, Card, Form, Input, Typography, message } from 'antd';
import { LockOutlined, UserOutlined } from '@ant-design/icons';
import { Navigate, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { errorMessage } from '../api/http';

interface LoginForm {
  username: string;
  password: string;
}

export default function LoginPage() {
  const { user, login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (user) {
    return <Navigate to="/" replace />;
  }

  const onFinish = async (values: LoginForm) => {
    setSubmitting(true);
    setError(null);
    try {
      const current = await login(values.username.trim(), values.password);
      message.success(`欢迎回来，${current.displayName || current.username}`);
      const params = new URLSearchParams(location.search);
      const redirect = params.get('redirect');
      const fallback = current.roles.includes('READER') && !current.roles.includes('LIBRARIAN') ? '/my-borrows' : '/books';
      navigate(redirect || fallback, { replace: true });
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div
      style={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        background: 'linear-gradient(135deg,#1f3b73 0%,#3b6ea5 100%)',
        padding: 16,
      }}
    >
      <Card style={{ width: 380, boxShadow: '0 12px 40px rgba(0,0,0,.25)' }}>
        <Typography.Title level={3} style={{ textAlign: 'center', marginBottom: 4 }}>
          图书管理系统
        </Typography.Title>
        <Typography.Paragraph type="secondary" style={{ textAlign: 'center', marginBottom: 24 }}>
          馆藏借阅 · 读者服务 · 运营统计
        </Typography.Paragraph>
        {error && <Alert type="error" showIcon message={error} style={{ marginBottom: 16 }} />}
        <Form<LoginForm> layout="vertical" onFinish={onFinish} requiredMark={false} autoComplete="on">
          <Form.Item name="username" label="用户名" rules={[{ required: true, message: '请输入用户名' }]}>
            <Input prefix={<UserOutlined />} placeholder="请输入用户名" size="large" autoFocus />
          </Form.Item>
          <Form.Item name="password" label="密码" rules={[{ required: true, message: '请输入密码' }]}>
            <Input.Password prefix={<LockOutlined />} placeholder="请输入密码" size="large" />
          </Form.Item>
          <Button type="primary" htmlType="submit" size="large" block loading={submitting}>
            登录
          </Button>
        </Form>
        <Typography.Paragraph type="secondary" style={{ marginTop: 20, marginBottom: 0, fontSize: 12 }}>
          初始账号：管理员 admin / Admin@123456（首次登录后请及时修改密码）
        </Typography.Paragraph>
      </Card>
    </div>
  );
}
