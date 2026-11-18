import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';

const { registerMock } = vi.hoisted(() => ({ registerMock: vi.fn() }));

vi.mock('../auth/AuthContext', () => ({
  useAuth: () => ({ user: null, register: registerMock }),
}));

import RegisterPage from './RegisterPage';

function renderPage() {
  return render(
    <MemoryRouter initialEntries={['/register']}>
      <RegisterPage />
    </MemoryRouter>,
  );
}

async function fillBaseForm(user: ReturnType<typeof userEvent.setup>) {
  await user.type(screen.getByPlaceholderText('请输入用户名'), 'newbie');
  await user.type(screen.getByPlaceholderText('请输入真实姓名'), '王小明');
  await user.type(screen.getByPlaceholderText('请输入密码'), 'secret123');
}

describe('RegisterPage', () => {
  beforeEach(() => {
    registerMock.mockReset();
  });

  it('两次密码不一致时提示且不提交注册', async () => {
    const user = userEvent.setup();
    renderPage();

    await fillBaseForm(user);
    await user.type(screen.getByPlaceholderText('请再次输入密码'), 'secret456');
    await user.click(screen.getByRole('button', { name: /注册并登录/ }));

    expect(await screen.findByText('两次输入的密码不一致')).toBeInTheDocument();
    expect(registerMock).not.toHaveBeenCalled();
  });

  it('用户名不符合规则时给出提示', async () => {
    const user = userEvent.setup();
    renderPage();

    await user.type(screen.getByPlaceholderText('请输入用户名'), 'a');
    await user.click(screen.getByRole('button', { name: /注册并登录/ }));

    expect(await screen.findByText('用户名需为 3-32 位字母、数字或下划线')).toBeInTheDocument();
    expect(registerMock).not.toHaveBeenCalled();
  });

  it('提交成功时去除首尾空格并调用注册接口', async () => {
    registerMock.mockResolvedValue({
      id: 9,
      username: 'newbie',
      displayName: '王小明',
      roles: ['READER'],
      readerId: 5,
    });
    const user = userEvent.setup();
    renderPage();

    await fillBaseForm(user);
    await user.type(screen.getByPlaceholderText('请再次输入密码'), 'secret123');
    await user.type(screen.getByPlaceholderText('用于联系与逾期提醒'), '13800000000');
    await user.click(screen.getByRole('button', { name: /注册并登录/ }));

    await waitFor(() =>
      expect(registerMock).toHaveBeenCalledWith({
        username: 'newbie',
        password: 'secret123',
        displayName: '王小明',
        phone: '13800000000',
        email: undefined,
      }),
    );
  });

  it('注册失败时展示后端返回的错误文案', async () => {
    const { ApiError } = await import('../api/http');
    registerMock.mockRejectedValue(new ApiError('用户名已被注册: newbie', 40901, 409));
    const user = userEvent.setup();
    renderPage();

    await fillBaseForm(user);
    await user.type(screen.getByPlaceholderText('请再次输入密码'), 'secret123');
    await user.click(screen.getByRole('button', { name: /注册并登录/ }));

    expect(await screen.findByText('用户名已被注册: newbie')).toBeInTheDocument();
  });
});
