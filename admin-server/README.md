# Telegram Admin Server (后台管理系统)

管理 Telegram 前端应用的后端后台管理系统，提供用户管理、频道/群组管理、内容审核、公告管理、系统配置等功能。

## 功能列表

| 模块 | 功能 | 状态 |
|------|------|------|
| 🔐 认证系统 | JWT Token 认证、管理员登录、密码修改 | ✅ |
| 📊 仪表盘 | 统计概览（用户数、频道数、群组数、待处理举报等） | ✅ |
| 👥 用户管理 | 搜索、创建、编辑、封禁/解封、限制、删除用户 | ✅ |
| 📢 频道管理 | 搜索、创建、编辑、暂停/激活、删除频道 | ✅ |
| 👨‍👩‍👧‍👦 群组管理 | 搜索、创建、编辑、暂停/激活、删除群组 | ✅ |
| 📣 公告管理 | 创建、编辑、启用/停用公告 | ✅ |
| 🛡️ 内容审核 | 查看举报、处理（警告/禁言/封号）、驳回 | ✅ |
| ⚙️ 系统配置 | 添加、编辑、删除系统配置项 | ✅ |
| 📋 审计日志 | 查看所有管理员操作记录 | ✅ |
| 📱 客户端API | 用户注册/心跳、状态检查、举报提交、公告获取、配置获取 | ✅ |

## 技术栈

- **后端**: Spring Boot 3.2.5 (Java 17)
- **数据库**: H2 (开发环境，可切换为 PostgreSQL)
- **认证**: JWT (JSON Web Token) + jjwt 0.12.5
- **前端**: HTML5 + CSS3 + Vanilla JavaScript (内嵌于 Spring Boot)
- **安全**: Spring Security + BCrypt 密码加密
- **构建**: Gradle 8.7

## 快速启动

### 方式一：直接运行

```bash
cd admin-server
./gradlew bootRun
```

### 方式二：构建后运行

```bash
cd admin-server
./gradlew bootJar
java -jar build/libs/admin-server.jar
```

### 方式三：Docker

```bash
cd admin-server
./gradlew bootJar
docker build -t telegram-admin .
docker run -p 8080:8080 telegram-admin
```

### 访问管理面板

- **URL**: http://localhost:8080
- **默认账号**: admin
- **默认密码**: admin123

## API 接口文档

### 认证 API

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/api/auth/login` | 管理员登录 | ❌ |
| POST | `/api/auth/change-password` | 修改密码 | ✅ |

### 仪表盘 API

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/api/dashboard/stats` | 获取统计数据 | ✅ |

### 用户管理 API

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/api/users?keyword=&status=&page=&size=` | 搜索用户列表 | ✅ |
| GET | `/api/users/{id}` | 获取用户详情 | ✅ |
| POST | `/api/users` | 创建用户（管理员手动添加） | ✅ |
| PUT | `/api/users/{id}` | 编辑用户信息 | ✅ |
| POST | `/api/users/{id}/ban` | 封禁用户 | ✅ |
| POST | `/api/users/{id}/unban` | 解封用户 | ✅ |
| POST | `/api/users/{id}/restrict` | 限制用户 | ✅ |
| DELETE | `/api/users/{id}` | 删除用户（软删除） | ✅ |

> **注意**: 管理员创建用户时，`telegramId` 和 `phoneNumber` 至少填写一个。通过手机号预创建的用户，客户端注册时会自动关联。

### 频道管理 API

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/api/channels` | 搜索频道列表 | ✅ |
| GET | `/api/channels/{id}` | 获取频道详情 | ✅ |
| POST | `/api/channels` | 创建频道 | ✅ |
| PUT | `/api/channels/{id}` | 更新频道 | ✅ |
| POST | `/api/channels/{id}/suspend` | 暂停频道 | ✅ |
| POST | `/api/channels/{id}/activate` | 激活频道 | ✅ |
| DELETE | `/api/channels/{id}` | 删除频道 | ✅ |

### 群组管理 API

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/api/groups` | 搜索群组列表 | ✅ |
| GET | `/api/groups/{id}` | 获取群组详情 | ✅ |
| POST | `/api/groups` | 创建群组 | ✅ |
| PUT | `/api/groups/{id}` | 更新群组 | ✅ |
| POST | `/api/groups/{id}/suspend` | 暂停群组 | ✅ |
| POST | `/api/groups/{id}/activate` | 激活群组 | ✅ |
| DELETE | `/api/groups/{id}` | 删除群组 | ✅ |

### 公告管理 API

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/api/announcements` | 获取公告列表 | ✅ |
| GET | `/api/announcements/{id}` | 获取公告详情 | ✅ |
| POST | `/api/announcements` | 创建公告 | ✅ |
| PUT | `/api/announcements/{id}` | 更新公告 | ✅ |
| POST | `/api/announcements/{id}/activate` | 激活公告 | ✅ |
| POST | `/api/announcements/{id}/deactivate` | 停用公告 | ✅ |

### 内容审核 API

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/api/reports` | 获取举报列表（支持 status 过滤） | ✅ |
| GET | `/api/reports/{id}` | 获取举报详情 | ✅ |
| POST | `/api/reports/{id}/resolve` | 处理举报 (action: WARNING/MUTE/BAN/DELETE_CONTENT) | ✅ |
| POST | `/api/reports/{id}/dismiss` | 驳回举报 | ✅ |

### 系统配置 API

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/api/configs` | 获取所有配置 | ✅ |
| GET | `/api/configs/category/{cat}` | 按分类获取配置 | ✅ |
| POST | `/api/configs` | 创建/更新配置 | ✅ |
| PUT | `/api/configs/{id}` | 更新配置值 | ✅ |
| DELETE | `/api/configs/{id}` | 删除配置 | ✅ |

### 审计日志 API

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/api/audit-logs` | 获取审计日志 | ✅ |

### 客户端 API (无需认证)

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/client/register` | 客户端注册/更新用户 |
| POST | `/api/client/heartbeat?telegramId=` | 客户端心跳（返回封禁状态） |
| GET | `/api/client/user-status?telegramId=` | 按 Telegram ID 检查用户状态 |
| GET | `/api/client/user-status?phoneNumber=` | 按手机号检查用户状态 |
| POST | `/api/client/report` | 提交举报 |
| GET | `/api/client/announcements` | 获取活跃公告 |
| GET | `/api/client/config?key=` | 获取单个配置值 |
| GET | `/api/client/configs` | 获取客户端配置列表 |

## Android 客户端对接

### 配置文件

| 文件 | 说明 |
|------|------|
| `AdminConfig.java` | 管理服务器 URL 和连接配置（默认: `http://10.0.2.2:8080`） |
| `AdminApiClient.java` | HTTP 客户端，提供所有 API 调用方法（单例模式, 2线程池） |

### 连接配置

```java
// AdminConfig.java 关键配置
DEFAULT_SERVER_URL = "http://10.0.2.2:8080"  // Android 模拟器指向宿主机
DEFAULT_ENABLED = true                        // 默认启用
DEFAULT_HEARTBEAT_INTERVAL = 300000           // 心跳间隔 5 分钟
```

### 使用示例

```java
// 注册用户
AdminApiClient.getInstance().registerUser(
    telegramId, firstName, lastName, username, phone,
    deviceInfo, appVersion,
    new AdminApiClient.ApiCallback() {
        @Override public void onSuccess(JSONObject response) { /* ... */ }
        @Override public void onError(String error) { /* ... */ }
    }
);

// 心跳检测 (检查封禁状态)
AdminApiClient.getInstance().sendHeartbeat(telegramId,
    new AdminApiClient.HeartbeatCallback() {
        @Override public void onResult(String status, String banReason) {
            if ("BANNED".equals(status)) {
                // 显示封禁提示
            }
        }
        @Override public void onError(String error) {
            // 网络错误等，不影响正常使用
        }
    }
);

// 检查用户状态
AdminApiClient.getInstance().checkUserStatus(telegramId,
    new AdminApiClient.HeartbeatCallback() {
        @Override public void onResult(String status, String banReason) {
            // status: ACTIVE / BANNED / RESTRICTED / DELETED / UNREGISTERED
        }
        @Override public void onError(String error) { /* ... */ }
    }
);

// 获取公告
AdminApiClient.getInstance().getAnnouncements(announcements -> {
    // 显示公告列表 (JSONArray)
});

// 提交举报
AdminApiClient.getInstance().submitReport(
    reporterId, reporterName, reportedUserId, reportedUserName,
    "SPAM", "MESSAGE", "用户在多个群组发送垃圾信息",
    new AdminApiClient.ApiCallback() {
        @Override public void onSuccess(JSONObject response) { /* ... */ }
        @Override public void onError(String error) { /* ... */ }
    }
);

// 获取远程配置
AdminApiClient.getInstance().getConfig("min_app_version", (key, value) -> {
    // value: "12.0.0"
});
```

### 客户端-服务端交互逻辑

1. **用户注册**: 客户端调用 `register`，服务端检查 `allow_registration` 配置，拒绝已封禁/删除用户，支持手机号自动关联管理员预创建用户
2. **心跳机制**: 每5分钟调用 `heartbeat`，服务端更新 `lastActiveAt`，自动解除过期封禁，返回当前封禁/限制状态
3. **封禁执行**: 客户端收到 BANNED/RESTRICTED 状态后，应在 UI 层面限制用户操作
4. **公告推送**: 客户端定期拉取活跃公告，展示系统维护、版本更新等通知
5. **远程配置**: 服务端可下发配置（如最低版本要求），客户端据此强制升级等

## 数据库

开发环境默认使用 H2 文件数据库。数据库控制台访问：

- **URL**: http://localhost:8080/h2-console
- **JDBC URL**: `jdbc:h2:file:./data/admin-db`
- **Username**: `sa`
- **Password**: `admin123`

### 切换为 PostgreSQL

```yaml
# application.yml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/telegram_admin
    driver-class-name: org.postgresql.Driver
    username: postgres
    password: your_password
  jpa:
    database-platform: org.hibernate.dialect.PostgreSQLDialect
```

同时在 `build.gradle` 添加驱动:
```gradle
runtimeOnly 'org.postgresql:postgresql'
```

## 数据模型

### 用户状态流转

```
注册 → ACTIVE ──→ BANNED (封禁，可带过期时间)
          │          │
          │          └→ ACTIVE (自动解封 / 手动解封)
          │
          ├──→ RESTRICTED (限制)
          │
          └──→ DELETED (软删除)
```

### 举报处理流程

```
客户端提交 → PENDING (待处理) ──→ REVIEWING (审核中)
                                    │
                                    ├→ RESOLVED (已处理: WARNING/MUTE/BAN/DELETE_CONTENT)
                                    │
                                    └→ DISMISSED (已驳回)
```

### 预置系统配置

| 配置键 | 默认值 | 类型 | 分类 | 说明 |
|--------|--------|------|------|------|
| `max_message_length` | 4096 | NUMBER | MESSAGING | 最大消息长度 |
| `max_file_size_mb` | 2048 | NUMBER | STORAGE | 最大文件大小(MB) |
| `max_group_members` | 200000 | NUMBER | MESSAGING | 超级群最大成员数 |
| `allow_registration` | true | BOOLEAN | GENERAL | 允许新用户注册 |
| `maintenance_mode` | false | BOOLEAN | GENERAL | 维护模式 |
| `min_app_version` | 12.0.0 | STRING | CLIENT | 最低支持 App 版本 |
| `rate_limit_messages` | 30 | NUMBER | SECURITY | 每分钟消息频率限制 |
| `enable_2fa` | true | BOOLEAN | SECURITY | 启用两步验证 |
| `default_language` | zh | STRING | GENERAL | 默认语言 |
| `push_notification_enabled` | true | BOOLEAN | NOTIFICATION | 推送通知开关 |

## 配置说明

| 配置项 | 默认值 | 环境变量 | 说明 |
|--------|--------|----------|------|
| `server.port` | 8080 | - | 服务端口 |
| `admin.jwt.secret` | (内置) | `JWT_SECRET` | JWT 签名密钥 (生产环境必须修改) |
| `admin.jwt.expiration-ms` | 86400000 | - | JWT 过期时间 (24小时) |
| `admin.default-admin.username` | admin | `ADMIN_USERNAME` | 默认管理员用户名 |
| `admin.default-admin.password` | admin123 | `ADMIN_PASSWORD` | 默认管理员密码 (生产环境必须修改) |
| `admin.default-admin.email` | admin@telegram.local | `ADMIN_EMAIL` | 管理员邮箱 |
| `admin.cors.allowed-origins` | localhost | `CORS_ORIGINS` | CORS 允许域名 |
| `spring.datasource.password` | admin123 | `DB_PASSWORD` | 数据库密码 |

## 安全注意事项

⚠️ **生产部署前必须修改以下默认值**:

1. `ADMIN_PASSWORD` - 修改管理员密码
2. `JWT_SECRET` - 设置强密钥 (至少 64 字符)
3. `DB_PASSWORD` - 修改数据库密码
4. `CORS_ORIGINS` - 限制为实际域名
5. 关闭 H2 控制台 (`spring.h2.console.enabled=false`)
6. 客户端 `AdminConfig.java` 中的 `DEFAULT_SERVER_URL` 改为生产地址
