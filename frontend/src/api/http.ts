import axios, { AxiosError, AxiosRequestConfig } from 'axios';
import type { ApiResponse, LoginResult } from './types';

const ACCESS_KEY = 'library.accessToken';
const REFRESH_KEY = 'library.refreshToken';

export const tokenStore = {
  get access(): string | null {
    return localStorage.getItem(ACCESS_KEY);
  },
  get refresh(): string | null {
    return localStorage.getItem(REFRESH_KEY);
  },
  save(result: Pick<LoginResult, 'accessToken' | 'refreshToken'>) {
    localStorage.setItem(ACCESS_KEY, result.accessToken);
    localStorage.setItem(REFRESH_KEY, result.refreshToken);
  },
  clear() {
    localStorage.removeItem(ACCESS_KEY);
    localStorage.removeItem(REFRESH_KEY);
  },
};

/** 业务异常：携带后端错误码，页面可据此做差异化提示 */
export class ApiError extends Error {
  readonly code: number;
  readonly status: number;

  constructor(message: string, code: number, status: number) {
    super(message);
    this.name = 'ApiError';
    this.code = code;
    this.status = status;
  }
}

export const http = axios.create({
  baseURL: '/api/v1',
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
});

http.interceptors.request.use((config) => {
  const token = tokenStore.access;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

let refreshPromise: Promise<string> | null = null;

/** 使用 refresh token 换新令牌，并发请求只触发一次 */
async function refreshAccessToken(): Promise<string> {
  const refreshToken = tokenStore.refresh;
  if (!refreshToken) {
    throw new ApiError('登录已过期，请重新登录', 40101, 401);
  }
  const response = await axios.post<ApiResponse<LoginResult>>(
    '/api/v1/auth/refresh',
    { refreshToken },
    { headers: { 'Content-Type': 'application/json' } },
  );
  const result = response.data.data;
  tokenStore.save(result);
  return result.accessToken;
}

function redirectToLogin() {
  tokenStore.clear();
  if (!window.location.pathname.startsWith('/login')) {
    const redirect = encodeURIComponent(window.location.pathname + window.location.search);
    window.location.replace(`/login?redirect=${redirect}`);
  }
}

http.interceptors.response.use(
  (response) => {
    const body = response.data as ApiResponse<unknown>;
    if (body && typeof body === 'object' && 'code' in body && body.code !== 0) {
      throw new ApiError(body.message ?? '请求失败', body.code, response.status);
    }
    return body?.data as never;
  },
  async (error: AxiosError<ApiResponse<unknown>>) => {
    const status = error.response?.status ?? 0;
    const body = error.response?.data;
    const original = error.config as (AxiosRequestConfig & { _retried?: boolean }) | undefined;
    const isAuthEndpoint = original?.url?.includes('/auth/login') || original?.url?.includes('/auth/refresh');

    if (status === 401 && !isAuthEndpoint && original && !original._retried) {
      original._retried = true;
      try {
        refreshPromise = refreshPromise ?? refreshAccessToken();
        const token = await refreshPromise;
        refreshPromise = null;
        original.headers = { ...original.headers, Authorization: `Bearer ${token}` };
        return http.request(original);
      } catch (refreshError) {
        refreshPromise = null;
        redirectToLogin();
        return Promise.reject(refreshError);
      }
    }

    if (status === 401 && !isAuthEndpoint) {
      redirectToLogin();
    }

    const message =
      body?.message ??
      (status === 0 ? '无法连接服务器，请确认后端已启动' : `请求失败（HTTP ${status}）`);
    return Promise.reject(new ApiError(message, body?.code ?? status, status));
  },
);

/** 统一取错误文案，供 antd message / Alert 使用 */
export function errorMessage(error: unknown): string {
  if (error instanceof ApiError) return error.message;
  if (error instanceof Error) return error.message;
  return '未知错误';
}
