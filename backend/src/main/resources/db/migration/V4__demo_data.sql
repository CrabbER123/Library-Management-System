-- 阶段6：演示数据（读者档案 + 历史借阅 + 在借记录），用于展示统计报表与逾期提醒。
-- 说明：
--   1) 借阅日期基于 CURDATE() 相对计算，任何时间启动项目都能看到"逾期/在借"两类数据；
--   2) 每条在借记录都会同步扣减对应图书的 available_stock，保持 total = available + 已借出；
--   3) borrow_records 的 uk_active_borrow 唯一索引保证同一读者对同一本书只会有一条在借记录。

INSERT INTO readers (id, user_id, card_no, name, phone, email, status, max_borrow, created_at, updated_at)
VALUES (2, NULL, 'R2024002', '李四', '13800000002', 'lisi@example.com', 'ACTIVE', 5, NOW(), NOW()),
       (3, NULL, 'R2024003', '王五', '13800000003', 'wangwu@example.com', 'ACTIVE', 3, NOW(), NOW());

-- 已归还的历史借阅：其中 reader 3 借《三体》逾期 10 天归还，罚款 10 * 0.50 = 5.00
INSERT INTO borrow_records (reader_id, book_id, borrow_date, due_date, return_date, renew_times, status,
                            fine_amount, operator_id, remark, created_at, updated_at)
VALUES (1, 1, DATE_SUB(CURDATE(), INTERVAL 90 DAY), DATE_SUB(CURDATE(), INTERVAL 60 DAY),
        DATE_SUB(CURDATE(), INTERVAL 65 DAY), 0, 'RETURNED', 0.00, 2, '按期归还', NOW(), NOW()),
       (2, 2, DATE_SUB(CURDATE(), INTERVAL 80 DAY), DATE_SUB(CURDATE(), INTERVAL 50 DAY),
        DATE_SUB(CURDATE(), INTERVAL 45 DAY), 1, 'RETURNED', 0.00, 2, '续借一次后按期归还', NOW(), NOW()),
       (3, 2, DATE_SUB(CURDATE(), INTERVAL 70 DAY), DATE_SUB(CURDATE(), INTERVAL 40 DAY),
        DATE_SUB(CURDATE(), INTERVAL 30 DAY), 0, 'RETURNED', 5.00, 2, '逾期 10 天归还', NOW(), NOW()),
       (1, 4, DATE_SUB(CURDATE(), INTERVAL 45 DAY), DATE_SUB(CURDATE(), INTERVAL 15 DAY),
        DATE_SUB(CURDATE(), INTERVAL 20 DAY), 0, 'RETURNED', 0.00, 2, '馆员代还', NOW(), NOW());

-- 在借记录：一条正常在借（reader 3 借《三体》），一条已逾期 10 天（reader 2 借《深入理解计算机系统》）
INSERT INTO borrow_records (reader_id, book_id, borrow_date, due_date, return_date, renew_times, status,
                            fine_amount, operator_id, remark, created_at, updated_at)
VALUES (3, 2, DATE_SUB(CURDATE(), INTERVAL 5 DAY), DATE_ADD(CURDATE(), INTERVAL 25 DAY),
        NULL, 0, 'BORROWED', 0.00, 2, NULL, NOW(), NOW()),
       (2, 3, DATE_SUB(CURDATE(), INTERVAL 40 DAY), DATE_SUB(CURDATE(), INTERVAL 10 DAY),
        NULL, 0, 'BORROWED', 0.00, 2, '已电话提醒', NOW(), NOW());

-- 同步库存：上面两条在借各占用 1 册
UPDATE books SET available_stock = available_stock - 1 WHERE id = 2;
UPDATE books SET available_stock = available_stock - 1 WHERE id = 3;
