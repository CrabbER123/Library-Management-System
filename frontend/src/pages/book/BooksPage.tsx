import { useCallback, useEffect, useState } from 'react';
import {
  Button,
  Card,
  Empty,
  Form,
  Image,
  Input,
  InputNumber,
  Modal,
  Popconfirm,
  Select,
  Space,
  Table,
  Tag,
  Tooltip,
  Typography,
  message,
} from 'antd';
import { EditOutlined, PlusOutlined, ReloadOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { bookApi, namedApi } from '../../api';
import { errorMessage } from '../../api/http';
import type { BookVO, NamedVO, PageResult } from '../../api/types';
import { useAuth } from '../../auth/AuthContext';
import BookFormModal from './BookFormModal';

const EMPTY: PageResult<BookVO> = { records: [], total: 0, page: 1, size: 10, pages: 0 };

export default function BooksPage() {
  const { isStaff } = useAuth();
  const [data, setData] = useState<PageResult<BookVO>>(EMPTY);
  const [loading, setLoading] = useState(false);
  const [categories, setCategories] = useState<NamedVO[]>([]);
  const [query, setQuery] = useState({ page: 1, size: 10, keyword: '', categoryId: undefined as number | undefined, status: undefined as string | undefined, sort: 'id,desc' });
  const [keyword, setKeyword] = useState('');
  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState<BookVO | null>(null);
  const [stockTarget, setStockTarget] = useState<BookVO | null>(null);
  const [stockValue, setStockValue] = useState(1);
  const [stockSaving, setStockSaving] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const result = await bookApi.list({
        page: query.page,
        size: query.size,
        keyword: query.keyword || undefined,
        categoryId: query.categoryId,
        status: query.status,
        sort: query.sort,
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
    namedApi
      .list('categories', { size: 100, sort: 'name,asc' })
      .then((result) => setCategories(result.records))
      .catch(() => setCategories([]));
  }, []);

  const changeStatus = async (record: BookVO) => {
    try {
      await bookApi.updateStatus(record.id, record.status === 'AVAILABLE' ? 'OFFLINE' : 'AVAILABLE');
      message.success(record.status === 'AVAILABLE' ? '图书已下架' : '图书已上架');
      load();
    } catch (err) {
      message.error(errorMessage(err));
    }
  };

  const remove = async (record: BookVO) => {
    try {
      await bookApi.remove(record.id);
      message.success('图书已删除');
      load();
    } catch (err) {
      message.error(errorMessage(err));
    }
  };

  const openStock = (record: BookVO) => {
    setStockTarget(record);
    setStockValue(record.totalStock);
  };

  const saveStock = async () => {
    if (!stockTarget) return;
    setStockSaving(true);
    try {
      await bookApi.updateStock(stockTarget.id, stockValue);
      message.success('库存已更新');
      setStockTarget(null);
      load();
    } catch (err) {
      message.error(errorMessage(err));
    } finally {
      setStockSaving(false);
    }
  };

  const columns: ColumnsType<BookVO> = [
    {
      title: '封面',
      dataIndex: 'coverUrl',
      width: 64,
      render: (url: string | null, record) =>
        url ? (
          <Image src={url} width={40} height={56} style={{ objectFit: 'cover', borderRadius: 4 }} fallback="" />
        ) : (
          <div
            style={{
              width: 40,
              height: 56,
              borderRadius: 4,
              background: '#f0f2f5',
              color: '#bfbfbf',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              fontSize: 12,
            }}
            aria-label={`${record.title} 无封面`}
          >
            无
          </div>
        ),
    },
    {
      title: '书名',
      dataIndex: 'title',
      render: (_, record) => (
        <Space direction="vertical" size={0}>
          <Typography.Text strong>{record.title}</Typography.Text>
          {record.subtitle && <Typography.Text type="secondary">{record.subtitle}</Typography.Text>}
          <Typography.Text type="secondary" style={{ fontSize: 12 }}>
            ISBN {record.isbn}
          </Typography.Text>
        </Space>
      ),
    },
    {
      title: '作者',
      dataIndex: 'authors',
      width: 160,
      render: (authors: BookVO['authors']) => (
        <Space size={[0, 4]} wrap>
          {authors.map((author) => (
            <Tag key={author.id}>{author.name}</Tag>
          ))}
        </Space>
      ),
    },
    { title: '出版社', dataIndex: 'publisherName', width: 130, render: (value: string | null) => value ?? '—' },
    { title: '分类', dataIndex: 'categoryName', width: 100, render: (value: string | null) => value ?? '—' },
    {
      title: '库存（可借/总数）',
      dataIndex: 'availableStock',
      width: 130,
      render: (_, record) => (
        <Space direction="vertical" size={0}>
          <Typography.Text strong={record.availableStock > 0} type={record.availableStock === 0 ? 'danger' : undefined}>
            {record.availableStock} / {record.totalStock}
          </Typography.Text>
          <Typography.Text type="secondary" style={{ fontSize: 12 }}>
            借出 {record.totalStock - record.availableStock}
          </Typography.Text>
        </Space>
      ),
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 90,
      render: (status: BookVO['status']) =>
        status === 'AVAILABLE' ? <Tag color="green">可借</Tag> : <Tag color="default">已下架</Tag>,
    },
    {
      title: '简介',
      dataIndex: 'summary',
      width: 90,
      render: (summary: string | null) =>
        summary ? (
          <Tooltip title={summary} placement="left">
            <Typography.Text type="secondary" style={{ cursor: 'help' }}>
              查看
            </Typography.Text>
          </Tooltip>
        ) : (
          '—'
        ),
    },
  ];

  if (isStaff) {
    columns.push({
      title: '操作',
      key: 'actions',
      width: 250,
      fixed: 'right',
      render: (_, record) => (
        <Space size={4} wrap>
          <Button
            size="small"
            type="link"
            icon={<EditOutlined />}
            onClick={() => {
              setEditing(record);
              setFormOpen(true);
            }}
          >
            编辑
          </Button>
          <Button size="small" type="link" onClick={() => openStock(record)}>
            库存
          </Button>
          <Button size="small" type="link" onClick={() => changeStatus(record)}>
            {record.status === 'AVAILABLE' ? '下架' : '上架'}
          </Button>
          <Popconfirm
            title="删除图书"
            description="有借阅记录的图书不能删除，可改为下架。确认删除？"
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
    });
  }

  return (
    <Card
      title="图书查询"
      extra={
        <Space wrap>
          <Input.Search
            allowClear
            placeholder="书名 / 副标题 / ISBN / 作者"
            style={{ width: 260 }}
            value={keyword}
            onChange={(event) => setKeyword(event.target.value)}
            onSearch={(value) => setQuery((prev) => ({ ...prev, keyword: value.trim(), page: 1 }))}
          />
          <Select
            allowClear
            placeholder="分类"
            style={{ width: 140 }}
            value={query.categoryId}
            options={categories.map((item) => ({ value: item.id, label: item.name }))}
            onChange={(value) => setQuery((prev) => ({ ...prev, categoryId: value, page: 1 }))}
          />
          <Select
            allowClear
            placeholder="状态"
            style={{ width: 110 }}
            value={query.status}
            options={[
              { value: 'AVAILABLE', label: '可借' },
              { value: 'OFFLINE', label: '已下架' },
            ]}
            onChange={(value) => setQuery((prev) => ({ ...prev, status: value, page: 1 }))}
          />
          <Select
            style={{ width: 130 }}
            value={query.sort}
            options={[
              { value: 'id,desc', label: '最新入库' },
              { value: 'title,asc', label: '书名 A→Z' },
              { value: 'price,desc', label: '价格从高到低' },
            ]}
            onChange={(value) => setQuery((prev) => ({ ...prev, sort: value, page: 1 }))}
          />
          <Button icon={<ReloadOutlined />} onClick={load}>
            刷新
          </Button>
          {isStaff && (
            <Button
              type="primary"
              icon={<PlusOutlined />}
              onClick={() => {
                setEditing(null);
                setFormOpen(true);
              }}
            >
              新增图书
            </Button>
          )}
        </Space>
      }
    >
      <Table<BookVO>
        rowKey="id"
        loading={loading}
        columns={columns}
        dataSource={data.records}
        scroll={{ x: 1100 }}
        locale={{ emptyText: <Empty description="没有符合条件的图书" /> }}
        pagination={{
          current: data.page,
          pageSize: data.size,
          total: data.total,
          showSizeChanger: true,
          showTotal: (total) => `共 ${total} 种图书`,
          onChange: (page, size) => setQuery((prev) => ({ ...prev, page, size })),
        }}
      />

      <BookFormModal
        open={formOpen}
        record={editing}
        onCancel={() => setFormOpen(false)}
        onSaved={() => {
          setFormOpen(false);
          load();
        }}
      />

      <Modal
        open={!!stockTarget}
        title={stockTarget ? `调整库存：${stockTarget.title}` : '调整库存'}
        onCancel={() => setStockTarget(null)}
        onOk={saveStock}
        confirmLoading={stockSaving}
        okText="保存"
        cancelText="取消"
      >
        <Form layout="vertical">
          <Form.Item
            label="总库存"
            extra={
              stockTarget
                ? `已借出 ${stockTarget.totalStock - stockTarget.availableStock} 本，总库存不能低于已借出数量`
                : undefined
            }
          >
            <InputNumber
              min={0}
              max={100000}
              precision={0}
              style={{ width: '100%' }}
              value={stockValue}
              onChange={(value) => setStockValue(Number(value ?? 0))}
            />
          </Form.Item>
        </Form>
      </Modal>
    </Card>
  );
}
