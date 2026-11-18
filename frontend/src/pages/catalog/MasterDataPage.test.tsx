import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import type { Mock } from 'vitest';

vi.mock('../../api', () => ({
  namedApi: {
    list: vi.fn(),
    create: vi.fn(),
    update: vi.fn(),
    remove: vi.fn(),
  },
}));

import { namedApi } from '../../api';
import MasterDataPage from './MasterDataPage';

const listMock = namedApi.list as unknown as Mock;

function renderPage() {
  return render(
    <MasterDataPage
      resource="authors"
      title="作者"
      description="维护图书作者信息"
      keywordPlaceholder="搜索作者姓名"
      columns={[
        { key: 'name', title: '作者' },
        { key: 'nationality', title: '国籍', width: 120 },
      ]}
      fields={[
        { name: 'name', label: '姓名', required: true },
        { name: 'nationality', label: '国籍' },
      ]}
    />,
  );
}

describe('MasterDataPage', () => {
  beforeEach(() => {
    listMock.mockReset();
    listMock.mockResolvedValue({
      records: [
        { id: 1, name: '曹雪芹', nationality: '中国' },
        { id: 2, name: '刘慈欣', nationality: '中国' },
      ],
      total: 2,
      page: 1,
      size: 10,
      pages: 1,
    });
  });

  it('按配置渲染标题、描述与列数据', async () => {
    renderPage();

    expect(screen.getByText('作者管理')).toBeInTheDocument();
    expect(screen.getByText('维护图书作者信息')).toBeInTheDocument();
    expect(await screen.findByText('曹雪芹')).toBeInTheDocument();
    expect(await screen.findByText('刘慈欣')).toBeInTheDocument();
    expect(listMock).toHaveBeenCalledWith('authors', {
      page: 1,
      size: 10,
      keyword: undefined,
      sort: 'name,asc',
    });
  });

  it('空字段展示占位符而非 null', async () => {
    listMock.mockResolvedValue({
      records: [{ id: 3, name: '佚名', nationality: null }],
      total: 1,
      page: 1,
      size: 10,
      pages: 1,
    });
    renderPage();

    expect(await screen.findByText('佚名')).toBeInTheDocument();
    expect(await screen.findByText('—')).toBeInTheDocument();
  });

  it('按 defaultSort 传入默认排序（分类页要求按编码倒序）', async () => {
    render(
      <MasterDataPage
        resource="categories"
        title="分类"
        description="单层分类"
        keywordPlaceholder="名称 / 编码"
        defaultSort="code,desc"
        columns={[
          { key: 'name', title: '名称' },
          { key: 'code', title: '编码' },
        ]}
        fields={[{ name: 'name', label: '名称', required: true }]}
      />,
    );

    await waitFor(() =>
      expect(listMock).toHaveBeenCalledWith('categories', {
        page: 1,
        size: 10,
        keyword: undefined,
        sort: 'code,desc',
      }),
    );
  });

  it('点击新增按钮打开按字段配置生成的表单弹窗', async () => {
    const user = userEvent.setup();
    renderPage();

    await user.click(screen.getByRole('button', { name: /新增作者/ }));

    const dialog = await screen.findByRole('dialog');
    expect(dialog).toBeInTheDocument();
    // 表单按 fields 配置动态生成：必填项带 *，其余为普通输入框
    expect(within(dialog).getByText('姓名')).toBeInTheDocument();
    expect(within(dialog).getByText('国籍')).toBeInTheDocument();
    expect(within(dialog).getAllByRole('textbox')).toHaveLength(2);
    expect(within(dialog).getByRole('button', { name: /保\s*存/ })).toBeInTheDocument();
  });
});
