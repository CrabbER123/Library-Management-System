import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import type { BookVO } from '../../api/types';

const { listMock, namedListMock, rulesMock, borrowMock } = vi.hoisted(() => ({
  listMock: vi.fn(),
  namedListMock: vi.fn(),
  rulesMock: vi.fn(),
  borrowMock: vi.fn(),
}));

vi.mock('../../api', () => ({
  bookApi: {
    list: listMock,
    get: vi.fn(),
    create: vi.fn(),
    update: vi.fn(),
    updateStatus: vi.fn(),
    updateStock: vi.fn(),
    remove: vi.fn(),
  },
  namedApi: { list: namedListMock, create: vi.fn(), update: vi.fn(), remove: vi.fn() },
  configApi: { borrowRules: rulesMock },
  borrowApi: { borrow: borrowMock },
}));

vi.mock('../../auth/AuthContext', () => ({
  useAuth: () => ({ isStaff: false, isAdmin: false, user: { id: 3, roles: ['READER'] } }),
}));

import BooksPage from './BooksPage';

const book: BookVO = {
  id: 3,
  isbn: '9787000000001',
  title: '用 ai 做副业赚钱',
  subtitle: null,
  authors: [{ id: 1, name: '某作者' }],
  publisherId: 1,
  publisherName: '某出版社',
  categoryId: null,
  categoryName: null,
  publishDate: null,
  language: null,
  price: null,
  coverUrl: null,
  summary: null,
  totalStock: 1,
  availableStock: 1,
  status: 'AVAILABLE',
  createdAt: '2026-01-01 10:00:00',
  updatedAt: '2026-01-01 10:00:00',
};

describe('BooksPage 读者自助借阅入口', () => {
  beforeEach(() => {
    listMock.mockReset();
    namedListMock.mockReset();
    rulesMock.mockReset();
    borrowMock.mockReset();
    namedListMock.mockResolvedValue({ records: [], total: 0, page: 1, size: 100, pages: 0 });
    rulesMock.mockResolvedValue({
      loanDays: 30,
      maxRenewTimes: 2,
      renewDays: 15,
      maxBorrowPerReader: 5,
      finePerDay: 0.5,
      allowRenewWhenOverdue: false,
      maxFineAmount: 0,
    });
  });

  it('读者在可借图书上看到借阅按钮，确认后提交借书请求', async () => {
    listMock.mockResolvedValue({ records: [book], total: 1, page: 1, size: 10, pages: 1 });
    borrowMock.mockResolvedValue({ id: 11, dueDate: '2026-02-01' });

    render(<BooksPage />);

    expect(await screen.findByText('用 ai 做副业赚钱')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: /借\s*阅/ }));

    const dialog = await screen.findByRole('tooltip');
    fireEvent.click(within(dialog).getByRole('button', { name: /借\s*阅/ }));

    await waitFor(() => expect(borrowMock).toHaveBeenCalledWith({ bookId: 3 }));
    await waitFor(() => expect(listMock).toHaveBeenCalledTimes(2));
  });

  it('无库存图书不展示借阅按钮', async () => {
    listMock.mockResolvedValue({
      records: [{ ...book, availableStock: 0 }],
      total: 1,
      page: 1,
      size: 10,
      pages: 1,
    });

    render(<BooksPage />);

    expect(await screen.findByText('无库存')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /借\s*阅/ })).not.toBeInTheDocument();
  });
});
