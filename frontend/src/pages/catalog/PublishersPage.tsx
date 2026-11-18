import MasterDataPage from './MasterDataPage';

export default function PublishersPage() {
  return (
    <MasterDataPage
      resource="publishers"
      title="出版社"
      description="被图书引用的出版社不能删除。"
      keywordPlaceholder="名称 / 地址 / 电话"
      columns={[
        { key: 'name', title: '名称', width: 200 },
        { key: 'address', title: '地址', ellipsis: true },
        { key: 'phone', title: '电话', width: 150 },
      ]}
      fields={[
        { name: 'name', label: '名称', required: true, max: 100, placeholder: '例如：人民文学出版社' },
        { name: 'address', label: '地址', max: 255, placeholder: '选填' },
        { name: 'website', label: '网址', max: 255, placeholder: 'https://…' },
        { name: 'phone', label: '电话', max: 32, placeholder: '选填' },
      ]}
    />
  );
}
