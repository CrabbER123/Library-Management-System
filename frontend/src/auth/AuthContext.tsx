import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { authApi } from '../api';
import { tokenStore } from '../api/http';
import type { CurrentUser, RegisterPayload } from '../api/types';

interface AuthContextValue {
  user: CurrentUser | null;
  initializing: boolean;
  login: (username: string, password: string) => Promise<CurrentUser>;
  register: (payload: RegisterPayload) => Promise<CurrentUser>;
  logout: () => Promise<void>;
  reload: () => Promise<void>;
  hasRole: (...roles: string[]) => boolean;
  isStaff: boolean;
  isAdmin: boolean;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<CurrentUser | null>(null);
  const [initializing, setInitializing] = useState(true);

  const reload = useCallback(async () => {
    const current = await authApi.me();
    setUser(current);
  }, []);

  useEffect(() => {
    let cancelled = false;
    if (!tokenStore.access) {
      setInitializing(false);
      return;
    }
    authApi
      .me()
      .then((current) => {
        if (!cancelled) setUser(current);
      })
      .catch(() => {
        tokenStore.clear();
        if (!cancelled) setUser(null);
      })
      .finally(() => {
        if (!cancelled) setInitializing(false);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const login = useCallback(async (username: string, password: string) => {
    const result = await authApi.login(username, password);
    tokenStore.save(result);
    const current = await authApi.me();
    setUser(current);
    return current;
  }, []);

  const register = useCallback(async (payload: RegisterPayload) => {
    const result = await authApi.register(payload);
    tokenStore.save(result);
    const current = await authApi.me();
    setUser(current);
    return current;
  }, []);

  const logout = useCallback(async () => {
    try {
      await authApi.logout();
    } catch {
      // 令牌可能已过期，忽略登出失败并清理本地状态
    }
    tokenStore.clear();
    setUser(null);
  }, []);

  const hasRole = useCallback((...roles: string[]) => !!user && roles.some((role) => user.roles.includes(role)), [user]);

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      initializing,
      login,
      register,
      logout,
      reload,
      hasRole,
      isStaff: hasRole('ADMIN', 'LIBRARIAN'),
      isAdmin: hasRole('ADMIN'),
    }),
    [user, initializing, login, register, logout, reload, hasRole],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth 必须在 AuthProvider 内使用');
  }
  return context;
}
