
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
