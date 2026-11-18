import { Button, Result } from 'antd';
import { useNavigate } from 'react-router-dom';

export default function ForbiddenPage() {
  const navigate = useNavigate();
  return (
    <Result
      status="403"
      title="403"
      subTitle="当前账号没有访问该页面的权限。如需操作，请联系管理员分配角色。"
      extra={
        <Button type="primary" onClick={() => navigate('/books')}>
          返回图书列表
        </Button>
      }
    />
  );
}
