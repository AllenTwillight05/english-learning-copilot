# 认证前端规约

## 1. 当前交付范围

认证前端已经接入 Spring Boot 后端提供的 JWT 接口，并保留 mock/http/mixed 数据源切换：

- 登录页：使用用户名或邮箱登录。
- 注册页：创建普通 `USER` 账号，成功后直接进入登录态。
- 应用启动时根据本地 token 恢复当前用户。
- HTTP 请求统一注入 `Authorization: Bearer <token>`。
- 顶部导航根据用户态显示登录入口或当前用户与登出按钮；注册仅从登录页的“创建账号”进入。
- 当前不启用全局路由保护，学习模块仍可匿名访问，避免影响其他成员联调；个人页会做页面内登录态判断。

## 2. 页面与路由

| 页面 | 路由 | 文件 | 说明 |
| --- | --- | --- | --- |
| 登录页 | `/login` | `frontend/src/pages/LoginPage.jsx` | 字段为账号和密码，账号支持用户名或邮箱 |
| 注册页 | `/register` | `frontend/src/pages/RegisterPage.jsx` | 字段为用户名、邮箱、展示名、密码 |
| 个人页 | `/profile` | `frontend/src/pages/ProfilePage.jsx` | 未登录显示空状态和“请先登录”，登录后展示个人数据 |

登录或注册成功后，优先跳转到 `location.state.from`，没有来源页时跳转到 `/profile`。已登录用户访问 `/login` 或 `/register` 会跳转 `/profile`。

## 3. 接口契约

认证接口集中在 `frontend/src/services/endpoints.js`，服务方法集中在 `auth` 模块：

```js
auth.register({ username, email, password, displayName })
auth.login({ account, password })
auth.me()
auth.logout()
```

真实接口路径：

```text
POST /api/auth/register
POST /api/auth/login
GET  /api/auth/me
POST /api/auth/logout
```

登录与注册成功响应：

```js
{
  token: "string",
  user: {
    id: 1,
    username: "string",
    email: "string",
    displayName: "string",
    role: "USER",
    enabled: true,
    createdAt: "2026-07-09T00:00:00Z",
    updatedAt: "2026-07-09T00:00:00Z",
    lastLoginAt: null
  }
}
```

后端错误结构遵循 `docs/api-contracts.md`。前端优先展示 `message`，如果存在 `fieldErrors`，则映射到对应表单字段。

## 4. 状态与存储

- 认证状态由 `frontend/src/auth/AuthContext.jsx` 统一管理。
- 页面通过 `useAuth()` 读取 `user/token/loading/isAuthenticated`，并调用 `login/register/logout/refreshCurrentUser`。
- 本地持久化使用 `localStorage`，key 固定为：

```text
englishLearningCopilot.auth
```

- 存储结构为 `{ token, user }`。
- 应用启动时如果存在 token，会调用 `auth.me()` 刷新当前用户。
- `auth.me()` 返回 401 或 403 时，前端清理本地认证状态。
- 登出接口为无状态占位，前端无论接口是否成功都会清理本地 token 和 user。
- `/profile` 依赖 `useAuth()` 判断登录态：未登录不调用 `profile.getSnapshot()`，只显示登录提示；登录后再加载个人页数据。

## 5. Mock 与 HTTP 模式

当前推荐本地联调模式：

- `VITE_API_MODE=mixed`：按模块选择真实 Spring Boot 或 mock 数据，适合多人并行开发各自负责的后端模块。
- mixed 默认值：`auth=http`，`dashboard/speaking/vocabulary/grammar/profile=mock`。
- 其他成员接入自己的模块时，只修改对应 `VITE_<MODULE>_API_MODE=http`，不要把全局切到 `http` 影响未完成模块。

mixed 示例：

```bash
VITE_API_MODE=mixed
VITE_API_BASE_URL=http://localhost:8080
VITE_AUTH_API_MODE=http
VITE_VOCABULARY_API_MODE=http
VITE_SPEAKING_API_MODE=mock
VITE_GRAMMAR_API_MODE=mock
VITE_PROFILE_API_MODE=mock
VITE_DASHBOARD_API_MODE=mock
```

默认 `VITE_API_MODE=mock`：

- mock可登录账号：`learner` 或 `learner@example.com`。
- 内置密码：`Password123`。
- 注册会在当前前端运行时加入 mock 用户列表，刷新后恢复初始 mock 状态，不会写入 MySQL。

全量真实后端联调：

```bash
VITE_API_MODE=http
VITE_API_BASE_URL=http://localhost:8080
```

HTTP 请求由 `frontend/src/services/httpClient.js` 统一处理 JSON 请求、Bearer token 和错误解析。只有在 `mixed` 中对应模块设为 `http`，或全局 `http` 模式下，该模块才会请求真实后端；只有在业务接口也已实现时，业务模块才应切到真实接口。

## 6. 验收清单

- `/login` 可以在 mock 模式下登录并跳转 `/profile`。
- `/register` 可以在 mock 模式下注册并跳转 `/profile`。
- 刷新页面后顶部导航仍显示当前用户。
- 点击退出后清理本地认证状态，导航恢复登录入口。
- 未登录时首页、口语、词汇、语法仍可匿名打开；个人页显示“请先登录”空状态。
- 从个人页点击“请先登录”进入登录页，登录成功后返回 `/profile`。
- `npm run build` 通过。
