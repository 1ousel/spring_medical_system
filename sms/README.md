# 医养结合云端后台管理系统 - 后端

本目录是系统后端工程，基于 Spring Boot 3 + MyBatis-Plus 实现，提供登录认证、用户管理、老人档案、健康预警、评估报告、设备管理、首页统计等 REST API。

## 技术栈

- **Java**：17
- **框架**：Spring Boot 3.2.5
- **权限认证**：Spring Security + JWT
- **ORM**：MyBatis-Plus 3.5.7
- **数据库**：MySQL
- **构建工具**：Maven
- **其他**：Lombok、JJWT

## 目录结构

```text
sms
├── src/main/java/com/medical/sms
│   ├── common          # 通用返回、分页对象
│   ├── config          # 安全、MyBatis 配置
│   ├── controller      # REST 控制器
│   ├── dto             # 请求 DTO
│   ├── entity          # 实体类
│   ├── mapper          # MyBatis-Plus Mapper
│   ├── security        # JWT 认证过滤器
│   ├── util            # 通用工具类
│   └── SmsApplication.java
├── src/main/resources
│   ├── application.yml
│   └── sql/init.sql
└── pom.xml
```

## 环境要求

- JDK 17+
- Maven 3.8+
- MySQL 8+

## 数据库配置

当前配置位于：

```text
src/main/resources/application.yml
```

默认数据库连接示例：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/sms_db?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false
    username: your_username
    password: your_password
```

初始化 SQL：

```text
src/main/resources/sql/init.sql
```

该 SQL 会创建：

- `sys_user`
- `elder`
- `health_alert`
- `assessment_report`
- `device`

系统启动时，`DataInitializer` 会自动初始化以下用户，默认密码均为 `123456`：

| 用户名 | 角色 | 说明 |
|--------|------|------|
| `admin` | `ADMIN` | 系统管理员 |
| `doctor1` | `DOCTOR` | 王建国 |
| `doctor2` | `DOCTOR` | 李晓梅 |
| `doctor3` | `DOCTOR` | 张宇 |
| `nurse1` | `NURSE` | 护士小张 |

## 启动服务

在 `sms` 目录下执行：

```bash
mvn spring-boot:run
```

默认端口：

```text
http://localhost:8088
```

## 打包

```bash
mvn clean package
```

运行打包后的 jar：

```bash
java -jar target/sms-1.0.0.jar
```

## 认证说明

登录接口：

```text
POST /api/auth/login
```

请求体：

```json
{
  "username": "admin",
  "password": "123456"
}
```

成功返回 JWT Token。除 `/api/auth/**` 外，其他接口均需要请求头：

```text
Authorization: Bearer <token>
```

## 主要接口

### 认证

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/auth/login` | 登录 |
| GET | `/api/auth/info` | 获取当前登录用户信息 |

### 首页统计

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/dashboard/stats` | 首页统计数据（含今日预警、本月评估、图表数据） |

### 用户管理

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/user/list` | 用户分页列表 |
| GET | `/api/user/{id}` | 用户详情 |
| POST | `/api/user` | 新增用户 |
| PUT | `/api/user/{id}` | 修改用户 |
| PUT | `/api/user/{id}/toggle` | 启用/禁用用户 |
| PUT | `/api/user/{id}/reset-password` | 重置密码为 `123456` |
| PUT | `/api/user/{id}/change-password` | 修改密码 |
| DELETE | `/api/user/{id}` | 删除用户 |
| GET | `/api/user/doctors` | 医生列表 |

`/api/user/list` 支持查询参数：

- `page`
- `size`
- `keyword`
- `role`

### 老人档案

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/elder/list` | 老人分页列表 |
| GET | `/api/elder/{id}` | 老人详情 |
| POST | `/api/elder` | 新增老人 |
| PUT | `/api/elder/{id}` | 修改老人 |
| DELETE | `/api/elder/{id}` | 删除老人 |
| GET | `/api/elder/count` | 老人总数 |

`/api/elder/list` 支持查询参数：

- `page`
- `size`
- `name`
- `healthStatus`
- `room`

### 健康预警

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/health-alert/list` | 预警分页列表 |
| GET | `/api/health-alert/{id}` | 预警详情 |
| POST | `/api/health-alert` | 新增预警 |
| PUT | `/api/health-alert/{id}/process` | 处理预警 |
| GET | `/api/health-alert/today-count` | 未处理预警数量 |

`/api/health-alert/list` 支持查询参数：

- `page`
- `size`
- `elderId`
- `level`
- `status`

### 评估报告

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/assessment/list` | 评估分页列表 |
| GET | `/api/assessment/{id}` | 评估详情 |
| POST | `/api/assessment` | 新增评估 |
| PUT | `/api/assessment/{id}` | 修改评估 |
| DELETE | `/api/assessment/{id}` | 删除评估 |

`/api/assessment/list` 支持查询参数：

- `page`
- `size`
- `elderId`
- `elderName`
- `assessType`

### 设备管理

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/device/list` | 设备分页列表 |
| GET | `/api/device/{id}` | 设备详情 |
| POST | `/api/device` | 新增设备 |
| PUT | `/api/device/{id}` | 修改设备 |
| DELETE | `/api/device/{id}` | 删除设备 |

`/api/device/list` 支持查询参数：

- `page`
- `size`
- `type`
- `status`

### 算法接口

算法接口由 Java 后端转发至本地 Python 服务（`http://localhost:5000`）。Python 服务未启动时，接口会返回演示数据，响应体中包含 `mock: true` 字段。

启动 Python 服务：

```bash
pip install -r requirements.txt
python app_fall_detection.py
```

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/algorithm/detect` | 目标检测（YOLO 跌倒检测） |
| POST | `/api/algorithm/image-classify` | 图像分类（演示数据） |
| POST | `/api/algorithm/segment` | 图像分割（演示数据） |
| POST | `/api/algorithm/asr` | 语音识别（Whisper） |
| POST | `/api/algorithm/tts` | 语音合成（Edge TTS） |
| POST | `/api/ai/chat/stream` | AI 智能对话（DeepSeek 流式 SSE，需 JWT） |

## 状态字段约定

### 老人健康状态

| 值 | 含义 |
|----|------|
| `good` | 良好 |
| `normal` | 一般 |
| `poor` | 较差 |
| `critical` | 危重 |

### 健康预警状态

| 值 | 含义 |
|----|------|
| `pending` | 未处理 |
| `processing` | 处理中 |
| `handled` | 已处理 |

### 设备状态

| 值 | 含义 |
|----|------|
| `normal` | 正常 |
| `offline` | 离线 |
| `fault` | 故障 |
| `maintaining` | 维护中 |

## 跨域与安全

- 已关闭 CSRF
- 已启用 CORS
- `/api/auth/**` 允许匿名访问
- 其他接口需要有效 JWT

## 注意事项

- Spring Boot 3.x 必须使用 `mybatis-plus-spring-boot3-starter`
- 修改 Java 代码后需要重启后端服务
- 前端开发环境默认代理到 `http://localhost:8088`
