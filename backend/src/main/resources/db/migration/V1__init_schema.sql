-- 图书管理系统 初始化表结构（MySQL 8.0+）
-- 约定：主键统一 BIGINT 自增；时间统一 DATETIME；金额 DECIMAL(10,2)；字符集 utf8mb4。

CREATE TABLE roles
(
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    code        VARCHAR(32)  NOT NULL COMMENT '角色编码：ADMIN / LIBRARIAN / READER',
    name        VARCHAR(64)  NOT NULL COMMENT '角色名称',
    description VARCHAR(255) NULL COMMENT '角色说明',
    PRIMARY KEY (id),
    UNIQUE KEY uk_roles_code (code)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='角色字典';

CREATE TABLE users
(
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    username      VARCHAR(32)  NOT NULL COMMENT '登录名',
    password_hash VARCHAR(100) NOT NULL COMMENT 'BCrypt 密码',
    display_name  VARCHAR(64)  NOT NULL COMMENT '显示名称',
    email         VARCHAR(128) NULL,
    phone         VARCHAR(32)  NULL,
    status        VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE / DISABLED',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_login_at DATETIME     NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_username (username),
    KEY idx_users_status (status)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='登录账号';

CREATE TABLE user_roles
(
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    KEY idx_user_roles_role (role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='用户角色关联';

CREATE TABLE refresh_tokens
(
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    user_id      BIGINT      NOT NULL,
    token_hash   CHAR(64)    NOT NULL COMMENT 'refresh token 的 SHA-256 摘要',
    expires_at   DATETIME    NOT NULL,
    revoked      TINYINT(1)  NOT NULL DEFAULT 0,
    created_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_used_at DATETIME    NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_refresh_token_hash (token_hash),
    KEY idx_refresh_user (user_id),
    KEY idx_refresh_expires (expires_at),
    CONSTRAINT fk_refresh_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='刷新令牌';

CREATE TABLE readers
(
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    user_id    BIGINT      NULL COMMENT '关联登录账号，可为空',
    card_no    VARCHAR(32) NOT NULL COMMENT '借阅证号',
    name       VARCHAR(64) NOT NULL,
    phone      VARCHAR(32) NULL,
    email      VARCHAR(128) NULL,
    status     VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE / SUSPENDED',
    max_borrow INT         NOT NULL DEFAULT 5 COMMENT '最大同时借阅数（空值回落到全局配置）',
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_readers_card_no (card_no),
    UNIQUE KEY uk_readers_user_id (user_id),
    KEY idx_readers_status (status),
    KEY idx_readers_name (name),
    CONSTRAINT fk_readers_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='读者档案';

CREATE TABLE categories
(
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(64)  NOT NULL,
    code        VARCHAR(32)  NULL COMMENT '分类编码，可选',
    parent_id   BIGINT       NULL COMMENT '预留：父分类，MVP 为单层不使用',
    description VARCHAR(255) NULL,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_categories_name (name),
    KEY idx_categories_parent (parent_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='图书分类';

CREATE TABLE authors
(
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(100) NOT NULL,
    nationality VARCHAR(64)  NULL,
    intro       TEXT         NULL COMMENT '作者简介',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_authors_name (name),
    KEY idx_authors_name_prefix (name)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='作者';

CREATE TABLE publishers
(
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    name       VARCHAR(100) NOT NULL,
    address    VARCHAR(255) NULL,
    website    VARCHAR(255) NULL,
    phone      VARCHAR(32)  NULL,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_publishers_name (name)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='出版社';

CREATE TABLE books
(
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    isbn            VARCHAR(20)   NOT NULL COMMENT 'ISBN-10/13 均可',
    title           VARCHAR(200)  NOT NULL,
    subtitle        VARCHAR(200)  NULL,
    publisher_id    BIGINT        NULL,
    category_id     BIGINT        NULL,
    publish_date    DATE          NULL,
    language        VARCHAR(32)   NULL,
    price           DECIMAL(10, 2) NULL,
    cover_url       VARCHAR(512)  NULL,
    summary         TEXT          NULL,
    total_stock     INT           NOT NULL DEFAULT 0 COMMENT '总库存',
    available_stock INT           NOT NULL DEFAULT 0 COMMENT '可借库存',
    status          VARCHAR(16)   NOT NULL DEFAULT 'AVAILABLE' COMMENT 'AVAILABLE / OFFLINE',
    created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_books_isbn (isbn),
    KEY idx_books_title (title),
    KEY idx_books_category (category_id),
    KEY idx_books_publisher (publisher_id),
    KEY idx_books_status (status),
    CONSTRAINT chk_books_stock CHECK (available_stock >= 0 AND available_stock <= total_stock),
    CONSTRAINT fk_books_category FOREIGN KEY (category_id) REFERENCES categories (id) ON DELETE SET NULL,
    CONSTRAINT fk_books_publisher FOREIGN KEY (publisher_id) REFERENCES publishers (id) ON DELETE SET NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='图书';

CREATE TABLE book_authors
(
    book_id   BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    PRIMARY KEY (book_id, author_id),
    KEY idx_book_authors_author (author_id),
    CONSTRAINT fk_book_authors_book FOREIGN KEY (book_id) REFERENCES books (id) ON DELETE CASCADE,
    CONSTRAINT fk_book_authors_author FOREIGN KEY (author_id) REFERENCES authors (id) ON DELETE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='图书作者关联';

CREATE TABLE borrow_records
(
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    reader_id   BIGINT        NOT NULL,
    book_id     BIGINT        NOT NULL,
    borrow_date DATE          NOT NULL,
    due_date    DATE          NOT NULL,
    return_date DATE          NULL,
    renew_times INT           NOT NULL DEFAULT 0,
    status      VARCHAR(16)   NOT NULL DEFAULT 'BORROWED' COMMENT 'BORROWED / RETURNED',
    fine_amount DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    operator_id BIGINT        NULL COMMENT '办理借出的馆员',
    remark      VARCHAR(255)  NULL,
    created_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    -- 仅未归还记录参与唯一约束：同一读者同一本书只能有一条在借记录
    active_flag TINYINT GENERATED ALWAYS AS (IF(status = 'BORROWED', 1, NULL)) STORED,
    PRIMARY KEY (id),
    UNIQUE KEY uk_active_borrow (reader_id, book_id, active_flag),
    KEY idx_borrow_reader_status (reader_id, status),
    KEY idx_borrow_book_status (book_id, status),
    KEY idx_borrow_due_status (due_date, status),
    KEY idx_borrow_date (borrow_date),
    CONSTRAINT fk_borrow_reader FOREIGN KEY (reader_id) REFERENCES readers (id) ON DELETE RESTRICT,
    CONSTRAINT fk_borrow_book FOREIGN KEY (book_id) REFERENCES books (id) ON DELETE RESTRICT,
    CONSTRAINT fk_borrow_operator FOREIGN KEY (operator_id) REFERENCES users (id) ON DELETE SET NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='借阅记录';
