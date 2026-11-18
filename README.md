# 图书管理系统（Library Management System）

一个可本地运行、分层清晰的图书管理系统，覆盖用户与权限、图书主数据、读者、借还书、逾期罚款与统计。
当前进度：**阶段 2 —— 后端基础工程 + 数据库迁移 + 登录鉴权 + 用户/角色管理**。

## 技术栈

| 层 | 选型 | 版本 |
| --- | --- | --- |
| 后端 | Java + Spring Boot | 21 / 3.3.5 |
| 安全 | Spring Security + JWT (jjwt) | 6.x / 0.12.6 |
| 持久层 | MyBatis-Plus | 3.5.7 |
| 数据库 | MySQL | 8.0 |
| 迁移 | Flyway | Boot 管理版本 |
| API 文档 | SpringDoc OpenAPI | 2.6.0 |
| 测试 | JUnit 5 + Mockito + Testcontainers | Boot 管理版本 |
| 前端（阶段 5） | React + TypeScript + Vite + Ant Design | 18 / 5.6 / 5.4 / 5 |
| 部署（阶段 7） | Docker Compose | — |

选型理由：Spring Boot 生态成熟、招聘与维护成本低；MyBatis-Plus 在复杂查询（分页、统计、联表）上比 JPA 更可控；
Flyway 保证多环境数据库结构一致；JWT 无状态鉴权便于前后端分离部署；React + Ant Design 适合中后台管理系统快速交付。

## 目录结构（当前）

```text
图书管理系统/
├── backend/
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/com/library/
│       │   │   ├── LibraryApplication.java
│       │   │   ├── common/          # 统一响应、异常、分页、排序、常量
│       │   │   ├── config/          # MyBatis-Plus、Jackson、OpenAPI 配置
│       │   │   ├── security/        # JWT、SecurityFilterChain、当前用户上下文
│       │   │   └── modules/
│       │   │       ├── auth/        # 登录、刷新、登出、当前用户
│       │   │       ├── user/        # 用户、角色、用户-角色
│       │   │       ├── reader/      # 读者档案与借阅证
│       │   │       ├── author/      # 作者
│       │   │       ├── publisher/   # 出版社
│       │   │       ├── category/    # 分类
│       │   │       ├── book/        # 图书与图书-作者关联
│       │   │       └── config/      # 只读系统配置（借阅规则）
│       │   └── resources/
│       │       ├── application.yml
│       │       ├── application-dev.yml
│       │       ├── application-prod.yml
│       │       └── db/migration/    # V1 建表、V2 账号种子、V3 图书主数据种子
│       └── test/java/com/library/   # 单元测试 + 集成测试
└── README.md
```

## 环境要求

- JDK 21
- Maven 3.9+
- MySQL 8.0（本机或 Docker）
- （阶段 5 起）Node.js 20 + pnpm
- （阶段 7 起）Docker Desktop

## 本地启动（后端）

1. 创建数据库与账号（MySQL 8.0）：

```sql
CREATE DATABASE library CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER 'library'@'%' IDENTIFIED BY 'library123';
GRANT ALL PRIVILEGES ON library.* TO 'library'@'%';
FLUSH PRIVILEGES;
```

2. 启动后端（Flyway 会自动建表并写入种子数据）：

```bash
cd backend
mvn spring-boot:run
```

默认连接 `localhost:3306/library`，可用环境变量覆盖：

| 环境变量 | 默认值 | 说明 |
| --- | --- | --- |
| `DB_HOST` / `DB_PORT` | `localhost` / `3306` | 数据库地址 |
| `DB_NAME` | `library` | 数据库名 |
| `DB_USERNAME` / `DB_PASSWORD` | `library` / `library123` | 数据库账号 |
| `JWT_SECRET` | 内置开发密钥 | 生产必须覆盖，长度 ≥ 32 字节 |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173,http://127.0.0.1:5173` | 允许的前端来源 |

3. 健康检查与文档：

- 健康检查：`GET http://localhost:8080/actuator/health`
- Swagger UI：`http://localhost:8080/swagger-ui.html`
- OpenAPI JSON：`http://localhost:8080/v3/api-docs`

## 默认账号（种子数据）

| 账号 | 密码 | 角色 | 说明 |
| --- | --- | --- | --- |
| `admin` | `Admin@123456` | 管理员 | 用户与角色管理、全部业务权限 |
| `librarian` | `Lib@123456` | 馆员 | 图书/读者/借还业务 |
| `reader01` | `Reader@123456` | 读者 | 仅查询图书与本人借阅记录（关联读者档案 R2024001） |

> 生产部署前请立即修改默认密码与 `JWT_SECRET`。

## 接口示例

登录：

```bash
curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"Admin@123456"}'
```

响应（统一结构）：

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "Kf3s...",
    "tokenType": "Bearer",
    "expiresIn": 1800,
    "user": { "id": 1, "username": "admin", "roles": ["ADMIN"] }
  },
  "timestamp": 1794979572236
}
```

携带 access token 访问受保护接口：

```bash
curl -s http://localhost:8080/api/v1/users?page=1&size=10 \
  -H "Authorization: Bearer <accessToken>"
```

刷新令牌（旧 refresh token 会立即失效，即轮换）：

```bash
curl -s -X POST http://localhost:8080/api/v1/auth/refresh \
  -H 'Content-Type: application/json' \
  -d '{"refreshToken":"<refreshToken>"}'
```

错误码约定：`0` 成功；`401xx` 未认证/令牌失效；`403xx` 无权限；`404xx` 不存在；`409xx` 冲突/重复；
`422xx` 业务规则或参数校验失败（如库存不足 `42201`、超出借阅上限 `42202`）；`50000` 服务器内部错误。

## 业务接口一览（阶段 2–3）

统一前缀 `/api/v1`，除登录/刷新外均需 `Authorization: Bearer <accessToken>`。

| 模块 | 方法与路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| 认证 | `POST /auth/login`、`POST /auth/refresh` | 匿名 | 登录、刷新（轮换旧 refresh token） |
| 认证 | `POST /auth/logout`、`GET /auth/me` | 登录 | 注销、当前用户（含 readerId） |
| 用户 | `GET/POST /users`、`GET/PUT/DELETE /users/{id}`、`PATCH /users/{id}/status`、`PUT /users/{id}/roles` | ADMIN | 用户与角色管理 |
| 角色 | `GET /roles` | ADMIN | 角色字典 |
| 图书 | `GET /books`、`GET /books/{id}` | 登录 | 分页搜索：`keyword`（书名/副标题/ISBN/作者名）、`categoryId`、`authorId`、`publisherId`、`status`、`sort` |
| 图书 | `POST /books`、`PUT /books/{id}`、`DELETE /books/{id}` | ADMIN/LIBRARIAN | 新增/编辑/删除（有借阅记录时拒绝删除） |
| 图书 | `PATCH /books/{id}/status`、`PUT /books/{id}/stock` | ADMIN/LIBRARIAN | 上下架、调整总库存（不得低于已借出数） |
| 作者 | `GET /authors`、`GET /authors/{id}` | 登录 | 分页搜索 |
| 作者 | `POST /authors`、`PUT /authors/{id}`、`DELETE /authors/{id}` | ADMIN/LIBRARIAN | 删除前检查是否被图书引用 |
| 出版社 | `/publishers` 同上 | 登录 / ADMIN+LIBRARIAN | 同上 |
| 分类 | `/categories` 同上 | 登录 / ADMIN+LIBRARIAN | 同上 |
| 读者 | `GET /readers`、`GET /readers/{id}`、`POST /readers`、`PUT /readers/{id}`、`PATCH /readers/{id}/status`、`DELETE /readers/{id}` | ADMIN/LIBRARIAN | 借阅证号唯一；可绑定登录账号；删除前检查借阅记录 |
| 读者 | `GET /readers/me` | 登录 | 读者本人档案（含当前在借数量） |
| 配置 | `GET /configs/borrow-rules` | 登录 | 借期/续借/罚款规则（前端据此展示） |

示例：新增图书（ISBN 允许带连字符，服务端会规范化并校验 10/13 位）：

```bash
curl -s -X POST http://localhost:8080/api/v1/books \
  -H "Authorization: Bearer <accessToken>" -H 'Content-Type: application/json' \
  -d '{"isbn":"978-7-5086-8353-0","title":"示例图书","authorIds":[2],"publisherId":3,"categoryId":2,"totalStock":3}'
```

图书列表（关键字会同时匹配书名、副标题、ISBN 与作者名）：

```bash
curl -s "http://localhost:8080/api/v1/books?keyword=%E4%B8%89%E4%BD%93&page=1&size=10&sort=id,desc" \
  -H "Authorization: Bearer <accessToken>"
```

## 测试

```bash
cd backend
mvn test          # 单元测试（42 例：JWT、登录/刷新、用户管理、图书、读者、作者规则）
```

集成测试基于 Testcontainers 启动真实 MySQL，需要 Docker：

```bash
mvn -Pit verify   # 集成测试：登录、/auth/me、匿名 401、读者越权 403、刷新令牌轮换
```

## 已实现与后续规划

- [x] 阶段 2：工程骨架、Flyway 建表与种子数据、JWT 登录/刷新/登出、RBAC、用户与角色管理、统一异常与响应
- [x] 阶段 3：作者/出版社/分类/图书/读者 CRUD、分页搜索与排序、库存一致性校验、引用检查删除、图书主数据种子（V3）
- [ ] 阶段 4：借书/还书/续借、逾期与罚款、事务与并发控制、核心规则单测
- [ ] 阶段 5：前端页面、权限路由、图书/读者/借阅界面
- [ ] 阶段 6：统计报表、种子数据扩充
- [ ] 阶段 7：Docker Compose、完整 README、验收清单

## 常见问题

1. **启动报 `Unknown database 'library'`**：先执行上文的建库 SQL（Flyway 只建表，不建库）。
2. **`Access denied for user 'library'`**：确认账号、密码与 `DB_USERNAME`/`DB_PASSWORD` 一致，且已授权 `library.*`。
3. **Flyway 校验失败（checksum mismatch）**：开发期可 `flyway clean` 或删除库重建；已上线的迁移文件禁止修改，只能新增版本。
4. **401 与 403 的区别**：401 表示未登录或令牌无效/过期；403 表示已登录但角色权限不足。
5. **跨域失败**：确认前端地址在 `CORS_ALLOWED_ORIGINS` 中（逗号分隔）。
