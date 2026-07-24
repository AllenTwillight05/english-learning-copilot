# 口语模块 Agent 与语音能力接入设计

## 目标

当前 speaking 模块已经跑通了基本链路：

```text
创建 session
-> 用户录音上传
-> mock ASR 转写
-> mock ISE 发音评分
-> mock Agent 回复
-> mock TTS
-> 保存消息
-> 反馈页展示
```

下一阶段的目标是把现在的 mock 服务逐步替换成真实服务。需要接入的能力包括：

- ASR：把用户录音转成英文文本。
- ISE：对用户录音进行发音评分。
- Conversation Agent：根据场景、历史对话和用户最新发言生成下一句回复。
- TTS：把 Agent 回复合成为语音，供前端播放。

这些能力应该继续放在后端完成。前端只负责录音、上传、展示消息、展示分数和播放音频。

## 当前代码里的接入位置

后端已经预留了三个语音服务接口：

```text
AsrService
IseService
TtsService
```

当前实现是：

```text
MockAsrService
MockIseService
MockTtsService
```

下一步应新增真实实现，例如：

```text
XfyunAsrService
XfyunIseService
XfyunTtsService
```

对话 Agent 当前也有 mock 实现：

```text
MockSpeakingAgentClient
```

后续可以新增：

```text
LlmSpeakingAgentClient
```

这样可以保持 `SpeakingServiceImpl.submitRecording(...)` 的主流程不变。主流程仍然只关心“调用 ASR、调用 ISE、调用 Agent、调用 TTS”，具体使用 mock 还是第三方接口，由 Spring 配置决定。

## 推荐总流程

用户在会话页点击“开始录音”，浏览器开始录音。

用户点击“停止录音”，前端把录音作为 multipart 表单上传到后端：

```text
POST /api/speaking/sessions/{sessionId}/messages
```

后端收到录音后执行：

```text
1. 保存用户录音文件
2. 调用 ASR，把录音转成英文文本
3. 调用 ISE，得到发音评分
4. 保存 USER 消息，包括转写文本、录音地址、发音分数
5. 调用 Conversation Agent，生成下一句回复
6. 调用 TTS，把 Agent 回复合成音频
7. 保存 AGENT 消息，包括回复文本、提示、音频地址
8. 返回本轮结果给前端
```

前端收到结果后：

```text
1. 展示用户转写文本
2. 展示 Agent 回复
3. 如有 Agent 音频，播放后端返回的音频
4. 如无 Agent 音频，继续使用浏览器 speechSynthesis 兜底播放
```

## 需要先确认的问题

### 1. 音频格式

这是接入语音服务前最重要的问题。

当前前端使用浏览器 `MediaRecorder` 录音。浏览器常见输出格式是：

```text
audio/webm;codecs=opus
```

但讯飞 ASR 和 ISE 不一定接受这种格式。第三方语音服务通常会要求明确的音频格式，例如：

```text
PCM
WAV
MP3
Speex
```

并且可能要求采样率、声道数和编码方式，例如：

```text
16kHz
mono
16bit
```

因此接入前必须确认：

- 讯飞 ASR 接受什么格式。
- 讯飞 ISE 接受什么格式。
- 浏览器当前上传的 webm 是否能直接使用。
- 如果不能直接使用，后端是否使用 FFmpeg 转码。

建议优先采用后端转码方案：

```text
browser webm/opus
-> backend
-> FFmpeg 转成 wav/pcm
-> 调用讯飞 ASR / ISE
```

这样前端录音逻辑可以保持简单，格式适配集中在后端处理。

### 2. ISE 是否有参考文本

ASR 和 ISE 的用途不同。

ASR 负责听懂用户说了什么。
ISE 负责判断用户说得准不准。

ISE 最适合的场景是“跟读”或“朗读”，因为它需要一个参考文本。例如：

```text
参考文本：Could you briefly introduce today's agenda?
用户录音：用户照着读这句话
```

这种情况下，发音评分比较稳定。

当前 conversation 页是自由对话。用户没有固定参考句，想说什么就说什么。为了先跑通流程，可以使用 ASR 转写结果作为 ISE 的参考文本：

```text
用户录音
-> ASR 得到文本
-> 把 ASR 文本作为 ISE referenceText
-> ISE 给出发音分数
```

这个方案可以用于当前阶段，但它不是最终最严谨的发音评测方案。长期更好的设计是把 speaking 分成两类练习：

```text
自由对话：重点是交流能力，使用 ASR + Agent
跟读练习：重点是发音评分，使用固定参考文本 + ISE
```

### 3. 第三方密钥管理

讯飞接口需要：

```text
APPID
APIKey
APISecret
```

这些不能写进 Git，也不能写进普通文档。应通过环境变量或服务器配置注入，例如：

```properties
xfyun.app-id=${XFYUN_APP_ID:}
xfyun.api-key=${XFYUN_API_KEY:}
xfyun.api-secret=${XFYUN_API_SECRET:}
```

本地没有密钥时继续使用 mock。服务器配置密钥并打开开关后，才使用真实服务。

## 配置开关

建议给每个能力单独配置开关：

```properties
xfyun.asr.enabled=false
xfyun.ise.enabled=false
xfyun.tts.enabled=false
agent.llm.enabled=false
```

这样可以逐步接入和排查问题。例如：

```text
第一步只打开 TTS
第二步打开 ASR
第三步打开 ISE
第四步打开真实 Agent
```

不要一口气把所有外部服务都打开。否则一旦流程失败，很难判断是鉴权、音频格式、ASR、ISE、Agent 还是 TTS 出了问题。

## 建议接入顺序

### 第一阶段：接入 TTS

TTS 是最容易验证的能力。

输入是一段文本：

```text
Thanks for clarifying. Could you explain the main risk?
```

输出是一段音频。

接入成功后，Agent 回复不再只靠浏览器 TTS，而是可以播放后端合成的真实语音。

完成标准：

- `XfyunTtsService.synthesize(text)` 可以返回非空音频字节。
- 后端能保存 Agent 音频文件。
- `agentMessage.audioUrl` 有值。
- 前端优先播放后端音频。
- 后端 TTS 失败时，前端仍能用浏览器 TTS 兜底。

### 第二阶段：接入 ASR

ASR 用来把用户录音转成文本。

完成标准：

- 用户停止录音后，后端能把录音发给讯飞 ASR。
- ASR 返回真实英文转写结果。
- USER 消息的 `content` 和 `transcribedText` 使用真实转写文本。
- ASR 失败时，后端返回清楚的错误信息，不再出现笼统的 `Unexpected server error.`。

这一阶段必须重点处理音频格式。如果讯飞不接受浏览器上传的 webm，需要先完成后端转码。

### 第三阶段：接入 ISE

ISE 用来给用户发音打分。

完成标准：

- 后端能把用户录音和参考文本发给讯飞 ISE。
- 能解析讯飞返回的评分结果。
- 能映射到项目现有的 `PronunciationScore`：

```text
totalScore
accuracy
fluency
integrity
speed
```

- USER 消息能保存 `pronunciationScore` 和 `pronunciationDetail`。
- 反馈页能使用真实发音分数，而不是随机 mock 分数。

当前自由对话阶段可以先用 ASR 转写文本作为参考文本。后续如果做跟读练习，再改为使用固定参考文本。

### 第四阶段：接入真实 Conversation Agent

真实 Agent 的输入应包括：

- 当前场景信息。
- 场景角色设定。
- 历史对话。
- 用户最新转写文本。
- 当前轮次。
- 练习目标。
- 可选的即时发音分数。

Agent 的输出应保持结构化：

```json
{
  "content": "string",
  "instantTip": "string"
}
```

不要让前端直接调用大模型。后端负责拼 prompt、调用模型、解析结果、做失败兜底。

完成标准：

- Agent 能根据场景和历史生成自然回复。
- 返回内容长度可控，适合口语练习。
- `instantTip` 简短、具体，不打断对话。
- Agent 失败时使用 mock 或固定回复兜底，保证会话不中断。

### 第五阶段：保存稳定反馈报告

当前 feedback 接口是按请求临时生成 mock 反馈。下一步应改为“交卷时生成并保存”。

建议实现：

```text
POST /api/speaking/sessions/{sessionId}/finish
```

后端在 finish 时：

```text
1. 检查 session 是否属于当前用户
2. 汇总所有 USER 消息
3. 汇总 ASR 转写、ISE 分数、Agent instantTip
4. 调用评分 Agent 生成总结
5. 保存 speaking_feedback
6. 把 session.status 改为 COMPLETED
7. 返回反馈结果
```

这样历史反馈会稳定，不会每次打开反馈页都随机变化。

## 错误处理要求

外部服务接入后，错误会变多。需要把错误分清楚，不要全部变成 `Unexpected server error.`。

建议区分：

- 鉴权失败：密钥错误或过期。
- 音频格式错误：第三方服务无法识别上传音频。
- 音频过长或过大：超过接口限制。
- 第三方服务超时：网络或服务端响应慢。
- 返回格式异常：第三方返回字段和预期不一致。
- 配额不足：讯飞账号余额或调用次数不足。

前端展示可以简洁，但后端日志必须保留具体原因，方便排查。

## 文件存储要求

录音文件和 TTS 音频文件不建议放进数据库。推荐继续使用：

```text
数据库保存音频路径
文件系统或对象存储保存音频文件
```

本地开发可以使用：

```properties
app.speaking.upload-dir=uploads/speaking
```

服务器部署应改成持久目录，例如：

```properties
app.speaking.upload-dir=/var/lib/english-learning-copilot/uploads/speaking
```

如果后续有多台服务器同时运行后端，应考虑对象存储：

```text
MinIO
阿里云 OSS
腾讯 COS
AWS S3
```

## 接入的一些形式

- 采用 WebAPI，不用 SDK。
- 语音转文字：
  - **录音文件转写大模型**：目前选择，非实时转写，个人免费时长5h，最高2并发；
    - APPID、APIKey、APISecret 只放在本地或服务器环境变量中，不写进 Git。
    - 操作文档：https://www.xfyun.cn/doc/spark/asr_llm/Ifasr_llm.html
  - 录音文件转写标准版：个人免费时长5h；
  - 后续开发实时转写，应用 WebSocket。推荐结构：
      浏览器-> WebSocket->你的后端-> WebSocket->讯飞实时语音大模型

- 本地 `.env.local` 配置格式
      本地开发时可以把讯飞录音文件转写配置放在 `backend/.env.local`。这个文件只用于本机开发，已gitignore

  推荐格式：

```bash
# Usage:
#   cd backend
#   source .env.local
#   mvn spring-boot:run

export JAVA_HOME=$(/usr/libexec/java_home -v 21)

export SPRING_DATASOURCE_USERNAME=root
export SPRING_DATASOURCE_PASSWORD='你的本地数据库密码'
export APP_JWT_SECRET='dev-only-change-me-dev-only-change-me-32-bytes'

export XFYUN_ASR_ENABLED=true
export XFYUN_APP_ID=你的APPID
export XFYUN_API_KEY=你的APIKey
export XFYUN_API_SECRET=你的APISecret

export XFYUN_ASR_BASE_URL=https://office-api-ist-dx.iflyaisol.com
export XFYUN_ASR_LANGUAGE=autodialect
export XFYUN_ASR_ACCENT=mandarin
export XFYUN_ASR_RESULT_TYPE=transfer
export XFYUN_ASR_DURATION_CHECK_DISABLE=true
export XFYUN_ASR_POLL_INTERVAL_MS=1000
export XFYUN_ASR_TIMEOUT_MS=30000
export XFYUN_ASR_FILE_NAME=recording.webm
```

说明：

- `XFYUN_ASR_ENABLED=true` 表示后端使用真实讯飞 ASR；如果改成 `false` 或不配置，就继续使用 `MockAsrService`。
- `XFYUN_ASR_FILE_NAME=recording.webm` 表示当前先按浏览器上传的录音文件名提交给讯飞。如果后续发现讯飞不接受 `webm/opus`，再在后端加入转码，把上传给讯飞的文件改成 `recording.wav`。
- 因为文件里已经写了 `export`，启动时使用 `source .env.local` 即可。

- **目前问题**:
  - 采用的不是流式处理，实时语音转文字，而是一次性录入一长条message，再将信息发送给讯飞大模型转文字。该操作费时较久，后续应改进为建立websocket连接，实时传送小粒度音频到讯飞转文字。

## 2026-07-23 超拟人语音合成接入记录

TTS 已切换为讯飞"超拟人语音合成"，默认音色为 `x6_lingxiaoxuan_pro`，采样率为 `24000`。

### 当前流程

```text
创建 session 时
-> mock Agent 生成开场白
-> 后端 TTS 合成 mp3
-> 保存 AGENT 消息和 audioUrl

用户停止录音后
-> ASR 转写
-> ISE 评分
-> mock Agent 生成回复
-> 后端 TTS 合成 mp3
-> 保存 AGENT 消息和 audioUrl
```

前端优先播放后端返回的 `audioUrl`；如果 TTS 没有返回音频，再回退到浏览器 `speechSynthesis`。

### 配置

```bash
XFYUN_TTS_ENABLED=true
XFYUN_TTS_URL=wss://cbm01.cn-huabei-1.xf-yun.com/v1/private/mcd9m97e6
XFYUN_TTS_VOICE=x6_lingxiaoxuan_pro
XFYUN_TTS_SAMPLE_RATE=24000
```

---

## Conversation Agent 接口说明（给接入 LLM 的同学参考）

### 接口定义

当前口语模块的 Agent 接口位于：

```
backend/src/main/java/com/englishlearningcopilot/backend/service/agent/SpeakingAgentClient.java
```

```java
public interface SpeakingAgentClient {

    SpeakingAgentReply reply(SpeakingScenario scenario, List<SpeakingMessage> history, String userMessage, int turnIndex);
}
```

### 返回值

```java
public record SpeakingAgentReply(
        String content,    // Agent 的对话回复文本
        String instantTip  // 即时提示（可选，可为 null）
) { }
```

- `content`：Agent 扮演场景角色回复用户的内容。必填，不能为空。
- `instantTip`：给用户的即时建议，例如语法纠正、更自然的表达方式。可选，不需要时返回 `null`。

### 调用时机

`SpeakingAgentClient.reply(...)` 在 `SpeakingServiceImpl.submitRecording(...)` 中被调用，流程如下：

```text
用户停止录音
-> 保存用户录音文件
-> ASR 转写（得到 transcribedText）
-> ISE 发音评分
-> 保存 USER 消息（含转写文本、发音分数）
-> 调用 SpeakingAgentClient.reply(...)   ← 在这里
-> TTS 合成 Agent 回复音频
-> 保存 AGENT 消息（含回复文本、即时提示、音频地址）
-> 返回本轮结果给前端
```

另外，创建 session 时也会调用一次 `agentClient.reply(...)`，此时 `turnIndex == 0`、`userMessage == ""`、`history` 为空列表。Agent 应返回场景的开场白（`scenario.getOpeningMessage()`）。

### 参数详解

#### 1. `SpeakingScenario scenario` — 当前场景信息

包含以下字段，接入 LLM 时建议全部用于构建 prompt：

| 字段 | 类型 | 说明 | 示例 |
|---|---|---|---|
| `id` | String | 场景唯一标识 | `"business-opening"` |
| `title` | String | 场景标题 | `"商务会议开场"` |
| `description` | String | 场景描述 | `"你正在参加一场跨国商务会议..."` |
| `difficulty` | String | 难度等级 | `"intermediate"` |
| `accent` | String | 推荐口音 | `"american"` |
| `duration` | String | 建议时长 | `"5-8 minutes"` |
| `summary` | String | 场景概要 | `"练习在商务会议中做开场白..."` |
| `tone` | String | 语气风格 | `"professional"` |
| `goal` | String | 练习目标 | `"学会如何自然地开启商务会议..."` |
| `keywords` | String | 关键词（逗号分隔） | `"agenda, objective, timeline, stakeholder"` |
| `rolePrompt` | String | 角色设定 prompt | `"你是一位有经验的会议主持人..."` |
| `openingMessage` | String | 开场白文本 | `"Good morning, everyone. Let's start today's meeting."` |
| `sampleDialogue` | String | 示例对话（可选） | `"A: Good morning... B: Thank you..."` |
| `targetTurns` | int | 建议对话轮数 | `6` |
| `scoringRubric` | String | 评分标准说明 | `"fluency: 考察表达是否流畅..."` |

**建议**：将 `rolePrompt` 作为 system prompt 的核心，将 `goal`、`keywords`、`scoringRubric` 等作为约束条件注入 LLM。

#### 2. `List<SpeakingMessage> history` — 历史消息列表

按 `turnIndex` 升序、`createdAt` 升序排列。每条消息包含：

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | Long | 消息 ID |
| `sender` | enum (USER / AGENT) | 发送者 |
| `content` | String | 消息文本内容 |
| `audioUrl` | String | 音频文件 URL（可能为 null） |
| `transcribedText` | String | ASR 转写文本（仅 USER 消息有值） |
| `pronunciationScore` | Double | 发音总分（仅 USER 消息有值） |
| `pronunciationDetail` | String | 发音评分详情 JSON（仅 USER 消息有值） |
| `instantTip` | String | 即时提示（仅 AGENT 消息有值） |
| `turnIndex` | int | 轮次编号（从 1 开始） |
| `createdAt` | Instant | 创建时间 |

**建议**：将 history 转换为对话格式传入 LLM。USER 消息优先使用 `transcribedText`（如果非空），否则使用 `content`。AGENT 消息使用 `content`。

#### 3. `String userMessage` — 用户最新转写文本

当前轮用户录音经 ASR 转写后的文本。即 `transcribedText`。如果 ASR 失败可能为空字符串。

#### 4. `int turnIndex` — 当前轮次

从 1 开始递增。`turnIndex == 0` 表示创建 session 时的开场白请求。

### 实现要求

#### 1. 实现类命名

新增实现类应命名为 `LlmSpeakingAgentClient`，放在同一包下：

```
backend/src/main/java/com/englishlearningcopilot/backend/service/agent/LlmSpeakingAgentClient.java
```

#### 2. Spring 条件注入

使用 `@ConditionalOnProperty` 控制启用开关，方便本地开发和测试：

```java
@Component
@ConditionalOnProperty(name = "agent.llm.enabled", havingValue = "true")
public class LlmSpeakingAgentClient implements SpeakingAgentClient {
    // ...
}
```

当 `agent.llm.enabled=false` 或未配置时，Spring 自动回退到 `MockSpeakingAgentClient`（当前默认实现）。

#### 3. 配置项

在 `application.properties` 中添加：

```properties
agent.llm.enabled=false
agent.llm.api-key=
agent.llm.model=gpt-4o-mini
agent.llm.temperature=0.7
agent.llm.max-tokens=300
agent.llm.timeout-ms=15000
```

本地开发时可在 `backend/.env.local` 中覆盖：

```bash
export AGENT_LLM_ENABLED=true
export AGENT_LLM_API_KEY=你的APIKey
export AGENT_LLM_MODEL=gpt-4o-mini
```

#### 4. Prompt 构建建议

建议将 prompt 分为以下几层：

**System Prompt**（从 scenario 构建）：
```
你是一个英语口语练习助手，正在扮演以下角色：

{scenario.rolePrompt}

场景：{scenario.title}
场景描述：{scenario.description}
练习目标：{scenario.goal}
语气风格：{scenario.tone}
关键词：{scenario.keywords}

对话要求：
1. 每次回复控制在 1-3 句话，适合口语练习。
2. 使用自然的口语表达，不要过于书面化。
3. 适当引导用户使用场景关键词。
4. 如果用户表达有语法问题，在 instantTip 中给出简短纠正。
5. 不要替用户把话说完，留出空间让用户自己表达。
```

**History 格式**（建议转为多轮对话）：
```
User: {transcribedText or content}
Assistant: {content}
User: ...
Assistant: ...
```

**当前轮输入**：
```
User (current turn): {userMessage}
```

**输出格式要求**（建议使用 JSON mode 或结构化输出）：
```json
{
  "content": "Agent 的对话回复",
  "instantTip": "即时提示（可选，不需要时返回 null）"
}
```

#### 5. 错误处理

- LLM 调用超时或失败时，应记录错误日志，然后回退到 `MockSpeakingAgentClient` 的回复逻辑，或返回一个固定的兜底回复（例如 `"Could you please repeat that?"`），保证会话不中断。
- 不要将 LLM 的原始错误信息直接返回给前端。
- 建议使用重试机制（retry 1-2 次），但总耗时不应超过 15 秒。

#### 6. 测试建议

- 单元测试：mock LLM 客户端，验证 prompt 构建逻辑和回复解析逻辑。
- 集成测试：使用真实 LLM（但使用低 token 消耗的模型），验证端到端流程。
- 手动测试：在本地打开 `agent.llm.enabled=true`，通过前端页面进行真实对话。

### 当前 Mock 实现参考

当前 `MockSpeakingAgentClient` 的实现逻辑：

- `turnIndex == 0`：返回 `scenario.getOpeningMessage()`。
- `turnIndex > 0`：根据 `scenario.getId()` 和 `turnIndex` 返回固定模板回复。
- `instantTip`：根据用户消息长度和标点符号生成简单提示。

接入真实 LLM 后，这些固定回复将被模型生成的动态回复替代。

### 配置开关总览

所有外部服务的开关统一在 `application.properties` 中管理：

```properties
# 语音服务
xfyun.asr.enabled=false
xfyun.ise.enabled=false
xfyun.tts.enabled=false

# 对话 Agent
agent.llm.enabled=false
```

建议接入顺序：先开 TTS → 再开 ASR → 再开 ISE → 最后开 Agent。不要一口气全部打开。
