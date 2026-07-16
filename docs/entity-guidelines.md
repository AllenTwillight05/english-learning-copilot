# Entity Guidelines

本文档记录后端 entity 的当前约定，供后续新增数据库实体时保持一致。

## 基本结构

- 位置： `backend/src/main/java/com/englishlearningcopilot/backend/entity/`。
- 每个数据库表对应一个独立 `@Entity` 类。
- 表名使用复数蛇形命名，例如 `speaking_sessions`。
- 字段使用 Java 驼峰命名，数据库列名不一致时用 `@Column(name = "...")` 显式声明。
- 主键字段统一命名为 `id`。

## Lombok 

可以使用 Lombok 减少 getter/setter 代码：

```java
@Getter
@Setter
@NoArgsConstructor
@Entity
public class ExampleEntity {
}
```

当前约定：

- 可以使用 `@Getter`、`@Setter`、`@NoArgsConstructor`。
- 不在 entity 上使用 `@Data`。
- 不在 entity 上自动生成 `@EqualsAndHashCode` 或 `@ToString`。
- 不优先在 entity 上使用 `@Builder`、`@AllArgsConstructor`。

原因：JPA entity 可能包含懒加载关联和代理对象，自动生成的 `equals`、`hashCode`、`toString` 容易触发懒加载、递归输出或持久化状态问题。

## 主键和时间字段

自增主键示例：

```java
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
@Setter(AccessLevel.NONE)
private Long id;
```

创建和更新时间字段由 entity 生命周期方法维护：

```java
@Column(nullable = false, updatable = false, name = "created_at")
@Setter(AccessLevel.NONE)
private Instant createdAt;

@Column(nullable = false, name = "updated_at")
@Setter(AccessLevel.NONE)
private Instant updatedAt;
```

约定：

- 数据库生成或生命周期维护的字段不开放 public setter。
- 使用 `@PrePersist` 初始化 `createdAt` 和 `updatedAt`。
- 使用 `@PreUpdate` 更新 `updatedAt`。
- 时间类型使用 `Instant`。

## 枚举

- 枚举单独放在 `entity` 包下，例如 `UserRole`、`SpeakingSessionStatus`。
- Entity 中枚举字段使用 `EnumType.STRING`，避免数据库存 ordinal 导致后续枚举顺序变更风险。

```java
@Enumerated(EnumType.STRING)
@Column(nullable = false, length = 20)
private SpeakingSessionStatus status;
```

## 关联关系

- 多对一关系优先使用懒加载：

```java
@ManyToOne(fetch = FetchType.LAZY, optional = false)
@JoinColumn(name = "user_id", nullable = false)
private AppUser user;
```

- 不把 entity 直接作为 controller response 返回。
- 对外返回数据时使用 DTO，避免暴露懒加载代理、敏感字段或过大的对象图。
- 新增双向关联前要谨慎评估，当前项目优先使用单向关联和 repository 查询。

## 字段约束

- 必填字段使用 `@Column(nullable = false)`。
- 字符串字段显式设置合理 `length`。
- 唯一约束：用 `@Column(unique = true)` 或在实体类上用@Table注解的 uniqueConstraints 属性来声明。
- 缺省值可以直接在字段上初始化，例如 `private boolean active = true;`。

## 目前实体

口语模块实体拆分：

- `SpeakingScenario`：口语练习场景模板。
- `SpeakingSession`：用户的一次口语练习会话。
- `SpeakingMessage`：会话中的用户或 Agent 消息。

用户模块实体：

- `AppUser`：系统用户、认证信息和权限角色。


