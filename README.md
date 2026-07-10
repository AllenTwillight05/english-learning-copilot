# English Learning Copilot

## A6 英语口语学习助手

这是一款智能应用，通过实时对话模拟、个性化练习和实时反馈，帮助用户提升英语口语水平并流利表达。可参考学长创业的“可栗口语”APP。

## 技术栈

- 后端：Spring Boot 3.4 + Spring Data JPA + Spring Security + JWT + MySQL
- 前端：React + JavaScript + Vite
- UI 组件库：Ant Design
- 设计风格：Apple 风格、轻拟物质感、高级感仪表盘界面

## 前端基础框架

- 前端工程位于 `frontend/` 目录，Spring Boot 后端位于 `backend/` 目录。
- 基于 Vite + React + JavaScript 搭建前端工程。
- 使用 Ant Design 作为基础组件库，并通过主题 token 与自定义样式统一视觉风格。
- 已完成路由拆分、布局解耦、服务层抽象、mock 数据和 HTTP 接口预留。
- 页面独有组件放在 `frontend/src/components/<PageName>/`，公共组件放在 `frontend/src/components/common/`。
- 当前首页只保留基础入口面板，覆盖：
  - 顶部导航
  - 项目简介
  - 口语、词汇、语法、个人四个入口
  - 今日概览
  - 个人进度

## 页面路由

- `/`：首页
- `/speaking`：口语页
- `/vocabulary`：词汇页
- `/grammar`：语法页
- `/profile`：个人页

## 接口切换

- 默认使用 mock 服务。
- 后续接入 Spring Boot 时，可通过 `VITE_API_MODE=http` 和 `VITE_API_BASE_URL` 切换到真实接口实现。

## 交付文档

- 前端交付说明：[docs/frontend-handoff.md](docs/frontend-handoff.md)
- 接口契约说明：[docs/api-contracts.md](docs/api-contracts.md)

## 推荐仓库结构

```text
english-learning-copilot/
├── frontend/   # React + Vite 前端
├── backend/    # Spring Boot 后端
├── docs/       # 交付与接口文档
└── README.md
```

## 启动方式

前端：

```bash
cd frontend
npm install
npm run dev
```

后端：

```bash
cd backend
mvn spring-boot:run
```

后端认证、MySQL、JWT、管理员种子、接口与测试说明见：[backend/README.md](backend/README.md)

## 基本需求

### 功能性需求

以下功能选 3 项：

1. 口语练习
   - 提供不同情境（餐厅订餐、商务谈判、旅行问路等）和不同形式（问答、对话、演讲等）的口语练习题目。
   - 支持录音功能，允许用户录制自己的回答并进行评估和反馈。

2. 词汇与语法练习
   - 提供词汇和语法练习题目，帮助用户扩展词汇量和提高语法水平。
   - 结合语音播放和文字提示，让用户能够更好地理解和掌握英语词汇和语法规则。

3. 实时反馈与评估
   - 提供实时反馈，包括发音准确性、语速、流利度等方面的评估。
   - 根据用户的表现，给出相应的建议和改进措施，帮助用户不断提升口语能力。

4. 个性化学习计划
   - 用户根据学习目标和水平，制定个性化的学习计划和练习内容。
   - 跟踪用户的学习进度，并进行相应的鼓励。

### 非功能性需求

- 客户端为 APP 或 Web 浏览器。
- 兼容性：适应用于客户端不同的分辨率/尺寸。
- 性能：数据量不少于 10k，在 100 并发的场景下，响应时间 < 3s。

## 进阶需求

- 虚拟对话伙伴：引入虚拟人工智能角色作为语言交流伙伴，能够进行自然语言对话，帮助用户更好地适应真实语境的交流。
- 个性化学习计划的智能规划：根据用户的口语水平、学习目标和学习偏好，自动生成个性化的学习路径，包括逐步提升口语流利度、拓展词汇量、改善发音准确性等方面，帮助用户有效地规划学习路线并达到目标。
- 其他创意的功能。
