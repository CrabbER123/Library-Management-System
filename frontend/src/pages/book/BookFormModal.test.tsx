import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import BookFormModal from './BookFormModal';

const createMock = vi.hoisted(() => vi.fn());
const updateMock = vi.hoisted(() => vi.fn());
const listMock = vi.hoisted(() => vi.fn());
const onSavedMock = vi.hoisted(() => vi.fn());

vi.mock('../../api', () => ({
  bookApi: { create: createMock, update: updateMock },
  namedApi: { list: listMock },
}));

const TIMEOUT = 20000;

describe('BookFormModal', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    listMock.mockResolvedValue({ records: [], total: 0, page: 1, size: 100, pages: 0 });
    createMock.mockResolvedValue({});
    updateMock.mockResolvedValue({});
  });

  const typeInto = (id: string, value: string) => {
    const input = document.getElementById(id) as HTMLInputElement;
    fireEvent.change(input, { target: { value } });
    return input;
  };

  const addAuthor = (name: string) => {
    const input = document.getElementById('authorNames') as HTMLInputElement;
    fireEvent.change(input, { target: { value: `${name},` } });
  };

  const submit = () => {
    fireEvent.click(screen.getByRole('button', { name: /保\s*存/ }));
  };

  it(
    '作者与出版社是自由输入框，提交时按名称发送',
    async () => {
      render(<BookFormModal open record={null} onCancel={vi.fn()} onSaved={onSavedMock} />);

      typeInto('title', '三体');
      typeInto('isbn', '9787536692930');

      const authorInput = typeInto('authorNames', '');
      expect(authorInput).toBeTruthy();
      addAuthor('刘慈欣');
      addAuthor('小岛秀夫');
      typeInto('publisherName', '科幻出版社');

      submit();

      await waitFor(() => expect(createMock).toHaveBeenCalledTimes(1));
      const payload = createMock.mock.calls[0][0];
      expect(payload.authorNames).toEqual(['刘慈欣', '小岛秀夫']);
      expect(payload.publisherName).toBe('科幻出版社');
      expect(payload.title).toBe('三体');
      expect(payload.isbn).toBe('9787536692930');
      expect(onSavedMock).toHaveBeenCalled();
    },
    TIMEOUT,
  );

  it(
    '未填写作者时不提交并提示',
    async () => {
      render(<BookFormModal open record={null} onCancel={vi.fn()} onSaved={onSavedMock} />);

      typeInto('title', '三体');
      typeInto('isbn', '9787536692930');
      submit();

      await waitFor(() => expect(screen.getAllByText('请至少填写一位作者').length).toBeGreaterThan(0));
      expect(createMock).not.toHaveBeenCalled();
    },
    TIMEOUT,
  );
});
