# 前端交付说明

## 1. 当前交付范围

当前仓库已经具备可并行开发的前端基础框架：

- React + JavaScript + Vite 工程已搭建。
- Ant Design 已接入，并统一了基础主题。
- 页面已按产品入口拆分为：首页、口语、词汇、语法、个人。
- 每个页面都有独立路由和独立页面文件。
- 数据访问通过 `services` 层注入，页面不直接依赖 mock 或真实 HTTP。
- 已预留 Spring Boot 后端接口路径。
- 后端已提供认证、JWT 和 `USER` / `ADMIN` 权限接口；前端已接入登录/注册 UI、token 状态和请求头注入。
- 默认使用 mock 数据，便于前端成员独立开发。

这份交付适合作为团队前端分工起点，不代表完整业务功能已经完成。

## 2. 启动方式

```bash
cd frontend
npm install
npm run dev
```

默认访问地址以终端输出为准，通常是：

```text
http://127.0.0.1:4173/
```

构建验证：

```bash
cd frontend
npm run build
```

## 3. 页面与路由

| 页面 | 路由 | 文件 | 适合负责的成员 |
| --- | --- | --- | --- |
| 首页 | `/` | `frontend/src/pages/DashboardPage.jsx` | 框架/集成负责人 |
| 口语 | `/speaking` | `frontend/src/pages/SpeakingPage.jsx` | 口语练习负责人 |
| 词汇 | `/vocabulary` | `frontend/src/pages/VocabularyPage.jsx` | 词汇练习负责人 |
| 语法 | `/grammar` | `frontend/src/pages/GrammarPage.jsx` | 语法练习负责人 |
| 个人 | `/profile` | `frontend/src/pages/ProfilePage.jsx` | 个人中心/学习计划负责人；未登录显示登录提示 |

## 4. 目录结构

```text
frontend/src
├── app                  # 应用入口、Provider、Ant Design 主题
├── components           # 公共组件、导航组件、页面私有组件
│   ├── Dashboard        # 首页私有组件
│   ├── Speaking         # 口语页私有组件
│   ├── Vocabulary       # 词汇页私有组件
│   ├── Grammar          # 语法页私有组件
│   ├── Profile          # 个人页私有组件
│   ├── common           # 跨页面公共组件
│   └── navigation       # 导航组件
├── auth                 # 登录态 Provider、useAuth 和本地 token 状态
├── hooks                # 通用 Hook
├── layouts              # 全局布局
├── pages                # 五个产品页面
├── router               # 路由配置和导航配置
├── services             # 接口形状说明、mock 服务、HTTP 服务
└── styles.css           # 全局布局和业务视觉样式
```

重点文件：

- `frontend/src/router/routes.jsx`：页面路由。
- `frontend/src/router/navigation.jsx`：顶部导航。
- `frontend/src/services/contracts.js`：前后端数据形状说明。
- `frontend/src/services/mockData.js`：前端开发用 mock 数据。
- `frontend/src/services/httpServices.js`：真实 HTTP 接口实现。
- `frontend/src/services/endpoints.js`：后端接口路径。
- `frontend/src/auth/AuthContext.jsx`：认证状态、登录、注册、登出和当前用户恢复。
- `frontend/src/services/authStorage.js`：`localStorage` token/user 持久化。
- `frontend/src/app/theme.js`：Ant Design 主题 token。
- `frontend/src/styles.css`：页面布局、卡片、响应式和 Apple 风格视觉。

组件放置约定：

- 页面文件只负责取数、组合页面和连接路由入口。
- 某个页面独有的组件放在 `frontend/src/components/<PageName>/`，例如口语页放 `frontend/src/components/Speaking/`。
- 多个页面都会复用的组件放在 `frontend/src/components/common/`。
- 导航相关组件放在 `frontend/src/components/navigation/`。

## 5. 服务接口边界

页面通过 `useAppServices()` 获取服务，不直接写 `fetch`。

当前服务模块：

- `dashboard.getOverview()`：首页概览。
- `speaking.listScenarios()`：口语场景列表。
- `speaking.getScenario(scenarioId)`：单个口语场景详情。
- `vocabulary.getSnapshot()`：词汇卡片、掌握度、复习目标。
- `grammar.getSnapshot()`：语法主题、例句、掌握度。
- `profile.getSnapshot()`：个人信息、学习计划、能力进度、最近反馈；当前仅在 `/profile` 已登录时调用。

认证服务模块已接入，mock 与 HTTP 模式保持同形：

- `auth.register()`：调用 `POST /api/auth/register`。
- `auth.login()`：调用 `POST /api/auth/login`。
- `auth.me()`：调用 `GET /api/auth/me`。
- `auth.logout()`：调用 `POST /api/auth/logout` 并清理本地 token。

注意：默认 `VITE_API_MODE=mock` 时，注册只写入前端运行时 mock 数据，不会写入 MySQL。需要真实入库时，必须启动 Spring Boot 后端，并以 `VITE_API_MODE=mixed` 或 `VITE_API_MODE=http` 启动前端；其中 mixed 模式下还需要保证 `VITE_AUTH_API_MODE=http`。

当前更推荐本地使用 `VITE_API_MODE=mixed`：每个模块单独决定走真实 Spring Boot 还是 mock 数据。默认认证模块走真实后端，首页、口语、词汇、语法、个人学习数据继续使用 mock。这样既能验证登录/注册和 MySQL 入库，也不会因为其他业务接口尚未实现而出现“页面数据加载失败”。

mixed 模式下的模块开关集中在 `frontend/src/services/index.js`，也可以通过 `.env.local` 覆盖：

```bash
VITE_AUTH_API_MODE=http
VITE_DASHBOARD_API_MODE=mock
VITE_SPEAKING_API_MODE=mock
VITE_VOCABULARY_API_MODE=mock
VITE_GRAMMAR_API_MODE=mock
VITE_PROFILE_API_MODE=mock
```

各模块负责人只打开自己负责且后端已实现的模块，例如词汇负责人只需要把 `VITE_VOCABULARY_API_MODE=http`，不要把全局 `VITE_API_MODE` 改成 `http` 影响其他未完成模块。

成员开发页面时，优先改自己负责的页面、页面私有组件目录和对应 service 数据形状，不要把其他模块逻辑塞进首页。

## 6. 后端接口预留

接口路径集中在 `frontend/src/services/endpoints.js`：

```text
/api/dashboard/overview
/api/speaking/scenarios
/api/speaking/scenarios/{scenarioId}
/api/vocabulary/snapshot
/api/grammar/snapshot
/api/profile/snapshot
```

认证与权限接口：

```text
/api/auth/register
/api/auth/login
/api/auth/me
/api/auth/logout
/api/admin/**
```

接 Spring Boot 时，后端返回 JSON 结构需要对齐 `frontend/src/services/contracts.js` 中记录的数据形状。

切换真实接口：

```bash
VITE_API_MODE=http
VITE_API_BASE_URL=http://localhost:8080
```

并行开发阶段更推荐：

```bash
VITE_API_MODE=mixed
VITE_API_BASE_URL=http://localhost:8080
```

在 `mixed` 模式下，再用 `VITE_<MODULE>_API_MODE=http` 单独开启已完成的模块接口。

可以参考 `frontend/.env.example`。

更完整的接口契约说明见：

- `docs/api-contracts.md`
- `docs/auth-frontend-spec.md`

认证对接注意：登录成功后保存后端返回的 `token` 和 `user`；真实 HTTP 请求统一在 `frontend/src/services/httpClient.js` 加上 `Authorization: Bearer <token>`。当前学习模块的 `GET` 接口后端暂时允许匿名访问，避免影响现有页面联调；个人页未登录时只展示空状态和“请先登录”按钮。

真实后端注册联调流程：

```bash
cd backend
mvn spring-boot:run
```

```bash
cd frontend
VITE_API_MODE=mixed VITE_API_BASE_URL=http://localhost:8080 npm run dev
```

注册成功后，可在 MySQL 中查询 `english_learning_copilot.users` 表确认用户记录。

## 7. 成员开发建议

口语负责人：

- 页面：`frontend/src/pages/SpeakingPage.jsx`
- 私有组件：`frontend/src/components/Speaking/`
- 数据：`speaking.listScenarios()`、`speaking.getScenario(scenarioId)`、`speaking.createSession(scenarioId)`、`speaking.submitRecording(sessionId, audioBlob)`、`speaking.getFeedback(sessionId)`
- 后续接口：结束 session、稳定保存评分报告、用户场景完成进度、真实 ASR/发音评分/Agent 接入。

词汇负责人：

- 页面：`frontend/src/pages/VocabularyPage.jsx`
- 私有组件：`frontend/src/components/Vocabulary/`
- 数据：`vocabulary.getSnapshot()`
- 后续接口：词卡列表、复习队列、掌握度更新。

语法负责人：

- 页面：`frontend/src/pages/GrammarPage.jsx`
- 私有组件：`frontend/src/components/Grammar/`
- 数据：`grammar.getSnapshot()`
- 后续接口：语法主题、例句、题目、练习结果。

个人负责人：

- 页面：`frontend/src/pages/ProfilePage.jsx`
- 私有组件：`frontend/src/components/Profile/`
- 数据：`profile.getSnapshot()`
- 后续接口：学习计划、进度统计、最近反馈、用户资料。

框架负责人：

- 路由：`frontend/src/router/routes.jsx`
- 导航：`frontend/src/router/navigation.jsx`
- 主题：`frontend/src/app/theme.js`
- 全局样式：`frontend/src/styles.css`

## 8. 交付检查清单

交付前至少确认：

- `npm install` 能成功安装依赖。
- `npm run dev` 能启动。
- `npm run build` 能通过。
- 首页只作为基础入口，不承载复杂业务。
- 五个页面路由都能打开。
- mock 数据能支持成员独立开发。
- 接口形状集中在 `contracts.js`，后续便于和 Spring Boot 对齐。

## 9. 后续建议

- 后续可按产品需要决定是否给 `/profile` 或更多业务页增加登录保护。
- 后端接口稳定后，可以用 OpenAPI 或接口表固化契约。
- 真实接口变多后，可以引入 TanStack Query 管理请求缓存。
- 口语实时反馈后续可扩展录音上传、波形展示和 WebSocket 流式反馈。
