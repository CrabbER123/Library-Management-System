import MasterDataPage from './MasterDataPage';

export default function CategoriesPage() {
  return (
    <MasterDataPage
      resource="categories"
      title="分类"
      description="当前版本为单层分类；被图书引用的分类不能删除。"
      keywordPlaceholder="名称 / 编码 / 描述"
      columns={[
        { key: 'name', title: '名称', width: 180 },
        { key: 'code', title: '编码', width: 140 },
        { key: 'description', title: '描述', ellipsis: true },
      ]}
      fields={[
        { name: 'name', label: '名称', required: true, max: 64, placeholder: '例如：计算机' },
        { name: 'code', label: '编码', max: 32, placeholder: '例如：CS' },
        { name: 'description', label: '描述', type: 'textarea', max: 255, placeholder: '选填' },
      ]}
    />
  );
}
