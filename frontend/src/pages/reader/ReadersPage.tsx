import { useCallback, useEffect, useState } from 'react';
import {
  Button,
  Card,
  Form,
  Input,
  InputNumber,
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
import { readerApi } from '../../api';
import { errorMessage } from '../../api/http';
import type { PageResult, ReaderPayload, ReaderVO } from '../../api/types';

const EMPTY: PageResult<ReaderVO> = { records: [], total: 0, page: 1, size: 10, pages: 0 };

export default function ReadersPage() {
  const [form] = Form.useForm<ReaderPayload>();
  const [data, setData] = useState<PageResult<ReaderVO>>(EMPTY);
  const [loading, setLoading] = useState(false);
  const [query, setQuery] = useState({ page: 1, size: 10, keyword: '', status: undefined as string | undefined });
  const [keyword, setKeyword] = useState('');
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<ReaderVO | null>(null);
  const [saving, setSaving] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const result = await readerApi.list({
        page: query.page,
        size: query.size,
        keyword: query.keyword || undefined,
        status: query.status,
        sort: 'id,desc',
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

  const openCreate = () => {
    setEditing(null);
    form.resetFields();
    form.setFieldsValue({ status: 'ACTIVE' });
    setOpen(true);
  };

  const openEdit = (record: ReaderVO) => {
    setEditing(record);
    form.resetFields();
    form.setFieldsValue({
      cardNo: record.cardNo,
      name: record.name,
      phone: record.phone,
      email: record.email,
      status: record.status,
      maxBorrow: record.maxBorrow,
      userId: record.userId,
    });
    setOpen(true);
  };

  const submit = async () => {
    const values = await form.validateFields();
    const payload: ReaderPayload = {
      cardNo: values.cardNo.trim(),
      name: values.name.trim(),
      phone: values.phone?.trim() || null,
      email: values.email?.trim() || null,
      status: values.status,
      maxBorrow: values.maxBorrow ?? null,
      userId: values.userId ?? null,
    };
    setSaving(true);
    try {
      if (editing) {
        await readerApi.update(editing.id, payload);
        message.success('读者已更新');
      } else {
        await readerApi.create(payload);
        message.success('读者已新增');
      }
      setOpen(false);
      load();
    } catch (err) {
      message.error(errorMessage(err));
    } finally {
      setSaving(false);
    }
  };

  const toggleStatus = async (record: ReaderVO) => {
    try {
      const next = record.status === 'ACTIVE' ? 'SUSPENDED' : 'ACTIVE';
      await readerApi.updateStatus(record.id, next);
      message.success(next === 'ACTIVE' ? '读者已启用' : '读者已停用');
      load();
    } catch (err) {
      message.error(errorMessage(err));
    }
  };

  const remove = async (record: ReaderVO) => {
    try {
      await readerApi.remove(record.id);
      message.success('读者已删除');
      load();
    } catch (err) {
      message.error(errorMessage(err));
    }
  };

  const columns: ColumnsType<ReaderVO> = [
    { title: '借阅证号', dataIndex: 'cardNo', width: 140 },
    {
      title: '姓名',
      dataIndex: 'name',
      width: 120,
      render: (name: string, record) => (
        <Space direction="vertical" size={0}>
          <Typography.Text strong>{name}</Typography.Text>
          {record.userId && (
            <Typography.Text type="secondary" style={{ fontSize: 12 }}>
              已绑定账号 #{record.userId}
            </Typography.Text>
          )}
        </Space>
      ),
    },
    { title: '电话', dataIndex: 'phone', width: 140, render: (value: string | null) => value ?? '—' },
    { title: '邮箱', dataIndex: 'email', ellipsis: true, render: (value: string | null) => value ?? '—' },
    {
      title: '状态',
      dataIndex: 'status',
      width: 90,
      render: (status: ReaderVO['status']) =>
        status === 'ACTIVE' ? <Tag color="green">正常</Tag> : <Tag color="red">停用</Tag>,
    },
    {
      title: '在借 / 上限',
      dataIndex: 'borrowingCount',
      width: 110,
      render: (_, record) => `${record.borrowingCount} / ${record.maxBorrow}`,
    },
    {
      title: '操作',
      key: 'actions',
      width: 200,
      render: (_, record) => (
        <Space size={4}>
          <Button size="small" type="link" onClick={() => openEdit(record)}>
            编辑
          </Button>
          <Popconfirm
            title={record.status === 'ACTIVE' ? '停用读者' : '启用读者'}
            description={
              record.status === 'ACTIVE'
                ? '停用后该读者不能再借阅、归还或续借图书，已借出的图书仍会计入逾期。确认停用？'
                : '启用后该读者恢复借阅、归还、续借权限。确认启用？'
            }
            onConfirm={() => toggleStatus(record)}
          >
            <Button size="small" type="link">
              {record.status === 'ACTIVE' ? '停用' : '启用'}
            </Button>
          </Popconfirm>
          <Popconfirm
            title="删除读者"
            description="有借阅记录的读者不能删除，可改为停用。确认删除？"
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
      title="读者管理"
      extra={
        <Space wrap>
          <Input.Search
            allowClear
            placeholder="证号 / 姓名 / 电话"
            style={{ width: 240 }}
            value={keyword}
            onChange={(event) => setKeyword(event.target.value)}
            onSearch={(value) => setQuery((prev) => ({ ...prev, keyword: value.trim(), page: 1 }))}
          />
          <Select
            allowClear
            placeholder="状态"
            style={{ width: 110 }}
            value={query.status}
            options={[
              { value: 'ACTIVE', label: '正常' },
              { value: 'SUSPENDED', label: '停用' },
            ]}
            onChange={(value) => setQuery((prev) => ({ ...prev, status: value, page: 1 }))}
          />
          <Button icon={<ReloadOutlined />} onClick={load}>
            刷新
          </Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>
            新增读者
          </Button>
        </Space>
      }
    >
      <Table<ReaderVO>
        rowKey="id"
        loading={loading}
        columns={columns}
        dataSource={data.records}
        scroll={{ x: 900 }}
        pagination={{
          current: data.page,
          pageSize: data.size,
          total: data.total,
          showSizeChanger: true,
          showTotal: (total) => `共 ${total} 位读者`,
          onChange: (page, size) => setQuery((prev) => ({ ...prev, page, size })),
        }}
      />

      <Modal
        open={open}
        title={editing ? `编辑读者：${editing.name}` : '新增读者'}
        onCancel={() => setOpen(false)}
        onOk={submit}
        confirmLoading={saving}
        okText="保存"
        cancelText="取消"
        destroyOnClose
      >
        <Form form={form} layout="vertical" requiredMark="optional">
          <Form.Item name="cardNo" label="借阅证号" rules={[{ required: true, message: '请输入借阅证号' }]}>
            <Input maxLength={32} placeholder="例如：R2024002" />
          </Form.Item>
          <Form.Item name="name" label="姓名" rules={[{ required: true, message: '请输入姓名' }]}>
            <Input maxLength={64} placeholder="读者姓名" />
          </Form.Item>
          <Form.Item name="phone" label="电话">
            <Input maxLength={32} placeholder="选填" />
          </Form.Item>
          <Form.Item name="email" label="邮箱" rules={[{ type: 'email', message: '邮箱格式不正确' }]}>
            <Input maxLength={128} placeholder="选填" />
          </Form.Item>
          <Form.Item name="status" label="状态">
            <Select
              options={[
                { value: 'ACTIVE', label: '正常' },
                { value: 'SUSPENDED', label: '停用（不能借阅、归还、续借）' },
              ]}
            />
          </Form.Item>
          <Form.Item name="maxBorrow" label="最大借阅数" extra="留空则使用系统默认值（5 册）">
            <InputNumber min={0} max={100} precision={0} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="userId" label="绑定登录账号 ID" extra="读者自助查询借阅记录需要绑定账号；留空表示暂不绑定">
            <InputNumber min={1} precision={0} style={{ width: '100%' }} placeholder="例如：3" />
          </Form.Item>
        </Form>
      </Modal>
    </Card>
  );
}
