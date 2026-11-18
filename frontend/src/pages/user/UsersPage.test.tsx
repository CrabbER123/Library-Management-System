import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import type { Mock } from 'vitest';

vi.mock('../../api', () => ({
  userApi: {
    list: vi.fn(),
    roles: vi.fn(),
    create: vi.fn(),
    update: vi.fn(),
    updateStatus: vi.fn(),
    updateRoles: vi.fn(),
    remove: vi.fn(),
  },
}));

vi.mock('../../auth/AuthContext', () => ({
  useAuth: () => ({
    user: { id: 1, username: 'admin', displayName: '管理员', roles: ['ADMIN'], readerId: null },
  }),
}));

import { userApi } from '../../api';
import UsersPage from './UsersPage';

const TIMEOUT = 20000;

const listMock = userApi.list as unknown as Mock;
const rolesMock = userApi.roles as unknown as Mock;

const ADMIN_ROW = {
  id: 1,
  username: 'admin',
  displayName: '系统管理员',
  email: null,
  phone: null,
  status: 'ACTIVE' as const,
  roles: ['ADMIN', 'LIBRARIAN'],
  createdAt: '2026-11-18 10:00:00',
  lastLoginAt: null,
};

/** GET /roles 返回对象数组；早期前端按字符串数组处理，下拉展开时整页白屏 */
const ROLE_DICTIONARY = [
  { id: 1, code: 'ADMIN', name: '管理员', description: '系统全部权限' },
  { id: 2, code: 'LIBRARIAN', name: '馆员', description: '借还与图书维护' },
  { id: 3, code: 'READER', name: '读者', description: '只读与自助注册' },
];

const ROLE_NAMES = ['管理员', '馆员', '读者'];

function optionTexts() {
  return Array.from(document.querySelectorAll('.ant-select-item-option-content')).map(
    (node) => node.textContent ?? '',
  );
}

describe('UsersPage', () => {
  beforeEach(() => {
    listMock.mockReset();
    rolesMock.mockReset();
    listMock.mockResolvedValue({ records: [ADMIN_ROW], total: 1, page: 1, size: 10, pages: 1 });
    rolesMock.mockResolvedValue(ROLE_DICTIONARY);
  });

  it(
    '编辑账号时展开角色下拉不会白屏，选项使用角色字典名称',
    async () => {
      render(<UsersPage />);

      expect(await screen.findByText('admin')).toBeInTheDocument();
      await waitFor(() => expect(rolesMock).toHaveBeenCalled());

      const user = userEvent.setup();
      await user.click(screen.getByRole('button', { name: '编辑' }));
      const dialog = await screen.findByRole('dialog');

      // 已选角色按字典名称展示为标签，而不是把对象当文本渲染
      expect(within(dialog).getByText('管理员')).toBeInTheDocument();
      expect(within(dialog).getByText('馆员')).toBeInTheDocument();

      await user.click(within(dialog).getByRole('combobox'));
      await waitFor(() => expect(optionTexts().length).toBeGreaterThan(0));

      // 选项文案都是中文字符串，出现 [object Object] 说明又退回成对象渲染
      expect(optionTexts().every((text) => ROLE_NAMES.includes(text))).toBe(true);
      expect(optionTexts()).toContain('管理员');
      expect(screen.getByRole('dialog')).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /保\s*存/ })).toBeInTheDocument();
    },
    TIMEOUT,
  );

  it(
    '角色字典接口失败时仍可用内置中文名称渲染',
    async () => {
      rolesMock.mockRejectedValue(new Error('网络异常'));
      render(<UsersPage />);

      await screen.findByText('admin');
      const user = userEvent.setup();
      await user.click(screen.getByRole('button', { name: '编辑' }));
      const dialog = await screen.findByRole('dialog');

      expect(within(dialog).getByText('管理员')).toBeInTheDocument();
      await user.click(within(dialog).getByRole('combobox'));
      await waitFor(() => expect(optionTexts().length).toBeGreaterThan(0));
      expect(optionTexts().every((text) => ROLE_NAMES.includes(text))).toBe(true);
    },
    TIMEOUT,
  );
});
