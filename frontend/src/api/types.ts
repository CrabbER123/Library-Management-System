// 与后端 DTO/VO 对应的类型定义（字段名保持后端 JSON 原名）
export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
  timestamp: number;
}

export interface PageResult<T> {
  records: T[];
  total: number;
  page: number;
  size: number;
  pages: number;
}

export interface PageQuery {
  page?: number;
  size?: number;
  keyword?: string;
  sort?: string;
}

export interface UserVO {
  id: number;
  username: string;
  displayName: string;
  email: string | null;
  phone: string | null;
  status: 'ACTIVE' | 'DISABLED';
  roles: string[];
  createdAt: string;
  lastLoginAt: string | null;
}

export interface CurrentUser {
  id: number;
  username: string;
  displayName: string;
  roles: string[];
  readerId: number | null;
}

export interface LoginResult {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  user: UserVO;
}

export interface RegisterPayload {
  username: string;
  password: string;
  displayName: string;
  phone?: string;
  email?: string;
}

export interface AuthorBrief {
  id: number;
  name: string;
}

export interface BookVO {
  id: number;
  isbn: string;
  title: string;
  subtitle: string | null;
  authors: AuthorBrief[];
  publisherId: number | null;
  publisherName: string | null;
  categoryId: number | null;
  categoryName: string | null;
  publishDate: string | null;
  language: string | null;
  price: number | null;
  coverUrl: string | null;
  summary: string | null;
  totalStock: number;
  availableStock: number;
  status: 'AVAILABLE' | 'OFFLINE';
  createdAt: string;
  updatedAt: string;
}

export interface BookPayload {
  isbn: string;
  title: string;
  subtitle?: string | null;
  /** 作者姓名（自由输入，服务端按名称复用或自动建档） */
  authorNames: string[];
  /** 出版社名称（自由输入，不存在时自动创建） */
  publisherName?: string | null;
  categoryId?: number | null;
  publishDate?: string | null;
  language?: string | null;
  price?: number | null;
  coverUrl?: string | null;
  summary?: string | null;
  totalStock: number;
  status?: 'AVAILABLE' | 'OFFLINE';
}

export interface NamedVO {
  id: number;
  name: string;
  code?: string | null;
  description?: string | null;
  address?: string | null;
  website?: string | null;
  phone?: string | null;
  nationality?: string | null;
  intro?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface ReaderVO {
  id: number;
  userId: number | null;
  cardNo: string;
  name: string;
  phone: string | null;
  email: string | null;
  status: 'ACTIVE' | 'SUSPENDED';
  maxBorrow: number;
  borrowingCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface ReaderPayload {
  cardNo: string;
  name: string;
  phone?: string | null;
  email?: string | null;
  status?: 'ACTIVE' | 'SUSPENDED';
  maxBorrow?: number | null;
  userId?: number | null;
}

export interface BorrowRecordVO {
  id: number;
  readerId: number;
  readerName: string | null;
  readerCardNo: string | null;
  bookId: number;
  bookTitle: string | null;
  bookIsbn: string | null;
  borrowDate: string;
  dueDate: string;
  returnDate: string | null;
  renewTimes: number;
  status: 'BORROWED' | 'RETURNED';
  fineAmount: number | null;
  overdueDays: number;
  currentFine: number;
  operatorId: number | null;
  createdAt: string;
}

export interface BorrowRules {
  loanDays: number;
  maxRenewTimes: number;
  renewDays: number;
  maxBorrowPerReader: number;
  finePerDay: number;
  allowRenewWhenOverdue: boolean;
  maxFineAmount: number;
}

export interface OverviewStats {
  totalBooks: number;
  totalTitles: number;
  availableStock: number;
  borrowedCount: number;
  overdueCount: number;
  totalReaders: number;
  activeReaders: number;
  totalBorrows: number;
}

export interface PopularBook {
  bookId: number;
  title: string;
  isbn: string;
  borrowCount: number;
}

export interface CategoryDistribution {
  categoryName: string;
  bookCount: number;
}
