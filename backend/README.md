# 后端说明

## 技术栈

- Java 21
- Spring Boot 3.4
- Spring Data JPA
- Spring Security
- JWT Bearer token
- MySQL
- H2 测试数据库

## 启动方式

在 `backend/` 目录执行：

```bash
mvn spring-boot:run
```

项目使用 Java 21。若本机同时安装多个 JDK，请先切到 JDK 21：

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
mvn spring-boot:run
```

后端使用 Spring 环境变量读取本地配置，不依赖提交到仓库的 `.env` 文件。本地开发时，可以在当前终端 `export` 环境变量，或者启动前 `source` 自己的私有配置文件。

示例：

```bash
export SPRING_DATASOURCE_USERNAME=root
export SPRING_DATASOURCE_PASSWORD='你的密码'
export APP_JWT_SECRET='dev-only-change-me-dev-only-change-me-32-bytes'

mvn spring-boot:run
```

## 数据库

默认连接地址：

```text
jdbc:mysql://localhost:3306/english_learning_copilot?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&createDatabaseIfNotExist=true
```

支持通过环境变量覆盖：

```bash
SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/english_learning_copilot?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&createDatabaseIfNotExist=true
SPRING_DATASOURCE_USERNAME=root
SPRING_DATASOURCE_PASSWORD=你的密码
APP_JWT_SECRET=至少32个字符的HMAC密钥
APP_JWT_EXPIRATION_MINUTES=120
```

首次运行建议先手动建库：

```sql
CREATE DATABASE english_learning_copilot
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;
```

当前 JPA 使用 `ddl-auto=update`，数据库连接成功后会自动创建和更新表结构。

## 初始管理员

公开注册接口只会创建 `USER`。如果需要在本地开发环境启动时自动创建管理员，可以设置：

```bash
APP_ADMIN_SEED_ENABLED=true
APP_ADMIN_SEED_USERNAME=admin
APP_ADMIN_SEED_EMAIL=admin@example.com
APP_ADMIN_SEED_PASSWORD=Admin123456
APP_ADMIN_SEED_DISPLAY_NAME=Administrator
```

## 认证接口

- `POST /api/auth/register`：注册普通用户，返回 `token` 和 `user`
- `POST /api/auth/login`：使用用户名或邮箱登录，返回 `token` 和 `user`
- `GET /api/auth/me`：根据 `Authorization: Bearer <token>` 返回当前用户
- `POST /api/auth/logout`：无状态登出占位接口，前端负责清理本地 token

## 管理端接口

所有 `/api/admin/**` 接口都要求 `ADMIN` 角色。

用户管理：

- `GET /api/admin/users`
- `PATCH /api/admin/users/{id}/role`
- `PATCH /api/admin/users/{id}/status`

业务占位接口：

- `/api/admin/question-types`
- `/api/admin/question-banks`
- `/api/admin/vocabulary-entries`

这些接口已经接入权限控制，目前返回 `501 NOT_IMPLEMENTED` 占位响应。后续题型、题库、词条等业务负责人只需要替换对应业务实现，不需要重新接入 Security。

## 测试

后端测试命令：

```bash
mvn test
```

测试使用 H2 数据库，覆盖：

- 注册成功和密码哈希存储
- 重复用户名/邮箱拒绝注册
- 登录成功和错误密码拒绝
- `/api/auth/me` 的 token 保护
- `USER` 禁止访问 admin 接口
- `ADMIN` 可以访问用户管理和预留接口

## 说明

- 前端认证闭环已接入，包含登录/注册页面、token 存储和 Bearer 请求头注入；具体前端规约见 `docs/auth-frontend-spec.md`。
- 本地开发可以临时使用 MySQL `root` 账号，但日常协作更推荐为本项目创建单独数据库用户。
