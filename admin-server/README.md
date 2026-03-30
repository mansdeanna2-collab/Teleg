# Telegram Admin Server (后台管理系统)

管理 Telegram 前端应用的后端后台管理系统，提供用户管理、频道/群组管理、内容审核、公告管理、系统配置等功能。

## 功能列表

| 模块 | 功能 | 状态 |
|------|------|------|
| 🔐 认证系统 | JWT Token 认证、管理员登录 | ✅ |
| 📊 仪表盘 | 统计概览（用户数、频道数、群组数、待处理举报等） | ✅ |
| 👥 用户管理 | 搜索、查看、封禁/解封、限制、删除用户 | ✅ |
| 📢 频道管理 | 搜索、查看、编辑、暂停/激活、删除频道 | ✅ |
| 👨‍👩‍👧‍👦 群组管理 | 搜索、查看、暂停/激活、删除群组 | ✅ |
| 📣 公告管理 | 创建、编辑、启用/停用公告 | ✅ |
| 🛡️ 内容审核 | 查看举报、处理（警告/禁言/封号）、驳回 | ✅ |
| ⚙️ 系统配置 | 添加、编辑、删除系统配置项 | ✅ |
| 📋 审计日志 | 查看所有管理员操作记录 | ✅ |
| 📱 客户端API | 用户注册/心跳、状态检查、举报提交、公告获取 | ✅ |

## 技术栈

- **后端**: Spring Boot 3.2.5 (Java 17)
- **数据库**: H2 (开发环境，可切换为 PostgreSQL)
- **认证**: JWT (JSON Web Token)
- **前端**: HTML5 + CSS3 + Vanilla JavaScript (内嵌于 Spring Boot)
- **安全**: Spring Security + BCrypt 密码加密

## 快速启动

### 方式一：直接运行

```bash
cd admin-server
gradle bootRun
```

### 方式二：构建后运行

```bash
cd admin-server
gradle bootJar
java -jar build/libs/admin-server.jar
```

### 方式三：Docker

```bash
cd admin-server
gradle bootJar
docker build -t telegram-admin .
docker run -p 8080:8080 telegram-admin
```

### 访问管理面板

- **URL**: http://localhost:8080
- **默认账号**: admin
- **默认密码**: admin123

## API 接口文档

### 认证 API

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/auth/login` | 管理员登录 |
| POST | `/api/auth/change-password` | 修改密码 |

### 仪表盘 API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/dashboard/stats` | 获取统计数据 |

### 用户管理 API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/users` | 搜索用户列表 |
| GET | `/api/users/{id}` | 获取用户详情 |
| POST | `/api/users/{id}/ban` | 封禁用户 |
| POST | `/api/users/{id}/unban` | 解封用户 |
| POST | `/api/users/{id}/restrict` | 限制用户 |
| DELETE | `/api/users/{id}` | 删除用户 |

### 频道管理 API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/channels` | 搜索频道列表 |
| GET | `/api/channels/{id}` | 获取频道详情 |
| POST | `/api/channels` | 创建频道 |
| PUT | `/api/channels/{id}` | 更新频道 |
| POST | `/api/channels/{id}/suspend` | 暂停频道 |
| POST | `/api/channels/{id}/activate` | 激活频道 |
| DELETE | `/api/channels/{id}` | 删除频道 |

### 群组管理 API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/groups` | 搜索群组列表 |
| GET | `/api/groups/{id}` | 获取群组详情 |
| POST | `/api/groups` | 创建群组 |
| PUT | `/api/groups/{id}` | 更新群组 |
| POST | `/api/groups/{id}/suspend` | 暂停群组 |
| POST | `/api/groups/{id}/activate` | 激活群组 |
| DELETE | `/api/groups/{id}` | 删除群组 |

### 公告管理 API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/announcements` | 获取公告列表 |
| GET | `/api/announcements/{id}` | 获取公告详情 |
| POST | `/api/announcements` | 创建公告 |
| PUT | `/api/announcements/{id}` | 更新公告 |
| POST | `/api/announcements/{id}/activate` | 激活公告 |
| POST | `/api/announcements/{id}/deactivate` | 停用公告 |

### 内容审核 API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/reports` | 获取举报列表 |
| GET | `/api/reports/{id}` | 获取举报详情 |
| POST | `/api/reports/{id}/resolve` | 处理举报 |
| POST | `/api/reports/{id}/dismiss` | 驳回举报 |

### 系统配置 API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/configs` | 获取所有配置 |
| GET | `/api/configs/category/{cat}` | 按分类获取配置 |
| POST | `/api/configs` | 创建/更新配置 |
| PUT | `/api/configs/{id}` | 更新配置值 |
| DELETE | `/api/configs/{id}` | 删除配置 |

### 审计日志 API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/audit-logs` | 获取审计日志 |

### 客户端 API (无需认证)

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/client/register` | 客户端注册/更新用户 |
| POST | `/api/client/heartbeat` | 客户端心跳 |
| GET | `/api/client/user-status` | 检查用户状态 |
| POST | `/api/client/report` | 提交举报 |
| GET | `/api/client/announcements` | 获取活跃公告 |
| GET | `/api/client/config` | 获取配置值 |
| GET | `/api/client/configs` | 获取客户端配置列表 |

## Android 客户端对接

### 配置文件

- `AdminConfig.java` - 管理服务器 URL 和连接配置
- `AdminApiClient.java` - HTTP 客户端，提供所有 API 调用方法

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

// 检查用户状态
AdminApiClient.getInstance().checkUserStatus(telegramId,
    (status, banReason) -> {
        if ("BANNED".equals(status)) {
            // 显示封禁提示
        }
    }
);

// 获取公告
AdminApiClient.getInstance().getAnnouncements(announcements -> {
    // 显示公告列表
});
```

## 数据库

开发环境默认使用 H2 内存数据库。数据库控制台访问：

- **URL**: http://localhost:8080/h2-console
- **JDBC URL**: `jdbc:h2:file:./data/admin-db`
- **Username**: `sa`
- **Password**: `admin123`

切换为 PostgreSQL:

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

## 配置说明

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `server.port` | 8080 | 服务端口 |
| `admin.jwt.secret` | (内置) | JWT 签名密钥 |
| `admin.jwt.expiration-ms` | 86400000 | JWT 过期时间(24小时) |
| `admin.default-admin.username` | admin | 默认管理员用户名 |
| `admin.default-admin.password` | admin123 | 默认管理员密码 |
