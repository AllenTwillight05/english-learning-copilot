# LLM Prompt Lab 使用文档

本文档记录 `llm-prompt-lab` 的当前用法和维护规则。后续每次调整这个独立实验模块时，优先同步更新这里。

## 模块目标

`llm-prompt-lab` 是一个和主前端、主后端解耦的口语大模型调试区。

它的当前目标是：

- 独立调试口语对话 system prompt。
- 直接调用交大 OpenAI-compatible API。
- 先跑通大模型文本输出，再接入文本转语音。
- 输出必须尽量是可直接朗读的自然文本。
- 不连接 Spring Boot，不写数据库，不影响正式项目页面。

这个模块不是正式产品入口，也不是后端服务。它是 prompt 和场景数据的实验台。

## 当前能力

当前支持两类口语练习：

- G 系列：日常场景角色扮演。
- IELTS 系列：雅思口语教练模式。

G 系列重点是完成真实任务，例如值机、点餐、入住、购物、看诊。

IELTS 系列重点是考试口语训练，例如 Part 1 短问答、Part 2 独白、Part 3 深度讨论。

## 启动方式

进入模块目录：

```bash
cd /mnt/c/SE26Project-13/llm-prompt-lab
```

运行默认场景：

```bash
node chat.mjs
```

指定场景：

```bash
node chat.mjs --scenario G-03-hotel
node chat.mjs --scenario IELTS-P1-practice
node chat.mjs --scenario IELTS-P2-practice
node chat.mjs --scenario IELTS-P3-practice
node chat.mjs --scenario IELTS-mock-test
```

正常对话时，终端里的 `Agent>` 后面才是大模型面向用户的输出，也是未来可以送入 TTS 的文本。

## 调试 System Prompt

查看完整渲染后的 system prompt：

```bash
node chat.mjs --scenario IELTS-P1-practice --print-system
```

注意：`--print-system` 输出会很长，这是给开发者看的调试内容，不是用户内容，也不应该转语音。

正常用户对话不要加 `--print-system`：

```bash
node chat.mjs --scenario IELTS-P1-practice
```

## 环境变量

`.env` 位于 `llm-prompt-lab/.env`，已经被 `.gitignore` 忽略，不要提交真实 key。

示例：

```bash
SJTU_AI_API_KEY=你的key
SJTU_AI_ENDPOINT=https://models.sjtu.edu.cn/api/v1
SJTU_AI_MODEL=deepseek-chat
SJTU_AI_TEMPERATURE=0.7
SJTU_AI_MAX_TOKENS=180
```

交大 API 只在学校网络下可访问。如果不在学校网络，CLI 可能无法请求模型，但仍然可以用 `--print-system` 检查 prompt 渲染。

## CLI 命令

进入对话后可输入：

```text
/help
/scenarios
/system
/reload
/reset
/save
/exit
```

含义：

- `/help`：查看命令。
- `/scenarios`：列出所有可用场景。
- `/system`：打印当前完整 system prompt。
- `/reload`：重新读取当前场景文件和 prompt 文件，适合边改边试。
- `/reset`：清空当前对话历史。
- `/save`：保存当前对话到 `transcripts/`。
- `/exit`：退出。

## 当前场景

G 系列：

```text
G-01-airport
G-02-restaurant
G-03-hotel
G-04-shopping
G-05-clinic
G-06-job-interview
G-07-business-meeting
G-08-small-talk
G-09-presentation
G-10-phone-call
G-11-directions
G-12-renting-apartment
```

IELTS 系列：

```text
IELTS-P1-practice
IELTS-P2-practice
IELTS-P3-practice
IELTS-mock-test
```

雅思现在分为专项练习和模拟考试。专项练习允许用户选择话题、中文要示范、每题获得轻反馈，并在结束时估分。模拟考试由考官控制题目，过程中不教学，最后统一评估。

## 文件结构

核心文件：

```text
chat.mjs
README.md
USAGE.md
common/
prompts/
scenarios/
```

G 系列：

```text
common/roleplay-core.md
prompts/G-xx-*-system.md
scenarios/G-xx-*.json
```

IELTS 系列：

```text
common/ielts-core.md
prompts/IELTS-P1-practice-system.md
prompts/IELTS-P2-practice-system.md
prompts/IELTS-P3-practice-system.md
prompts/IELTS-mock-test-system.md
scenarios/IELTS-P1-practice.json
scenarios/IELTS-P2-practice.json
scenarios/IELTS-P3-practice.json
scenarios/IELTS-mock-test.json
```

早期按单话题拆分的 IELTS 入口已经清理。日常调试、批量测试和后续接入都只使用上面四个正式入口。

## Prompt 组合规则

G 系列场景的 system prompt 由三部分组成：

```text
common/roleplay-core.md
+ prompts/{scenarioId}-system.md
+ scenarios/{scenarioId}.json 渲染出来的变量
```

IELTS 场景的 system prompt 由三部分组成：

```text
common/ielts-core.md
+ prompts/对应 IELTS 入口的 system.md
+ scenarios/对应 IELTS 入口的 json 渲染出来的变量
```

`chat.mjs` 会根据场景 ID 自动判断：

- `IELTS-P1-practice` 使用 `IELTS-P1-practice-system.md`
- `IELTS-P2-practice` 使用 `IELTS-P2-practice-system.md`
- `IELTS-P3-practice` 使用 `IELTS-P3-practice-system.md`
- `IELTS-mock-test` 使用 `IELTS-mock-test-system.md`
- 其他场景按 `prompts/{scenarioId}-system.md` 加载

任何新的 IELTS 入口都应该先明确是否属于这四类之一。不要再新增按单话题拆分的 IELTS 入口，话题应维护在四个正式场景的 `conversationFlow`、`expressionHelp` 或 `sampleDialogue` 里。

## TTS 输出要求

这个模块的核心约束之一是：模型最终回复要能直接转成语音。

面向用户的 `Agent>` 输出应该满足：

- 像真人口播。
- 短句优先。
- 不使用 Markdown。
- 不使用项目符号。
- 不使用编号列表。
- 不输出 JSON。
- 不输出表格。
- 不输出 `Feedback:`、`Question:`、`Cue card:` 这类标签。
- 不把完整 system prompt 或内部规则暴露给用户。

如果模型实际输出了列表、标题、标签、大段教学内容，就继续压 prompt。

## G 系列测试建议

机场：

```text
Here is my passport and booking reference.
Sorry, here is my 护照 and 机票.
I want 托运行李.
I want 点菜.
```

餐厅：

```text
I want 点菜.
I don't want anything too spicy.
Can I have the bill, please?
```

酒店：

```text
I have a reservation under the name John Sandals.
Here is my 护照.
Do you accept American Express?
I want 点菜.
```

目标行为：

- 当前场景相关的中文或中英混合表达，要先解释并给自然英文。
- 跨场景表达要教一句英文，但不要强行推进当前场景。
- 正常英文输入要保持角色扮演，不变成老师讲课。

## IELTS 测试建议

Part 1 专项练习：

```bash
node chat.mjs --scenario IELTS-P1-practice
```

可选话题：

```text
Work and Study, Hometown, Home and Accommodation, Hobbies, Travel and Holidays, Technology, Food and Cooking, Weather and Seasons, Sports and Exercise, Music and Movies, Shopping
```

Part 2 专项练习：

```bash
node chat.mjs --scenario IELTS-P2-practice
```

可选题卡：

```text
Describe a person you admire, Describe a memorable event, Describe a favorite place, Describe something important that you own, Describe a hobby or activity, Describe a daily routine, Describe a gift you received, Describe a city you visited, Describe a time you helped someone
```

Part 3 专项练习：

```bash
node chat.mjs --scenario IELTS-P3-practice
```

可选话题：

```text
Education, Technology, Environment, Society, Culture, Work and Economy, Family, Media, Health, Travel
```

模拟考试：

```bash
node chat.mjs --scenario IELTS-mock-test
```

专项练习允许用户自由选择内置话题。中文或英文都可以，例如 `我想练环境话题`、`Part 3 culture`、`Part 2 重要物品`。如果用户说了暂时没有内置的问题，模型应该提示可选的相近话题。

模拟考试不让用户选题。模型应该像考官一样按 Part 1、Part 2、Part 3 顺序出题，过程中不主动教学，结束后统一评估。

中文要示范：

```text
我想说生活节奏比较慢，但是交通越来越堵。
我想说我敬佩我的高中物理老师。
我想说现在教育太应试了。
```

英文练习：

```text
I'm from Chengdu.
The traffic become bad.
He is patient and help me.
Technology is good but also bad.
```

结束并评分：

```text
结束，给我评分。
That's all. Please score me.
Can you give me feedback and a band estimate?
```

目标行为：

- Part 1 专项：每题后短反馈、优化表达、继续下一题，结束后估分。
- Part 2 专项：用户完整回答后反馈结构和表达，允许重答，结束后估分。
- Part 3 专项：每题后反馈观点深度和表达，继续追问，结束后估分。
- 模拟考试：过程中不打断、不教学、不每轮评分，结束后统一评估。
- 用户中文或中英混合时，专项练习先把想法转成自然 IELTS 英文。
- 然后要求用户再用英文试一次。
- 用户明确说结束、评分、总结时，再给总评和估分。
- 发音评测暂时不由大模型完成，也不在文本 lab 中评分。
- 输出要适合 TTS 朗读。

## 调 Prompt 的流程

推荐流程：

1. 选择一个场景启动 CLI。
2. 用正常英文输入测试主流程。
3. 用中文或中英混合输入测试表达帮助。
4. 用跨场景输入测试场景判断。
5. 如果输出太长、太像说明书、不适合朗读，改 common prompt。
6. 如果某个场景表达不自然，改对应 scenario JSON 的 `expressionHelp` 或 `sampleDialogue`。
7. CLI 输入 `/reload`，继续测试。
8. 有价值的对话用 `/save` 保存。
9. 修改完成后更新本文档。

## 修改原则

优先级：

1. 先保证最终输出可朗读。
2. 再保证场景角色和任务流程正确。
3. 再保证中文/中英混合时能教学。
4. 最后再补更多表达和示例。

不要做的事：

- 不要把所有失败案例都硬编码成单词规则。
- 不要把 prompt 写成超长说明书。
- 不要让用户实际听到标签、列表或内部规则。
- 不要修改主前端或主后端，除非明确要求接入正式项目。

## 验证命令

检查所有场景 JSON 是否合法：

```bash
cd /mnt/c/SE26Project-13
for f in llm-prompt-lab/scenarios/*.json; do
  node -e "JSON.parse(require('fs').readFileSync(process.argv[1], 'utf8'))" "$f" || exit 1
done
```

检查所有场景 system prompt 是否能渲染：

```bash
cd /mnt/c/SE26Project-13/llm-prompt-lab
for f in scenarios/*.json; do
  id=$(basename "$f" .json)
  node chat.mjs --scenario "$id" --print-system > /tmp/${id}.system.txt || exit 1
  if rg -q '\{\{[A-Z_]+\}\}' /tmp/${id}.system.txt; then
    echo "unrendered template: $id"
    exit 1
  fi
done
```

运行 G 系列联网回归测试：

```bash
cd /mnt/c/SE26Project-13/llm-prompt-lab
node test-all.mjs
```

运行 IELTS 四入口联网回归测试：

```bash
cd /mnt/c/SE26Project-13/llm-prompt-lab
node test-ielts.mjs
```

联网测试会调用交大 API，并把日志写到 `test-outputs/`。如果不在学校网络或没有配置 `SJTU_AI_API_KEY`，先用 `--print-system` 做静态渲染检查。

## 后续计划

短期：

- 继续调 IELTS 四个正式入口的实际 Agent 输出。
- 收集“不适合 TTS”的坏例子。
- 压缩过长反馈。
- 补更多雅思 Part 1 话题。

中期：

- 细化 IELTS mock test 的状态推进和最终评分口径。
- 增加 TOEFL 口语独立实验场景。
- 将优秀 prompt 和场景数据整理成可接入正式口语模块的格式。

长期：

- 再考虑是否接入主项目前端。
- 再考虑是否做自动评测或样本集。
- 再考虑是否 fine-tune。当前阶段优先 prompt 和语料驱动。
