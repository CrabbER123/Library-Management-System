import { Component, type ErrorInfo, type ReactNode } from 'react';
import { Button, Result, Space, Typography, type ResultProps } from 'antd';

interface Props {
  children: ReactNode;
  /** 变化时自动清除错误状态，例如切换路由后重新渲染 */
  resetKey?: string;
  status?: ResultProps['status'];
  title?: string;
}

interface State {
  message: string | null;
}

/**
 * 页面级错误边界：单个页面渲染异常时只影响内容区，不再整页白屏。
 */
export default class ErrorBoundary extends Component<Props, State> {
  state: State = { message: null };

  static getDerivedStateFromError(error: unknown): State {
    return { message: error instanceof Error ? error.message : String(error) };
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    // 浏览器控制台保留完整堆栈，便于定位
    console.error('页面渲染失败', error, info.componentStack);
  }

  componentDidUpdate(prevProps: Props) {
    if (prevProps.resetKey !== this.props.resetKey && this.state.message) {
      this.setState({ message: null });
    }
  }

  private reload = () => {
    window.location.reload();
  };

  render() {
    if (!this.state.message) {
      return this.props.children;
    }
    return (
      <Result
        status={this.props.status ?? 'error'}
        title={this.props.title ?? '页面出错了'}
        subTitle="该页面在渲染时遇到异常，你可以刷新重试，或返回上一页继续使用其他功能。"
        extra={
          <Space direction="vertical" size="middle" style={{ width: '100%' }}>
            <Typography.Paragraph type="secondary" style={{ marginBottom: 0, textAlign: 'center' }}>
              错误信息：{this.state.message}
            </Typography.Paragraph>
            <Space>
              <Button type="primary" onClick={this.reload}>
                刷新页面
              </Button>
              <Button onClick={() => window.history.back()}>返回上一页</Button>
            </Space>
          </Space>
        }
      />
    );
  }
}
