import { useMemo, useState } from 'react';
import { Avatar, Dropdown, Layout, Menu, Space, Tag, Typography, type MenuProps } from 'antd';
import {
  BarChartOutlined,
  BookOutlined,
  DashboardOutlined,
  LogoutOutlined,
  ReadOutlined,
  SolutionOutlined,
  TeamOutlined,
  TagsOutlined,
  UserOutlined,
} from '@ant-design/icons';
import { Link, Outlet, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import ErrorBoundary from './ErrorBoundary';

const { Header, Sider, Content } = Layout;

const ROLE_LABEL: Record<string, string> = {
  ADMIN: '管理员',
  LIBRARIAN: '馆员',
  READER: '读者',
};

export default function AppLayout() {
  const { user, logout, isStaff, isAdmin } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();
  const [collapsed, setCollapsed] = useState(false);

  const items = useMemo<MenuProps['items']>(() => {
    const menu: MenuProps['items'] = [
      { key: '/books', icon: <BookOutlined />, label: <Link to="/books">图书查询</Link> },
      { key: '/my-borrows', icon: <ReadOutlined />, label: <Link to="/my-borrows">我的借阅</Link> },
    ];
    if (isStaff) {
      menu.push(
        { key: '/borrows', icon: <SolutionOutlined />, label: <Link to="/borrows">借阅管理</Link> },
        { key: '/readers', icon: <TeamOutlined />, label: <Link to="/readers">读者管理</Link> },
        {
          key: 'catalog',
          icon: <TagsOutlined />,
          label: '基础数据',
          children: [
            { key: '/catalog/authors', label: <Link to="/catalog/authors">作者</Link> },
            { key: '/catalog/publishers', label: <Link to="/catalog/publishers">出版社</Link> },
            { key: '/catalog/categories', label: <Link to="/catalog/categories">分类</Link> },
          ],
        },
        { key: '/stats', icon: <BarChartOutlined />, label: <Link to="/stats">统计概览</Link> },
      );
    }
    if (isAdmin) {
      menu.push({ key: '/users', icon: <UserOutlined />, label: <Link to="/users">用户与角色</Link> });
    }
    return menu;
  }, [isAdmin, isStaff]);

  const selectedKey = location.pathname.startsWith('/catalog') ? location.pathname : location.pathname;
  const openKeys = location.pathname.startsWith('/catalog') ? ['catalog'] : [];

  const onLogout = async () => {
    await logout();
    navigate('/login', { replace: true });
  };

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Sider collapsible collapsed={collapsed} onCollapse={setCollapsed} width={216} theme="dark">
        <div
          style={{
            height: 56,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            color: '#fff',
            fontWeight: 600,
            letterSpacing: 1,
          }}
        >
          <DashboardOutlined style={{ marginRight: collapsed ? 0 : 8 }} />
          {!collapsed && '图书管理系统'}
        </div>
        <Menu
          theme="dark"
          mode="inline"
          selectedKeys={[selectedKey]}
          defaultOpenKeys={openKeys}
          items={items}
        />
      </Sider>
      <Layout>
        <Header
          style={{
            background: '#fff',
            padding: '0 24px',
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            boxShadow: '0 1px 4px rgba(0,21,41,.08)',
          }}
        >
          <Typography.Text type="secondary">
            {isStaff ? '后台管理' : '读者服务'} · 默认借期 30 天，可续借 2 次
          </Typography.Text>
          <Dropdown
            menu={{
              items: [{ key: 'logout', icon: <LogoutOutlined />, label: '退出登录', onClick: onLogout }],
            }}
          >
            <Space style={{ cursor: 'pointer' }}>
              <Avatar size="small" icon={<UserOutlined />} />
              <span>{user?.displayName || user?.username}</span>
              {user?.roles.map((role) => (
                <Tag key={role} color={role === 'ADMIN' ? 'gold' : role === 'LIBRARIAN' ? 'blue' : 'green'}>
                  {ROLE_LABEL[role] ?? role}
                </Tag>
              ))}
            </Space>
          </Dropdown>
        </Header>
        <Content style={{ margin: 16 }}>
          <ErrorBoundary resetKey={location.pathname}>
            <Outlet />
          </ErrorBoundary>
        </Content>
      </Layout>
    </Layout>
  );
}
