# CodeCoach AI

CodeCoach AI 是一个面向 Java 后端求职者的 AI 项目面试训练平台。

当前项目处于 MVP 版本，核心目标是跑通“项目拷打训练闭环”：用户创建项目档案，基于项目发起训练，AI 生成追问，用户回答后 AI 继续反馈与追问，训练结束后生成报告，并沉淀历史记录。

## 项目简介

很多实习、校招和初级开发者有项目经历，但在面试中容易出现“做过但讲不清”“八股和项目脱节”“不知道面试官会怎么追问”等问题。

CodeCoach AI 尝试把项目经历结构化保存下来，并通过大模型模拟项目面试追问，帮助用户训练项目表达、技术选型说明、异常场景分析和复盘能力。

## 核心功能

MVP 当前覆盖：

- 用户注册、登录与 JWT 鉴权
- 获取当前登录用户信息
- 项目档案 CRUD
- 基于项目创建 AI 面试训练会话
- 多轮回答、AI 反馈与追问
- 最后一轮回答后自动结束训练并生成报告
- 手动结束训练并生成报告
- 查询训练会话详情、训练历史和训练报告
- OpenAI-Compatible 大模型 API 接入
- Mock AI 模式，用于无真实模型配置时的本地联调
- Redis 防重复提交锁，降低慢模型响应导致的重复回答和重复 AI 调用

MVP 暂不包含：

- 简历 / PDF / Markdown 文档上传解析
- RAG 知识库
- 语音或视频面试
- 支付系统
- 企业组织管理
- 复杂管理员后台

## 技术栈

后端：

- Java 21
- Spring Boot 3.x
- Spring MVC
- Spring Security
- JWT
- MyBatis-Plus
- MySQL 8
- Redis 7
- Maven
- springdoc-openapi

前端：

- React 19
- TypeScript
- Vite
- React Router
- Axios
- Zustand
- Ant Design

AI：

- 统一 `AiInterviewService` 接口
- OpenAI-Compatible Chat Completions API
- 本地 Mock AI 实现

部署与环境：

- Docker Compose
- MySQL
- Redis
- 后续可接入 Nginx 托管前端与反向代理

## 系统架构

MVP 采用前后端分离的单体架构：

```text
React 前端
    |
    | HTTP / JSON
    v
Spring Boot 后端
    |
    | MyBatis-Plus / JDBC
    v
MySQL
    |
    | RedisTemplate
    v
Redis
    |
    | OpenAI-Compatible API
    v
外部大模型服务
```

后端按业务模块组织：

- `auth`：注册、登录
- `user`：当前用户信息
- `project`：项目档案
- `interview`：训练会话和消息
- `report`：训练报告
- `ai`：AI 能力抽象与实现

## 本地开发环境

建议版本：

| 工具 | 版本 |
|---|---|
| JDK | 21 |
| Maven | 3.9.x |
| Node.js | 20 LTS 或更新 |
| Docker Desktop | 可用版本 |
| MySQL | 8.x，推荐通过 Docker Compose 启动 |
| Redis | 7.x，推荐通过 Docker Compose 启动 |

## 启动方式

### 1. 启动 MySQL 和 Redis

```bash
cd deploy
docker compose up -d
```

默认服务：

- MySQL：`localhost:3306`
- Redis：`localhost:6379`
- 数据库：`codecoach_ai`
- MySQL root 密码：`123456`

首次启动数据库后，手动执行初始化 SQL：

```bash
cd ../backend
mysql -h 127.0.0.1 -P 3306 -uroot -p123456 codecoach_ai < src/main/resources/sql/schema.sql
```

如需插入示例数据，可再执行：

```bash
mysql -h 127.0.0.1 -P 3306 -uroot -p123456 codecoach_ai < src/main/resources/sql/data.sql
```

### 2. 配置本地 AI 参数

在 `backend` 目录下创建 `application-local.yml`，或使用 `backend/src/main/resources/application-local.yml`。该文件已被 Git 忽略，不要提交真实 API Key。

示例：

```yaml
ai:
  provider: openai-compatible
  openai-compatible:
    base-url: https://your-api-base-url/v1
    api-key: your-api-key
    model: your-model-name
    timeout-seconds: 30
```

也可以通过环境变量配置：

```bash
export AI_PROVIDER=openai-compatible
export AI_BASE_URL=https://your-api-base-url/v1
export AI_API_KEY=your-api-key
export AI_MODEL=your-model-name
export AI_TIMEOUT_SECONDS=30
```

如果只想本地跑通流程，可切换 Mock AI：

```yaml
ai:
  provider: mock
```

### 3. 启动后端

```bash
cd backend
mvn spring-boot:run
```

后端默认运行在：

```text
http://localhost:8080
```

健康检查：

```bash
curl http://localhost:8080/api/health
```

### 4. 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端默认运行在：

```text
http://localhost:5173
```

前端 API 地址可参考 `frontend/.env.example`：

```env
VITE_API_BASE_URL=http://localhost:8080
```

## 环境变量配置

后端常用配置：

| 变量 | 说明 | 示例 |
|---|---|---|
| `AI_PROVIDER` | AI provider，支持 `openai-compatible` 或 `mock` | `openai-compatible` |
| `AI_BASE_URL` | OpenAI-Compatible API Base URL | `https://your-api-base-url/v1` |
| `AI_API_KEY` | 大模型 API Key | 不要提交到 Git |
| `AI_MODEL` | 模型 ID | `your-model-name` |
| `AI_TIMEOUT_SECONDS` | AI 请求超时时间 | `30` |

前端常用配置：

| 变量 | 说明 | 示例 |
|---|---|---|
| `VITE_API_BASE_URL` | 后端 API 地址 | `http://localhost:8080` |

注意：

- 不要在仓库中提交真实 API Key。
- `application-local.yml` 和生产私密配置应只保存在本地或服务器环境。
- 真实模型模式下，AI 调用失败会返回业务错误，不会自动 fallback 到 Mock。

## 项目目录结构

```text
codecoach-ai
├── README.md
├── docs
│   ├── 00_项目立项说明.md
│   ├── 04_数据库设计.md
│   ├── 05_API接口设计.md
│   ├── 06_技术选型与项目结构.md
│   ├── 07_开发任务拆分.md
│   └── 09_产品设计与视觉规范.md
├── backend
│   ├── pom.xml
│   └── src/main
│       ├── java/com/codecoach
│       │   ├── common
│       │   ├── config
│       │   ├── security
│       │   └── module
│       │       ├── ai
│       │       ├── auth
│       │       ├── health
│       │       ├── interview
│       │       ├── project
│       │       ├── report
│       │       └── user
│       └── resources
│           ├── application.yml
│           ├── application-dev.yml
│           └── sql
├── frontend
│   ├── package.json
│   └── src
│       ├── api
│       ├── components
│       ├── layouts
│       ├── pages
│       ├── router
│       ├── store
│       ├── styles
│       └── types
└── deploy
    └── docker-compose.yml
```

## 当前进度

已完成：

- 后端基础设施：统一响应、全局异常、MyBatis-Plus、MySQL、Redis、JWT、CORS
- 用户模块：注册、登录、当前用户信息
- 项目模块：创建、列表、详情、修改、逻辑删除
- AI 模块：统一接口、Mock 实现、OpenAI-Compatible API 实现
- 训练模块：创建会话、查询会话、提交回答、重复提交保护、最后一轮自动结束
- 报告模块：生成报告、查询报告
- 历史模块：分页查询训练历史
- 前端 MVP 页面：登录、注册、项目档案、训练页、报告页、历史页

仍在打磨：

- 真实模型调用稳定性与错误提示
- 前端交互细节和加载状态
- 页面视觉一致性
- 端到端测试和部署脚本

## 后续规划

后续计划逐步扩展：

- SSE 流式输出，降低 AI 响应等待感
- 更完整的模型配置和模型切换能力
- 八股问答专项训练
- 场景题训练和模拟面试
- 简历 / 项目文档上传解析
- RAG 知识库问答
- 训练能力画像和成长趋势
- 错题复习与学习计划
- 生产环境部署、监控和日志分析
