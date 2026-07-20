
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
- 每一轮前端提交用户录音。当前前端已经移除了文本输入框，不再支持手动打字发送。
- 后端收到录音后，会先保存音频文件，再把音频交给 ASR 和发音评分服务处理。现在这两个服务还是 mock，实现目标是先跑通流程。
- 后端保存用户发言，然后把以下内容交给 Agent：
    当前场景、角色设定、历史对话、用户最新转写文本、目标能力、当前轮次。

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

POST   /api/speaking/sessions/{sessionId}/messages  // multipart，字段名 audio
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
- audio_url
- transcribed_text
- pronunciation_score
- pronunciation_detail
- instant_tip
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
- 第一阶段 原本计划先做文本对话闭环：场景列表、创建 session、用户发一句、Agent 回一句、保存历史。当前实现已经把前端文本输入移除，实际主链路改成“录音上传 + mock 转写 + mock 回复”。
- 第二阶段 结束评分：把完整对话发给评分 Agent，返回结构化 JSON，落库并展示。
- 第三阶段 接语音能力：前端录音上传，后端调用语音转文字；如果要评 pronunciation，再保存音频或转写置信度，并让 Agent 根据转写和语音分析结果评分。
- 第四阶段 历史记录和回放：替代现在前端 localStorage，让历史练习来自数据库。

## 目前的数据流

### 数据内容
1. Scenarios：Spring Boot 启动时会执行 SpeakingScenarioSeedConfig.java 中的 CommandLineRunner seedSpeakingScenarios(...)，往 speaking_scenarios 表里插入 4 条默认场景：
    - business-opening
    - airport-checkin
    - dinner-smalltalk
    - clinic-visit
2. User：来自 users 表，由注册 api 写入（POST /api/auth/register）；speaking 创建 session 时会根据当前 JWT 里的用户名查 users 表。
3. Session：调用会话 api（POST /api/speaking/sessions），后端在 speaking_sessions 表创建一条记录，包含：
    - user_id
    - scenario_id
    - status
    - started_at
    - target_turns
    - current_turn
4. Message：调用消息 api（POST /api/speaking/sessions/{sessionId}/messages），后端会写入两类消息到 speaking_messages：
    - USER：用户录音转写后的文本，同时保存 audioUrl、transcribedText、pronunciationScore、pronunciationDetail。
    - AGENT：后端 agent 回复，同时保存 instantTip。当前 TTS 是 mock，所以 agent audioUrl 通常为空，前端会退回浏览器 TTS 播放。
5. Audio：录音文件不直接放数据库。当前存到本地文件系统，路径来自 `app.speaking.upload-dir`，默认是 `uploads/speaking`。数据库只保存音频路径。
6. Feedback：当前没有单独落 `speaking_feedback` 表。反馈接口会根据 session 里的消息和发音分数临时生成一份 mock 评分结果。

### 数据传输
后端 speaking 模块当前一次可跑通的数据流：

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

这一步目前可以跑通。场景详情页的“对话示例”使用 `sampleDialogue`，真实会话不依赖旧的 `scenario.prompts`。

**4. 创建练习会话阶段**

前端在用户进入会话页时调用：

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

这一步目前前后端已经接上。会话页加载后，第一条 Agent 开场白来自后端 session.messages。

**5. 用户录音消息阶段**

前端点击“开始录音”后使用浏览器 MediaRecorder 获取音频。点击“停止录音”后，前端把录音 Blob 作为 multipart 表单提交：

```text
POST /api/speaking/sessions/{sessionId}/messages
```

请求格式：

```http
Content-Type: multipart/form-data
```

字段：

```text
audio: recording.webm
```

后端会：

- 读取上传的音频文件。
- 先写入一条占位 `USER` 消息，拿到 message id 后保存音频文件。
- 调用 `AsrService` 生成转写文本。当前是 `MockAsrService`，返回固定 mock 文本。
- 调用 `IseService` 生成发音分数。当前是 `MockIseService`，返回随机 mock 分数。
- 更新 `USER` 消息，写入 content、audioUrl、transcribedText、pronunciationScore、pronunciationDetail。
- 调用 `SpeakingAgentClient` 生成回复。当前是 `MockSpeakingAgentClient`，按场景和轮次返回固定风格的回复。
- 调用 `TtsService` 生成 Agent 语音。当前是 `MockTtsService`，返回空音频；前端会用浏览器 `speechSynthesis` 读出 Agent 文本。
- 写入一条 `AGENT` 消息。
- 更新 `speaking_sessions.currentTurn`。
- 返回本轮用户消息、Agent 消息、发音分数和 session 状态。

这一步目前可以跑通，真实 ASR、真实发音评分、真实大模型 Agent 还没有接入。

**6. 查询历史和反馈阶段**

前端反馈页优先使用 URL 里的 `sessionId` 查询会话：

```text
GET /api/speaking/sessions/{sessionId}
```

如果没有 `sessionId`，前端会调用历史接口，找到当前场景最新的一条 session：

```text
GET /api/speaking/history
```

反馈数据来自：

```text
GET /api/speaking/sessions/{sessionId}/feedback
```

当前反馈接口不是最终评分算法，它会生成 mock 总分、发音准确性、流畅度、语速、问题句子、建议和每轮反馈。它的作用是先让“录音练习 -> 会话保存 -> 反馈页展示 -> 回放”这条路跑起来。

### 当前还没有做完的地方

1. ASR 还没有接真实模型，现在转写文本来自 `MockAsrService`。
2. 发音评分还没有接真实模型，现在 pronunciation score 来自 `MockIseService`。
3. Agent 还没有接真实大模型，现在回复来自 `MockSpeakingAgentClient`。
4. TTS 还没有接真实语音合成，现在后端不生成 Agent 音频，前端使用浏览器 TTS。
5. `POST /api/speaking/sessions/{sessionId}/finish` 还没有实现。当前“交卷”只是前端跳转到反馈页，并不会把 session 状态改成 completed。
6. `speaking_feedback` 表还没有真正使用。反馈结果目前是按请求临时生成，不是稳定保存的历史评分报告。
7. 录音文件当前使用本地文件系统保存。部署到服务器时需要把 `app.speaking.upload-dir` 配到服务器上的持久目录；如果多实例部署，应该换成对象存储或共享存储。
8. 文本输入发送功能已经从前端和 service 中移除。现在 `/messages` 只接受 multipart 录音，不再接受 JSON 文本消息。

所以现在的真实进度可以理解为：

```text
场景列表/详情：已后端化，可从 MySQL 跑通
认证 + 用户入库：已后端化，可跑通
创建 session：前后端已接通
录音上传：前后端已接通，音频保存到文件系统，路径写入消息记录
语音转写：已接 mock，未接真实模型
发音评分：已接 mock，未接真实模型
Agent 回复：已接 mock，未接真实大模型
反馈报告：接口已接通，当前是 mock 评分，尚未落 speaking_feedback 表
历史和回放：已从后端 session/history 读取，不再依赖前端 localStorage 保存练习结果
```
