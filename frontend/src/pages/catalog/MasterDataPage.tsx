import { useCallback, useEffect, useState } from 'react';
import {
  Button,
  Card,
  Form,
  Input,
  InputNumber,
  Modal,
  Popconfirm,
  Space,
  Table,
  Typography,
  message,
} from 'antd';
import { PlusOutlined, ReloadOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { namedApi, type NamedResource } from '../../api';
import { errorMessage } from '../../api/http';
import type { NamedVO, PageResult } from '../../api/types';

export interface FieldDef {
  name: string;
  label: string;
  type?: 'text' | 'textarea' | 'number';
  required?: boolean;
  max?: number;
  placeholder?: string;
  extra?: string;
}

export interface ColumnDef {
  key: string;
  title: string;
  width?: number;
  ellipsis?: boolean;
}

interface Props {
  resource: NamedResource;
  title: string;
  description: string;
  columns: ColumnDef[];
  fields: FieldDef[];
  keywordPlaceholder: string;
}

/** 作者/出版社/分类共用的一览页：结构一致，仅字段配置不同 */
export default function MasterDataPage({ resource, title, description, columns, fields, keywordPlaceholder }: Props) {
  const [form] = Form.useForm<Record<string, string | number | null | undefined>>();
  const [data, setData] = useState<PageResult<NamedVO>>({ records: [], total: 0, page: 1, size: 10, pages: 0 });
  const [loading, setLoading] = useState(false);
  const [keyword, setKeyword] = useState('');
  const [query, setQuery] = useState({ page: 1, size: 10, keyword: '' });
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<NamedVO | null>(null);
  const [saving, setSaving] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const result = await namedApi.list(resource, {
        page: query.page,
        size: query.size,
        keyword: query.keyword || undefined,
        sort: 'name,asc',
      });
      setData(result);
    } catch (err) {
      message.error(errorMessage(err));
    } finally {
      setLoading(false);
    }
  }, [resource, query]);

  useEffect(() => {
    load();
  }, [load]);

  const openCreate = () => {
    setEditing(null);
    form.resetFields();
    setOpen(true);
  };

  const openEdit = (record: NamedVO) => {
    setEditing(record);
    form.resetFields();
    form.setFieldsValue(record as unknown as Record<string, string | number | null | undefined>);
    setOpen(true);
  };

  const submit = async () => {
    const values = await form.validateFields();
    setSaving(true);
    try {
      if (editing) {
        await namedApi.update(resource, editing.id, values);
        message.success(`${title}已更新`);
      } else {
        await namedApi.create(resource, values);
        message.success(`${title}已新增`);
      }
      setOpen(false);
      load();
    } catch (err) {
      message.error(errorMessage(err));
    } finally {
      setSaving(false);
    }
  };

  const remove = async (record: NamedVO) => {
    try {
      await namedApi.remove(resource, record.id);
      message.success('已删除');
      load();
    } catch (err) {
      message.error(errorMessage(err));
    }
  };

  const tableColumns: ColumnsType<NamedVO> = [
    ...columns.map((column) => ({
      title: column.title,
      dataIndex: column.key,
      width: column.width,
      ellipsis: column.ellipsis,
      render: (value: unknown) => (value === null || value === undefined || value === '' ? '—' : String(value)),
    })),
    {
      title: '操作',
      key: 'actions',
      width: 140,
      fixed: 'right' as const,
      render: (_: unknown, record: NamedVO) => (
        <Space size={4}>
          <Button size="small" type="link" onClick={() => openEdit(record)}>
            编辑
          </Button>
          <Popconfirm
            title="确认删除？"
            description="被图书引用的记录无法删除"
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
      title={`${title}管理`}
      extra={
        <Space wrap>
          <Input.Search
            allowClear
            placeholder={keywordPlaceholder}
            style={{ width: 240 }}
            value={keyword}
            onChange={(event) => setKeyword(event.target.value)}
            onSearch={(value) => setQuery((prev) => ({ ...prev, keyword: value.trim(), page: 1 }))}
          />
          <Button icon={<ReloadOutlined />} onClick={load}>
            刷新
          </Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>
            新增{title}
          </Button>
        </Space>
      }
    >
      <Typography.Paragraph type="secondary">{description}</Typography.Paragraph>
      <Table<NamedVO>
        rowKey="id"
        loading={loading}
        columns={tableColumns}
        dataSource={data.records}
        scroll={{ x: 700 }}
        pagination={{
          current: data.page,
          pageSize: data.size,
          total: data.total,
          showSizeChanger: true,
          showTotal: (total) => `共 ${total} 条`,
          onChange: (page, size) => setQuery((prev) => ({ ...prev, page, size })),
        }}
      />

      <Modal
        open={open}
        title={editing ? `编辑${title}` : `新增${title}`}
        onCancel={() => setOpen(false)}
        onOk={submit}
        confirmLoading={saving}
        okText="保存"
        cancelText="取消"
        destroyOnClose
      >
        <Form form={form} layout="vertical" requiredMark="optional">
          {fields.map((field) => {
            const rules = field.required ? [{ required: true, message: `请输入${field.label}` }] : undefined;
            if (field.type === 'textarea') {
              return (
                <Form.Item key={field.name} name={field.name} label={field.label} rules={rules} extra={field.extra}>
                  <Input.TextArea rows={4} maxLength={field.max ?? 5000} showCount placeholder={field.placeholder} />
                </Form.Item>
              );
            }
            if (field.type === 'number') {
              return (
                <Form.Item key={field.name} name={field.name} label={field.label} rules={rules} extra={field.extra}>
                  <InputNumber style={{ width: '100%' }} min={0} precision={0} placeholder={field.placeholder} />
                </Form.Item>
              );
            }
            return (
              <Form.Item key={field.name} name={field.name} label={field.label} rules={rules} extra={field.extra}>
                <Input maxLength={field.max ?? 200} placeholder={field.placeholder} />
              </Form.Item>
            );
          })}
        </Form>
      </Modal>
    </Card>
  );
}
