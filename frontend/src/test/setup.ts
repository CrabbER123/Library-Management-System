import '@testing-library/jest-dom/vitest';

// antd 部分组件依赖 matchMedia / ResizeObserver，jsdom 中没有实现，这里做最小桩
Object.defineProperty(window, 'matchMedia', {
  writable: true,
  value: (query: string) => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: () => {},
    removeListener: () => {},
    addEventListener: () => {},
    removeEventListener: () => {},
    dispatchEvent: () => false,
  }),
});

class ResizeObserverStub {
  observe() {}
  unobserve() {}
  disconnect() {}
}
window.ResizeObserver = window.ResizeObserver ?? (ResizeObserverStub as unknown as typeof ResizeObserver);

// jsdom 未实现伪元素样式查询，antd 测量滚动条宽度时会触发 "Not implemented" 报错，
// 这里退化为忽略伪元素参数，避免测试输出被噪声淹没
const realGetComputedStyle = window.getComputedStyle.bind(window);
window.getComputedStyle = ((element: Element) => realGetComputedStyle(element)) as typeof window.getComputedStyle;
