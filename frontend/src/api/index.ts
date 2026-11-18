import { http } from './http';
import type {
  BookPayload,
  BookVO,
  BorrowRecordVO,
  BorrowRules,
  CategoryDistribution,
  CurrentUser,
  LoginResult,
  NamedVO,
  OverviewStats,
  PageResult,
  PopularBook,
  ReaderPayload,
  ReaderVO,
  RegisterPayload,
  RoleVO,
  UserVO,
} from './types';

// ---------------------------------------------------------------- 认证

export const authApi = {
  login: (username: string, password: string) =>
    http.post('/auth/login', { username, password }) as unknown as Promise<LoginResult>,
  register: (payload: RegisterPayload) =>
    http.post('/auth/register', payload) as unknown as Promise<LoginResult>,
  me: () => http.get('/auth/me') as unknown as Promise<CurrentUser>,
  logout: () => http.post('/auth/logout') as unknown as Promise<void>,
};

// ---------------------------------------------------------------- 用户与角色

export interface UserQuery {
  page?: number;
  size?: number;
  keyword?: string;
  role?: string;
  status?: string;
  sort?: string;
}

export const userApi = {
  list: (params: UserQuery) => http.get('/users', { params }) as unknown as Promise<PageResult<UserVO>>,
  roles: () => http.get('/roles') as unknown as Promise<RoleVO[]>,
  create: (payload: Record<string, unknown>) => http.post('/users', payload) as unknown as Promise<UserVO>,
  update: (id: number, payload: Record<string, unknown>) =>
    http.put(`/users/${id}`, payload) as unknown as Promise<UserVO>,
  updateStatus: (id: number, status: string) =>
    http.patch(`/users/${id}/status`, { status }) as unknown as Promise<UserVO>,
  updateRoles: (id: number, roles: string[]) =>
    http.put(`/users/${id}/roles`, { roles }) as unknown as Promise<void>,
  remove: (id: number) => http.delete(`/users/${id}`) as unknown as Promise<void>,
};

// ---------------------------------------------------------------- 图书

export interface BookQuery {
  page?: number;
  size?: number;
  keyword?: string;
  categoryId?: number;
  authorId?: number;
  publisherId?: number;
  status?: string;
  sort?: string;
}

export const bookApi = {
  list: (params: BookQuery) => http.get('/books', { params }) as unknown as Promise<PageResult<BookVO>>,
  get: (id: number) => http.get(`/books/${id}`) as unknown as Promise<BookVO>,
  create: (payload: BookPayload) => http.post('/books', payload) as unknown as Promise<BookVO>,
  update: (id: number, payload: BookPayload) =>
    http.put(`/books/${id}`, payload) as unknown as Promise<BookVO>,
  updateStatus: (id: number, status: string) =>
    http.patch(`/books/${id}/status`, { status }) as unknown as Promise<BookVO>,
  updateStock: (id: number, totalStock: number) =>
    http.put(`/books/${id}/stock`, { totalStock }) as unknown as Promise<BookVO>,
  remove: (id: number) => http.delete(`/books/${id}`) as unknown as Promise<void>,
};

// ---------------------------------------------------------------- 主数据（作者/出版社/分类）

export type NamedResource = 'authors' | 'publishers' | 'categories';

export interface NamedQuery {
  page?: number;
  size?: number;
  keyword?: string;
  sort?: string;
}

export const namedApi = {
  list: (resource: NamedResource, params: NamedQuery) =>
    http.get(`/${resource}`, { params }) as unknown as Promise<PageResult<NamedVO>>,
  create: (resource: NamedResource, payload: Record<string, unknown>) =>
    http.post(`/${resource}`, payload) as unknown as Promise<NamedVO>,
  update: (resource: NamedResource, id: number, payload: Record<string, unknown>) =>
    http.put(`/${resource}/${id}`, payload) as unknown as Promise<NamedVO>,
  remove: (resource: NamedResource, id: number) =>
    http.delete(`/${resource}/${id}`) as unknown as Promise<void>,
};

// ---------------------------------------------------------------- 读者

export interface ReaderQuery {
  page?: number;
  size?: number;
  keyword?: string;
  status?: string;
  sort?: string;
}

export const readerApi = {
  list: (params: ReaderQuery) => http.get('/readers', { params }) as unknown as Promise<PageResult<ReaderVO>>,
  get: (id: number) => http.get(`/readers/${id}`) as unknown as Promise<ReaderVO>,
  me: () => http.get('/readers/me') as unknown as Promise<ReaderVO>,
  create: (payload: ReaderPayload) => http.post('/readers', payload) as unknown as Promise<ReaderVO>,
  update: (id: number, payload: ReaderPayload) =>
    http.put(`/readers/${id}`, payload) as unknown as Promise<ReaderVO>,
  updateStatus: (id: number, status: string) =>
    http.patch(`/readers/${id}/status`, { status }) as unknown as Promise<ReaderVO>,
  remove: (id: number) => http.delete(`/readers/${id}`) as unknown as Promise<void>,
};

// ---------------------------------------------------------------- 借阅

export interface BorrowQuery {
  page?: number;
  size?: number;
  keyword?: string;
  status?: string;
  readerId?: number;
  bookId?: number;
  overdueOnly?: boolean;
  sort?: string;
}

export const borrowApi = {
  list: (params: BorrowQuery) => http.get('/borrows', { params }) as unknown as Promise<PageResult<BorrowRecordVO>>,
  my: (params: { page?: number; size?: number; status?: string }) =>
    http.get('/borrows/my', { params }) as unknown as Promise<PageResult<BorrowRecordVO>>,
  get: (id: number) => http.get(`/borrows/${id}`) as unknown as Promise<BorrowRecordVO>,
  // 馆员借书需传 readerId；读者自助借书不传，服务端按当前登录账号绑定档案
  borrow: (payload: { readerId?: number | null; bookId: number; remark?: string }) =>
    http.post('/borrows', payload) as unknown as Promise<BorrowRecordVO>,
  returnBook: (id: number) => http.post(`/borrows/${id}/return`) as unknown as Promise<BorrowRecordVO>,
  renew: (id: number) => http.post(`/borrows/${id}/renew`) as unknown as Promise<BorrowRecordVO>,
};

// ---------------------------------------------------------------- 配置与统计

export const configApi = {
  borrowRules: () => http.get('/configs/borrow-rules') as unknown as Promise<BorrowRules>,
};

export const statsApi = {
  overview: () => http.get('/stats/overview') as unknown as Promise<OverviewStats>,
  popularBooks: (limit = 10) =>
    http.get('/stats/popular-books', { params: { limit } }) as unknown as Promise<PopularBook[]>,
  categoryDistribution: () =>
    http.get('/stats/category-distribution') as unknown as Promise<CategoryDistribution[]>,
};
