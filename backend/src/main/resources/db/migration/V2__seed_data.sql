-- 初始种子数据：三个角色 + 三个演示账号 + 一个读者档案
-- 演示密码（BCrypt）：
--   admin     / Admin@123456
--   librarian / Lib@123456
--   reader01  / Reader@123456

INSERT INTO roles (id, code, name, description)
VALUES (1, 'ADMIN', '系统管理员', '用户、角色与系统配置'),
       (2, 'LIBRARIAN', '图书馆员', '图书、读者与借还业务'),
       (3, 'READER', '读者', '查询图书与本人借阅记录');

INSERT INTO users (id, username, password_hash, display_name, email, phone, status)
VALUES (1, 'admin', '$2a$10$CuQjVl/kdgcOlAzJGBwNu.rvGad55bHF79kq6KODssOBLN50s7yHK',
        '系统管理员', 'admin@library.local', NULL, 'ACTIVE'),
       (2, 'librarian', '$2a$10$qsLAqET04A23n4k/F.HW/.Ig5dECnpH5TxfBX65L1xckjBuA3U6Fi',
        '王馆员', 'librarian@library.local', NULL, 'ACTIVE'),
       (3, 'reader01', '$2a$10$./b8G1z46KLCkUz7xPOjC.Iue7qE.1f5NxTjuoYei6VQbVI71bxAa',
        '张三', NULL, '13800000001', 'ACTIVE');

INSERT INTO user_roles (user_id, role_id)
VALUES (1, 1),
       (2, 2),
       (3, 3);

INSERT INTO readers (id, user_id, card_no, name, phone, email, status, max_borrow)
VALUES (1, 3, 'R2024001', '张三', '13800000001', NULL, 'ACTIVE', 5);
