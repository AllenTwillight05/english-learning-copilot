# 词汇模块前端设计说明

## 1. 当前交付范围

词汇模块已经从单一入口页扩展为一个完整的前端练习闭环：

- 词汇练习主页：展示记忆留存率、今日练习进度、难度入口和单词本入口。
- 词汇练习页：支持三种题型、题型切换、答题反馈、单词卡、FSRS 四档评分和结束练习。
- 练习总结页：展示本次练习题数、正确数、错误数和评分占比环状图。
- 单词本页：支持查看学过的单词和收藏的单词。
- 数据访问已按项目 service 层结构接入，页面不直接依赖 mock 数据。

当前实现以 mock 数据支持前端独立开发，后续接 Spring Boot 时只需要对齐 `contracts.js` 中的数据结构。

## 2. 页面与路由

| 页面 | 路由 | 文件 | 说明 |
| --- | --- | --- | --- |
| 词汇主页 | `/vocabulary` | `frontend/src/pages/VocabularyPage.jsx` | 展示练习概览、难度入口、单词本入口 |
| 词汇练习页 | `/vocabulary/practice/:level` | `frontend/src/pages/VocabularyPracticePage.jsx` | 根据难度进入练习，会话数据前端临时累计 |
| 练习总结页 | `/vocabulary/result` | `frontend/src/pages/VocabularyResultPage.jsx` | 展示本次练习统计，数据通过路由 state 传入 |
| 单词本页 | `/vocabulary/wordbook` | `frontend/src/pages/VocabularyWordbookPage.jsx` | 展示学过单词和收藏单词 |

路由配置集中在：

```text
frontend/src/router/routes.jsx
```

## 3. 页面职责

### 3.1 词汇主页

文件：

```text
frontend/src/pages/VocabularyPage.jsx
```

主要能力：

- 获取 `vocabularyMemory`，展示当前记忆留存率和统计卡片。
- 获取 `vocabularyPracticeProgress`，展示今日练习环形进度。
- 提供四个练习等级入口：
  - 入门
  - 基础
  - 中级
  - 进阶
- 点击等级后跳转到：

```text
/vocabulary/practice/:level
```

- 点击单词本后跳转到：

```text
/vocabulary/wordbook
```

### 3.2 词汇练习页

文件：

```text
frontend/src/pages/VocabularyPracticePage.jsx
```

主要能力：

- 调用 `vocabulary.getVocabularyPracticeWords({ level })` 获取练习词表。
- 支持三种题型：
  - 英文选中文
  - 中文选英文
  - 语音选中文
- 右上角按钮可在同一个单词下切换题型。
- 答题后显示正确/错误反馈。
- 答题后展示单词卡。
- 单词卡支持：
  - 完整中文释义
  - 英文释义
  - Collins / Oxford / BNC / FRQ
  - 词形变化
  - 英音/美音播放
  - 收藏按钮
  - FSRS 四档评分：重来、困难、良好、简单
- 评分支持键盘快捷键：
  - `1`：重来
  - `2`：困难
  - `3`：良好
  - `4`：简单
- 点击结束练习后进入练习总结页。

当前练习记录在前端会话中临时累计。后续建议在结束练习时提交后端，由后端持久化并更新 FSRS。

### 3.3 练习总结页

文件：

```text
frontend/src/pages/VocabularyResultPage.jsx
```

主要能力：

- 展示本次练习总题数。
- 展示答对数量。
- 展示答错数量。
- 展示四档评分占比环状图：
  - 重来
  - 困难
  - 良好
  - 简单
- 支持返回词汇主页。
- 支持按当前等级再练一次。

当前数据来源：

```text
VocabularyPracticePage
  -> 前端累计 answerRecords
  -> buildSummary()
  -> navigate("/vocabulary/result", { state })
  -> VocabularyResultPage 读取 location.state
```

刷新结果页会丢失本次 state，因此后续应由后端保存练习 session 并返回正式 summary。

### 3.4 单词本页

文件：

```text
frontend/src/pages/VocabularyWordbookPage.jsx
```

主要能力：

- 调用 `vocabulary.getVocabularyWordbookWords()` 获取单词本列表。
- 支持两个 tab：
  - 学过的单词
  - 收藏的单词
- 单词条目展示：
  - 单词
  - 音标
  - 简短中文释义
  - 英文释义
  - 标签
  - 发音按钮
  - 收藏/取消收藏

当前收藏切换只在前端本地状态中生效。后续建议补充后端收藏接口。

## 4. 组件拆分

词汇模块组件统一放在：

```text
frontend/src/components/Vocabulary/
```

组件遵循“一个小组件一个文件”的结构，不把整页封装成大组件。

| 组件 | 文件 | 说明 |
| --- | --- | --- |
| 记忆留存率面板 | `MemoryRetentionPanel.jsx` | 展示留存率、已掌握、可能遗忘、今日待练 |
| 今日练习进度环 | `PracticeProgressRing.jsx` | 展示如“20 题中已完成 8 题” |
| 难度入口卡 | `VocabularyLevelCard.jsx` | 单个练习等级按钮 |
| 单词本入口 | `VocabularyWordbookButton.jsx` | 主页单词本按钮 |
| 英文选中文题 | `EnglishToChineseQuestion.jsx` | 英文题干，中文选项 |
| 中文选英文题 | `ChineseToEnglishQuestion.jsx` | 中文题干，英文选项 |
| 语音选中文题 | `AudioToChineseQuestion.jsx` | 发音按钮，中文选项 |
| 练习单词卡 | `VocabularyWordCard.jsx` | 答题后展示完整词典信息和 FSRS 评分 |
| 总结评分环图 | `VocabularyRatingDonut.jsx` | 展示四档评分占比 |
| 单词本 tab | `VocabularyWordbookTabs.jsx` | 学过/收藏切换 |
| 单词本条目 | `VocabularyWordbookItem.jsx` | 单词本中的单个单词 |

## 5. 数据服务

词汇模块通过 `useAppServices()` 获取数据，不在页面中直接 import mock 数据。

当前已接入的 service 方法：

```js
vocabulary.getVocabularyMemory()
vocabulary.getVocabularyPracticeProgress()
vocabulary.getVocabularyPracticeWords({ level })
vocabulary.getVocabularyWordbookWords()
```

相关文件：

```text
frontend/src/services/contracts.js
frontend/src/services/mockData.js
frontend/src/services/mockServices.js
frontend/src/services/endpoints.js
frontend/src/services/httpServices.js
```

### 5.1 vocabularyMemory

用途：词汇主页记忆留存率和统计卡片。

接口路径：

```text
GET /api/vocabulary/memory
```

结构：

```js
{
  retentionLabel: "string",
  retentionRate: 0,
  stats: [{ value: "string", label: "string" }]
}
```

### 5.2 vocabularyPracticeProgress

用途：词汇主页今日练习进度环。

接口路径：

```text
GET /api/vocabulary/practice-progress
```

结构：

```js
{
  completed: 0,
  total: 0
}
```

### 5.3 vocabularyPracticeWords

用途：词汇练习页题目数据来源。

接口路径：

```text
GET /api/vocabulary/practice-words
```

当前前端调用时会传入 `{ level }`，后端后续可按难度返回不同词表。

结构：

```js
[
  {
    id: "string",
    word: "string",
    phonetic: "string",
    definition: "string",
    briefTranslation: "string",
    translation: "string",
    collins: "string",
    oxford: "string",
    tag: "string",
    bnc: "string",
    frq: "string",
    exchange: "string",
    uk_audio: "string",
    us_audio: "string",
    chineseOptions: ["string"],
    englishOptions: ["string"]
  }
]
```

字段说明：

- `translation`：完整中文释义，只在单词卡中展示。
- `briefTranslation`：简短中文释义，用于题干和正确答案判断。
- `chineseOptions`：中文题目选项，应使用简短中文，不带词性。
- `englishOptions`：英文题目选项。
- `uk_audio` / `us_audio`：英音/美音音频地址。

### 5.4 vocabularyWordbookWords

用途：单词本页展示学过单词和收藏单词。

接口路径：

```text
GET /api/vocabulary/wordbook-words
```

结构：

```js
[
  {
    id: "string",
    word: "string",
    phonetic: "string",
    definition: "string",
    briefTranslation: "string",
    tag: "string",
    us_audio: "string",
    favorited: true
  }
]
```

## 6. 当前前端状态管理

### 6.1 练习会话状态

`VocabularyPracticePage` 内部维护：

- 当前题号 `questionIndex`
- 当前题型 `questionTypeIndex`
- 用户选中的答案 `selectedAnswer`
- 用户选中的评分 `selectedRating`
- 本次答题记录 `answerRecords`

点击下一题时，当前题结果会写入 `answerRecords`。

点击结束练习时，会把当前已作答题目也纳入统计，然后跳转总结页。

### 6.2 收藏状态

当前收藏状态有两种表现：

- 练习页单词卡：组件内部维护临时收藏状态。
- 单词本页：页面内部维护 `words` 状态，切换收藏后仅当前页面生效。

后续接后端时，建议将收藏操作改为接口：

```text
PATCH /api/vocabulary/words/{id}/favorite
```

## 7. 后端建议

后续词汇模块建议由后端负责这些数据和操作：

- 按难度返回练习词表。
- 接收练习会话提交。
- 保存每题作答结果。
- 保存 FSRS 四档评分。
- 根据评分计算下次复习时间。
- 更新错题本。
- 更新单词本和收藏状态。
- 计算记忆留存率。
- 计算今日练习进度。

推荐新增接口：

```text
POST /api/vocabulary/practice-sessions
PATCH /api/vocabulary/words/{id}/favorite
GET /api/vocabulary/mistakes
GET /api/vocabulary/wordbook-words
GET /api/vocabulary/practice-progress
GET /api/vocabulary/memory
```

其中 `POST /api/vocabulary/practice-sessions` 建议接收：

```js
{
  level: "starter",
  answers: [
    {
      wordId: "string",
      questionType: "englishToChinese",
      selectedAnswer: "string",
      correct: true,
      rating: "良好"
    }
  ]
}
```

后端返回：

```js
{
  total: 0,
  correct: 0,
  wrong: 0,
  ratings: {
    重来: 0,
    困难: 0,
    良好: 0,
    简单: 0
  }
}
```

## 8. 开发约定

后续继续开发词汇模块时遵循：

- 先加小组件，再加 page。
- 页面负责组合组件和页面状态。
- 临时 UI 配置优先放 page。
- 只有明确需要后端数据时，才加入 service、mock、contract 和 endpoint。
- 题目用简短中文字段，不展示完整词典释义。
- 单词卡才展示完整词典信息。
- service 方法命名使用模块语义，例如 `getVocabularyPracticeWords()`。

## 9. 验证方式

开发后至少运行：

```bash
cd frontend
npm run build
```

当前已知构建提示：

- Ant Design chunk 较大，Vite 会提示超过 500 kB。
- 这是当前依赖体积提示，不影响页面功能。
