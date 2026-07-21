# FSRS 语法复习接入说明

更新时间：2026-07-17

## 当前结论

语法做题记录已经接入 FSRS 复习算法，行为和词汇模块保持一致：

- 普通语法练习答题后，仍然通过 `user_grammarbook` 记录错题状态。
- 普通语法练习提交自评后，会写入或更新 `user_word_progress`，并把 `question_type` 标记为 `grammar`。
- 语法复习页读取 `/api/grammar/review-grammar`，只返回当前用户已经到期的语法题。
- 语法复习页继续提交自评时，会复用同一个 `/api/grammar/practice-ratings` 接口，继续按 FSRS 更新下一次复习时间。

注意：FSRS 不是“刚做完马上复习”的队列。首次自评后，题目的 `due` 通常会被排到未来，例如 1 天后或更久。复习页只显示 `due <= now` 的题目，所以刚新学的语法题不立刻出现在复习页是正常行为。

## 数据设计

语法复习复用词汇 FSRS 使用的进度表：

```text
user_word_progress
```

用 `question_type` 区分不同题型：

```text
vocabulary  -> 词汇
grammar     -> 语法
```

语法题的 `question_id` 保存为 `grammar_question.id` 的字符串形式。例如语法题 `id = 40` 时，进度表保存：

```text
user_id: 当前用户 id
question_id: "40"
question_type: "grammar"
```

核心字段含义：

```text
difficulty   FSRS 难度
stability    FSRS 稳定性
interval     当前复习间隔
reps         复习次数
lapses       遗忘次数
state        卡片状态，0 表示 New，1 表示 Review
due          下次到期时间
last_review  上次复习时间
```

唯一约束仍然是：

```text
user_id + question_id + question_type
```

因此同一个用户可以同时拥有：

```text
question_id = "40", question_type = "grammar"
question_id = "40", question_type = "vocabulary"
```

两者互不覆盖。

## 后端接口

### 普通语法练习取题

```http
GET /api/grammar/practice-questions?category={category}
```

当前逻辑不变：按语法分类取当前用户未做过的题。

### 普通语法练习提交答题结果

```http
POST /api/grammar/practice-results
```

请求体：

```json
{
  "grammarQuestionId": 40,
  "incorrect": true
}
```

当前行为：

- 写入或更新 `user_grammarbook`。
- 只负责错题本状态，不负责 FSRS。

### 普通语法练习提交自评

```http
POST /api/grammar/practice-ratings
```

请求体：

```json
{
  "grammarQuestionId": 40,
  "score": 3
}
```

当前行为：

- 校验语法题是否存在。
- 查找当前用户在 `user_word_progress` 中的 `question_type = "grammar"` 进度。
- 如果不存在，则创建一条新的 FSRS 进度。
- 调用 `FSRS.review(...)`。
- 保存新的 `difficulty`、`stability`、`interval`、`reps`、`lapses`、`state`、`due`、`last_review`。
- 不写入 `user_grammarbook`，避免自评和错题本职责混在一起。

### 语法复习取到期题

```http
GET /api/grammar/review-grammar
```

当前行为：

- 需要登录。
- 查询当前用户 `question_type = "grammar"` 且 `due <= now` 的进度。
- 按 `due` 从早到晚排序。
- 根据 `question_id` 回表读取 `grammar_question`。
- 返回和普通练习一致的 `GrammarPracticeQuestionResponse`，所以前端可以复用语法练习页。

未登录访问该接口现在返回 `401`，不会再因为 `Principal` 为空导致 `500`。

## 主要代码结构

### ReviewService

文件：

```text
backend/src/main/java/com/englishlearningcopilot/backend/service/ReviewService.java
```

新增能力：

- `QUESTION_TYPE_GRAMMAR = "grammar"`
- `submitGrammarRating(Long userId, Integer grammarQuestionId, int rating)`
- `getDueGrammar(Long userId)`
- `newProgress(Long userId, String questionId, String questionType)`
- `parseGrammarQuestionId(String questionId)`

`ReviewService` 现在是词汇和语法共用的 FSRS 编排层：

- 词汇用 `question_type = "vocabulary"`。
- 语法用 `question_type = "grammar"`。
- 两者共用 `FSRS.CardState` 映射和 `applyCardState(...)` 保存逻辑。

### GrammarService

文件：

```text
backend/src/main/java/com/englishlearningcopilot/backend/service/GrammarService.java
```

新增方法：

```java
List<GrammarPracticeQuestionResponse> getReviewQuestions(String username);
```

### GrammarServiceImpl

文件：

```text
backend/src/main/java/com/englishlearningcopilot/backend/service/impl/GrammarServiceImpl.java
```

关键变化：

- 注入 `ReviewService`。
- `getReviewQuestions(...)` 获取当前用户后调用 `reviewService.getDueGrammar(user.getId())`。
- `submitRating(...)` 从原来的“只接受评分”改为真正写入 FSRS：

```java
reviewService.submitGrammarRating(user.getId(), request.grammarQuestionId(), request.score());
```

### GrammarController

文件：

```text
backend/src/main/java/com/englishlearningcopilot/backend/controller/GrammarController.java
```

新增接口：

```java
@GetMapping("/review-grammar")
public List<GrammarPracticeQuestionResponse> getReviewQuestions(Principal principal)
```

评分接口的语义也已经更新为“提交语法自评并更新 FSRS 复习进度”。

### SecurityConfig

文件：

```text
backend/src/main/java/com/englishlearningcopilot/backend/config/SecurityConfig.java
```

补充了需要登录的复习接口：

```java
.requestMatchers(HttpMethod.GET, "/api/vocabulary/review-vocabulary").authenticated()
.requestMatchers(HttpMethod.GET, "/api/grammar/review-grammar").authenticated()
```

这样前端未携带 token 时会拿到明确的 `401`。

### 前端

语法前端已有复习入口和接口封装，不需要新增页面：

```text
frontend/src/pages/GrammarPracticePage.jsx
frontend/src/services/endpoints.js
frontend/src/services/httpServices.js
```

复习路由会调用：

```javascript
grammar.getReviewGrammar()
```

语法自评会调用：

```javascript
grammar.submitGrammarRating({
  grammarQuestionId: currentQuestion.id,
  score: ratingScores[rating]
})
```

因此后端接通后，语法普通练习和语法复习都会走同一套 FSRS 评分更新逻辑。

## 测试覆盖

新增和调整的集成测试在：

```text
backend/src/test/java/com/englishlearningcopilot/backend/GrammarNotebookIntegrationTest.java
```

覆盖点：

- 语法自评会创建 `user_word_progress` 记录。
- 新记录使用 `question_type = "grammar"`。
- 语法自评不会写入 `user_grammarbook`。
- 语法复习接口只返回已经到期的语法题。
- 未到期语法题不会出现在复习列表。

已验证命令：

```bash
JAVA_HOME=/tmp/jdk21 PATH=/tmp/jdk21/bin:$PATH mvn test -Dtest=GrammarNotebookIntegrationTest
```

结果：

```text
Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
```

完整后端测试也已通过：

```bash
JAVA_HOME=/tmp/jdk21 PATH=/tmp/jdk21/bin:$PATH mvn test
```

结果：

```text
Tests run: 17, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## 本地运行状态

后端已重新打包并用最新 jar 启动：

```text
backend/target/backend-0.1.0-SNAPSHOT.jar
```

当前监听：

```text
localhost:8080
```

启动日志显示 Spring Boot 已成功启动，未登录访问：

```http
GET /api/grammar/review-grammar
```

返回：

```text
HTTP/1.1 401
```

这说明接口已经进入安全链路，后续需要前端携带登录 token 才能正常获取当前用户的到期语法题。
