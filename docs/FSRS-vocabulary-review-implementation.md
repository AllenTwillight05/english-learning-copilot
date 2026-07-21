# FSRS 词汇复习后端实现说明

更新时间：2026-07-17

## 当前结论

FSRS 词汇复习链路已经接通：

- 普通词汇练习提交评分后，会把单词加入用户单词本。
- 同一次评分也会写入 `user_word_progress`，创建或更新 FSRS 复习进度。
- 复习页读取 `/api/vocabulary/review-vocabulary`，只返回 `due <= now` 的到期复习词。
- 复习页提交评分会调用 `/api/vocabulary/review`，继续按 FSRS 更新下一次复习时间。

注意：新学单词不会一定立刻出现在复习页。FSRS 首次评分后通常会把 `due` 排到未来，例如 1 天后或更久。复习页只显示已经到期的词，所以“刚刚学完立刻看复习页为空”不一定是错误。

## 关键接口

### 普通练习取词

```http
GET /api/vocabulary/practice-words?level=starter
```

返回字段包含：

```json
{
  "id": 7011,
  "word": "jar",
  "briefTranslation": "广口瓶, 震动, 刺耳声",
  "chineseOptions": ["...", "广口瓶, 震动, 刺耳声"],
  "englishOptions": ["economics", "expound", "aware", "jar"]
}
```

### 普通练习提交评分

```http
POST /api/vocabulary/practice-ratings
```

请求体：

```json
{
  "vocabularyId": 7011,
  "score": 3
}
```

当前行为：

- 写入 `user_wordbook`，表示用户学过该词。
- 调用 FSRS，写入或更新 `user_word_progress`。

### 复习页取到期词

```http
GET /api/vocabulary/review-vocabulary
```

当前行为：

- 只返回当前登录用户 `user_word_progress.due < now` 的词。
- 如果新学词还没有到期，接口不会返回它。

### 复习页提交评分

```http
POST /api/vocabulary/review
```

请求体：

```json
{
  "questionId": "7011",
  "rating": 3
}
```

当前行为：

- 根据 `questionId` 查对应 vocabulary。
- 读取已有 `user_word_progress`。
- 映射成 FSRS `CardState`。
- 调用 `FSRS.review(...)`。
- 保存新的难度、稳定性、间隔、复习次数、遗忘次数、下次 due 时间。

## 数据表

### vocabulary

词汇表当前统一使用 snake_case 业务列：

```text
brief_translation
chinese_options
english_options
```

之前 `data/vocabulary_mysql.sql` 使用过 camelCase 列：

```text
briefTranslation
chineseOptions
englishOptions
```

这会导致 Hibernate 又补出 snake_case 空列，后端读到空数据，前端页面没有选项。已经修复：

- `Vocabulary` Entity 读取 snake_case 列。
- `data/vocabulary_mysql.sql` 建表和 INSERT 字段也改为 snake_case。
- 当前本地 MySQL 已把 camelCase 列数据同步到 snake_case 列。

同步 SQL：

```sql
UPDATE vocabulary
SET brief_translation = briefTranslation,
    chinese_options = chineseOptions,
    english_options = englishOptions
WHERE brief_translation IS NULL
   OR chinese_options IS NULL
   OR english_options IS NULL;
```

同步后检查：

```sql
SELECT COUNT(*) AS total,
       SUM(brief_translation IS NULL) AS brief_nulls,
       SUM(chinese_options IS NULL) AS chinese_nulls,
       SUM(english_options IS NULL) AS english_nulls
FROM vocabulary;
```

期望结果：

```text
total: 14183
brief_nulls: 0
chinese_nulls: 0
english_nulls: 0
```

### user_word_progress

FSRS 进度表字段：

```text
id
user_id
question_id
question_type
difficulty
stability
interval
reps
lapses
state
due
last_review
created_at
updated_at
```

唯一约束：

```text
user_id + question_id + question_type
```

索引：

```text
user_id + question_type + due
```

## 代码改动

### FSRS 算法

文件：

```text
backend/src/main/java/com/englishlearningcopilot/backend/fsrs/FSRS.java
```

说明：

- 从旧包 `com.se26project.fsrs` 迁移到后端主包下。
- 提供 `FSRS.defaultParams()`。
- 提供 `review(CardState, Rating)`。
- `Rating` 和 `CardState` 作为 `FSRS` 的 public nested type，便于 service 调用。

### 词汇 Entity

文件：

```text
backend/src/main/java/com/englishlearningcopilot/backend/entity/Vocabulary.java
```

关键修复：

```java
@Column(name = "brief_translation", length = 100)
private String briefTranslation;

@JdbcTypeCode(SqlTypes.JSON)
@Column(name = "chinese_options", columnDefinition = "json")
private List<String> chineseOptions;

@JdbcTypeCode(SqlTypes.JSON)
@Column(name = "english_options", columnDefinition = "json")
private List<String> englishOptions;
```

### FSRS 进度 Entity

文件：

```text
backend/src/main/java/com/englishlearningcopilot/backend/entity/UserWordProgress.java
```

说明：

- 映射 `user_word_progress`。
- 保存每个用户对每个词的 FSRS 状态。
- `@PrePersist` 和 `@PreUpdate` 维护时间戳。

### Repository

文件：

```text
backend/src/main/java/com/englishlearningcopilot/backend/repository/UserWordProgressRepository.java
```

核心方法：

```java
Optional<UserWordProgress> findByUserIdAndQuestionIdAndQuestionType(
        Long userId,
        String questionId,
        String questionType
);

List<UserWordProgress> findByUserIdAndQuestionTypeAndDueBeforeOrderByDueAsc(
        Long userId,
        String questionType,
        Instant due
);
```

### ReviewService

文件：

```text
backend/src/main/java/com/englishlearningcopilot/backend/service/ReviewService.java
```

核心职责：

- `submitRating(Long userId, String questionId, int rating)`：创建或更新 FSRS 进度。
- `getDueVocabulary(Long userId)`：查到期复习词。
- `getUserIdByUsername(String username)`：给 controller 从 Principal 转 userId。

### VocabularyServiceImpl

文件：

```text
backend/src/main/java/com/englishlearningcopilot/backend/service/impl/VocabularyServiceImpl.java
```

关键改动：

- 注入 `ReviewService`。
- 普通练习提交评分时，不再只写 wordbook。
- 现在会调用：

```java
reviewService.submitRating(user.getId(), String.valueOf(vocabularyId), request.score());
```

这保证新学单词进入 FSRS 进度表。

### ReviewController

文件：

```text
backend/src/main/java/com/englishlearningcopilot/backend/controller/ReviewController.java
```

接口：

```text
GET  /api/vocabulary/review-vocabulary
POST /api/vocabulary/review
```

### 前端练习页

文件：

```text
frontend/src/pages/VocabularyPracticePage.jsx
```

关键修复：

- 普通练习调用 `/api/vocabulary/practice-ratings`。
- 复习模式调用 `/api/vocabulary/review`。

逻辑：

```js
const request = isReview
  ? vocabulary.submitReviewVocabulary({
      questionId: String(currentQuestion.id),
      rating: score
    })
  : vocabulary.submitVocabularyRating({
      vocabularyId: currentQuestion.id,
      score
    });
```

## 验证结果

### 后端聚焦测试

新增测试：

```text
VocabularyPracticeDifficultyIntegrationTest.practiceRatingAddsWordToFsrsReviewProgress
```

验证内容：

- 普通练习评分后，`user_wordbook` 有记录。
- 同时 `user_word_progress` 有对应 FSRS 记录。

运行命令：

```bash
JAVA_HOME=/tmp/jdk21 PATH=/tmp/jdk21/bin:$PATH mvn test -Dtest=VocabularyPracticeDifficultyIntegrationTest
```

结果：

```text
Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

### 本地接口验证

命令：

```bash
curl 'http://localhost:8080/api/vocabulary/practice-words?level=starter'
```

结果已经返回非空：

```json
{
  "briefTranslation": "广口瓶, 震动, 刺耳声",
  "chineseOptions": ["...", "广口瓶, 震动, 刺耳声"],
  "englishOptions": ["economics", "expound", "aware", "jar"]
}
```

## 使用注意

### 新学词为什么不马上出现在复习页

当前 FSRS 的 `nextInterval` 最小值是 1 天。也就是说：

- 用户今天学了一个新词。
- 提交评分后，系统创建 FSRS 记录。
- 该词的 `due` 通常是明天或更晚。
- 今天立刻进入复习页，不一定能看到它。

这是 FSRS 调度策略，不是数据没写入。

如果产品希望“刚学完也能立刻在复习页看到”，需要额外定义一个模式，例如：

- “全部已学词复习”
- “今日新学词巩固”
- 或者把首次学习后的 `due` 设为当前时间

这些都属于产品策略调整，不是当前 FSRS 基础接入的 bug。

### 当前启动方式

当前本地后端用临时 JDK 启动：

```bash
/tmp/jdk21/bin/java -jar target/backend-0.1.0-SNAPSHOT.jar
```

如果 `/tmp` 被清理，需要重新准备 Java 21 JDK，或者安装系统级 JDK 21。
