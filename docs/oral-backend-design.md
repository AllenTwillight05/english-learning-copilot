
- 应先保证“创建练习-多轮对话-结束评分-结果保存”的主链路闭环，再逐步接入语音转写、发音评分和历史回放，避免一开始就把 Agent、音频、评分和存储全部耦合在一起。

## 整体流程
**1. 场景选择**
- 前端请求口语场景列表，例如商务开场、校园问路、面试自我介绍等。
- 后端返回场景信息、难度、目标能力、建议轮数、开场白、评分维度。

**2. 创建练习会话**
- 用户点击开始后，后端创建一次 speaking session，记录：
    userId、scenarioId、开始时间、状态、当前轮次、目标轮数。
- 返回 sessionId 给前端。

**3. 对话交互**
- 每一轮前端提交用户输入，可以先支持文本，后续再接语音转文字。
- 后端保存用户发言，然后把以下内容交给 Agent：
    当前场景、角色设定、历史对话、用户最新发言、目标能力、当前轮次。

**4. Agent 生成回复**
Agent 返回两类内容：
- “对话回复”，继续扮演场景角色和用户交流；
- “即时提示”，比如用户表达是否自然、有没有明显语法问题、可替换表达。（optional）
后端保存 Agent 回复，再返回给前端展示。

**5. 结束练习**
- 达到目标轮数，或用户主动结束后，后端将整段对话交给评分 Agent。
- 评分 Agent 按维度输出结构化结果，例如：
    fluency 流利度、grammar 语法、vocabulary 词汇、pronunciation 发音、relevance 场景贴合度、overallScore 总分、summary 总评、suggestions 改进建议。

**6. 保存评分结果**
- 后端将 session 状态改为 completed，保存总分、分项分数、建议、完整对话记录。
- 前端可进入反馈页展示评分、逐句建议和历史回放。

## 建议的后端接口

```
GET    /api/speaking/scenarios
GET    /api/speaking/scenarios/{scenarioId}

POST   /api/speaking/sessions
GET    /api/speaking/sessions/{sessionId}

POST   /api/speaking/sessions/{sessionId}/messages
POST   /api/speaking/sessions/{sessionId}/finish

GET    /api/speaking/sessions/{sessionId}/feedback
GET    /api/speaking/history
核心数据表
speaking_scenario
- id
- title
- description
- difficulty
- role_prompt
- opening_message
- target_turns
- scoring_rubric

speaking_session
- id
- user_id
- scenario_id
- status
- started_at
- completed_at
- overall_score
- feedback_summary

speaking_message
- id
- session_id
- sender  // USER or AGENT
- content
- turn_index
- created_at

speaking_feedback
- id
- session_id
- fluency_score
- grammar_score
- vocabulary_score
- pronunciation_score
- relevance_score
- overall_score
- strengths
- weaknesses
- suggestions
- raw_agent_result
```

## 开发顺序
- 第一阶段 文本对话闭环：场景列表、创建 session、用户发一句、Agent 回一句、保存历史。
- 第二阶段 结束评分：把完整对话发给评分 Agent，返回结构化 JSON，落库并展示。
- 第三阶段 接语音能力：前端录音上传，后端调用语音转文字；如果要评 pronunciation，再保存音频或转写置信度，并让 Agent 根据转写和语音分析结果评分。
- 第四阶段 历史记录和回放：替代现在前端 localStorage，让历史练习来自数据库。

## 目前的数据流

### 数据内容
1. Scenarios：Spring Boot 启动时会执行SpeakingScenarioSeedConfig.java中的 CommandLineRunner seedSpeakingScenarios(...)，往 speaking_scenarios 表里插入 4 条默认场景（待取代）：
    - business-opening
    - airport-checkin
    - dinner-smalltalk
    - clinic-visit
2. Usr：来自 users 表，由注册api写入（POST /api/auth/register）；speaking 创建 session 时会根据当前 JWT 里的用户名查 users 表
3. Session：调用会话api（POST /api/speaking/sessions），后端在 speaking_sessions 表创建一条记录，包含：
    - user_id
    - scenario_id
    - status
    - started_at
    - target_turns
    - current_turn
4. Message：调用消息api（POST /api/speaking/sessions/{sessionId}/messages），后端会写入两类消息到 speaking_messages：
    - USER：用户发来的内容
    - AGENT：后端 agent 回复
    目前 agent 回复来自后端的 mock agent client。

### 数据传输
后端 speaking 模块一次“理想全流程”数据流：

**1. 启动阶段：准备场景数据**

后端启动时执行：
```java
SpeakingScenarioSeedConfig.seedSpeakingScenarios(...)
```
往 `speaking_scenarios` 表插入 4 个默认场景：
```text
business-opening
airport-checkin
dinner-smalltalk
clinic-visit
```
如果数据库里已有同 id 场景，就不会重复插入。

**2. 用户认证阶段**

用户通过认证模块注册或登录：
```text
POST /api/auth/register
POST /api/auth/login
```
成功后前端拿到 JWT token。之后需要登录的 speaking 接口都会带：

```http
Authorization: Bearer <token>
```
用户数据来源是 `users` 表。

**3. 场景列表/详情阶段**

前端请求：

```text
GET /api/speaking/scenarios
GET /api/speaking/scenarios/{scenarioId}
```

后端从 `speaking_scenarios` 表读取场景基础信息，返回给前端。

这一步目前可以跑通。

**4. 创建练习会话阶段**

前端理论上应该在用户点击“进入会话/开始练习”时调用：

```text
POST /api/speaking/sessions
```

请求体：

```json
{
  "scenarioId": "business-opening"
}
```

后端会：

- 根据 JWT 找到当前用户。
- 根据 `scenarioId` 找到场景。
- 写入一条 `speaking_sessions`。
- 自动把场景的 `openingMessage` 写入 `speaking_messages`，作为第一条 Agent 消息。
- 返回 session 和 messages。

这一步后端已有，但当前前端还没接上。

**5. 用户发送文本消息阶段**

前端理论上应该调用：

```text
POST /api/speaking/sessions/{sessionId}/messages
```

请求体类似：

```json
{
  "content": "Sure. Today I'd like to start with the project update."
}
```

后端会：

- 写入一条 `USER` 消息到 `speaking_messages`。
- 调用 `SpeakingAgentClient` 生成回复。
- 写入一条 `AGENT` 消息。
- 更新 `speaking_sessions.currentTurn`。
- 返回本轮用户消息、Agent 消息和 session 状态。

这一步后端也已有，但当前是 mock agent，不是真 AI；前端也还没接上。

**目前无法完整跑通的缺口**

1. 前端 speaking 会话页还在用 `scenario.prompts` 跑固定脚本，而后端场景接口不返回 `prompts`。
2. 前端还没有调用后端的 `POST /api/speaking/sessions` 创建真实 session。
3. 前端还没有调用 `POST /api/speaking/sessions/{sessionId}/messages` 发送用户消息。
4. 当前录音流程只是前端模拟，没有上传录音接口接入。
5. 后端还没有真实语音识别、发音评分、语速、流利度评估。
6. 后端还没有完整 feedback 接口，例如设计文档里提到的：
```text
GET /api/speaking/sessions/{sessionId}/feedback
```
7. 当前 `SpeakingAgentClient` 是 mock 回复，不是真正的大模型或语音对话伙伴。
8. 前端反馈页还在读 `scenario.feedback`，但后端不返回 `feedback`，所以真实 HTTP 下会显示“评分数据缺失”。

所以现在的真实进度可以理解为：

```text
场景列表/详情：已后端化，可从 MySQL 跑通
认证 + 用户入库：已后端化，可跑通
创建 session：后端已实现，前端未接
发送文本消息：后端已实现，前端未接
录音上传/语音评估/反馈报告：尚未实现完整后端闭环
```
