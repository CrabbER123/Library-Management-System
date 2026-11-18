import { useCallback, useEffect, useState } from 'react';
import {
  Button,
  Card,
  Form,
  Input,
  Modal,
  Popconfirm,
  Select,
  Space,
  Table,
  Tag,
  Typography,
  message,
} from 'antd';
import { PlusOutlined, ReloadOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { userApi } from '../../api';
import { errorMessage } from '../../api/http';
import type { PageResult, UserVO } from '../../api/types';
import { useAuth } from '../../auth/AuthContext';

const EMPTY: PageResult<UserVO> = { records: [], total: 0, page: 1, size: 10, pages: 0 };

const ROLE_LABEL: Record<string, string> = {
  ADMIN: '管理员',
  LIBRARIAN: '馆员',
  READER: '读者',
};

const ROLE_COLOR: Record<string, string> = {
  ADMIN: 'gold',
  LIBRARIAN: 'blue',
  READER: 'green',
};

interface AccountForm {
  username: string;
  password?: string;
  displayName: string;
  email?: string;
  phone?: string;
  roles: string[];
}

export default function UsersPage() {
  const { user: currentUser } = useAuth();
  const [form] = Form.useForm<AccountForm>();
  const [data, setData] = useState<PageResult<UserVO>>(EMPTY);
  const [loading, setLoading] = useState(false);
  const [roles, setRoles] = useState<string[]>(['ADMIN', 'LIBRARIAN', 'READER']);
  const [keyword, setKeyword] = useState('');
  const [query, setQuery] = useState({ page: 1, size: 10, keyword: '', role: undefined as string | undefined, status: undefined as string | undefined });
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<UserVO | null>(null);
  const [saving, setSaving] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const result = await userApi.list({
        page: query.page,
        size: query.size,
        keyword: query.keyword || undefined,
        role: query.role,
        status: query.status,
        sort: 'id,asc',
      });
      setData(result);
    } catch (err) {
      message.error(errorMessage(err));
    } finally {
      setLoading(false);
    }
  }, [query]);

  useEffect(() => {
    load();
  }, [load]);

  useEffect(() => {
    userApi.roles().then(setRoles).catch(() => undefined);
  }, []);

  const openCreate = () => {
    setEditing(null);
    form.resetFields();
    form.setFieldsValue({ roles: ['READER'] });
    setOpen(true);
  };

  const openEdit = (record: UserVO) => {
    setEditing(record);
    form.resetFields();
    form.setFieldsValue({
      username: record.username,
      displayName: record.displayName,
      email: record.email ?? undefined,
      phone: record.phone ?? undefined,
      roles: record.roles,
    });
    setOpen(true);
  };

  const submit = async () => {
    const values = await form.validateFields();
    setSaving(true);
    try {
      if (editing) {
        await userApi.update(editing.id, {
          displayName: values.displayName.trim(),
          email: values.email?.trim() || null,
          phone: values.phone?.trim() || null,
          password: values.password ? values.password : '',
        });
        await userApi.updateRoles(editing.id, values.roles);
        message.success('账号已更新');
      } else {
        await userApi.create({
          username: values.username.trim(),
          password: values.password,
          displayName: values.displayName.trim(),
          email: values.email?.trim() || null,
          phone: values.phone?.trim() || null,
          roles: values.roles,
        });
        message.success('账号已创建');
      }
      setOpen(false);
      load();
    } catch (err) {
      message.error(errorMessage(err));
    } finally {
      setSaving(false);
    }
  };

  const toggleStatus = async (record: UserVO) => {
    try {
      const next = record.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE';
      await userApi.updateStatus(record.id, next);
      message.success(next === 'ACTIVE' ? '账号已启用' : '账号已停用');
      load();
    } catch (err) {
      message.error(errorMessage(err));
    }
  };

  const remove = async (record: UserVO) => {
    try {
      await userApi.remove(record.id);
      message.success('账号已删除');
      load();
    } catch (err) {
      message.error(errorMessage(err));
    }
  };

  const columns: ColumnsType<UserVO> = [
    {
      title: '用户名',
      dataIndex: 'username',
      width: 160,
      render: (username: string, record) => (
        <Space direction="vertical" size={0}>
          <Typography.Text strong>{username}</Typography.Text>
          {record.id === currentUser?.id && (
            <Typography.Text type="secondary" style={{ fontSize: 12 }}>
              当前登录账号
            </Typography.Text>
          )}
        </Space>
      ),
    },
    { title: '姓名', dataIndex: 'displayName', width: 130 },
    {
      title: '角色',
      dataIndex: 'roles',
      width: 180,
      render: (rolesValue: string[]) => (
        <Space size={[0, 4]} wrap>
          {rolesValue.map((role) => (
            <Tag key={role} color={ROLE_COLOR[role]}>
              {ROLE_LABEL[role] ?? role}
            </Tag>
          ))}
        </Space>
      ),
    },
    { title: '邮箱', dataIndex: 'email', ellipsis: true, render: (value: string | null) => value ?? '—' },
    { title: '电话', dataIndex: 'phone', width: 140, render: (value: string | null) => value ?? '—' },
    {
      title: '状态',
      dataIndex: 'status',
      width: 90,
      render: (status: UserVO['status']) =>
        status === 'ACTIVE' ? <Tag color="green">启用</Tag> : <Tag color="red">停用</Tag>,
    },
    {
      title: '最近登录',
      dataIndex: 'lastLoginAt',
      width: 170,
      render: (value: string | null) => value ?? '从未登录',
    },
    {
      title: '操作',
      key: 'actions',
      width: 200,
      fixed: 'right',
      render: (_, record) => (
        <Space size={4}>
          <Button size="small" type="link" onClick={() => openEdit(record)}>
            编辑
          </Button>
          <Button size="small" type="link" onClick={() => toggleStatus(record)}>
            {record.status === 'ACTIVE' ? '停用' : '启用'}
          </Button>
          <Popconfirm
            title="删除账号"
            description="不能删除自己或最后一个管理员，确认删除？"
            okText="删除"
            cancelText="取消"
            onConfirm={() => remove(record)}
          >
            <Button size="small" type="link" danger>
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <Card
      title="用户与角色"
      extra={
        <Space wrap>
          <Input.Search
            allowClear
            placeholder="用户名 / 姓名 / 电话"
            style={{ width: 240 }}
            value={keyword}
            onChange={(event) => setKeyword(event.target.value)}
            onSearch={(value) => setQuery((prev) => ({ ...prev, keyword: value.trim(), page: 1 }))}
          />
          <Select
            allowClear
            placeholder="角色"
            style={{ width: 120 }}
            value={query.role}
            options={roles.map((role) => ({ value: role, label: ROLE_LABEL[role] ?? role }))}
            onChange={(value) => setQuery((prev) => ({ ...prev, role: value, page: 1 }))}
          />
          <Select
            allowClear
            placeholder="状态"
            style={{ width: 110 }}
            value={query.status}
            options={[
              { value: 'ACTIVE', label: '启用' },
              { value: 'DISABLED', label: '停用' },
            ]}
            onChange={(value) => setQuery((prev) => ({ ...prev, status: value, page: 1 }))}
          />
          <Button icon={<ReloadOutlined />} onClick={load}>
            刷新
          </Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>
            新增账号
          </Button>
        </Space>
      }
    >
      <Table<UserVO>
        rowKey="id"
        loading={loading}
        columns={columns}
        dataSource={data.records}
        scroll={{ x: 1150 }}
        pagination={{
          current: data.page,
          pageSize: data.size,
          total: data.total,
          showSizeChanger: true,
          showTotal: (total) => `共 ${total} 个账号`,
          onChange: (page, size) => setQuery((prev) => ({ ...prev, page, size })),
        }}
      />

      <Modal
        open={open}
        title={editing ? `编辑账号：${editing.username}` : '新增账号'}
        onCancel={() => setOpen(false)}
        onOk={submit}
        confirmLoading={saving}
        okText="保存"
        cancelText="取消"
        destroyOnClose
      >
        <Form form={form} layout="vertical" requiredMark="optional">
          <Form.Item
            name="username"
            label="用户名"
            rules={[
              { required: true, message: '请输入用户名' },
              { pattern: /^[a-zA-Z0-9_]{3,32}$/, message: '只能包含字母、数字、下划线，长度 3-32' },
            ]}
          >
            <Input disabled={!!editing} maxLength={32} placeholder="登录用户名" />
          </Form.Item>
          <Form.Item
            name="password"
            label={editing ? '重置密码' : '密码'}
            rules={
              editing
                ? [{ min: 6, max: 64, message: '密码长度需在 6-64 之间' }]
                : [
                    { required: true, message: '请输入密码' },
                    { min: 6, max: 64, message: '密码长度需在 6-64 之间' },
                  ]
            }
            extra={editing ? '留空表示不修改密码' : undefined}
          >
            <Input.Password maxLength={64} placeholder="至少 6 位" autoComplete="new-password" />
          </Form.Item>
          <Form.Item name="displayName" label="姓名" rules={[{ required: true, message: '请输入姓名' }]}>
            <Input maxLength={64} />
          </Form.Item>
          <Form.Item name="email" label="邮箱" rules={[{ type: 'email', message: '邮箱格式不正确' }]}>
            <Input maxLength={128} placeholder="选填" />
          </Form.Item>
          <Form.Item name="phone" label="电话">
            <Input maxLength={32} placeholder="选填" />
          </Form.Item>
          <Form.Item name="roles" label="角色" rules={[{ required: true, message: '至少选择一个角色' }]}>
            <Select
              mode="multiple"
              options={roles.map((role) => ({ value: role, label: ROLE_LABEL[role] ?? role }))}
              placeholder="选择角色"
            />
          </Form.Item>
        </Form>
      </Modal>
    </Card>
  );
}
