import { Spin } from 'antd';
import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import ForbiddenPage from '../pages/ForbiddenPage';

/** 登录守卫：未登录跳转登录页并记录来源地址 */
export function RequireAuth() {
  const { user, initializing } = useAuth();
  const location = useLocation();

  if (initializing) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', paddingTop: 160 }}>
        <Spin size="large" tip="加载中…" />
      </div>
    );
  }
  if (!user) {
    return <Navigate to={`/login?redirect=${encodeURIComponent(location.pathname)}`} replace />;
  }
  return <Outlet />;
}

/** 角色守卫：当前用户角色不在白名单内时展示 403 页面 */
export function RequireRole({ roles }: { roles: string[] }) {
  const { user } = useAuth();
  if (!user) return <Navigate to="/login" replace />;
  if (!roles.some((role) => user.roles.includes(role))) {
    return <ForbiddenPage />;
  }
  return <Outlet />;
}
