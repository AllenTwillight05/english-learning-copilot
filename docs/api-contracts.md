# API Contracts

本文件说明前端已经预留的 Spring Boot 接口。后端返回 JSON 需要对齐 `frontend/src/services/contracts.js` 中记录的数据形状。

默认前端使用 mock 数据。并行联调各自负责模块时，创建 `.env.local` 使用 mixed 模式：

```bash
VITE_API_MODE=mixed
VITE_API_BASE_URL=http://localhost:8080
VITE_AUTH_API_MODE=http
VITE_SPEAKING_API_MODE=mock
VITE_VOCABULARY_API_MODE=mock
VITE_GRAMMAR_API_MODE=mock
VITE_PROFILE_API_MODE=mock
```

某个模块后端接口完成后，只把对应 `VITE_<MODULE>_API_MODE` 改为 `http`。不要在其他模块未完成时直接使用全局 `VITE_API_MODE=http`。

全量接入真实业务接口时，再切换为：

```bash
VITE_API_MODE=http
VITE_API_BASE_URL=http://localhost:8080
```

## 0. 认证与权限

后端已提供 Spring Security + JWT Bearer token 认证接口。前端已接入登录/注册页面、认证状态和 `frontend/src/services/httpClient.js` 的 Bearer token 注入。

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

`/api/admin/**` 仅允许 `ADMIN` 访问。当前提供用户管理接口和业务占位接口（未实现）：

```text
GET    /api/admin/users
PATCH  /api/admin/users/{id}/role
PATCH  /api/admin/users/{id}/status

GET/POST/PUT/DELETE /api/admin/question-types
GET/POST/PUT/DELETE /api/admin/question-banks
GET/POST/PUT/DELETE /api/admin/vocabulary-entries
```

题型、题库、词条接口目前返回 `501 NOT_IMPLEMENTED` 占位响应，后续业务模块需替换实现。

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

口语模块不再提供页面级 `GET /api/speaking/catalog` 聚合接口。口语首页直接使用场景接口；后续星级完成情况、练习难度选择等能力应扩展到场景或用户进度接口，不再放入 catalog。

### 2.1 口语场景列表

`GET /api/speaking/scenarios`

用途：获取可选口语练习场景。该接口允许匿名访问。

核心结构：

```js
[
  {
    id: "business-opening",
    title: "Business Meeting",
    description: "string",
    difficulty: "B2",
    level: "B2",
    accent: "American business English",
    duration: "18 min",
    summary: "string",
    tone: "blue",
    goal: "string",
    keywords: ["agenda", "clarify"],
    openingMessage: "Good morning. Could you briefly introduce today's agenda?",
    sampleDialogue: "Coach: Good morning...\nLearner: Sure...",
    targetTurns: 6,
    scoringRubric: "string"
  }
]
```

后续 scenario grid 可扩展字段：

```js
{
  completionStars: 0,
  availableDifficulties: ["A2", "B1", "B2"],
  selectedDifficulty: "B1"
}
```

如完成情况依赖当前用户，优先新增用户进度接口，例如 `GET /api/speaking/progress`，再由前端按 `scenarioId` 合并展示。

### 2.2 创建口语会话

`POST /api/speaking/sessions`

用途：登录用户选择场景后创建一次口语练习 session，并自动保存场景开场白作为第一条 Agent 消息。

请求头：

```http
Authorization: Bearer <token>
```

请求结构：

```js
{
  scenarioId: "business-opening"
}
```

响应核心结构：

```js
{
  id: 1,
  userId: 1,
  scenario: { id: "business-opening" },
  status: "ACTIVE",
  startedAt: "2026-07-14T00:00:00Z",
  completedAt: null,
  currentTurn: 0,
  targetTurns: 6,
  messages: [
    {
      id: 1,
      sender: "AGENT",
      content: "Good morning. Could you briefly introduce today's agenda?",
      instantTip: null,
      turnIndex: 0,
      createdAt: "2026-07-14T00:00:00Z"
    }
  ]
}
```

### 2.3 提交录音消息

`POST /api/speaking/sessions/{sessionId}/messages`

用途：上传用户本轮录音。后端保存音频文件，生成转写文本和发音分数，再调用预留的 Agent 接口生成回复并保存 Agent 回复。

该接口目前只接受 multipart 录音，不再接受 JSON 文本消息。

请求头：

```http
Authorization: Bearer <token>
Content-Type: multipart/form-data
```

表单字段：

```text
audio: recording.webm
```

响应核心结构：

```js
{
  userMessage: {
    id: 2,
    sender: "USER",
    content: "This is a mock transcript of the recorded speech.",
    audioUrl: "speaking/1/2.webm",
    transcribedText: "This is a mock transcript of the recorded speech.",
    pronunciationScore: 85.3,
    pronunciationDetail: "{\"totalScore\":85.3,\"accuracy\":86.1}",
    instantTip: null,
    turnIndex: 1,
    createdAt: "2026-07-14T00:01:00Z"
  },
  agentMessage: {
    id: 3,
    sender: "AGENT",
    content: "Thanks for clarifying. Could you also explain the main risk we should discuss today?",
    audioUrl: null,
    instantTip: "Try expanding your answer with one reason or detail so the conversation feels more natural.",
    turnIndex: 1,
    createdAt: "2026-07-14T00:01:01Z"
  },
  pronunciationScore: {
    totalScore: 85.3,
    accuracy: 86.1,
    fluency: 82.4,
    integrity: 88.0,
    speed: 121.0
  },
  session: {
    id: 1,
    currentTurn: 1,
    messages: []
  }
}
```

当前 ASR、发音评分、Agent 和 TTS 都是 mock 实现：

- `MockAsrService` 返回固定转写文本。
- `MockIseService` 返回 70–95 分附近的随机发音分数。
- `MockSpeakingAgentClient` 按场景和轮次返回模拟回复。
- `MockTtsService` 返回空音频，前端会退回浏览器 TTS 播放 Agent 文本。

### 2.4 查询会话与历史

```text
GET /api/speaking/sessions/{sessionId}
GET /api/speaking/history
```

用途：查询当前用户自己的口语会话详情或历史 session 列表。接口需要登录；访问他人 session 返回 `403`。

`GET /api/speaking/history` 返回当前用户所有口语 session，按 `startedAt` 倒序排列。前端详情页点击”历史记录”时，会调用该接口，并按当前 `scenarioId` 取最新一条 session 作为回放入口。

`GET /api/speaking/sessions/{sessionId}` 返回单条 session 的完整消息列表。前端反馈/回放页优先使用 URL 中的 `sessionId` 调用该接口；如果 URL 没有 `sessionId`，则回退到 `GET /api/speaking/history` 查找当前情景最新一条 session。

响应核心结构：

```js
{
  id: 1,
  userId: 1,
  scenario: {
    id: “business-opening”,
    title: “Business Meeting”
  },
  status: “ACTIVE”,
  startedAt: “2026-07-14T00:00:00Z”,
  completedAt: null,
  currentTurn: 1,
  targetTurns: 6,
  messages: [
    {
      id: 1,
      sender: “AGENT”,
      content: “Good morning. Could you briefly introduce today's agenda?”,
      audioUrl: null,
      transcribedText: null,
      pronunciationScore: null,
      pronunciationDetail: null,
      instantTip: null,
      turnIndex: 0,
      createdAt: “2026-07-14T00:00:00Z”
    },
    {
      id: 2,
      sender: “USER”,
      content: “This is a mock transcript of the recorded speech.”,
      audioUrl: “speaking/1/2.webm”,
      transcribedText: “This is a mock transcript of the recorded speech.”,
      pronunciationScore: 85.3,
      pronunciationDetail: “{...}”,
      instantTip: null,
      turnIndex: 1,
      createdAt: “2026-07-14T00:01:00Z”
    }
  ]
}
```

### 2.5 获取口语反馈

`GET /api/speaking/sessions/{sessionId}/feedback`

用途：获取某次口语练习的评分反馈。接口需要登录；访问他人 session 返回 `403`。

当前阶段后端使用 mock 评分。它会从 session 消息中读取用户发言和已保存的发音分数，再生成总分、建议和每轮反馈。后续可接入真实 ASR、发音评估和 LLM 反馈。

请求头：

```http
Authorization: Bearer <token>
```

响应核心结构：

```js
{
  totalScore: 88,
  pronunciation: 91,
  fluency: 84,
  speed: “136 WPM”,
  issueSentences: [
    “Today I would like to discuss the delivery timeline.”
  ],
  suggestions: [
    “Try adding more detail to make your responses fuller and more natural.”,
    “Pay attention to sentence stress — emphasize key words for clearer communication.”,
    “Practice linking words together to improve your overall fluency.”
  ],
  scenarioTitle: "Business Meeting",
  totalTurns: 1,
  averagePronunciationScore: 85.3,
  turns: [
    {
      turnIndex: 1,
      userText: "This is a mock transcript of the recorded speech.",
      agentText: "Thanks for clarifying. Could you also explain the main risk we should discuss today?",
      score: {
        totalScore: 85.3,
        accuracy: 86.1,
        fluency: 82.4,
        integrity: 88.0,
        speed: 121.0
      }
    }
  ],
  agentOverallComment: "Keep practicing! Focus on clarity and natural pacing."
}
```

字段说明：

| 字段 | 类型 | 说明 |
|------|------|------|
| `totalScore` | `int` | 总分，范围 0–100 |
| `pronunciation` | `int` | 发音准确性评分，范围 0–100 |
| `fluency` | `int` | 流畅度评分，范围 0–100 |
| `speed` | `string` | 语速，格式如 `”136 WPM”` |
| `issueSentences` | `string[]` | 需要改进的句子列表（从用户消息中选取） |
| `suggestions` | `string[]` | 改进建议列表 |
| `scenarioTitle` | `string` | 当前 session 对应的场景标题 |
| `totalTurns` | `int` | 当前 session 已完成的轮数 |
| `averagePronunciationScore` | `number` | 用户消息发音分数均值。没有用户录音时为 0 |
| `turns` | `array` | 每轮用户文本、Agent 文本和发音分数 |
| `agentOverallComment` | `string` | 总体建议 |

录音文件当前不进数据库。后端把文件写到 `app.speaking.upload-dir` 配置的目录里，数据库保存 `audioUrl` 作为引用路径。生产部署时应把该目录配置到服务器持久化路径，或者改成对象存储。

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
- 登录或注册成功后，前端会保存 `token` 和 `user`，后续 HTTP 请求统一在 `frontend/src/services/httpClient.js` 注入 `Authorization: Bearer <token>`。
- 当前后端使用 JWT 无状态鉴权，不需要 CSRF token。
