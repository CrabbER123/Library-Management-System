import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';

const { loginMock } = vi.hoisted(() => ({ loginMock: vi.fn() }));

vi.mock('../auth/AuthContext', () => ({
  useAuth: () => ({ user: null, login: loginMock }),
}));

import LoginPage from './LoginPage';

function renderPage() {
  return render(
    <MemoryRouter initialEntries={['/login']}>
      <LoginPage />
    </MemoryRouter>,
  );
}

describe('LoginPage', () => {
  beforeEach(() => {
    loginMock.mockReset();
  });

  it('空表单提交时提示用户名与密码必填', async () => {
    const user = userEvent.setup();
    renderPage();

    await user.click(screen.getByRole('button', { name: /登\s*录/ }));

    expect(await screen.findByText('请输入用户名')).toBeInTheDocument();
    expect(await screen.findByText('请输入密码')).toBeInTheDocument();
    expect(loginMock).not.toHaveBeenCalled();
  });

  it('提交时去除用户名首尾空格并调用登录', async () => {
    loginMock.mockResolvedValue({
      id: 1,
      username: 'admin',
      displayName: '系统管理员',
      roles: ['ADMIN'],
      readerId: null,
    });
    const user = userEvent.setup();
    renderPage();

    await user.type(screen.getByPlaceholderText(/admin \/ librarian/), '  admin  ');
    await user.type(screen.getByPlaceholderText('请输入密码'), 'Admin@123456');
    await user.click(screen.getByRole('button', { name: /登\s*录/ }));

    await waitFor(() => expect(loginMock).toHaveBeenCalledWith('admin', 'Admin@123456'));
  });

  it('登录失败时展示后端返回的错误文案', async () => {
    const { ApiError } = await import('../api/http');
    loginMock.mockRejectedValue(new ApiError('用户名或密码错误', 40100, 401));
    const user = userEvent.setup();
    renderPage();

    await user.type(screen.getByPlaceholderText(/admin \/ librarian/), 'admin');
    await user.type(screen.getByPlaceholderText('请输入密码'), 'wrong-password');
    await user.click(screen.getByRole('button', { name: /登\s*录/ }));

    expect(await screen.findByText('用户名或密码错误')).toBeInTheDocument();
  });
});
