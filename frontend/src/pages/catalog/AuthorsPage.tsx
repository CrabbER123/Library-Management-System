import MasterDataPage from './MasterDataPage';

export default function AuthorsPage() {
  return (
    <MasterDataPage
      resource="authors"
      title="作者"
      description="图书可关联多位作者；被图书引用的作者不能删除。"
      keywordPlaceholder="姓名 / 国籍"
      columns={[
        { key: 'name', title: '姓名', width: 180 },
        { key: 'nationality', title: '国籍', width: 140 },
        { key: 'intro', title: '简介', ellipsis: true },
      ]}
      fields={[
        { name: 'name', label: '姓名', required: true, max: 100, placeholder: '例如：刘慈欣' },
        { name: 'nationality', label: '国籍', max: 64, placeholder: '例如：中国' },
        { name: 'intro', label: '简介', type: 'textarea', max: 5000, placeholder: '作者简介（选填）' },
      ]}
    />
  );
}
