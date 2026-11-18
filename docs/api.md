# API 说明

- 在线文档（Swagger UI）：`http://localhost:8080/swagger-ui.html`
- OpenAPI JSON：`http://localhost:8080/v3/api-docs`
- 统一前缀：`/api/v1`，除登录/刷新外都需要请求头 `Authorization: Bearer <accessToken>`

Swagger 是接口契约的"唯一事实来源"，本文件只做导航与示例补充。

## 统一响应结构

```json
{
  "code": 0,
  "message": "ok",
  "data": {},
  "timestamp": 1794979572236
}
```

- `code = 0` 表示成功，其余为业务错误码（与 HTTP 状态码配合使用）。
- 分页数据统一为 `{ "records": [...], "total": 11, "page": 1, "size": 10, "pages": 2 }`。
- 参数校验失败时 `data` 为字段错误数组：`[{ "field": "isbn", "message": "ISBN 格式不正确" }]`。
- 分页列表统一支持 `sort=字段,asc|desc`（服务端白名单，非法字段会被忽略）；各模块默认排序：图书 `id` 倒序、分类 `code` 升序、作者/出版社 `name` 升序、借阅记录 `id` 倒序。
- `GET /roles` 返回角色字典对象数组 `[{ "id": 1, "code": "ADMIN", "name": "管理员", "description": "..." }]`，前端用 `code` 作为提交值、`name` 作为展示文案。

## 错误码

| 错误码 | HTTP | 含义 |
| --- | --- | --- |
| `0` | 200/201 | 成功 |
| `40001` | 400 | 请求体不可解析、缺少参数或类型不匹配 |
| `40100` | 401 | 未登录、令牌无效或账号密码错误 |
| `40101` | 401 | access token 已过期 |
| `40102` | 401 | refresh token 无效、已撤销或重复使用 |
| `40300` | 403 | 已登录但角色权限不足 |
| `40301` | 403 | 账号已被停用 |
| `40400` | 404 | 资源不存在 |
| `40900` | 409 | 数据冲突（如外键引用） |
| `40901` | 409 | 唯一约束重复（用户名、ISBN、卡号等） |
| `40902` | 409 | 不允许移除/删除最后一个管理员 |
| `40903` | 409 | 不允许对当前登录账号执行该操作 |
| `42200` | 422 | 参数校验失败（字段级错误） |
| `42201` | 422 | 库存不足 |
| `42202` | 422 | 超出读者最大借阅数 |
| `42203` | 422 | 读者已被停用（借阅、归还、续借均被拒绝） |
| `42204` | 422 | 续借次数已达上限 |
| `42205` | 422 | 逾期图书不允许续借 |
| `42206` | 422 | 图书不可借（下架或已被该读者借走未还） |
| `50000` | 500 | 服务器内部错误 |

## 模块与权限

| 模块 | 路径 | 匿名 | 读者 | 馆员 | 管理员 |
| --- | --- | :-: | :-: | :-: | :-: |
| 认证 | `/auth/login`、`/auth/refresh`、`/auth/register` | ✅ | | | |
| 认证 | `/auth/logout`、`/auth/me` | | ✅ | ✅ | ✅ |
| 图书 | `GET /books`、`GET /books/{id}` | | ✅ | ✅ | ✅ |
| 图书 | `POST/PUT/DELETE /books`、`PATCH /books/{id}/status`、`PUT /books/{id}/stock` | | | ✅ | ✅ |
| 作者/出版社/分类 | `GET` 查询 | | ✅ | ✅ | ✅ |
| 作者/出版社/分类 | 新增/编辑/删除 | | | ✅ | ✅ |
| 读者 | `GET /readers/me` | | ✅ | ✅ | ✅ |
| 读者 | 列表/详情/增删改/状态 | | | ✅ | ✅ |
| 借阅 | `POST /borrows`、`/borrows/{id}/return`、`/borrows/{id}/renew`、`GET /borrows` | | | ✅ | ✅ |
| 借阅 | `GET /borrows/my` | | ✅ | ✅ | ✅ |
| 统计 | `GET /stats/overview`、`/stats/popular-books`、`/stats/category-distribution` | | | ✅ | ✅ |
| 用户/角色 | `/users/**`、`GET /roles` | | | | ✅ |
| 配置 | `GET /configs/borrow-rules` | | ✅ | ✅ | ✅ |

## 常用示例

登录并提取令牌（bash + jq）：

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"Admin@123456"}' | jq -r '.data.accessToken')
```

读者自助注册（成功返回 201 与令牌，可直接进入系统）：

```bash
curl -s -X POST http://localhost:8080/api/v1/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"username":"newbie","password":"Reader@123456","displayName":"王小明","phone":"13800000000"}'
```

新增图书（`authorNames` / `publisherName` 为自由文本，不存在时自动创建；也可改用 `authorIds` / `publisherId`）：

```bash
curl -s -X POST http://localhost:8080/api/v1/books \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"isbn":"978-7-5366-9293-0","title":"三体","authorNames":["刘慈欣"],"publisherName":"重庆出版社","categoryId":1,"totalStock":5,"price":23.00}'
```

说明：`authorNames` 最多 20 个、单个不超过 100 字符；`publisherName` 不超过 100 字符；至少提供一位作者，否则返回 `422/42200`。

图书检索（关键字同时匹配书名、副标题、ISBN、作者名，支持分页与排序白名单）：

```bash
curl -s "http://localhost:8080/api/v1/books?keyword=java&categoryId=1&status=AVAILABLE&page=1&size=10&sort=title,asc" \
  -H "Authorization: Bearer $TOKEN"
```

借书 / 还书 / 续借：

```bash
curl -s -X POST http://localhost:8080/api/v1/borrows \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"readerId":2,"bookId":5,"remark":"柜台办理"}'

curl -s -X POST http://localhost:8080/api/v1/borrows/12/return -H "Authorization: Bearer $TOKEN"
curl -s -X POST http://localhost:8080/api/v1/borrows/12/renew  -H "Authorization: Bearer $TOKEN"
```

只看逾期在借：

```bash
curl -s "http://localhost:8080/api/v1/borrows?overdueOnly=true&page=1&size=20" \
  -H "Authorization: Bearer $TOKEN"
```

统计：

```bash
curl -s http://localhost:8080/api/v1/stats/overview -H "Authorization: Bearer $TOKEN"
curl -s "http://localhost:8080/api/v1/stats/popular-books?limit=5" -H "Authorization: Bearer $TOKEN"
curl -s http://localhost:8080/api/v1/stats/category-distribution -H "Authorization: Bearer $TOKEN"
```

刷新令牌：

```bash
curl -s -X POST http://localhost:8080/api/v1/auth/refresh \
  -H 'Content-Type: application/json' \
  -d '{"refreshToken":"<refreshToken>"}'
```

> refresh token 采用"一次性轮换"：每次刷新都会撤销旧令牌，旧的再次使用会返回 `40102`。
