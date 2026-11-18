-- 初始种子数据：只保留三个角色字典 + 一个管理员账号
-- 账号：admin / Admin@123456（BCrypt 存储，登录后请立即修改密码）
-- 业务数据（图书、作者、出版社、分类、读者、借阅）一律由系统内手工录入，不再预置演示数据。

INSERT INTO roles (id, code, name, description)
VALUES (1, 'ADMIN', '系统管理员', '用户、角色与系统配置'),
       (2, 'LIBRARIAN', '图书馆员', '图书、读者与借还业务'),
       (3, 'READER', '读者', '查询图书与本人借阅记录');

INSERT INTO users (id, username, password_hash, display_name, email, phone, status)
VALUES (1, 'admin', '$2a$10$CuQjVl/kdgcOlAzJGBwNu.rvGad55bHF79kq6KODssOBLN50s7yHK',
        '系统管理员', 'admin@library.local', NULL, 'ACTIVE');

INSERT INTO user_roles (user_id, role_id)
VALUES (1, 1);
