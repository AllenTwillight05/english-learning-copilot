# Codex 任务：口语模块 — Mock Agent 替换为大模型驱动

> 你的工作目录是 C:\SE26Project-13

---

## 先读这三份参考文件

1. `docs/口语对话素材总汇.md` — 按场景分类的对话样本（IELTS Part 1/2/3、G-01~G-12 通用场景、VOA 剧情对话）
2. `docs/口语模块-技术方案.md` — 技术架构、接口设计、阶段规划
3. `docs/口语场景分类参考.md` — 场景分类体系、YAML 模板、System Prompt 设计思路、API 契约

---

## 任务目标

把 `MockSpeakingAgentClient` 替换成真的大模型 Agent。

### 当前状态

```java
// SpeakingAgentClient.java — 接口已定义，不用改
public interface SpeakingAgentClient {
    SpeakingAgentReply reply(SpeakingScenario scenario, List<SpeakingMessage> history, String userMessage, int turnIndex);
}

// MockSpeakingAgentClient.java — 目前是硬编码回复，需要替换
@Component
public class MockSpeakingAgentClient implements SpeakingAgentClient {
    // 按场景 switch，根据 turnIndex % 3 返回固定话术
}
```

### 要实现的

新建 `RealSpeakingAgentClient.java`，实现 `SpeakingAgentClient` 接口：

1. **构建 System Prompt** — 根据 `SpeakingScenario` 对象的字段动态组装
   - 角色定义（scenario.rolePrompt）
   - 场景描述（scenario.description + goal）
   - 对话规则（输出格式、语言约束、边界处理）
   - 级别适配说明
   - Few-shot 示例（scenario.sampleDialogue）
   - 评分标准（scenario.scoringRubric）

2. **调用大模型 API**
   - 构建 messages 数组：[system prompt, 历史对话..., 用户最新输入]
   - 调用 Qwen/DeepSeek 的 chat completion API
   - 从返回中提取回复内容
   - 返回 `SpeakingAgentReply(content, instantTip)`

2. **instantTip 生成** — 基于用户输入的简短实时建议
   - 如果用户回答太短（<5 个词）："Try adding a reason or example to your answer."
   - 如果用户回答包含中文混用："Try to express this idea in English."
   - 其他情况返回 null

---

## System Prompt 模板（每个场景复用此结构）

```
## 角色定义
{scenario.rolePrompt}

## 当前场景
{scenario.description}
目标：{scenario.goal}

## 对话规则
- 每轮回复 1-3 句话
- 不使用 Markdown、emoji、特殊符号
- 用户说中文时，用英语提醒 "Please speak English"
- 用户问无关问题时，礼貌地引回当前场景
- 达到 {scenario.targetTurns} 轮后，准备自然地结束对话

## 级别适配
初始级别由场景 difficulty 决定：
- A1-A2：简短短句，基础词汇
- B1-B2：完整句子，适当细节
- 根据用户回答长度自动升降级

## 评分标准
{scenario.scoringRubric}

## 示例对话
{scenario.sampleDialogue}
```

---

## 具体场景 ID 列表

从 `docs/口语分类场景参考.md` 和 `docs/口语对话素材总汇.md` 提取：

### IELTS 场景
| ID | 标题 | Part | targetTurns |
|----|------|------|-------------|
| ielts-p1-work-study | IELTS Part 1 — Work/Study | 1 | 6 |
| ielts-p1-hometown | IELTS Part 1 — Hometown | 1 | 6 |
| ielts-p1-hobbies | IELTS Part 1 — Hobbies | 1 | 6 |
| ielts-p2-person | IELTS Part 2 — Describe a person | 2 | 2 |
| ielts-p2-event | IELTS Part 2 — Describe an event | 2 | 2 |
| ielts-p3-education | IELTS Part 3 — Education | 3 | 5 |

### 通用场景
| ID | 标题 | 级别 | targetTurns |
|----|------|------|-------------|
| G-01-airport | Airport Check-in | A1-B2 | 8 |
| G-02-restaurant | Restaurant Ordering | A1-A2 | 8 |
| G-03-hotel | Hotel Check-in | A1-B2 | 8 |
| G-04-shopping | Shopping | A1-A2 | 8 |
| G-05-clinic | Clinic Visit | A1-A2 | 8 |
| G-06-job-interview | Job Interview | A1-B1 | 8 |
| G-07-business-meeting | Business Meeting | B1-B2 | 10 |
| G-08-small-talk | Small Talk | A1-A2 | 8 |
| G-09-presentation | Giving a Presentation | B1-B2 | 8 |
| G-10-phone-call | Making a Phone Call | A1-A2 | 6 |
| G-11-directions | Asking for Directions | A1-A2 | 8 |
| G-12-rent | Renting an Apartment | A1-A2 | 8 |

---

## 数据填充

`SpeakingScenario` 实体已经有以下字段可以用来存 prompt 组件：

```java
rolePrompt      // 角色定义（length=2000）
openingMessage  // 开场白（length=1000）  
sampleDialogue  // Few-shot 示例（length=4000）
scoringRubric   // 评分标准（length=2000）
description     // 场景描述
goal            // 目标
keywords        // 关键词
difficulty      // 级别
targetTurns     // 目标轮次
```

需要写一个数据初始化脚本（或者 SQL），从 `docs/口语对话素材总汇.md` 中提取内容填充这些字段。

---

## 不需要改的部分

- `SpeakingAgentClient` 接口（保持 `reply()` 签名不变）
- `SpeakingServiceImpl`（它已经依赖注入 `SpeakingAgentClient`，直接切换实现就行）
- 前端代码
- 数据库表结构

只需要：
1. 新建 `RealSpeakingAgentClient.java`
2. 新建或修改初始化数据，填充 19 个场景的 prompt 字段
3. 确保 `@Component` / `@Primary` 让 Spring 注入新的实现

---

## 大模型 API 配置

使用上海交通大学模型服务，**仅限校内网络访问**：

| 项目 | 值 |
|------|-----|
| Base URL | `https://models.sjtu.edu.cn/api/v1` |
| API Key | `sk-PRnW2uK3s_v6s4WGKxlk2g` |
| 模型（推荐） | `deepseek-chat`（DeepSeek V4 Flash） |
| 备选模型 | `qwen3.6-27b`（Qwen3.6-27B） |
| 限流 | 10 req/min, 100K tokens/min |

API 兼容 OpenAI 格式，调用示例：

```bash
curl https://models.sjtu.edu.cn/api/v1/chat/completions \
  -H "Authorization: Bearer sk-PRnW2uK3s_v6s4WGKxlk2g" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "deepseek-chat",
    "messages": [{"role": "user", "content": "Hello"}]
  }'
```

`RealSpeakingAgentClient` 用这个 endpoint 调用大模型。建议在 `application.yml` 里配成：

```yaml
ai:
  endpoint: https://models.sjtu.edu.cn/api/v1
  api-key: sk-PRnW2uK3s_v6s4WGKxlk2g
  model: deepseek-chat
```
