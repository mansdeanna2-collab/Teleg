# Telegram for Android — 带后台管理系统

基于 [Telegram Android 官方源码](https://github.com/DrKLO/Telegram) 的定制版本，集成了完整的后台管理系统（Admin Server），支持用户管理、内容审核、频道/群组管理、系统配置和公告推送等功能。

## 📋 项目概览

| 组件 | 技术栈 | 说明 |
|------|--------|------|
| **Android 客户端** | Java / Android SDK 35 / Gradle | 基于 Telegram v12.5.1，新增 Admin API 对接 |
| **后台管理服务** | Spring Boot 3.2.5 / Java 17 / H2 | 完整的管理后台，含 Web Dashboard |
| **数据库** | H2（开发）/ PostgreSQL（生产） | JPA/Hibernate 自动建表 |
| **安全认证** | JWT + BCrypt + Spring Security | 管理端 JWT 认证，客户端 API 免认证 |

## 🏗️ 系统架构

```
┌─────────────────────┐         ┌─────────────────────────────┐
│   Android 客户端     │  HTTP   │      Admin Server           │
│  (Telegram Fork)    │◄───────►│   (Spring Boot 3.2.5)       │
│                     │         │                             │
│  AdminApiClient.java│         │  /api/client/* (免认证)      │
│  AdminConfig.java   │         │  /api/* (JWT 认证)           │
└─────────────────────┘         │  Web Dashboard (内嵌)        │
                                └──────────┬──────────────────┘
                                           │
                                     ┌─────▼─────┐
                                     │  Database  │
                                     │ H2 / PgSQL │
                                     └───────────┘
```

### 客户端与服务端交互流程

```
1. 用户启动 App → 调用 /api/client/register 注册/更新用户信息
2. 定时心跳 (5min) → 调用 /api/client/heartbeat 检查封禁状态
3. 用户举报 → 调用 /api/client/report 提交举报
4. 获取公告 → 调用 /api/client/announcements 拉取公告
5. 获取配置 → 调用 /api/client/config 获取远程配置
```

## 🚀 快速启动

### 1. 启动后台管理服务

```bash
cd admin-server
./gradlew bootRun
```

访问管理面板: http://localhost:8080
- 默认用户名: `admin`
- 默认密码: `admin123`

### 2. 构建 Android 客户端

**前提条件**: Android Studio, Android SDK 35, Android NDK 21.4.7075529

```bash
# 克隆项目
git clone <本仓库地址>

# 用 Android Studio 打开项目 (注意: 是"打开"不是"导入")
# 配置 release.keystore 和 google-services.json 后构建
```

> **客户端连接配置**: 默认连接 `http://10.0.2.2:8080`（Android 模拟器 → 宿主机 localhost）。生产环境请修改 `AdminConfig.java` 中的 `DEFAULT_SERVER_URL`。

### 3. Docker 部署（后台服务）

```bash
cd admin-server
./gradlew bootJar
docker build -t telegram-admin .
docker run -p 8080:8080 \
  -e ADMIN_PASSWORD=your_secure_password \
  -e JWT_SECRET=your_jwt_secret_key_at_least_64_chars \
  -e DB_PASSWORD=your_db_password \
  telegram-admin
```

## 📁 项目结构

```
Teleg/
├── admin-server/                    # 后台管理服务 (Spring Boot)
│   ├── src/main/java/org/telegram/admin/
│   │   ├── controller/              # REST API 控制器 (8个)
│   │   ├── model/                   # JPA 数据模型 (8个)
│   │   ├── repository/              # 数据仓库 (8个)
│   │   ├── service/                 # 业务逻辑层 (9个)
│   │   ├── security/                # JWT 认证过滤器
│   │   ├── config/                  # 安全配置 & 数据初始化
│   │   └── dto/                     # 数据传输对象
│   ├── src/main/resources/
│   │   ├── application.yml          # 服务配置
│   │   └── static/                  # Web Dashboard (HTML/JS/CSS)
│   ├── Dockerfile                   # 容器化部署
│   └── build.gradle                 # Gradle 构建配置
│
├── TMessagesProj/                   # Android Telegram 客户端
│   └── src/main/java/org/telegram/messenger/
│       ├── AdminApiClient.java      # Admin API HTTP 客户端
│       ├── AdminConfig.java         # 后台连接配置管理
│       └── BuildVars.java           # 构建变量 (API ID/Hash)
│
├── TMessagesProj_App/               # 主应用构建模块
├── TMessagesProj_AppStandalone/     # 独立版构建模块
├── TMessagesProj_AppHuawei/         # 华为版构建模块
├── Dockerfile                       # Android 构建环境容器
└── build.gradle                     # 根构建配置
```

## 🔧 功能模块

### 后台管理功能

| 模块 | 功能 | 状态 |
|------|------|------|
| 🔐 认证系统 | JWT Token 认证、管理员登录、密码修改 | ✅ |
| 📊 仪表盘 | 统计概览（用户数、频道数、群组数、待处理举报等） | ✅ |
| 👥 用户管理 | 搜索、创建、编辑、封禁/解封、限制、删除用户 | ✅ |
| 📢 频道管理 | 搜索、创建、编辑、暂停/激活、删除频道 | ✅ |
| 👨‍👩‍👧‍👦 群组管理 | 搜索、创建、编辑、暂停/激活、删除群组 | ✅ |
| 📣 公告管理 | 创建、编辑、启用/停用公告，客户端可拉取 | ✅ |
| 🛡️ 内容审核 | 查看举报、处理（警告/禁言/封号/删除内容）、驳回 | ✅ |
| ⚙️ 系统配置 | 添加、编辑、删除系统配置项（远程配置下发） | ✅ |
| 📋 审计日志 | 所有管理员操作记录追溯 | ✅ |

### Android 客户端对接功能

| 功能 | API 端点 | 说明 |
|------|----------|------|
| 用户注册 | `POST /api/client/register` | 首次启动时注册，支持手机号自动关联 |
| 心跳检测 | `POST /api/client/heartbeat` | 定时上报活跃状态，获取封禁/限制信息 |
| 状态查询 | `GET /api/client/user-status` | 支持 Telegram ID 和手机号双重查询 |
| 举报提交 | `POST /api/client/report` | 提交垃圾信息/滥用/暴力/欺诈等举报 |
| 公告获取 | `GET /api/client/announcements` | 获取当前活跃公告列表 |
| 配置获取 | `GET /api/client/config` | 获取单个远程配置值 |
| 配置列表 | `GET /api/client/configs` | 获取所有客户端配置 |

## 🔒 安全说明

### ⚠️ 生产部署前必须修改

| 配置项 | 默认值 | 环境变量 | 说明 |
|--------|--------|----------|------|
| 管理员密码 | `admin123` | `ADMIN_PASSWORD` | **必须修改** |
| JWT 密钥 | 内置默认值 | `JWT_SECRET` | **必须修改**，至少 64 字符 |
| 数据库密码 | `admin123` | `DB_PASSWORD` | **必须修改** |
| CORS 域名 | `localhost` | `CORS_ORIGINS` | 生产环境设置实际域名 |
| H2 控制台 | 开启 | 配置 `application.yml` | 生产环境应关闭 |
| Admin Server URL | `http://10.0.2.2:8080` | 修改 `AdminConfig.java` | 改为生产服务器地址 |

### 安全架构

- **管理端**: JWT 令牌认证（24小时有效期），BCrypt 密码加密，CSRF 禁用（RESTful 无状态）
- **客户端 API**: 免认证（`/api/client/**`），用于 App 注册和状态上报
- **审计追踪**: 所有管理员操作均记录到 AuditLog（含操作人、操作类型、目标、IP 地址）
- **封禁机制**: 支持临时封禁（带过期时间自动解封）和永久封禁

## 📊 数据模型

| 实体 | 表名 | 主要字段 |
|------|------|----------|
| AppUser | `app_users` | telegramId, phoneNumber, status(ACTIVE/BANNED/RESTRICTED/DELETED), banReason, banExpiresAt |
| Admin | `admins` | username, password(BCrypt), role(SUPER_ADMIN/ADMIN/MODERATOR) |
| Channel | `channels` | channelId, title, status, memberCount, isVerified |
| ChatGroup | `chat_groups` | groupId, title, groupType(BASIC/SUPERGROUP), memberCount |
| Announcement | `announcements` | title, content, type, priority, isActive, expiresAt |
| ModerationReport | `moderation_reports` | reportType(SPAM/ABUSE/VIOLENCE/FRAUD), status(PENDING/RESOLVED), actionTaken |
| SystemConfig | `system_configs` | configKey, configValue, configType, category |
| AuditLog | `audit_logs` | adminUsername, action, targetType, targetId, details |

## 🛠️ 环境配置

### 环境变量

| 变量名 | 默认值 | 说明 |
|--------|--------|------|
| `DB_USERNAME` | `sa` | 数据库用户名 |
| `DB_PASSWORD` | `admin123` | 数据库密码 |
| `JWT_SECRET` | 内置 | JWT 签名密钥 |
| `ADMIN_USERNAME` | `admin` | 默认管理员用户名 |
| `ADMIN_PASSWORD` | `admin123` | 默认管理员密码 |
| `ADMIN_EMAIL` | `admin@telegram.local` | 管理员邮箱 |
| `CORS_ORIGINS` | `http://localhost:8080,http://localhost:3000` | CORS 允许域名 |
| `SPRING_PROFILES_ACTIVE` | - | 设置为 `production` 启用生产模式 |

### 切换为 PostgreSQL

修改 `admin-server/src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/telegram_admin
    driver-class-name: org.postgresql.Driver
    username: postgres
    password: your_password
  jpa:
    database-platform: org.hibernate.dialect.PostgreSQLDialect
```

需在 `build.gradle` 中添加 PostgreSQL 驱动依赖:
```gradle
runtimeOnly 'org.postgresql:postgresql'
```

## 📖 详细文档

- [后台管理服务详细文档](admin-server/README.md) - 完整 API 接口文档、数据库说明、部署指南

## 📄 基于 Telegram 开源协议

本项目基于 [Telegram for Android](https://github.com/DrKLO/Telegram) 开源代码，遵循 GNU GPL v2 协议。

### 注意事项

1. [**获取自己的 api_id**](https://core.telegram.org/api/obtaining_api_id) 用于你的应用。
2. **不要** 使用 Telegram 名称 - 确保用户知道这是非官方版本。
3. **不要** 使用 Telegram 官方 Logo。
4. 请学习 [**安全指南**](https://core.telegram.org/mtproto/security_guidelines)，妥善保护用户数据隐私。
5. 请记得 **公开你的代码** 以遵守开源协议。

### 相关文档

- Telegram API 文档: https://core.telegram.org/api
- MTproto 协议文档: https://core.telegram.org/mtproto
