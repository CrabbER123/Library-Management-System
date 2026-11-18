import { useCallback, useEffect, useState } from 'react';
import {
  Alert,
  Button,
  Card,
  Form,
  Input,
  Modal,
  Popconfirm,
  Select,
  Space,
  Switch,
  Table,
  Tag,
  Typography,
  message,
} from 'antd';
import { PlusOutlined, ReloadOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import dayjs from 'dayjs';
import { bookApi, borrowApi, configApi, readerApi } from '../../api';
import { errorMessage } from '../../api/http';
import type { BorrowRecordVO, BorrowRules, PageResult, ReaderVO } from '../../api/types';

const EMPTY: PageResult<BorrowRecordVO> = { records: [], total: 0, page: 1, size: 10, pages: 0 };

interface BorrowForm {
  readerId: number;
  bookId: number;
  remark?: string;
}

export default function BorrowsPage() {
  const [form] = Form.useForm<BorrowForm>();
  const [data, setData] = useState<PageResult<BorrowRecordVO>>(EMPTY);
  const [loading, setLoading] = useState(false);
  const [rules, setRules] = useState<BorrowRules | null>(null);
  const [keyword, setKeyword] = useState('');
  const [query, setQuery] = useState({ page: 1, size: 10, keyword: '', status: undefined as string | undefined, overdueOnly: false });
  const [open, setOpen] = useState(false);
  const [saving, setSaving] = useState(false);
  const [readers, setReaders] = useState<ReaderVO[]>([]);
  const [books, setBooks] = useState<{ value: number; label: string }[]>([]);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const result = await borrowApi.list({
        page: query.page,
        size: query.size,
        keyword: query.keyword || undefined,
        status: query.status,
        overdueOnly: query.overdueOnly || undefined,
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

  useEffect(() => {
    configApi.borrowRules().then(setRules).catch(() => setRules(null));
  }, []);

  const searchReaders = (value: string) => {
    readerApi
      // 只允许给正常状态的读者办借书；停用读者由后端再次拦截（READER_INACTIVE）
      .list({ keyword: value || undefined, size: 20, status: 'ACTIVE' })
      .then((result) => setReaders(result.records))
      .catch(() => setReaders([]));
  };

  const searchBooks = (value: string) => {
    bookApi
      .list({ keyword: value || undefined, size: 20, status: 'AVAILABLE' })
      .then((result) =>
        setBooks(
          result.records
            .filter((book) => book.availableStock > 0)
            .map((book) => ({ value: book.id, label: `${book.title}（可借 ${book.availableStock}/${book.totalStock}）` })),
        ),
      )
      .catch(() => setBooks([]));
  };

  const openBorrow = () => {
    form.resetFields();
    setReaders([]);
    setBooks([]);
    searchReaders('');
    searchBooks('');
    setOpen(true);
  };

  const submitBorrow = async () => {
    const values = await form.validateFields();
    setSaving(true);
    try {
      const record = await borrowApi.borrow(values.readerId, values.bookId, values.remark?.trim() || undefined);
      message.success(`借书成功，应还日期 ${record.dueDate}`);
      setOpen(false);
      load();
    } catch (err) {
      message.error(errorMessage(err));
    } finally {
      setSaving(false);
    }
  };

  const doReturn = async (record: BorrowRecordVO) => {
    try {
      const result = await borrowApi.returnBook(record.id);
      if (result.fineAmount && Number(result.fineAmount) > 0) {
        message.warning(`已归还，逾期 ${result.overdueDays} 天，应收罚款 ${Number(result.fineAmount).toFixed(2)} 元`);
      } else {
        message.success('已归还，未产生逾期罚款');
      }
      load();
    } catch (err) {
      message.error(errorMessage(err));
    }
  };

  const doRenew = async (record: BorrowRecordVO) => {
    try {
      const result = await borrowApi.renew(record.id);
      message.success(`续借成功，新的应还日期 ${result.dueDate}（已续借 ${result.renewTimes} 次）`);
      load();
    } catch (err) {
      message.error(errorMessage(err));
    }
  };

  const columns: ColumnsType<BorrowRecordVO> = [
    {
      title: '读者',
      dataIndex: 'readerName',
      width: 150,
      render: (_, record) => (
        <Space direction="vertical" size={0}>
          <Typography.Text strong>{record.readerName ?? `#${record.readerId}`}</Typography.Text>
          <Typography.Text type="secondary" style={{ fontSize: 12 }}>
            {record.readerCardNo ?? '—'}
          </Typography.Text>
        </Space>
      ),
    },
    {
      title: '图书',
      dataIndex: 'bookTitle',
      render: (_, record) => (
        <Space direction="vertical" size={0}>
          <Typography.Text>{record.bookTitle ?? `#${record.bookId}`}</Typography.Text>
          <Typography.Text type="secondary" style={{ fontSize: 12 }}>
            ISBN {record.bookIsbn ?? '—'}
          </Typography.Text>
        </Space>
      ),
    },
    { title: '借出日期', dataIndex: 'borrowDate', width: 110 },
    {
      title: '应还日期',
      dataIndex: 'dueDate',
      width: 120,
      render: (value: string, record) => {
        const overdue = record.status === 'BORROWED' && record.overdueDays > 0;
        return (
          <Typography.Text type={overdue ? 'danger' : undefined}>
            {value}
            {overdue && `（逾期 ${record.overdueDays} 天）`}
          </Typography.Text>
        );
      },
    },
    { title: '归还日期', dataIndex: 'returnDate', width: 110, render: (value: string | null) => value ?? '—' },
    { title: '续借', dataIndex: 'renewTimes', width: 70, render: (value: number) => `${value} 次` },
    {
      title: '状态',
      dataIndex: 'status',
      width: 90,
      render: (status: BorrowRecordVO['status']) =>
        status === 'BORROWED' ? <Tag color="blue">借阅中</Tag> : <Tag color="green">已归还</Tag>,
    },
    {
      title: '罚款（元）',
      dataIndex: 'fineAmount',
      width: 110,
      render: (_, record) => {
        const fine = record.status === 'RETURNED' ? record.fineAmount ?? 0 : record.currentFine;
        return Number(fine) > 0 ? (
          <Typography.Text type="danger">{Number(fine).toFixed(2)}</Typography.Text>
        ) : (
          <Typography.Text type="secondary">0.00</Typography.Text>
        );
      },
    },
    {
      title: '操作',
      key: 'actions',
      width: 150,
      fixed: 'right',
      render: (_, record) =>
        record.status === 'BORROWED' ? (
          <Space size={4}>
            <Popconfirm
              title="确认归还？"
              description="归还后将按逾期天数自动结算罚款"
              okText="归还"
              cancelText="取消"
              onConfirm={() => doReturn(record)}
            >
              <Button size="small" type="link">
                还书
              </Button>
            </Popconfirm>
            <Popconfirm
              title="确认续借？"
              description={rules ? `续借后应还日期延长 ${rules.renewDays} 天，最多 ${rules.maxRenewTimes} 次` : undefined}
              okText="续借"
              cancelText="取消"
              onConfirm={() => doRenew(record)}
            >
              <Button size="small" type="link">
                续借
              </Button>
            </Popconfirm>
          </Space>
        ) : (
          <Typography.Text type="secondary">—</Typography.Text>
        ),
    },
  ];

  return (
    <Card
      title="借阅管理"
      extra={
        <Space wrap>
          <Input.Search
            allowClear
            placeholder="读者姓名 / 证号 / 书名 / ISBN"
            style={{ width: 260 }}
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
              { value: 'BORROWED', label: '借阅中' },
              { value: 'RETURNED', label: '已归还' },
            ]}
            onChange={(value) => setQuery((prev) => ({ ...prev, status: value, page: 1 }))}
          />
          <Space size={4}>
            <span>只看逾期</span>
            <Switch
              checked={query.overdueOnly}
              onChange={(checked) => setQuery((prev) => ({ ...prev, overdueOnly: checked, page: 1 }))}
            />
          </Space>
          <Button icon={<ReloadOutlined />} onClick={load}>
            刷新
          </Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={openBorrow}>
            借书
          </Button>
        </Space>
      }
    >
      {rules && (
        <Alert
          type="info"
          showIcon
          style={{ marginBottom: 16 }}
          message={`借阅规则：借期 ${rules.loanDays} 天 · 最多续借 ${rules.maxRenewTimes} 次（每次 +${rules.renewDays} 天）· 每人最多 ${rules.maxBorrowPerReader} 册 · 逾期罚款 ${rules.finePerDay} 元/天`}
        />
      )}
      <Table<BorrowRecordVO>
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
          showTotal: (total) => `共 ${total} 条记录`,
          onChange: (page, size) => setQuery((prev) => ({ ...prev, page, size })),
        }}
      />

      <Modal
        open={open}
        title="借书"
        onCancel={() => setOpen(false)}
        onOk={submitBorrow}
        confirmLoading={saving}
        okText="确认借出"
        cancelText="取消"
        destroyOnClose
      >
        <Form form={form} layout="vertical" requiredMark="optional">
          <Form.Item name="readerId" label="读者" rules={[{ required: true, message: '请选择读者' }]}>
            <Select
              showSearch
              filterOption={false}
              placeholder="输入姓名或借阅证号搜索"
              onSearch={searchReaders}
              options={readers.map((item) => ({
                value: item.id,
                label: `${item.name}（${item.cardNo}）${item.status === 'SUSPENDED' ? ' · 已停用' : ` · 在借 ${item.borrowingCount}/${item.maxBorrow}`}`,
              }))}
            />
          </Form.Item>
          <Form.Item name="bookId" label="图书" rules={[{ required: true, message: '请选择图书' }]}>
            <Select
              showSearch
              filterOption={false}
              placeholder="输入书名、ISBN 或作者搜索"
              onSearch={searchBooks}
              options={books}
            />
          </Form.Item>
          <Form.Item name="remark" label="备注">
            <Input.TextArea rows={2} maxLength={255} showCount placeholder="选填，例如：读者加急" />
          </Form.Item>
        </Form>
        <Typography.Text type="secondary" style={{ fontSize: 12 }}>
          借出后应还日期为 {dayjs().add(rules?.loanDays ?? 30, 'day').format('YYYY-MM-DD')}，系统会校验库存、读者状态与借阅上限。
        </Typography.Text>
      </Modal>
    </Card>
  );
}
