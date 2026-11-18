# 验收清单

按顺序执行即可完成验收；每条都标注了验证方式与预期结果。命令均在项目根目录执行。

## 0. 前置条件

- [ ] JDK 21（`java -version` 输出 21.x）
- [ ] Maven 3.9+（`mvn -v`）
- [ ] Node.js 20+ 与 pnpm（`node -v`、`pnpm -v`）
- [ ] MySQL 8.0 可用（本机或 Docker）；或 Docker Desktop 已启动用于 `docker compose`

## 1. 后端单元测试

```bash
cd backend
mvn test
```

- [ ] 输出 `Tests run: 68, Failures: 0, Errors: 0`
- [ ] 覆盖：JWT 生成/解析/过期/篡改、登录与刷新轮换、读者自助注册（证号生成/重复用户名/密码加密）、用户与角色、图书（ISBN 规范化/库存重算/删除校验/作者按名称复用与自动建档/出版社自动创建）、读者、作者、借还续与逾期罚款、统计聚合

## 2. 前端组件测试与构建

```bash
cd frontend
pnpm test
pnpm build
```

- [ ] `Test Files 5 passed`，`Tests 18 passed`
- [ ] `tsc --noEmit` 无类型错误，`vite build` 产出 `dist/`（含 react/antd 独立 chunk）

## 3. 本地联调启动

```bash
# 终端 1：先建库（仅首次）
mysql -h 127.0.0.1 -P 3306 -u root -p -e "CREATE DATABASE library CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci; CREATE USER 'library'@'%' IDENTIFIED BY 'library123'; GRANT ALL ON library.* TO 'library'@'%';"

# 终端 2：后端
cd backend && mvn spring-boot:run

# 终端 3：前端
cd frontend && pnpm dev
```

- [ ] `GET http://localhost:8080/actuator/health` 返回 `{"status":"UP"}`
- [ ] `http://localhost:8080/swagger-ui.html` 可打开并列出全部分组
- [ ] Flyway 日志显示 V1/V2 全部 `Success`，`flyway_schema_history` 2 条记录
- [ ] `http://localhost:5173` 打开登录页，`/api` 代理到后端（登录成功）
- [ ] 首次启动后数据库只有 1 个账号（admin），图书/作者/出版社/分类/读者/借阅全为空

## 4. 账号与权限

初始只有管理员账号，馆员与读者需要手工创建：

| 账号 | 密码 | 创建方式 | 预期 |
| --- | --- | --- | --- |
| `admin` | `Admin@123456` | 迁移脚本内置 | 可见"用户与角色"菜单，可增删改用户 |
| `librarian` | `Lib@123456` | 管理员在"用户与角色"新增，角色选 `LIBRARIAN` | 可见借阅/读者/基础数据/统计，看不到用户管理 |
| `reader` | `Reader@123456` | 管理员新增读者并绑定该账号（或读者自助注册） | 只有"图书查询"和"我的借阅"，访问 `/users` 返回 403 |

- [ ] 登录成功返回 `accessToken` / `refreshToken`，`expiresIn=1800`
- [ ] 登录页「立即注册」可自助注册读者：提交后自动登录并跳转「我的借阅」
- [ ] 自助注册的读者自动获得借阅证（证号形如 `R000005`），`GET /readers/me` 有数据
- [ ] 重复用户名注册返回 `409/40901`，非法用户名/密码返回 `422/42200`
- [ ] 直接访问 `http://localhost:5173/users`（读者账号）显示 403 页面
- [ ] 刷新令牌后旧 refresh token 再用返回 `40102`
- [ ] 停用某账号后该账号无法登录

## 5. 图书与主数据

先手工录入基础数据：至少 1 个分类，再新增 2 本图书（含库存）；作者与出版社可在新增图书时直接输入。

- [ ] 初始图书列表为空（`total=0`），新增后分页正常
- [ ] 图书表单中作者为可输入的下拉框（回车添加多个姓名）、出版社为可输入文本框，不是只能选的固定下拉
- [ ] 输入不存在的作者名/出版社名保存后，作者与出版社列表自动新增对应记录
- [ ] 再次输入同名作者/出版社不会重复建档（复用同一条记录）
- [ ] 关键字搜索作者名能命中对应图书（作者名子查询生效）
- [ ] 新增图书时 ISBN 带连字符可保存，入库为 13 位纯数字
- [ ] 重复 ISBN 返回 `409/40901`，非法 ISBN 返回 `422/42200`，作者为空返回 `422/42200`
- [ ] 总库存不能小于已借出数（返回 `422`）
- [ ] 被图书引用的作者/出版社/分类无法删除（返回 `409`）
- [ ] 已产生借阅记录的图书无法删除（提示改为下架）

## 6. 读者

- [ ] 初始读者列表为空，新增读者后列表出现该记录
- [ ] 借阅证号重复返回 `409/40901`
- [ ] 一个登录账号不能被绑定到两个读者档案
- [ ] 读者账号 `GET /readers/me` 返回本人 `cardNo`、`maxBorrow`、`borrowingCount`

## 7. 借书 / 还书 / 续借

- [ ] 借书成功后：图书 `availableStock -1`，借阅记录 `status=BORROWED`，`dueDate = 今天 + 30 天`
- [ ] 同一读者重复借同一本书 → `422/42206`
- [ ] 读者在借数达到上限 → `422/42202`
- [ ] 停用读者借书 → `422/42203`；下架图书借书 → `422/42206`
- [ ] 库存为 0 时借书 → `422/42201`
- [ ] 续借成功：`dueDate +15 天`、`renewTimes=1`；第 3 次续借 → `422/42204`
- [ ] 逾期记录续借 → `422/42205`（默认不允许）
- [ ] 还书成功后：`status=RETURNED`、`returnDate=今天`、`availableStock +1`
- [ ] 逾期 10 天还书：`fineAmount=5.00`、`overdueDays=10`
- [ ] 重复还书 → `409/40900`

## 8. 统计

- [ ] `GET /stats/overview` 返回图书种类/册数/可借/借出/逾期/读者等 8 项指标，且与数据库一致
- [ ] `GET /stats/popular-books?limit=5` 按借阅次数倒序，次数最多的图书排在首位
- [ ] `GET /stats/category-distribution` 覆盖全部分类（含 0 本的分类）
- [ ] `limit` 超出 1~50 返回 `422/42200`
- [ ] 前端"统计概览"页 4 张卡片、排行榜、分类分布渲染正常

## 9. 前端页面

- [ ] 登录页表单校验：空提交提示"请输入用户名/密码"
- [ ] 图书页：搜索、分类筛选、状态筛选、排序、分页、封面缩略图、库存列
- [ ] 图书新增/编辑弹窗：作者与出版社为自由输入（可新建）、分类下拉、库存、上下架
- [ ] 借阅管理页：借书弹窗（读者/图书远程搜索）、还书、续借、逾期红色标记与罚款列
- [ ] 我的借阅页：借阅证信息 + 记录表格；未绑定档案时显示提示
- [ ] 基础数据三个页面（作者/出版社/分类）复用同一套通用表格 + 动态表单
- [ ] 用户管理页：新增/编辑（密码留空不改）/启停用/删除，管理员账号不可删除自己
- [ ] 任一接口报错时页面右上角出现后端返回的中文错误文案

## 10. Docker Compose 一键启动

```bash
cp .env.example .env       # 可选：修改端口与密码
docker compose up -d --build
docker compose ps
```

- [ ] 三个容器 `library-mysql` / `library-backend` / `library-frontend` 均为 healthy/running
- [ ] `docker compose logs backend | grep Flyway` 显示 V1/V2 执行成功
- [ ] `http://localhost` 可登录，`http://localhost:8080/swagger-ui.html` 可访问
- [ ] 重启容器后数据仍在（`mysql-data` 卷持久化）
- [ ] `docker compose down` 停止；`docker compose down -v` 清空数据

> **本机验证状态**：`docker compose config --quiet` 已通过（services = mysql/backend/frontend），但 `up --build` 未能在当前机器上执行 —— Docker Desktop 的 Linux 引擎不可用（`docker info` 返回 `500 Internal Server Error ... /pipe/dockerDesktopLinuxEngine`，`wsl -l -v` 提示未安装 WSL 发行版）。这不是项目配置问题，排查顺序：① 在 BIOS 开启虚拟化（VT-x/AMD-V）；② `wsl --install` 并重启；③ Docker Desktop → Settings → General 勾选 WSL 2 based engine；④ `docker info` 能返回版本号后再执行上面的命令。

## 11. 文档完整性

- [ ] `README.md`：环境要求、启动步骤、默认账号、接口示例、常见问题
- [ ] `docs/er-diagram.md`：Mermaid ER 图、约束与索引、借阅规则配置
- [ ] `docs/api.md`：错误码、权限矩阵、curl 示例
- [ ] 本清单可被新开发者独立执行通过
