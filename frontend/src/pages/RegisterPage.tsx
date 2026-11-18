import { useState } from 'react';
import { Alert, Button, Card, Form, Input, Typography, message } from 'antd';
import { IdcardOutlined, LockOutlined, MailOutlined, PhoneOutlined, UserOutlined } from '@ant-design/icons';
import { Link, Navigate, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { errorMessage } from '../api/http';
import type { RegisterPayload } from '../api/types';

interface RegisterForm extends RegisterPayload {
  confirmPassword: string;
}

/**
 * 读者自助注册：注册成功后自动登录并跳转到「我的借阅」。
 * 馆员账号不开放注册，只能由管理员在用户管理中创建。
 */
export default function RegisterPage() {
  const { user, register } = useAuth();
  const navigate = useNavigate();
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (user) {
    return <Navigate to="/" replace />;
  }

  const onFinish = async (values: RegisterForm) => {
    setSubmitting(true);
    setError(null);
    try {
      const current = await register({
        username: values.username.trim(),
        password: values.password,
        displayName: values.displayName.trim(),
        phone: values.phone?.trim() || undefined,
        email: values.email?.trim() || undefined,
      });
      message.success(`注册成功，欢迎你，${current.displayName || current.username}`);
      navigate('/my-borrows', { replace: true });
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
      <Card style={{ width: 420, boxShadow: '0 12px 40px rgba(0,0,0,.25)' }}>
        <Typography.Title level={3} style={{ textAlign: 'center', marginBottom: 4 }}>
          读者注册
        </Typography.Title>
        <Typography.Paragraph type="secondary" style={{ textAlign: 'center', marginBottom: 24 }}>
          注册后自动开通借阅证，可在线查询馆藏与借阅记录
        </Typography.Paragraph>
        {error && <Alert type="error" showIcon message={error} style={{ marginBottom: 16 }} />}
        <Form<RegisterForm> layout="vertical" onFinish={onFinish} requiredMark={false}>
          <Form.Item
            name="username"
            label="登录用户名"
            rules={[
              { required: true, message: '请输入用户名' },
              { pattern: /^[A-Za-z0-9_]{3,32}$/, message: '用户名需为 3-32 位字母、数字或下划线' },
            ]}
          >
            <Input prefix={<UserOutlined />} placeholder="请输入用户名" size="large" autoFocus />
          </Form.Item>
          <Form.Item
            name="displayName"
            label="姓名"
            rules={[
              { required: true, message: '请输入姓名' },
              { max: 64, message: '姓名不能超过 64 个字符' },
            ]}
          >
            <Input prefix={<IdcardOutlined />} placeholder="请输入真实姓名" size="large" />
          </Form.Item>
          <Form.Item
            name="password"
            label="密码"
            rules={[
              { required: true, message: '请输入密码' },
              { min: 6, max: 64, message: '密码长度需为 6-64 位' },
            ]}
          >
            <Input.Password prefix={<LockOutlined />} placeholder="请输入密码" size="large" />
          </Form.Item>
          <Form.Item
            name="confirmPassword"
            label="确认密码"
            dependencies={['password']}
            rules={[
              { required: true, message: '请再次输入密码' },
              ({ getFieldValue }) => ({
                validator(_, value) {
                  if (!value || getFieldValue('password') === value) {
                    return Promise.resolve();
                  }
                  return Promise.reject(new Error('两次输入的密码不一致'));
                },
              }),
            ]}
          >
            <Input.Password prefix={<LockOutlined />} placeholder="请再次输入密码" size="large" />
          </Form.Item>
          <Form.Item
            name="phone"
            label="手机号（选填）"
            rules={[{ max: 32, message: '手机号不能超过 32 个字符' }]}
          >
            <Input prefix={<PhoneOutlined />} placeholder="用于联系与逾期提醒" size="large" />
          </Form.Item>
          <Form.Item
            name="email"
            label="邮箱（选填）"
            rules={[{ type: 'email', message: '邮箱格式不正确' }]}
          >
            <Input prefix={<MailOutlined />} placeholder="示例：reader@example.com" size="large" />
          </Form.Item>
          <Button type="primary" htmlType="submit" size="large" block loading={submitting}>
            注册并登录
          </Button>
        </Form>
        <Typography.Paragraph style={{ marginTop: 16, marginBottom: 0, textAlign: 'center' }}>
          已有账号？<Link to="/login">返回登录</Link>
        </Typography.Paragraph>
      </Card>
    </div>
  );
}
