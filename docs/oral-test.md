# 口语模块测试说明

## 测试代码

口语模块测试集中在：

```text
frontend/test/speaking/
  setup.js
  playwright.config.js
  unit/
  e2e/
  utils/
```

主要测试文件：

- `unit/mockData.test.js`：检查口语 mock 数据字段完整性。
- `unit/SpeakingPage.test.jsx`：检查口语入口页和卡片跳转。
- `unit/SpeakingScenarioDetailPage.test.jsx`：检查情景详情页和历史记录按钮。
- `unit/SpeakingConversationPage.test.jsx`：检查会话页创建后端风格 session、单句回放，以及文本输入框已移除。
- `unit/SpeakingFeedbackPage.test.jsx`：检查反馈页回放弹窗和暂停/开始控制。
- `unit/SpeakingErrorStates.test.jsx`：检查异常数据场景。
- `e2e/speaking-flow.spec.js`：检查真实浏览器中的完整口语流程。

## 测试方式

单元/页面测试使用：

```text
Vitest + React Testing Library + jsdom
```

E2E 测试使用：

```text
Playwright + Chromium + @axe-core/playwright
```

测试中 mock 了浏览器能力：

- `localStorage`
- `speechSynthesis`
- `SpeechSynthesisUtterance`
- `Audio`
- `MediaRecorder`
- `navigator.mediaDevices`

## 测试流程

单元/页面测试覆盖：

- mock 数据结构是否满足页面依赖。
- `/speaking` 是否展示 6 个口语情景。
- 情景详情页是否能进入会话页。
- 历史记录按钮是否根据后端 history 数据启用。
- 会话页是否能创建后端风格 session，并展示后端返回的开场白。
- 会话页是否不再展示打字输入框和“发送”按钮。
- 反馈页是否能打开回放弹窗。
- 回放弹窗是否展示聊天记录和分轮控制。
- 异常数据是否有显式提示，并阻断依赖动作。

E2E 测试覆盖：

- 从口语入口进入情景详情。
- 进入会话页。
- 模拟开始/停止录音 3 轮。
- 点击交卷进入反馈页。
- 检查后端风格 session/history 回放数据。
- 打开并操作回放弹窗。
- 检查 AntD Modal 行为。
- 检查移动端布局。
- 检查 critical 级别无障碍问题。
- 检查入口页和回放弹窗截图。

## 异常数据规则

当前异常数据按三类处理：

- 不影响主流程的字段缺失：降级展示。
- 影响主流程的字段缺失：显示错误，并禁用后续动作。
- 外部输入解析失败：`try/catch` 捕获，并给用户可见提示。

已覆盖的异常场景：

- scenario id 不存在。
- `keywords` 缺失。
- `sampleDialogue` 或旧 mock `prompts` 缺失时的降级展示。
- feedback 缺失或字段不完整。
- history/session 查询失败时的错误展示。

## 运行命令

进入前端目录：

```bash
cd frontend
```

运行单元/页面测试：

```bash
npm run test:run
```

运行 E2E 测试：

```bash
npm run test:e2e
```

更新视觉回归截图：

```bash
npm run test:e2e -- --update-snapshots
```

## 测试结果

单元/页面测试：

```text
npm run test:run

Test Files  7 passed (7)
Tests       23 passed (23)
```

E2E 测试：

```text
npm run test:e2e

11 passed
1 skipped
```

说明：

- `1 skipped` 是桌面项目中跳过移动端布局专属测试。
- 同一个移动端布局测试已在 `chromium-mobile` 项目中执行并通过。

## 当前限制

- 没有覆盖真实麦克风授权和真实录音文件。
- 没有后端接口联调。
- 无障碍测试只断言 critical 级别问题为 0。
- 视觉回归截图可能受系统字体和浏览器环境影响。
