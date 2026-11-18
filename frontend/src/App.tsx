import { Navigate, Route, Routes } from 'react-router-dom';
import { useAuth } from './auth/AuthContext';
import AppLayout from './components/AppLayout';
import { RequireAuth, RequireRole } from './components/RequireAuth';
import LoginPage from './pages/LoginPage';
import NotFoundPage from './pages/NotFoundPage';
import RegisterPage from './pages/RegisterPage';
import BooksPage from './pages/book/BooksPage';
import MyBorrowsPage from './pages/borrow/MyBorrowsPage';
import BorrowsPage from './pages/borrow/BorrowsPage';
import AuthorsPage from './pages/catalog/AuthorsPage';
import CategoriesPage from './pages/catalog/CategoriesPage';
import PublishersPage from './pages/catalog/PublishersPage';
import ReadersPage from './pages/reader/ReadersPage';
import StatsPage from './pages/stats/StatsPage';
import UsersPage from './pages/user/UsersPage';

/** 首页按角色分流：读者进"我的借阅"，馆员/管理员进图书列表 */
function HomeRedirect() {
  const { user } = useAuth();
  const isReaderOnly = !!user && user.roles.includes('READER') && !user.roles.includes('LIBRARIAN') && !user.roles.includes('ADMIN');
  return <Navigate to={isReaderOnly ? '/my-borrows' : '/books'} replace />;
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route element={<RequireAuth />}>
        <Route element={<AppLayout />}>
          <Route index element={<HomeRedirect />} />
          <Route path="books" element={<BooksPage />} />
          <Route path="my-borrows" element={<MyBorrowsPage />} />
          <Route element={<RequireRole roles={['ADMIN', 'LIBRARIAN']} />}>
            <Route path="borrows" element={<BorrowsPage />} />
            <Route path="readers" element={<ReadersPage />} />
            <Route path="catalog/authors" element={<AuthorsPage />} />
            <Route path="catalog/publishers" element={<PublishersPage />} />
            <Route path="catalog/categories" element={<CategoriesPage />} />
            <Route path="stats" element={<StatsPage />} />
          </Route>
          <Route element={<RequireRole roles={['ADMIN']} />}>
            <Route path="users" element={<UsersPage />} />
          </Route>
          <Route path="*" element={<NotFoundPage />} />
        </Route>
      </Route>
    </Routes>
  );
}
