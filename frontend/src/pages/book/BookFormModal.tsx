import { useEffect, useState } from 'react';
import { DatePicker, Form, Input, InputNumber, Modal, Select, message } from 'antd';
import dayjs from 'dayjs';
import { bookApi, namedApi } from '../../api';
import { errorMessage } from '../../api/http';
import type { BookPayload, BookVO, NamedVO } from '../../api/types';

interface Props {
  open: boolean;
  record: BookVO | null;
  onCancel: () => void;
  onSaved: () => void;
}

interface FormValues {
  title: string;
  subtitle?: string;
  isbn: string;
  authorIds: number[];
  publisherId?: number;
  categoryId?: number;
  publishDate?: dayjs.Dayjs | null;
  language?: string;
  price?: number;
  totalStock: number;
  coverUrl?: string;
  summary?: string;
  status?: 'AVAILABLE' | 'OFFLINE';
}

export default function BookFormModal({ open, record, onCancel, onSaved }: Props) {
  const [form] = Form.useForm<FormValues>();
  const [authors, setAuthors] = useState<NamedVO[]>([]);
  const [publishers, setPublishers] = useState<NamedVO[]>([]);
  const [categories, setCategories] = useState<NamedVO[]>([]);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!open) return;
    Promise.all([
      namedApi.list('authors', { size: 100, sort: 'name,asc' }),
      namedApi.list('publishers', { size: 100, sort: 'name,asc' }),
      namedApi.list('categories', { size: 100, sort: 'name,asc' }),
    ])
      .then(([a, p, c]) => {
        setAuthors(a.records);
        setPublishers(p.records);
        setCategories(c.records);
      })
      .catch((err) => message.error(errorMessage(err)));
  }, [open]);

  useEffect(() => {
    if (!open) return;
    if (record) {
      form.setFieldsValue({
        title: record.title,
        subtitle: record.subtitle ?? undefined,
        isbn: record.isbn,
        authorIds: record.authors.map((author) => author.id),
        publisherId: record.publisherId ?? undefined,
        categoryId: record.categoryId ?? undefined,
        publishDate: record.publishDate ? dayjs(record.publishDate) : null,
        language: record.language ?? undefined,
        price: record.price ?? undefined,
        totalStock: record.totalStock,
        coverUrl: record.coverUrl ?? undefined,
        summary: record.summary ?? undefined,
        status: record.status,
      });
    } else {
      form.resetFields();
      form.setFieldsValue({ totalStock: 1, language: '中文' });
    }
  }, [open, record, form]);

  const submit = async () => {
    const values = await form.validateFields();
    const payload: BookPayload = {
      title: values.title.trim(),
      subtitle: values.subtitle?.trim() || null,
      isbn: values.isbn.trim(),
      authorIds: values.authorIds,
      publisherId: values.publisherId ?? null,
      categoryId: values.categoryId ?? null,
      publishDate: values.publishDate ? values.publishDate.format('YYYY-MM-DD') : null,
      language: values.language?.trim() || null,
      price: values.price ?? null,
      coverUrl: values.coverUrl?.trim() || null,
      summary: values.summary?.trim() || null,
      totalStock: values.totalStock,
      status: values.status ?? 'AVAILABLE',
    };
    setSaving(true);
    try {
      if (record) {
        await bookApi.update(record.id, payload);
        message.success('图书已更新');
      } else {
        await bookApi.create(payload);
        message.success('图书已新增');
      }
      onSaved();
    } catch (err) {
      message.error(errorMessage(err));
    } finally {
      setSaving(false);
    }
  };

  return (
    <Modal
      open={open}
      title={record ? `编辑图书：${record.title}` : '新增图书'}
      onCancel={onCancel}
      onOk={submit}
      confirmLoading={saving}
      okText="保存"
      cancelText="取消"
      width={720}
      destroyOnClose
      maskClosable={false}
    >
      <Form form={form} layout="vertical" requiredMark="optional">
        <Form.Item name="title" label="书名" rules={[{ required: true, message: '请输入书名' }]}>
          <Input placeholder="例如：三体" maxLength={200} />
        </Form.Item>
        <Form.Item name="subtitle" label="副标题">
          <Input placeholder="选填" maxLength={200} />
        </Form.Item>
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16 }}>
          <Form.Item
            name="isbn"
            label="ISBN"
            rules={[
              { required: true, message: '请输入 ISBN' },
              { pattern: /^[\dXx\-\s]{10,20}$/, message: 'ISBN 仅支持数字、X 和连字符' },
            ]}
          >
            <Input placeholder="9787536692930" maxLength={20} />
          </Form.Item>
          <Form.Item name="authorIds" label="作者" rules={[{ required: true, message: '请至少选择一位作者' }]}>
            <Select
              mode="multiple"
              showSearch
              optionFilterProp="label"
              placeholder="选择作者（可多选）"
              options={authors.map((author) => ({ value: author.id, label: author.name }))}
            />
          </Form.Item>
          <Form.Item name="publisherId" label="出版社">
            <Select
              allowClear
              showSearch
              optionFilterProp="label"
              placeholder="选择出版社"
              options={publishers.map((item) => ({ value: item.id, label: item.name }))}
            />
          </Form.Item>
          <Form.Item name="categoryId" label="分类">
            <Select
              allowClear
              showSearch
              optionFilterProp="label"
              placeholder="选择分类"
              options={categories.map((item) => ({ value: item.id, label: item.name }))}
            />
          </Form.Item>
          <Form.Item name="publishDate" label="出版日期">
            <DatePicker style={{ width: '100%' }} placeholder="选择日期" />
          </Form.Item>
          <Form.Item name="language" label="语言">
            <Input placeholder="中文" maxLength={32} />
          </Form.Item>
          <Form.Item name="price" label="定价（元）">
            <InputNumber style={{ width: '100%' }} min={0} max={99999} precision={2} placeholder="39.00" />
          </Form.Item>
          <Form.Item
            name="totalStock"
            label="总库存"
            extra={record ? `当前可借 ${record.availableStock} 本，已借出 ${record.totalStock - record.availableStock} 本` : undefined}
            rules={[{ required: true, message: '请输入总库存' }]}
          >
            <InputNumber style={{ width: '100%' }} min={0} max={100000} precision={0} />
          </Form.Item>
          <Form.Item name="status" label="状态">
            <Select
              options={[
                { value: 'AVAILABLE', label: '可借' },
                { value: 'OFFLINE', label: '已下架' },
              ]}
            />
          </Form.Item>
          <Form.Item name="coverUrl" label="封面地址">
            <Input placeholder="https://…（选填）" maxLength={500} />
          </Form.Item>
        </div>
        <Form.Item name="summary" label="简介">
          <Input.TextArea rows={3} maxLength={5000} showCount placeholder="内容简介（选填）" />
        </Form.Item>
      </Form>
    </Modal>
  );
}
