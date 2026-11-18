import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import type { BorrowRecordVO, ReaderVO } from '../../api/types';

const { myMock, returnMock, renewMock, meMock, rulesMock } = vi.hoisted(() => ({
  myMock: vi.fn(),
  returnMock: vi.fn(),
  renewMock: vi.fn(),
  meMock: vi.fn(),
  rulesMock: vi.fn(),
}));

vi.mock('../../api', () => ({
  borrowApi: { my: myMock, returnBook: returnMock, renew: renewMock },
  readerApi: { me: meMock },
  configApi: { borrowRules: rulesMock },
}));

import MyBorrowsPage from './MyBorrowsPage';

const profile: ReaderVO = {
  id: 2,
  userId: 3,
  cardNo: 'R000002',
  name: '陈依航',
  phone: null,
  email: null,
  status: 'ACTIVE',
  maxBorrow: 5,
  borrowingCount: 1,
  createdAt: '2026-01-01 10:00:00',
  updatedAt: '2026-01-01 10:00:00',
};

const borrowedRecord: BorrowRecordVO = {
  id: 11,
  readerId: 2,
  readerName: '陈依航',
  readerCardNo: 'R000002',
  bookId: 3,
  bookTitle: '用 ai 做副业赚钱',
  bookIsbn: '9787000000001',
  borrowDate: '2026-01-01',
  dueDate: '2026-01-31',
  returnDate: null,
  renewTimes: 0,
  status: 'BORROWED',
  fineAmount: 0,
  overdueDays: 0,
  currentFine: 0,
  operatorId: null,
  createdAt: '2026-01-01 10:00:00',
};

const returnedRecord: BorrowRecordVO = {
  ...borrowedRecord,
  id: 12,
  status: 'RETURNED',
  returnDate: '2026-01-20',
};

const page = (records: BorrowRecordVO[]) => ({
  records,
  total: records.length,
  page: 1,
  size: 10,
  pages: 1,
});

describe('MyBorrowsPage 读者自助还书与续借', () => {
  beforeEach(() => {
    myMock.mockReset();
    returnMock.mockReset();
    renewMock.mockReset();
    meMock.mockReset();
    rulesMock.mockReset();
    meMock.mockResolvedValue(profile);
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

  it('借阅中的记录提供还书与续借按钮，确认后调用对应接口', async () => {
    myMock.mockResolvedValueOnce(page([borrowedRecord])).mockResolvedValue(page([returnedRecord]));
    returnMock.mockResolvedValue({ ...borrowedRecord, status: 'RETURNED', returnDate: '2026-01-25' });

    render(<MyBorrowsPage />);

    const returnButton = await screen.findByRole('button', { name: /还\s*书/ });
    expect(screen.getByRole('button', { name: /续\s*借/ })).toBeEnabled();

    fireEvent.click(returnButton);
    const dialog = await screen.findByRole('tooltip');
    fireEvent.click(within(dialog).getByRole('button', { name: /归\s*还/ }));

    await waitFor(() => expect(returnMock).toHaveBeenCalledWith(11));
    await waitFor(() => expect(myMock).toHaveBeenCalledTimes(2));
  });

  it('续借按钮打开确认框并提交续借请求', async () => {
    myMock.mockResolvedValue(page([borrowedRecord]));
    renewMock.mockResolvedValue({ ...borrowedRecord, dueDate: '2026-02-15', renewTimes: 1 });

    render(<MyBorrowsPage />);

    fireEvent.click(await screen.findByRole('button', { name: /续\s*借/ }));
    const dialog = await screen.findByRole('tooltip');
    fireEvent.click(within(dialog).getByRole('button', { name: /续\s*借/ }));

    await waitFor(() => expect(renewMock).toHaveBeenCalledWith(11));
  });

  it('已归还的记录不再展示还书按钮', async () => {
    myMock.mockResolvedValue(page([returnedRecord]));

    render(<MyBorrowsPage />);

    expect((await screen.findAllByText('已归还')).length).toBeGreaterThan(0);
    expect(screen.queryByRole('button', { name: /还\s*书/ })).not.toBeInTheDocument();
  });

  it('借阅证停用时还书与续借按钮均被禁用', async () => {
    meMock.mockResolvedValue({ ...profile, status: 'SUSPENDED' });
    myMock.mockResolvedValue(page([borrowedRecord]));

    render(<MyBorrowsPage />);

    expect(await screen.findByRole('button', { name: /还\s*书/ })).toBeDisabled();
    expect(screen.getByRole('button', { name: /续\s*借/ })).toBeDisabled();
  });
});
