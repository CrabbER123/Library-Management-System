import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import type { ReaderVO } from '../../api/types';

const { listMock, updateStatusMock } = vi.hoisted(() => ({
  listMock: vi.fn(),
  updateStatusMock: vi.fn(),
}));

vi.mock('../../api', () => ({
  readerApi: {
    list: listMock,
    updateStatus: updateStatusMock,
    create: vi.fn(),
    update: vi.fn(),
    remove: vi.fn(),
  },
}));

import ReadersPage from './ReadersPage';

const reader: ReaderVO = {
  id: 1,
  userId: null,
  cardNo: 'R000001',
  name: '张三',
  phone: null,
  email: null,
  status: 'ACTIVE',
  maxBorrow: 5,
  borrowingCount: 0,
  createdAt: '2026-01-01 10:00:00',
  updatedAt: '2026-01-01 10:00:00',
};

describe('ReadersPage 读者状态入口', () => {
  beforeEach(() => {
    listMock.mockReset();
    updateStatusMock.mockReset();
    listMock.mockResolvedValue({ records: [reader], total: 1, page: 1, size: 10, pages: 1 });
  });

  it('停用前提示不能再借阅/归还/续借，确认后提交 SUSPENDED', async () => {
    render(<ReadersPage />);

    expect(await screen.findByText('张三')).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: /停\s*用/ }));
    await waitFor(() => expect(document.body.textContent).toContain('不能再借阅、归还或续借'), {
      timeout: 10000,
    });

    updateStatusMock.mockResolvedValue(undefined);
    fireEvent.click(screen.getByRole('button', { name: /确\s*定|OK/ }));

    await waitFor(() => expect(updateStatusMock).toHaveBeenCalledWith(1, 'SUSPENDED'));
  }, 20000);
});
