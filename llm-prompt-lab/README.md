# LLM Prompt Lab

这是一个和 English Learning Copilot 主前端、主后端都解耦的独立调试模块。它不连接 Spring Boot，不写数据库，不改正式口语场景，只用一个 Node.js CLI 直接调用交大 OpenAI-compatible API，方便你先调 system prompt。

日常启动、调试、TTS 输出要求和维护规则见 `USAGE.md`。后续模块用法变化时优先更新那里。

当前按 `docs/口语场景分类参考.md` 的 G 系列通用口语场景编号组织：

- `G-01-airport`: Airport Check-in
- `G-02-restaurant`: Restaurant Ordering
- `G-03-hotel`: Hotel Check-in
- `G-04-shopping`: Shopping
- `G-05-clinic`: Clinic Visit
- `G-06-job-interview`: Job Interview
- `G-07-business-meeting`: Business Meeting
- `G-08-small-talk`: Small Talk
- `G-09-presentation`: Giving a Presentation
- `G-10-phone-call`: Making a Phone Call
- `G-11-directions`: Asking for Directions
- `G-12-renting-apartment`: Renting an Apartment

IELTS 系列现在只保留四个正式入口：

- `IELTS-P1-practice`: Part 1 short-answer topic practice
- `IELTS-P2-practice`: Part 2 cue-card monologue practice
- `IELTS-P3-practice`: Part 3 discussion practice
- `IELTS-mock-test`: Full IELTS Speaking mock test

## 文件结构

- `chat.mjs`: 命令行对话脚本，零第三方依赖。
- `common/roleplay-core.md`: G 系列场景共用的口语训练核心规则。
- `common/ielts-core.md`: IELTS 系列共用的口语考试和教练规则。
- `prompts/*-system.md`: 每个场景独立的 system prompt，只写场景角色、流程、状态和示例。
- `scenarios/*.json`: 每个场景的数据、关键词、开场白、样例对话和测试输入。
- `test-all.mjs`: G 系列批量回归测试。
- `test-ielts.mjs`: IELTS 四个正式入口的批量回归测试。
- `.env.example`: 环境变量模板。实际 `.env` 被 `.gitignore` 忽略。

G 系列运行时实际发送给大模型的 system prompt 是：

```text
common/roleplay-core.md
+ prompts/{scenarioId}-system.md
+ scenarios/{scenarioId}.json 渲染出来的场景变量
```

IELTS 系列运行时实际发送给大模型的 system prompt 是：

```text
common/ielts-core.md
+ prompts/IELTS-P1-practice-system.md 或对应 IELTS 入口 prompt
+ scenarios/IELTS-P1-practice.json 或对应 IELTS 入口场景变量
```

## 配置

在学校网络下使用。不要把真实 key 提交到仓库。

```bash
cd /mnt/c/SE26Project-13/llm-prompt-lab
cp .env.example .env
```

编辑 `.env`：

```bash
SJTU_AI_API_KEY=你的key
SJTU_AI_ENDPOINT=https://models.sjtu.edu.cn/api/v1
SJTU_AI_MODEL=deepseek-chat
SJTU_AI_TEMPERATURE=0.7
SJTU_AI_MAX_TOKENS=180
```

也可以不用 `.env`，直接在 shell 里 export：

```bash
export SJTU_AI_API_KEY='你的key'
export SJTU_AI_ENDPOINT='https://models.sjtu.edu.cn/api/v1'
export SJTU_AI_MODEL='deepseek-chat'
```

## 运行

```bash
cd /mnt/c/SE26Project-13/llm-prompt-lab
node chat.mjs
```

指定场景：

```bash
node chat.mjs --scenario G-03-hotel
node chat.mjs --scenario G-01-airport
node chat.mjs --scenario G-07-business-meeting
node chat.mjs --scenario IELTS-P1-practice
node chat.mjs --scenario IELTS-P2-practice
node chat.mjs --scenario IELTS-P3-practice
node chat.mjs --scenario IELTS-mock-test
```

调整采样参数：

```bash
node chat.mjs --temperature 0.5 --max-tokens 140
```

只查看渲染后的 system prompt，不发 API 请求：

```bash
node chat.mjs --scenario G-03-hotel --print-system
node chat.mjs --scenario IELTS-P1-practice --print-system
```

## 对话命令

- `/help`: 查看命令。
- `/scenarios`: 查看当前可切换的场景 ID。
- `/system`: 打印当前渲染后的 system prompt。
- `/reload`: 重新读取 prompt 和 scenario 文件，适合边改 prompt 边试。
- `/reset`: 清空当前对话历史，保留 system prompt。
- `/save`: 保存当前对话到 `transcripts/`。
- `/exit`: 退出。

## 调 prompt 的建议流程

1. 运行 `node chat.mjs`。
2. 先输入正常酒店入住语句，例如 `I have a reservation under the name John Sandals.`
3. 再测试短回答、语法错误、中文输入、跑题输入。
4. 修改当前场景对应的 prompt，例如 `prompts/G-03-hotel-system.md`。
5. 在 CLI 输入 `/reload`，继续测试。
6. 满意后输入 `/save` 保存 transcript，后续再把成熟 prompt 接回正式口语模块。

## Prompt 结构

G 系列场景 prompt 按“通用核心 + 场景状态机”的方式设计：

- 通用核心：只输出下一轮、英文回复、隐性纠错、中文处理、跑题处理、水平适配。
- 场景状态机：每个场景自己的任务流程，例如酒店入住、机场值机、餐厅点餐。
- 场景边界：每个场景自己的跑题拉回话术。
- 表达帮助：用户不会表达、输入中文或中英混合时，先给简短中文说明和可复用英文表达，再回到角色对话。
- 参考对话：来自 `docs/口语对话素材总汇.md` 的示范，作为风格和流程参考。
- 回归输入：每个 `scenarios/*.json` 里有 `testInputs`，用于后续检查 prompt 是否稳定。

IELTS 系列 prompt 按“四入口 + 共享考试核心”的方式设计：

- `IELTS-P1-practice`: 用户选择 Part 1 话题，逐题练习，每题轻反馈，结束时估分。
- `IELTS-P2-practice`: 用户选择 cue card，完成独白后反馈结构和表达，支持重答和估分。
- `IELTS-P3-practice`: 用户选择抽象讨论话题，练习观点、原因、例子和比较，结束时估分。
- `IELTS-mock-test`: 模拟真实考官流程，过程中不教学、不纠错、不评分，最后统一评估。
- 旧的按单话题拆分入口已经清理，不再作为日常调试对象。

表达帮助模式示例：

```text
用户：我想办理入住。
模型：可以这样说："I'd like to check in, please." 这句话适合到酒店前台办理入住时使用。Do you have a reservation with us?

用户：I want 点菜.
模型：可以这样说："I'd like to order, please." 这是点餐时很自然的开场。Would you like to see the menu?
```
