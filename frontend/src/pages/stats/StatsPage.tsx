import { useEffect, useState } from 'react';
import { Card, Col, Empty, List, Progress, Row, Statistic, Table, Tag, Typography, message } from 'antd';
import { BookOutlined, ClockCircleOutlined, ReadOutlined, TeamOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { statsApi } from '../../api';
import { errorMessage } from '../../api/http';
import type { CategoryDistribution, OverviewStats, PopularBook } from '../../api/types';

export default function StatsPage() {
  const [overview, setOverview] = useState<OverviewStats | null>(null);
  const [popular, setPopular] = useState<PopularBook[]>([]);
  const [distribution, setDistribution] = useState<CategoryDistribution[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    Promise.all([statsApi.overview(), statsApi.popularBooks(10), statsApi.categoryDistribution()])
      .then(([o, p, d]) => {
        setOverview(o);
        setPopular(p);
        setDistribution(d);
      })
      .catch((err) => message.error(errorMessage(err)))
      .finally(() => setLoading(false));
  }, []);

  const popularColumns: ColumnsType<PopularBook> = [
    {
      title: '排名',
      key: 'rank',
      width: 70,
      render: (_, __, index) => {
        const colors = ['gold', 'silver', '#cd7f32'];
        return index < 3 ? <Tag color={colors[index]}>{index + 1}</Tag> : index + 1;
      },
    },
    { title: '书名', dataIndex: 'title' },
    { title: 'ISBN', dataIndex: 'isbn', width: 160 },
    {
      title: '借阅次数',
      dataIndex: 'borrowCount',
      width: 120,
      sorter: (a, b) => a.borrowCount - b.borrowCount,
      defaultSortOrder: 'descend',
    },
  ];

  const maxCategory = Math.max(1, ...distribution.map((item) => item.bookCount));

  return (
    <Row gutter={[16, 16]}>
      <Col span={24}>
        <Row gutter={[16, 16]}>
          <Col xs={12} md={6}>
            <Card loading={loading}>
              <Statistic
                title="图书种类"
                value={overview?.totalTitles ?? 0}
                prefix={<BookOutlined />}
                suffix="种"
              />
              <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                馆藏 {overview?.totalBooks ?? 0} 册
              </Typography.Text>
            </Card>
          </Col>
          <Col xs={12} md={6}>
            <Card loading={loading}>
              <Statistic
                title="可借册数"
                value={overview?.availableStock ?? 0}
                prefix={<ReadOutlined />}
                suffix="册"
                valueStyle={{ color: '#3f8600' }}
              />
              <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                已借出 {overview?.borrowedCount ?? 0} 册
              </Typography.Text>
            </Card>
          </Col>
          <Col xs={12} md={6}>
            <Card loading={loading}>
              <Statistic
                title="逾期未还"
                value={overview?.overdueCount ?? 0}
                prefix={<ClockCircleOutlined />}
                suffix="条"
                valueStyle={{ color: (overview?.overdueCount ?? 0) > 0 ? '#cf1322' : undefined }}
              />
              <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                累计借阅 {overview?.totalBorrows ?? 0} 次
              </Typography.Text>
            </Card>
          </Col>
          <Col xs={12} md={6}>
            <Card loading={loading}>
              <Statistic
                title="读者"
                value={overview?.totalReaders ?? 0}
                prefix={<TeamOutlined />}
                suffix="人"
              />
              <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                正常状态 {overview?.activeReaders ?? 0} 人
              </Typography.Text>
            </Card>
          </Col>
        </Row>
      </Col>

      <Col xs={24} lg={14}>
        <Card title="借阅排行 TOP 10" loading={loading}>
          <Table<PopularBook>
            rowKey="bookId"
            size="small"
            columns={popularColumns}
            dataSource={popular}
            pagination={false}
            locale={{ emptyText: <Empty description="暂无借阅记录" /> }}
          />
        </Card>
      </Col>

      <Col xs={24} lg={10}>
        <Card title="分类馆藏分布" loading={loading}>
          {distribution.length === 0 ? (
            <Empty description="暂无分类数据" />
          ) : (
            <List
              dataSource={distribution}
              renderItem={(item) => (
                <List.Item>
                  <div style={{ width: '100%' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 4 }}>
                      <span>{item.categoryName}</span>
                      <Typography.Text type="secondary">{item.bookCount} 种</Typography.Text>
                    </div>
                    <Progress
                      percent={Math.round((item.bookCount / maxCategory) * 100)}
                      showInfo={false}
                      strokeColor="#1677ff"
                      size="small"
                    />
                  </div>
                </List.Item>
              )}
            />
          )}
        </Card>
      </Col>
    </Row>
  );
}
