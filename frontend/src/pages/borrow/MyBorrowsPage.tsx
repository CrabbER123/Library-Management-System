import { useCallback, useEffect, useState } from 'react';
import { Alert, Card, Descriptions, Select, Space, Table, Tag, Typography, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { borrowApi, configApi, readerApi } from '../../api';
import { errorMessage } from '../../api/http';
import type { BorrowRecordVO, BorrowRules, PageResult, ReaderVO } from '../../api/types';

const EMPTY: PageResult<BorrowRecordVO> = { records: [], total: 0, page: 1, size: 10, pages: 0 };

export default function MyBorrowsPage() {
  const [profile, setProfile] = useState<ReaderVO | null>(null);
  const [profileError, setProfileError] = useState<string | null>(null);
  const [rules, setRules] = useState<BorrowRules | null>(null);
  const [data, setData] = useState<PageResult<BorrowRecordVO>>(EMPTY);
  const [loading, setLoading] = useState(false);
  const [status, setStatus] = useState<string | undefined>(undefined);
  const [pagination, setPagination] = useState({ page: 1, size: 10 });

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const result = await borrowApi.my({ page: pagination.page, size: pagination.size, status });
      setData(result);
    } catch (err) {
      message.error(errorMessage(err));
    } finally {
      setLoading(false);
    }
  }, [pagination, status]);

  useEffect(() => {
    load();
  }, [load]);

  useEffect(() => {
    readerApi
      .me()
      .then(setProfile)
      .catch((err) => setProfileError(errorMessage(err)));
    configApi.borrowRules().then(setRules).catch(() => setRules(null));
  }, []);

  const columns: ColumnsType<BorrowRecordVO> = [
    {
      title: '图书',
      dataIndex: 'bookTitle',
      render: (_, record) => (
        <Space direction="vertical" size={0}>
          <Typography.Text strong>{record.bookTitle ?? `#${record.bookId}`}</Typography.Text>
          <Typography.Text type="secondary" style={{ fontSize: 12 }}>
            ISBN {record.bookIsbn ?? '—'}
          </Typography.Text>
        </Space>
      ),
    },
    { title: '借出日期', dataIndex: 'borrowDate', width: 120 },
    {
      title: '应还日期',
      dataIndex: 'dueDate',
      width: 150,
      render: (value: string, record) => (
        <Typography.Text type={record.status === 'BORROWED' && record.overdueDays > 0 ? 'danger' : undefined}>
          {value}
          {record.status === 'BORROWED' && record.overdueDays > 0 && `（已逾期 ${record.overdueDays} 天）`}
        </Typography.Text>
      ),
    },
    { title: '归还日期', dataIndex: 'returnDate', width: 120, render: (value: string | null) => value ?? '—' },
    { title: '续借次数', dataIndex: 'renewTimes', width: 90, render: (value: number) => `${value} 次` },
    {
      title: '状态',
      dataIndex: 'status',
      width: 100,
      render: (value: BorrowRecordVO['status']) =>
        value === 'BORROWED' ? <Tag color="blue">借阅中</Tag> : <Tag color="green">已归还</Tag>,
    },
    {
      title: '罚款（元）',
      dataIndex: 'fineAmount',
      width: 120,
      render: (_, record) => {
        const fine = record.status === 'RETURNED' ? record.fineAmount ?? 0 : record.currentFine;
        return Number(fine) > 0 ? (
          <Typography.Text type="danger">{Number(fine).toFixed(2)}</Typography.Text>
        ) : (
          <Typography.Text type="secondary">0.00</Typography.Text>
        );
      },
    },
  ];

  return (
    <Space direction="vertical" size={16} style={{ width: '100%' }}>
      {profileError ? (
        <Alert type="warning" showIcon message="尚未绑定读者档案" description={profileError} />
      ) : (
        profile && (
          <Card title="我的借阅证">
            <Descriptions column={{ xs: 1, sm: 2, md: 4 }} size="small">
              <Descriptions.Item label="姓名">{profile.name}</Descriptions.Item>
              <Descriptions.Item label="借阅证号">{profile.cardNo}</Descriptions.Item>
              <Descriptions.Item label="状态">
                {profile.status === 'ACTIVE' ? <Tag color="green">正常</Tag> : <Tag color="red">停用</Tag>}
              </Descriptions.Item>
              <Descriptions.Item label="在借 / 上限">
                {profile.borrowingCount} / {profile.maxBorrow}
              </Descriptions.Item>
            </Descriptions>
            {rules && (
              <Typography.Text type="secondary">
                借期 {rules.loanDays} 天，可续借 {rules.maxRenewTimes} 次（每次 +{rules.renewDays} 天）；逾期按 {rules.finePerDay} 元/天计收罚款。
              </Typography.Text>
            )}
          </Card>
        )
      )}

      <Card
        title="我的借阅记录"
        extra={
          <Select
            allowClear
            placeholder="全部状态"
            style={{ width: 130 }}
            value={status}
            options={[
              { value: 'BORROWED', label: '借阅中' },
              { value: 'RETURNED', label: '已归还' },
            ]}
            onChange={(value) => {
              setStatus(value);
              setPagination((prev) => ({ ...prev, page: 1 }));
            }}
          />
        }
      >
        <Table<BorrowRecordVO>
          rowKey="id"
          loading={loading}
          columns={columns}
          dataSource={data.records}
          scroll={{ x: 800 }}
          pagination={{
            current: data.page,
            pageSize: data.size,
            total: data.total,
            showSizeChanger: true,
            showTotal: (total) => `共 ${total} 条记录`,
            onChange: (page, size) => setPagination({ page, size }),
          }}
        />
      </Card>
    </Space>
  );
}
