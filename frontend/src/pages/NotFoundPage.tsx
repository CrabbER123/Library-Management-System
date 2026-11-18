import { Button, Result } from 'antd';
import { useNavigate } from 'react-router-dom';

export default function NotFoundPage() {
  const navigate = useNavigate();
  return (
    <Result
      status="404"
      title="404"
      subTitle="页面不存在，可能是链接已失效。"
      extra={
        <Button type="primary" onClick={() => navigate('/books')}>
          回到首页
        </Button>
      }
    />
  );
}
