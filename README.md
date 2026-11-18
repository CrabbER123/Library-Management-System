# 图书管理系统（Library Management System）

一个可直接运行、分层清晰、带测试与文档的图书管理系统，覆盖**账号与 RBAC 权限、图书与主数据、读者、借还续、逾期罚款、统计报表**，并提供 React 管理端与 Docker Compose 一键部署。

- 当前进度：**阶段 0–7 全部完成**（MVP 交付）
- 后端 65 个单元测试、前端 16 个组件测试全部通过
- 数据库结构由 Flyway 管理（V1 建表 → V2 角色字典与管理员账号；业务数据不预置）

## 目录

- [功能概览](#功能概览)
- [技术栈与选型理由](#技术栈与选型理由)
- [系统架构](#系统架构)
- [目录结构](#目录结构)
- [快速开始](#快速开始)
  - [方式一：Docker Compose（推荐）](#方式一docker-compose推荐)
  - [方式二：本地开发](#方式二本地开发)
- [默认账号](#默认账号)
- [配置项](#配置项)
- [核心业务规则](#核心业务规则)
- [接口一览](#接口一览)
- [测试](#测试)
- [文档](#文档)
- [常见问题](#常见问题)
- [后续迭代建议](#后续迭代建议)

## 功能概览

| 模块 | 能力 |
| --- | --- |
| 认证与权限 | 用户名密码登录、JWT（access 30 分钟 / refresh 7 天轮换）、登出、当前用户；ADMIN / LIBRARIAN / READER 三类角色，接口级 + 前端路由级双重控制 |
| 图书管理 | 图书 CRUD、ISBN 规范化与校验、作者多对多、出版社、分类、封面、简介、语言、价格、上下架、总库存调整 |
| 基础主数据 | 作者、出版社、分类的增删改查、分页搜索、被引用时禁止删除 |
| 读者管理 | 读者档案 CRUD、借阅证号唯一、状态（正常/停用）、最大借阅数、绑定登录账号、`/readers/me` 本人档案 |
| 借阅管理 | 借书、还书、续借、借阅记录分页查询、只看逾期、本人借阅记录 |
| 逾期与罚款 | 借期 30 天、最多续借 2 次（每次 +15 天）、逾期 0.5 元/天、还书时结算并落库 |
| 查询与分页 | 关键字（书名/副标题/ISBN/作者名）、分类、作者、出版社、状态过滤，排序白名单，统一分页结构 |
| 统计 | 馆藏总览（8 项指标）、借阅排行 TOP N、分类藏书分布 |
| 前端 | 登录、图书查询、我的借阅、借阅管理、读者管理、基础数据、用户与角色、统计概览，403/404 页面与全局错误提示 |

## 技术栈与选型理由

| 层 | 选型 | 版本 |
| --- | --- | --- |
| 语言/运行时 | Java | 21（LTS） |
| 后端框架 | Spring Boot | 3.3.5 |
| 安全 | Spring Security 6 + jjwt | 0.12.6 |
| 持久层 | MyBatis-Plus | 3.5.7 |
| 数据库 | MySQL | 8.0（utf8mb4） |
| 迁移 | Flyway | Boot 管理版本 |
| API 文档 | SpringDoc OpenAPI | 2.6.0 |
| 后端测试 | JUnit 5 + Mockito + Testcontainers | Boot 管理版本 |
| 前端 | React 18 + TypeScript 5.6 + Vite 5.4 + Ant Design 5 | — |
| 前端测试 | Vitest + Testing Library + jsdom | — |
| 部署 | Docker Compose + Nginx | mysql 8.0 / nginx 1.27 |

**为什么这样选：**

- **Spring Boot + MyBatis-Plus**：管理后台的列表查询大量需要动态条件、联表与聚合统计，MyBatis-Plus 的 `QueryWrapper` + 手写 SQL 比 JPA 更可控，也便于看清 SQL 与索引使用情况。
- **Flyway**：数据库结构、种子数据都进版本库，新环境一条命令即可得到一致的库；迁移文件只增不改，避免环境漂移。
- **JWT（access + opaque refresh）**：access token 无状态、短期有效；refresh token 落库存 SHA-256，可撤销、可轮换，兼顾分布式部署与"强制下线"。
- **React + Ant Design**：中后台表单/表格组件齐全，交付快；Vite 冷启动与构建都快；TypeScript 保证前后端 DTO 对齐。
- **Docker Compose**：三个容器（MySQL、后端、前端 Nginx）即可跑起完整系统，Nginx 同时承担静态资源与 `/api` 反向代理，避免额外的 CORS 配置。

## 系统架构

```text
┌──────────────────────────────┐        ┌──────────────────────────────┐
│ 浏览器（React + AntD SPA）    │        │  Swagger UI / OpenAPI        │
└──────────────┬───────────────┘        └──────────────┬───────────────┘
               │ HTTPS/HTTP (JSON)                     │
               ▼                                       ▼
┌──────────────────────────────────────────────────────────────────────┐
│ Nginx（生产：静态资源 + /api 反向代理；开发：Vite dev server 代理）    │
└──────────────────────────────┬───────────────────────────────────────┘
                               ▼
┌──────────────────────────────────────────────────────────────────────┐
│ Spring Boot 应用（模块化单体）                                        │
│  Controller（参数校验/鉴权/DTO↔VO）                                   │
│  Service（业务规则、@Transactional）                                  │
│  Mapper（MyBatis-Plus + 少量 XML/注解 SQL）                           │
│  Security（JWT 过滤器、方法级 @PreAuthorize、统一 401/403 JSON）        │
└──────────────────────────────┬───────────────────────────────────────┘
                               ▼
┌──────────────────────────────────────────────────────────────────────┐
│ MySQL 8.0（Flyway 管理结构；utf8mb4；借阅记录生成列唯一索引防重复在借）  │
└──────────────────────────────────────────────────────────────────────┘
```

分层约定：

- Controller 只做参数接收、校验、权限注解与 DTO↔VO 转换，**不写业务规则**；
- Service 承担事务边界与业务规则，跨模块调用也走 Service；
- Mapper 只做数据访问，复杂查询用 `@Select` 显式 SQL，列表装配统一走批量查询避免 N+1；
- 全站统一 `ApiResponse{code,message,data,timestamp}` 与 `PageResult{records,total,page,size,pages}`。

## 目录结构

```text
图书管理系统/
├── backend/
│   ├── Dockerfile                    # 多阶段构建（Maven → JRE 21）
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/library/
│       │   ├── LibraryApplication.java
│       │   ├── common/               # 统一响应、异常与错误码、分页、排序、常量
│       │   ├── config/               # MyBatis-Plus、Jackson、OpenAPI、借阅规则绑定
│       │   ├── security/             # JWT 签发/解析、过滤器、SecurityConfig、当前用户
│       │   └── modules/
│       │       ├── auth/             # 登录、刷新、登出、当前用户
│       │       ├── user/             # 用户、角色、用户-角色
│       │       ├── reader/           # 读者档案与借阅证
│       │       ├── author/           # 作者
│       │       ├── publisher/        # 出版社
│       │       ├── category/         # 分类
│       │       ├── book/             # 图书与图书-作者关联
│       │       ├── borrow/           # 借书、还书、续借、逾期与罚款
│       │       ├── stats/            # 馆藏统计、借阅排行、分类分布
│       │       └── config/           # 只读配置（借阅规则）
│       ├── main/resources/
│       │   ├── application.yml / -dev.yml / -prod.yml / -docker.yml
│       │   └── db/migration/         # V1 建表、V2 角色字典与管理员账号
│       └── test/java/com/library/    # 单元测试（Mockito）+ 集成测试（Testcontainers）
├── frontend/
│   ├── Dockerfile                    # 多阶段构建（Node → Nginx）
│   ├── nginx.conf                    # SPA fallback + /api 代理 + 缓存策略
│   ├── package.json / pnpm-lock.yaml / pnpm-workspace.yaml
│   ├── vite.config.ts / vitest.config.ts / tsconfig.json
│   └── src/
│       ├── api/                      # axios 实例、Token 刷新、DTO 类型、接口封装
│       ├── auth/                     # 登录态上下文
│       ├── components/               # 布局、路由守卫
│       ├── pages/                    # login / book / catalog / reader / borrow / user / stats
│       ├── test/                     # 测试初始化
│       ├── App.tsx / main.tsx
├── docs/
│   ├── er-diagram.md                 # ER 图、约束索引、借阅规则说明
│   ├── api.md                        # 错误码、权限矩阵、curl 示例
│   └── acceptance-checklist.md       # 验收清单（11 组）
├── docker-compose.yml
├── .env.example
└── README.md
```

## 快速开始

### 方式一：Docker Compose（推荐）

前置条件：Docker Desktop 已启动（Windows/macOS）或 docker + compose 插件可用（Linux）。

```bash
# 可选：复制并修改环境变量（端口、密码、JWT 密钥）
cp .env.example .env      # Windows PowerShell: Copy-Item .env.example .env

docker compose up -d --build
docker compose ps
```

启动后：

| 入口 | 地址 |
| --- | --- |
| 前端 | <http://localhost> |
| 后端 API | <http://localhost:8080/api/v1> |
| Swagger UI | <http://localhost:8080/swagger-ui.html> |
| 健康检查 | <http://localhost:8080/actuator/health> |

常用命令：

```bash
docker compose logs -f backend     # 查看后端日志（Flyway 迁移记录也在这里）
docker compose down                # 停止（保留数据）
docker compose down -v             # 停止并清空数据库数据
```

> MySQL 端口默认不映射到宿主机（避免与本机已安装的 MySQL 冲突，后端通过 compose 内网访问即可）；如需用客户端连库，取消 `docker-compose.yml` 中 `ports` 注释，或用 `docker compose exec mysql mysql -uroot -p` 进入容器操作。

### 方式二：本地开发

前置条件：JDK 21、Maven 3.9+、Node.js 20+、pnpm 9+、MySQL 8.0。

1）创建数据库与账号（仅首次）：

```sql
CREATE DATABASE library CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER 'library'@'%' IDENTIFIED BY 'library123';
GRANT ALL PRIVILEGES ON library.* TO 'library'@'%';
FLUSH PRIVILEGES;
```

2）启动后端（Flyway 自动建表并写入管理员账号）：

```bash
cd backend
mvn spring-boot:run
```

3）启动前端：

```bash
cd frontend
pnpm install
pnpm dev
```

打开 <http://localhost:5173>，Vite 已把 `/api` 代理到 `http://127.0.0.1:8080`。

## 默认账号

| 账号 | 密码 | 角色 |
| --- | --- | --- |
| `admin` | `Admin@123456` | 管理员 |

初始数据只包含这个管理员账号和三个角色字典（ADMIN / LIBRARIAN / READER）。**图书、作者、出版社、分类、读者与借阅记录全部为空**，需要由管理员在系统中手工录入：

- 馆员账号：管理员登录 → 用户与角色 → 新增用户时选择 `LIBRARIAN`；
- 读者账号：读者可在登录页点「立即注册」自助注册（自动创建 `READER` 账号 + 借阅证，并直接登录），也可由管理员登录 → 读者管理 → 新增读者并绑定账号；
- 图书：先建作者/出版社/分类，再在图书页新增图书并录入库存。

自助注册规则：

- 用户名 3–32 位字母/数字/下划线，密码 6–64 位，姓名必填，手机号与邮箱选填；
- 注册成功后自动开通借阅证，证号形如 `R000005`，初始借阅上限取 `library.borrow.max-borrow-per-reader`（默认 5）；
- 馆员与管理员账号**不开放**注册，只能由管理员在「用户与角色」中创建。

> **上线前务必**修改默认密码、`JWT_SECRET` 与数据库密码。

## 配置项

### 环境变量

| 变量 | 默认值 | 说明 |
| --- | --- | --- |
| `DB_HOST` / `DB_PORT` | `localhost` / `3306` | 数据库地址（compose 中为 `mysql`/`3306`） |
| `DB_NAME` | `library` | 数据库名 |
| `DB_USERNAME` / `DB_PASSWORD` | `library` / `library123` | 数据库账号 |
| `JWT_SECRET` | 内置开发密钥 | 生产必须覆盖，长度 ≥ 32 字节 |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173,http://127.0.0.1:5173` | 允许的前端来源（逗号分隔） |
| `MYSQL_ROOT_PASSWORD` | `root123456` | 仅 compose 使用 |
| `MYSQL_PORT` / `BACKEND_PORT` / `FRONTEND_PORT` | `3306` / `8080` / `80` | 仅 compose 使用 |

### 借阅规则（`application.yml` → `library.borrow`）

| 配置 | 默认 | 含义 |
| --- | --- | --- |
| `loan-days` | 30 | 借期天数 |
| `max-renew-times` | 2 | 最多续借次数 |
| `renew-days` | 15 | 每次续借延长天数 |
| `max-borrow-per-reader` | 5 | 默认最大在借数（读者档案可覆盖） |
| `fine-per-day` | 0.50 | 每日罚款（元） |
| `allow-renew-when-overdue` | false | 逾期是否允许续借 |
| `max-fine-amount` | 0 | 罚款上限，0 = 不封顶 |

前端在借阅页通过 `GET /api/v1/configs/borrow-rules` 读取同一份规则，避免"文案与规则不一致"。

## 核心业务规则

1. **借书**：读者存在 → 状态正常 → 图书存在且 `AVAILABLE` → 在借数未超限 → 未重复在借 → 条件更新扣减库存（`available_stock > 0`）→ 插入借阅记录。任一步失败整体回滚。
2. **还书**：仅 `BORROWED` 可还；按 `return_date - due_date` 计算逾期天数并结算罚款（`天数 × fine-per-day`，可按 `max-fine-amount` 封顶）；库存回补（不超过总库存）。
3. **续借**：仅 `BORROWED` 可续；逾期（默认）不可续；续借次数 < 2；`due_date += 15`，`renew_times += 1`。
4. **重复在借**：`borrow_records` 的生成列 `active_flag`（BORROWED=1，否则 NULL）+ `UNIQUE(reader_id, book_id, active_flag)`，应用层提示 + 数据库兜底。
5. **库存调整**：`total_stock` 不得低于"已借出数量"（`total - available`），调整后按此重算 `available_stock`。
6. **删除保护**：有借阅记录的图书/读者不可删除（提示改为下架/停用）；被图书引用的作者/出版社/分类不可删除。
7. **权限**：写操作仅 ADMIN/LIBRARIAN；读者只能读图书、自己的借阅与档案；用户与角色管理仅 ADMIN；不能删除/停用自己，不能移除最后一个管理员。

更多细节见 [docs/er-diagram.md](docs/er-diagram.md)。

## 接口一览

统一前缀 `/api/v1`，除登录/刷新外均需 `Authorization: Bearer <accessToken>`；失败统一返回业务错误码（见 [docs/api.md](docs/api.md)）。

| 模块 | 方法与路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| 认证 | `POST /auth/login`、`POST /auth/refresh` | 匿名 | 登录、刷新（旧 refresh token 立即失效） |
| 认证 | `POST /auth/register` | 匿名 | 读者自助注册：创建 READER 账号 + 借阅证，直接返回令牌（馆员仍由管理员创建） |
| 认证 | `POST /auth/logout`、`GET /auth/me` | 登录 | 注销、当前用户（含 `readerId`） |
| 用户 | `GET/POST /users`、`GET/PUT/DELETE /users/{id}`、`PATCH /users/{id}/status`、`PUT /users/{id}/roles` | ADMIN | 用户与角色管理 |
| 角色 | `GET /roles` | ADMIN | 角色字典 |
| 图书 | `GET /books`、`GET /books/{id}` | 登录 | 分页搜索：`keyword`、`categoryId`、`authorId`、`publisherId`、`status`、`sort` |
| 图书 | `POST/PUT/DELETE /books`、`PATCH /books/{id}/status`、`PUT /books/{id}/stock` | ADMIN/LIBRARIAN | 新增/编辑/删除、上下架、调整库存 |
| 作者 | `/authors` 全套 | 登录 / ADMIN+LIBRARIAN | 查询 / 维护 |
| 出版社 | `/publishers` 全套 | 登录 / ADMIN+LIBRARIAN | 查询 / 维护 |
| 分类 | `/categories` 全套 | 登录 / ADMIN+LIBRARIAN | 查询 / 维护 |
| 读者 | `GET /readers`、`GET/POST/PUT/DELETE /readers/{id}`、`PATCH /readers/{id}/status` | ADMIN/LIBRARIAN | 读者档案管理 |
| 读者 | `GET /readers/me` | 登录 | 本人档案（含在借数量） |
| 借阅 | `POST /borrows`、`POST /borrows/{id}/return`、`POST /borrows/{id}/renew` | ADMIN/LIBRARIAN | 借书、还书、续借 |
| 借阅 | `GET /borrows`、`GET /borrows/{id}` | ADMIN/LIBRARIAN | 分页：`keyword`、`status`、`readerId`、`bookId`、`overdueOnly`、`sort` |
| 借阅 | `GET /borrows/my` | 登录 | 本人借阅记录 |
| 统计 | `GET /stats/overview`、`GET /stats/popular-books?limit=10`、`GET /stats/category-distribution` | ADMIN/LIBRARIAN | 总览、排行、分类分布 |
| 配置 | `GET /configs/borrow-rules` | 登录 | 借阅规则 |

### 示例

```bash
# 登录
curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"Admin@123456"}'

# 图书检索（关键字同时匹配书名、副标题、ISBN、作者名）
curl -s "http://localhost:8080/api/v1/books?keyword=%E4%B8%89%E4%BD%93&page=1&size=10&sort=title,asc" \
  -H "Authorization: Bearer <accessToken>"

# 借书 / 还书 / 续借
curl -s -X POST http://localhost:8080/api/v1/borrows \
  -H "Authorization: Bearer <accessToken>" -H 'Content-Type: application/json' \
  -d '{"readerId":2,"bookId":5,"remark":"柜台办理"}'
curl -s -X POST http://localhost:8080/api/v1/borrows/12/return -H "Authorization: Bearer <accessToken>"
curl -s -X POST http://localhost:8080/api/v1/borrows/12/renew  -H "Authorization: Bearer <accessToken>"

# 统计
curl -s http://localhost:8080/api/v1/stats/overview -H "Authorization: Bearer <accessToken>"
```

## 测试

后端（65 例单元测试，纯 Mockito，不需要数据库）：

```bash
cd backend
mvn test
```

覆盖：JWT 生成/解析/过期/篡改、登录与刷新轮换、用户与角色（重名、最后一个管理员、自删保护）、图书（ISBN 规范化、库存重算、已借出下限、删除校验）、读者（卡号唯一、账号绑定、默认借阅上限）、作者、借还续与逾期罚款、统计聚合。

集成测试（Testcontainers 启动真实 MySQL，需要 Docker）：

```bash
cd backend
mvn -Pit verify
```

前端：

```bash
cd frontend
pnpm test        # Vitest + Testing Library，16 例
pnpm build       # tsc --noEmit + vite build
```

## 文档

- [数据库设计与 ER 图](docs/er-diagram.md)
- [API 说明与错误码](docs/api.md)
- [验收清单](docs/acceptance-checklist.md)
- 在线 Swagger：`/swagger-ui.html`

## 常见问题

1. **启动报 `Unknown database 'library'`**：Flyway 只建表不建库，先执行建库 SQL（或直接用 compose）。
2. **`Access denied for user 'library'`**：核对 `DB_USERNAME`/`DB_PASSWORD`，并确认已 `GRANT ALL ON library.*`。
3. **Flyway checksum mismatch**：迁移文件一旦发布不可修改；开发期可删库重建，已上线环境请新增 `V5__xxx.sql`。
4. **401 与 403 的区别**：401 = 未登录 / 令牌无效或过期；403 = 已登录但角色不足（如读者访问 `/users`）。
5. **refresh token 用不了（40102）**：令牌是一次性轮换的，旧 token 在刷新后立即失效；前端已做单飞刷新与重放。
6. **compose 报 `project name must not be empty`**：本项目中文目录名无法推导项目名，`docker-compose.yml` 顶部已显式写 `name: library`，请勿删除。
7. **`docker info` 报 `500 Internal Server Error (...dockerDesktopLinuxEngine)`**：Docker Desktop 的 Linux 引擎没起来，通常是 BIOS 未开虚拟化或 WSL 2 未安装。处理：BIOS 开启 VT-x/AMD-V → `wsl --install` 重启 → Docker Desktop 勾选 "Use WSL 2 based engine"。
8. **前端跨域失败**：开发环境确认 Vite 代理存在；生产环境确认 Nginx `/api/` 代理与后端 `CORS_ALLOWED_ORIGINS`。
9. **中文乱码**：库/表/连接均需 `utf8mb4`，JDBC URL 已带 `characterEncoding=utf8`。

## 后续迭代建议

按优先级排列，均已在 MVP 中预留数据模型或接口位置：

1. **罚款缴纳记录**：`fines` 表记录缴纳流水与减免原因，读者可查历史罚款。
2. **预约/排队借书**：`reservations` 表 + 到书通知，库存归还时自动分配。
3. **图书副本条码管理**：由"库存数量"升级为 `book_copies`（每册一码），支持精确到册的借还。
4. **操作审计日志**：借还、库存调整、用户变更落审计表，支持按操作人回溯。
5. **导入导出**：图书/读者 CSV 导入（带行级校验报告）与借阅记录导出。
6. **消息通知**：到期前 3 天与逾期提醒（站内信 + 邮件）。
7. **报表增强**：按月借阅趋势、读者活跃度、滞纳金汇总，前端图表化。
8. **多分馆**：`branches` 表 + 库存按分馆维度拆分，借阅记录增加分馆字段。

## 开发约定

- 代码标识符英文，注释与文档中文；提交信息遵循 Conventional Commits（`feat(scope): ...`）。
- 新增迁移只增不改；DTO/VO 用 `record`，Entity 用 Lombok。
- 业务规则必须有单元测试；跨层约定变更需同步更新 `docs/api.md` 与前端类型定义。
