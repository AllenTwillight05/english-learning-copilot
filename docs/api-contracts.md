# API Contracts

本文件说明前端已经预留的 Spring Boot 接口。后端返回 JSON 需要对齐 `frontend/src/services/contracts.js` 中记录的数据形状。

默认前端使用 mock 数据。接入真实接口时，创建 `.env`：

```bash
VITE_API_MODE=http
VITE_API_BASE_URL=http://localhost:8080
```

## 0. 认证与权限

后端已提供 Spring Security + JWT Bearer token 认证接口。前端登录功能尚未接入，后续需要在 `frontend/src/services/httpClient.js` 统一注入 token。

### 0.1 注册

`POST /api/auth/register`

用途：注册普通用户，默认角色为 `USER`。

请求结构：

```js
{
  username: "string",
  email: "string",
  password: "string",
  displayName: "string"
}
```

响应结构：

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

### 0.2 登录

`POST /api/auth/login`

用途：使用用户名或邮箱登录。

请求结构：

```js
{
  account: "string",
  password: "string"
}
```

响应结构同注册接口，返回 `token` 和 `user`。

### 0.3 当前用户

`GET /api/auth/me`

用途：根据 Bearer token 获取当前用户。

请求头：

```http
Authorization: Bearer <token>
```

响应结构：注册响应中的 `user` 对象。

### 0.4 登出

`POST /api/auth/logout`

用途：无状态登出占位。后端不保存 token 黑名单，前端负责清理本地 token。

响应结构：

```js
{
  message: "string"
}
```

### 0.5 角色与管理端权限

用户角色：

```text
USER
ADMIN
```

`/api/admin/**` 仅允许 `ADMIN` 访问。当前已提供用户管理接口和业务占位接口：

```text
GET    /api/admin/users
PATCH  /api/admin/users/{id}/role
PATCH  /api/admin/users/{id}/status

GET/POST/PUT/DELETE /api/admin/question-types
GET/POST/PUT/DELETE /api/admin/question-banks
GET/POST/PUT/DELETE /api/admin/vocabulary-entries
```

题型、题库、词条接口目前返回 `501 NOT_IMPLEMENTED` 占位响应，用于后续业务模块替换实现。

### 0.6 错误响应

后端统一错误响应结构：

```js
{
  timestamp: "2026-07-09T00:00:00Z",
  status: 409,
  error: "Conflict",
  message: "string",
  path: "/api/auth/register",
  fieldErrors: null
}
```

常见状态码：

- `400`：请求参数校验失败。
- `401`：未登录、token 缺失或登录失败。
- `403`：已登录但权限不足。
- `409`：用户名或邮箱重复。
- `501`：预留接口尚未实现。

## 1. 首页概览

`GET /api/dashboard/overview`

用途：渲染首页简介、行动按钮和今日概览。

核心结构：

```js
{
  productTag: "string",
  stackTag: "string",
  headline: "string",
  description: "string",
  primaryActionLabel: "string",
  secondaryActionLabel: "string",
  quickStats: [{ label: "string", value: "string", icon: "microphone" }],
  focusIntensity: "string",
  suggestedDuration: "string"
}
```

## 2. 口语模块

`GET /api/speaking/catalog`

用途：渲染口语页的练习模式、场景卡片和脚本预览。

核心结构：

```js
{
  modes: ["string"],
  scriptPreviewTitle: "string",
  scriptPreviewLines: ["string"],
  scenarios: [
    {
      id: "string",
      title: "string",
      level: "string",
      accent: "string",
      duration: "string",
      summary: "string",
      tone: "blue"
    }
  ]
}
```

后续可扩展接口：

- `POST /api/speaking/sessions`：创建口语练习会话。
- `POST /api/speaking/recordings`：上传录音。
- `GET /api/speaking/sessions/{id}/feedback`：获取口语反馈。

## 3. 词汇模块

`GET /api/vocabulary/snapshot`

用途：渲染词汇页卡片、复习目标和掌握度。

核心结构：

```js
{
  dailyGoal: "string",
  retentionHint: "string",
  cards: [{ id: "string", word: "string", usage: "string", progress: 0, tag: "string" }]
}
```

后续可扩展接口：

- `POST /api/vocabulary/review-queue`：加入复习队列。
- `PATCH /api/vocabulary/cards/{id}/progress`：更新掌握度。

## 4. 语法模块

`GET /api/grammar/snapshot`

用途：渲染语法页主题、例句和掌握度。

核心结构：

```js
{
  focus: "string",
  topics: [
    {
      id: "string",
      title: "string",
      summary: "string",
      examples: ["string"],
      progress: 0,
      tag: "string"
    }
  ]
}
```

后续可扩展接口：

- `GET /api/grammar/topics/{id}`：查看语法主题详情。
- `POST /api/grammar/exercises/{id}/submit`：提交语法练习结果。

## 5. 个人模块

`GET /api/profile/snapshot`

用途：渲染个人页资料、学习计划、能力进度和最近反馈。

核心结构：

```js
{
  learnerName: "string",
  level: "string",
  streak: "string",
  feedback: {
    statusLabel: "string",
    playbackActionLabel: "string",
    metrics: [{ key: "string", label: "string", value: "string" }],
    notes: ["string"]
  },
  dailyPlan: {
    autoPilotEnabled: true,
    weeklyImprovement: "string",
    items: [{ id: "string", time: "string", task: "string", meta: "string", done: false }],
    progress: [{ id: "string", label: "string", value: 0, tone: "default" }]
  }
}
```

后续可扩展接口：

- `PATCH /api/profile`：更新用户资料。
- `POST /api/profile/plan/regenerate`：重新生成学习计划。
- `GET /api/profile/progress`：获取详细进度曲线。

## 6. 对接约定

- 前端页面只通过 `useAppServices()` 访问数据。
- 新增接口时，先更新 `contracts.js`，再更新 `mockData.js` 和 `httpServices.js`。
- 后端字段命名建议使用 camelCase，与前端数据形状保持一致。
- 错误处理后续集中放在 `frontend/src/services/httpClient.js`。
- 登录成功后，前端应保存 `token` 和 `user`，后续 HTTP 请求统一在 `frontend/src/services/httpClient.js` 注入 `Authorization: Bearer <token>`。
- 当前后端使用 JWT 无状态鉴权，不需要 CSRF token。
