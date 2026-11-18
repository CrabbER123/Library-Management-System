import { beforeEach, describe, expect, it } from 'vitest';
import { ApiError, errorMessage, tokenStore } from './http';

describe('errorMessage', () => {
  it('优先展示后端业务错误文案', () => {
    expect(errorMessage(new ApiError('库存不足，无法借出', 42206, 422))).toBe('库存不足，无法借出');
  });

  it('普通 Error 展示其 message', () => {
    expect(errorMessage(new Error('网络异常'))).toBe('网络异常');
  });

  it('未知类型给出兜底文案', () => {
    expect(errorMessage('boom')).toBe('未知错误');
    expect(errorMessage(null)).toBe('未知错误');
  });
});

describe('ApiError', () => {
  it('保留错误码与 HTTP 状态', () => {
    const error = new ApiError('登录已过期', 40101, 401);
    expect(error.code).toBe(40101);
    expect(error.status).toBe(401);
    expect(error.name).toBe('ApiError');
  });
});

describe('tokenStore', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it('保存、读取与清除令牌', () => {
    tokenStore.save({ accessToken: 'access-1', refreshToken: 'refresh-1' });
    expect(tokenStore.access).toBe('access-1');
    expect(tokenStore.refresh).toBe('refresh-1');
    expect(localStorage.getItem('library.accessToken')).toBe('access-1');

    tokenStore.clear();
    expect(tokenStore.access).toBeNull();
    expect(tokenStore.refresh).toBeNull();
  });
});
