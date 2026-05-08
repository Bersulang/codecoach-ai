# CodeCoach AI API 接口设计

## 1. 文档说明

本文档用于描述 CodeCoach AI MVP 版本的后端 API 接口设计。

MVP 版本核心业务闭环为：

用户注册登录 -> 创建项目档案 -> 发起项目拷打训练 -> 提交回答 -> AI 生成反馈和追问 -> 结束训练 -> 查看训练报告。

本文档是前后端协作的主要依据。

---

## 2. 通用约定

## 2.1 接口前缀

所有接口统一使用 `/api` 前缀。

示例：

```http
POST /api/auth/login
```

---

## 2.2 请求格式

除文件上传接口外，请求体统一使用 JSON：

```http
Content-Type: application/json
```

---

## 2.3 响应格式

所有接口统一返回以下格式：

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

字段说明：

| 字段 | 类型 | 说明 |
|---|---|---|
| code | number | 业务状态码 |
| message | string | 响应消息 |
| data | any | 响应数据 |

---

## 2.4 成功响应示例

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1
  }
}
```

---

## 2.5 失败响应示例

```json
{
  "code": 400,
  "message": "用户名不能为空",
  "data": null
}
```

---

## 2.6 HTTP 状态码约定

| HTTP 状态码 | 说明 |
|---|---|
| 200 | 请求成功 |
| 400 | 请求参数错误 |
| 401 | 未登录或 Token 无效 |
| 403 | 无权限访问 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |

---

## 2.7 业务状态码约定

| code | 说明 |
|---|---|
| 200 | 成功 |
| 400 | 请求参数错误 |
| 401 | 未登录 |
| 403 | 无权限 |
| 404 | 资源不存在 |
| 500 | 系统异常 |
| 1001 | 用户名已存在 |
| 1002 | 用户名或密码错误 |
| 2001 | 项目不存在 |
| 2002 | 无权访问该项目 |
| 3001 | 训练会话不存在 |
| 3002 | 训练会话已结束 |
| 3003 | AI 调用失败 |
| 4001 | 报告不存在 |

---

## 2.8 认证方式

除注册、登录接口外，其余接口都需要携带 Token。

请求头格式：

```http
Authorization: Bearer <token>
```

示例：

```http
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.xxx
```

---

## 2.9 分页请求参数

分页查询统一使用以下参数：

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|
| pageNum | number | 否 | 1 | 页码 |
| pageSize | number | 否 | 10 | 每页条数 |

---

## 2.10 分页响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [],
    "total": 0,
    "pageNum": 1,
    "pageSize": 10,
    "pages": 0
  }
}
```

字段说明：

| 字段 | 类型 | 说明 |
|---|---|---|
| records | array | 当前页数据 |
| total | number | 总记录数 |
| pageNum | number | 当前页码 |
| pageSize | number | 每页条数 |
| pages | number | 总页数 |

---

# 3. 用户认证接口

## 3.1 用户注册

### 接口说明

用户注册账号。

### 请求方式

```http
POST /api/auth/register
```

### 是否需要登录

否。

### 请求参数

```json
{
  "username": "zhangsan",
  "password": "123456",
  "confirmPassword": "123456"
}
```

### 参数说明

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| username | string | 是 | 用户名，长度 4-20 |
| password | string | 是 | 密码，长度 6-32 |
| confirmPassword | string | 是 | 确认密码 |

### 成功响应

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "userId": 1
  }
}
```

### 失败情况

| 场景 | code | message |
|---|---|---|
| 用户名为空 | 400 | 用户名不能为空 |
| 密码为空 | 400 | 密码不能为空 |
| 两次密码不一致 | 400 | 两次密码不一致 |
| 用户名已存在 | 1001 | 用户名已存在 |

---

## 3.2 用户登录

### 接口说明

用户使用用户名和密码登录系统。

### 请求方式

```http
POST /api/auth/login
```

### 是否需要登录

否。

### 请求参数

```json
{
  "username": "zhangsan",
  "password": "123456"
}
```

### 参数说明

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| username | string | 是 | 用户名 |
| password | string | 是 | 密码 |

### 成功响应

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9.xxx",
    "user": {
      "id": 1,
      "username": "zhangsan",
      "nickname": "张三",
      "role": "USER"
    }
  }
}
```

### 失败情况

| 场景 | code | message |
|---|---|---|
| 用户名为空 | 400 | 用户名不能为空 |
| 密码为空 | 400 | 密码不能为空 |
| 用户名或密码错误 | 1002 | 用户名或密码错误 |
| 用户被禁用 | 403 | 用户已被禁用 |

---

## 3.3 获取当前登录用户

### 接口说明

获取当前登录用户基本信息。

### 请求方式

```http
GET /api/users/me
```

### 是否需要登录

是。

### 请求头

```http
Authorization: Bearer <token>
```

### 成功响应

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "username": "zhangsan",
    "nickname": "张三",
    "avatarUrl": null,
    "role": "USER"
  }
}
```

### 失败情况

| 场景 | code | message |
|---|---|---|
| 未携带 Token | 401 | 未登录 |
| Token 无效 | 401 | 登录状态已失效 |

---

## 3.4 退出登录

### 接口说明

用户退出登录。

MVP 阶段如果 JWT 不做服务端黑名单，可以由前端删除本地 Token；后端接口可选。

### 请求方式

```http
POST /api/auth/logout
```

### 是否需要登录

是。

### 成功响应

```json
{
  "code": 200,
  "message": "success",
  "data": true
}
```

---

# 4. 项目档案接口

## 4.1 创建项目档案

### 接口说明

用户创建一个项目档案。

### 请求方式

```http
POST /api/projects
```

### 是否需要登录

是。

### 请求参数

```json
{
  "name": "高并发营销系统",
  "description": "该项目面向营销活动场景，支持优惠券发放、抽奖和秒杀等业务能力。",
  "techStack": "Spring Boot, MySQL, Redis, RabbitMQ",
  "role": "负责优惠券秒杀模块和库存扣减逻辑",
  "highlights": "使用 Redis 预扣库存，结合 MQ 异步削峰。",
  "difficulties": "需要处理超卖、重复消费、缓存一致性和接口限流等问题。"
}
```

### 参数说明

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| name | string | 是 | 项目名称 |
| description | string | 是 | 项目描述 |
| techStack | string | 是 | 技术栈 |
| role | string | 否 | 负责模块 |
| highlights | string | 否 | 项目亮点 |
| difficulties | string | 否 | 项目难点 |

### 成功响应

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "projectId": 1
  }
}
```

### 失败情况

| 场景 | code | message |
|---|---|---|
| 项目名称为空 | 400 | 项目名称不能为空 |
| 项目描述为空 | 400 | 项目描述不能为空 |
| 技术栈为空 | 400 | 技术栈不能为空 |

---

## 4.2 查询项目列表

### 接口说明

分页查询当前登录用户的项目档案列表。

### 请求方式

```http
GET /api/projects
```

### 是否需要登录

是。

### 查询参数

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|
| keyword | string | 否 | - | 按项目名称搜索 |
| pageNum | number | 否 | 1 | 页码 |
| pageSize | number | 否 | 10 | 每页条数 |

### 请求示例

```http
GET /api/projects?keyword=营销&pageNum=1&pageSize=10
```

### 成功响应

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [
      {
        "id": 1,
        "name": "高并发营销系统",
        "description": "该项目面向营销活动场景...",
        "techStack": "Spring Boot, MySQL, Redis, RabbitMQ",
        "role": "负责优惠券秒杀模块",
        "highlights": "使用 Redis 预扣库存，结合 MQ 异步削峰。",
        "difficulties": "需要处理超卖、重复消费等问题。",
        "createdAt": "2026-05-08 10:00:00",
        "updatedAt": "2026-05-08 10:00:00"
      }
    ],
    "total": 1,
    "pageNum": 1,
    "pageSize": 10,
    "pages": 1
  }
}
```

---

## 4.3 查询项目详情

### 接口说明

查询某个项目档案详情。

### 请求方式

```http
GET /api/projects/{id}
```

### 是否需要登录

是。

### 路径参数

| 参数 | 类型 | 说明 |
|---|---|---|
| id | number | 项目 ID |

### 成功响应

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "name": "高并发营销系统",
    "description": "该项目面向营销活动场景，支持优惠券发放、抽奖和秒杀等业务能力。",
    "techStack": "Spring Boot, MySQL, Redis, RabbitMQ",
    "role": "负责优惠券秒杀模块和库存扣减逻辑",
    "highlights": "使用 Redis 预扣库存，结合 MQ 异步削峰。",
    "difficulties": "需要处理超卖、重复消费、缓存一致性和接口限流等问题。",
    "createdAt": "2026-05-08 10:00:00",
    "updatedAt": "2026-05-08 10:00:00"
  }
}
```

### 失败情况

| 场景 | code | message |
|---|---|---|
| 项目不存在 | 2001 | 项目不存在 |
| 访问其他用户项目 | 2002 | 无权访问该项目 |

---

## 4.4 修改项目档案

### 接口说明

修改当前用户自己的项目档案。

### 请求方式

```http
PUT /api/projects/{id}
```

### 是否需要登录

是。

### 请求参数

```json
{
  "name": "高并发营销系统",
  "description": "更新后的项目描述",
  "techStack": "Spring Boot, MySQL, Redis, RabbitMQ",
  "role": "负责优惠券秒杀模块",
  "highlights": "使用 Redis + Lua 保证库存扣减原子性。",
  "difficulties": "处理超卖、幂等、缓存一致性等问题。"
}
```

### 成功响应

```json
{
  "code": 200,
  "message": "success",
  "data": true
}
```

### 失败情况

| 场景 | code | message |
|---|---|---|
| 项目不存在 | 2001 | 项目不存在 |
| 访问其他用户项目 | 2002 | 无权访问该项目 |
| 必填字段为空 | 400 | 参数错误 |

---

## 4.5 删除项目档案

### 接口说明

逻辑删除当前用户自己的项目档案。

### 请求方式

```http
DELETE /api/projects/{id}
```

### 是否需要登录

是。

### 成功响应

```json
{
  "code": 200,
  "message": "success",
  "data": true
}
```

### 失败情况

| 场景 | code | message |
|---|---|---|
| 项目不存在 | 2001 | 项目不存在 |
| 访问其他用户项目 | 2002 | 无权访问该项目 |

---

# 5. 项目拷打训练接口

## 5.1 创建训练会话

### 接口说明

用户基于一个项目创建项目拷打训练会话。

创建成功后，系统会生成第一道 AI 面试问题。

### 请求方式

```http
POST /api/interview-sessions
```

### 是否需要登录

是。

### 请求参数

```json
{
  "projectId": 1,
  "targetRole": "Java 后端实习",
  "difficulty": "NORMAL"
}
```

### 参数说明

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| projectId | number | 是 | 项目 ID |
| targetRole | string | 是 | 目标岗位 |
| difficulty | string | 是 | 难度：EASY / NORMAL / HARD |

### 成功响应

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "sessionId": 1001,
    "firstQuestion": {
      "messageId": 5001,
      "role": "ASSISTANT",
      "messageType": "AI_QUESTION",
      "content": "请你先介绍一下这个项目的核心业务流程。",
      "roundNo": 1,
      "createdAt": "2026-05-08 10:00:00"
    }
  }
}
```

### 失败情况

| 场景 | code | message |
|---|---|---|
| 项目不存在 | 2001 | 项目不存在 |
| 访问其他用户项目 | 2002 | 无权访问该项目 |
| AI 调用失败 | 3003 | AI 调用失败，请稍后重试 |

---

## 5.2 查询训练会话详情

### 接口说明

查询一次训练会话详情，包括项目信息和历史消息。

### 请求方式

```http
GET /api/interview-sessions/{sessionId}
```

### 是否需要登录

是。

### 路径参数

| 参数 | 类型 | 说明 |
|---|---|---|
| sessionId | number | 训练会话 ID |

### 成功响应

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1001,
    "projectId": 1,
    "projectName": "高并发营销系统",
    "targetRole": "Java 后端实习",
    "difficulty": "NORMAL",
    "status": "IN_PROGRESS",
    "currentRound": 1,
    "maxRound": 5,
    "messages": [
      {
        "id": 5001,
        "role": "ASSISTANT",
        "messageType": "AI_QUESTION",
        "content": "请你先介绍一下这个项目的核心业务流程。",
        "roundNo": 1,
        "createdAt": "2026-05-08 10:00:00"
      }
    ]
  }
}
```

### 失败情况

| 场景 | code | message |
|---|---|---|
| 会话不存在 | 3001 | 训练会话不存在 |
| 访问其他用户会话 | 403 | 无权限访问 |

---

## 5.3 提交回答

### 接口说明

用户提交当前问题的回答，系统生成本轮反馈和下一轮追问。

### 请求方式

```http
POST /api/interview-sessions/{sessionId}/answer
```

### 是否需要登录

是。

### 请求参数

```json
{
  "answer": "这个项目主要面向营销活动场景，我负责优惠券秒杀模块。整体流程是用户参与活动后，系统先校验活动状态和用户资格，然后通过 Redis 预扣库存，最后发送 MQ 异步落库。"
}
```

### 参数说明

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| answer | string | 是 | 用户回答内容 |

### 成功响应

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "userAnswer": {
      "messageId": 5002,
      "role": "USER",
      "messageType": "USER_ANSWER",
      "content": "这个项目主要面向营销活动场景...",
      "roundNo": 1,
      "createdAt": "2026-05-08 10:01:00"
    },
    "aiFeedback": {
      "messageId": 5003,
      "role": "ASSISTANT",
      "messageType": "AI_FEEDBACK",
      "content": "你的回答说明了整体业务流程，但对 Redis 预扣库存的原子性保障讲得还不够具体。",
      "roundNo": 1,
      "createdAt": "2026-05-08 10:01:05"
    },
    "nextQuestion": {
      "messageId": 5004,
      "role": "ASSISTANT",
      "messageType": "AI_FOLLOW_UP",
      "content": "你提到了 Redis 预扣库存，那么如果 Redis 扣减成功，但 MQ 发送失败，你会怎么处理？",
      "roundNo": 2,
      "createdAt": "2026-05-08 10:01:05"
    },
    "finished": false
  }
}
```

### 失败情况

| 场景 | code | message |
|---|---|---|
| 回答为空 | 400 | 回答不能为空 |
| 会话不存在 | 3001 | 训练会话不存在 |
| 会话已结束 | 3002 | 训练会话已结束 |
| AI 调用失败 | 3003 | AI 调用失败，请稍后重试 |

---

## 5.4 结束训练会话

### 接口说明

用户主动结束训练，系统生成训练报告。

### 请求方式

```http
POST /api/interview-sessions/{sessionId}/finish
```

### 是否需要登录

是。

### 成功响应

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "reportId": 9001,
    "sessionId": 1001,
    "totalScore": 78
  }
}
```

### 失败情况

| 场景 | code | message |
|---|---|---|
| 会话不存在 | 3001 | 训练会话不存在 |
| 会话已结束 | 3002 | 训练会话已结束 |
| AI 调用失败 | 3003 | AI 调用失败，请稍后重试 |

---

## 5.5 查询训练历史

### 接口说明

分页查询当前用户的训练历史。

### 请求方式

```http
GET /api/interview-sessions
```

### 是否需要登录

是。

### 查询参数

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|
| projectId | number | 否 | - | 按项目筛选 |
| status | string | 否 | - | 按状态筛选 |
| pageNum | number | 否 | 1 | 页码 |
| pageSize | number | 否 | 10 | 每页条数 |

### 请求示例

```http
GET /api/interview-sessions?projectId=1&status=FINISHED&pageNum=1&pageSize=10
```

### 成功响应

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [
      {
        "id": 1001,
        "projectId": 1,
        "projectName": "高并发营销系统",
        "targetRole": "Java 后端实习",
        "difficulty": "NORMAL",
        "status": "FINISHED",
        "currentRound": 5,
        "maxRound": 5,
        "totalScore": 78,
        "reportId": 9001,
        "createdAt": "2026-05-08 10:00:00",
        "endedAt": "2026-05-08 10:10:00"
      }
    ],
    "total": 1,
    "pageNum": 1,
    "pageSize": 10,
    "pages": 1
  }
}
```

---

# 6. 训练报告接口

## 6.1 查询训练报告

### 接口说明

查询某次训练生成的报告详情。

### 请求方式

```http
GET /api/reports/{reportId}
```

### 是否需要登录

是。

### 路径参数

| 参数 | 类型 | 说明 |
|---|---|---|
| reportId | number | 报告 ID |

### 成功响应

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 9001,
    "sessionId": 1001,
    "projectId": 1,
    "projectName": "高并发营销系统",
    "targetRole": "Java 后端实习",
    "difficulty": "NORMAL",
    "totalScore": 78,
    "summary": "本次训练中，你能够说明项目的基本业务流程，也能提到 Redis 和 MQ 的作用，但对异常场景和一致性问题的回答还不够深入。",
    "strengths": [
      "能够清楚描述项目背景和核心业务流程",
      "能够说明 Redis 预扣库存和 MQ 异步削峰的基本作用"
    ],
    "weaknesses": [
      "对 Redis 扣减成功但 MQ 发送失败的处理方案不够完整",
      "没有充分说明消息重复消费和接口幂等设计"
    ],
    "suggestions": [
      "补充分布式锁、Lua 脚本和库存扣减原子性的知识",
      "准备 MQ 可靠投递、重复消费和最终一致性的回答模板",
      "结合项目补充限流、降级和异常补偿方案"
    ],
    "qaReview": [
      {
        "question": "请你先介绍一下这个项目的核心业务流程。",
        "answer": "这个项目主要面向营销活动场景...",
        "feedback": "回答整体清晰，但技术细节不够深入。"
      }
    ],
    "createdAt": "2026-05-08 10:10:00"
  }
}
```

### 失败情况

| 场景 | code | message |
|---|---|---|
| 报告不存在 | 4001 | 报告不存在 |
| 访问其他用户报告 | 403 | 无权限访问 |

---

# 7. AI 调用日志接口，MVP 可选

## 7.1 查询 AI 调用日志

### 接口说明

查询当前用户的 AI 调用日志。

MVP 阶段该接口可以暂不对前端开放，主要用于后端调试。

### 请求方式

```http
GET /api/ai-call-logs
```

### 是否需要登录

是。

### 查询参数

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|
| pageNum | number | 否 | 1 | 页码 |
| pageSize | number | 否 | 10 | 每页条数 |

### 成功响应

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [
      {
        "id": 1,
        "provider": "deepseek",
        "modelName": "deepseek-chat",
        "requestType": "GENERATE_QUESTION",
        "promptTokens": 800,
        "completionTokens": 120,
        "totalTokens": 920,
        "latencyMs": 2300,
        "success": true,
        "createdAt": "2026-05-08 10:00:00"
      }
    ],
    "total": 1,
    "pageNum": 1,
    "pageSize": 10,
    "pages": 1
  }
}
```

---

# 8. 前后端接口对应关系

## 8.1 登录页

| 页面操作 | 接口 |
|---|---|
| 登录 | `POST /api/auth/login` |

## 8.2 注册页

| 页面操作 | 接口 |
|---|---|
| 注册 | `POST /api/auth/register` |

## 8.3 项目列表页

| 页面操作 | 接口 |
|---|---|
| 加载项目列表 | `GET /api/projects` |
| 搜索项目 | `GET /api/projects?keyword=xxx` |
| 删除项目 | `DELETE /api/projects/{id}` |
| 开始拷打 | `POST /api/interview-sessions` |

## 8.4 项目创建页

| 页面操作 | 接口 |
|---|---|
| 创建项目 | `POST /api/projects` |

## 8.5 项目编辑页

| 页面操作 | 接口 |
|---|---|
| 加载项目详情 | `GET /api/projects/{id}` |
| 保存修改 | `PUT /api/projects/{id}` |

## 8.6 项目拷打训练页

| 页面操作 | 接口 |
|---|---|
| 加载训练会话 | `GET /api/interview-sessions/{sessionId}` |
| 提交回答 | `POST /api/interview-sessions/{sessionId}/answer` |
| 结束训练 | `POST /api/interview-sessions/{sessionId}/finish` |

## 8.7 训练报告页

| 页面操作 | 接口 |
|---|---|
| 查看报告 | `GET /api/reports/{reportId}` |
| 再来一次 | `POST /api/interview-sessions` |

## 8.8 训练历史页

| 页面操作 | 接口 |
|---|---|
| 加载历史记录 | `GET /api/interview-sessions` |
| 查看报告 | `GET /api/reports/{reportId}` |
| 继续训练 | `GET /api/interview-sessions/{sessionId}` |

---

# 9. MVP 接口优先级

## 9.1 P0 必须实现

| 接口 | 说明 |
|---|---|
| `POST /api/auth/register` | 用户注册 |
| `POST /api/auth/login` | 用户登录 |
| `GET /api/users/me` | 获取当前用户 |
| `POST /api/projects` | 创建项目 |
| `GET /api/projects` | 查询项目列表 |
| `GET /api/projects/{id}` | 查询项目详情 |
| `POST /api/interview-sessions` | 创建训练会话 |
| `GET /api/interview-sessions/{sessionId}` | 查询训练会话 |
| `POST /api/interview-sessions/{sessionId}/answer` | 提交回答 |
| `POST /api/interview-sessions/{sessionId}/finish` | 结束训练 |
| `GET /api/reports/{reportId}` | 查询训练报告 |

## 9.2 P1 尽量实现

| 接口 | 说明 |
|---|---|
| `PUT /api/projects/{id}` | 修改项目 |
| `DELETE /api/projects/{id}` | 删除项目 |
| `GET /api/interview-sessions` | 查询训练历史 |
| `POST /api/auth/logout` | 退出登录 |

## 9.3 P2 后续实现

| 接口 | 说明 |
|---|---|
| `GET /api/ai-call-logs` | 查询 AI 调用日志 |
| 文档上传相关接口 | 后续 RAG 版本 |
| 知识库相关接口 | 后续 RAG 版本 |
| Prompt 模板接口 | 后续管理端 |

---

# 10. 接口设计注意事项

## 10.1 用户数据隔离

所有涉及用户业务数据的接口，都必须从 Token 中获取当前用户 ID。

不能信任前端传入的 `userId`。

错误示例：

```json
{
  "userId": 1,
  "projectId": 100
}
```

正确做法：

```text
userId 从登录态中获取；
projectId 从请求参数中获取；
后端查询时同时校验 projectId 和 userId。
```

例如：

```sql
SELECT * FROM project
WHERE id = #{projectId}
  AND user_id = #{currentUserId}
  AND is_deleted = 0;
```

---

## 10.2 AI 接口失败处理

AI 调用失败时，不能让系统数据进入混乱状态。

建议规则：

1. 用户回答先保存；
2. AI 反馈生成失败时返回友好错误；
3. 训练会话可以保持 `IN_PROGRESS`；
4. 用户可以稍后重试；
5. 记录 AI 调用失败日志。

MVP 阶段也可以简化为：

```text
AI 调用失败时，本轮回答不保存，直接返回错误。
```

最终实现时需要在代码层明确选择一种策略。

---

## 10.3 时间格式

所有时间字段统一返回字符串：

```text
yyyy-MM-dd HH:mm:ss
```

示例：

```json
{
  "createdAt": "2026-05-08 10:00:00"
}
```

---

## 10.4 Long 类型 ID 处理

后端使用 `Long` 作为 ID 类型。

如果未来出现前端 JS 精度问题，可以将 ID 序列化为字符串。

MVP 阶段可以先返回 number。

---

## 10.5 接口幂等性

MVP 阶段重点关注以下接口：

| 接口 | 幂等性策略 |
|---|---|
| 注册 | 通过用户名唯一索引避免重复注册 |
| 删除项目 | 逻辑删除，重复删除返回成功或项目不存在均可 |
| 结束训练 | 如果报告已存在，避免重复生成 |
| 提交回答 | MVP 阶段暂不做重复提交幂等，前端按钮禁用防重复提交 |

---

# 11. 后续接口扩展方向

MVP 完成后，可以继续扩展：

1. 文档上传接口；
2. 知识库 CRUD 接口；
3. RAG 问答接口；
4. SSE 流式输出接口；
5. Prompt 模板管理接口；
6. 用户调用额度接口；
7. 管理员后台接口；
8. 系统监控接口。