# CodeCoach AI 主链路与 POC 边界说明

## 1. 已进入主链路

- Agent Runtime：Guide、Review、Mock Interview 均记录 AgentRun / AgentStep。
- AiModelGateway：Guide、Review、Resume analysis、Project Training、Question Training 已走 Gateway；所有 Gateway 调用写 `ai_call_log` 并带 traceId。
- Tool Registry / Tool Executor：站内工具统一注册、统一风险等级、统一执行和 ToolTrace。
- Function Calling 只读工具：Guide 使用只读 ToolTrace 获取上下文；Review Retriever 至少记录能力、训练、简历、记忆、模拟面试、回放和 RAG 类工具证据。
- RAG Pipeline：知识文章、用户文档、项目相关检索统一走 `RagRetrievalService`。
- Semantic Memory：Guide、Review、Mock Interview Plan / Report 和 nextActions 使用长期记忆；Review / Mock Report 后触发强化与语义索引。
- Multi-Agent：Review Center 和 Mock Interview Report 记录 Retriever / Evaluator / Coach steps。
- Single-flight / Redis 锁：Review 生成、Mock Interview Report、Project / Question Training Report、Resume Analysis、RAG Index、问题/项目回答等高成本路径已有 Redis 或本地 fallback 治理，并通过 `/dev/observability` 的 Single-flight Trace 展示锁动作、缓存命中和失败原因摘要。
- 长会话状态：Mock Interview、Project Training、Question Training、Review 依托 MySQL 长期状态和 Redis 运行态锁，支持刷新后恢复主要状态或报告结果。
- Observability：`/dev/observability` 展示 AgentRun、AgentStep、ToolTrace、AiCall、RagTrace、错误摘要、慢调用。

## 2. Profile / Optional 能力

### Spring AI Provider

- Maven profile：`spring-ai-poc`。
- 配置：`AI_GATEWAY_PRIMARY=spring-ai`、`SPRING_AI_POC_ENABLED=true`、`SPRING_AI_MODEL=...`。
- 角色：AiModelGateway Provider，不替代 Agent Runtime。

### Spring AI Alibaba Provider

- Maven profile：`spring-ai-alibaba-poc`。
- 配置：`AI_GATEWAY_PRIMARY=spring-ai-alibaba`、`SPRING_AI_POC_ENABLED=true`、`SPRING_AI_PROVIDER=spring-ai-alibaba`、`SPRING_AI_MODEL=...`。
- 当前实现通过 Spring AI 反射适配 ChatClient；依赖或云端配置不可用时会回退到 openai-compatible。

### MCP Server

- 默认关闭：`MCP_POC_ENABLED=false`。
- 本地限制：`MCP_LOCAL_ONLY=true`。
- endpoint：`GET /api/dev/mcp/tools`、`POST /api/dev/mcp/tools/{name}/call`。
- 只暴露低风险摘要工具，不暴露命令类 Tool。

### OpenTelemetry Exporter

- 默认关闭：`OTEL_ENABLED=false`。
- 可选本地 Jaeger：`docker compose --profile otel up -d`，再配置 `OTEL_EXPORTER_ENDPOINT=http://localhost:4318`。
- 当前站内 observability 是默认调试入口；OTel 用于外部链路系统对接。

## 3. 仍保留稳定原链路的能力

- 项目训练、八股训练的模型调用已经进入 Gateway，token 级流式输出保留原交互体验。
- 简历分析已经进入 Gateway；简历生成项目档案仍是业务规则草稿生成，不直接调用模型。
- 语音输入保留前端能力和后端预留，不引入后端 ASR/TTS。

## 4. 本地测试

### 默认构建

```bash
cd backend
mvn -q -DskipTests package
```

### Spring AI profile 构建

```bash
cd backend
mvn -q -Pspring-ai-poc -DskipTests package
mvn -q -Pspring-ai-alibaba-poc -DskipTests package
```

### 前端构建

```bash
cd frontend
npm run build
```

### 基础设施

```bash
cd deploy
docker compose up -d mysql redis qdrant
docker compose --profile otel up -d jaeger
```

### MCP-style 调用

```bash
export MCP_POC_ENABLED=true
curl http://localhost:8080/api/dev/mcp/tools
curl -X POST http://localhost:8080/api/dev/mcp/tools/GET_USER_MEMORY_SUMMARY/call \
  -H 'Content-Type: application/json' \
  -d '{"query":"下一步训练建议","topK":3}'
```

## 5. 安全边界

- userId 只从登录态获取。
- Observability、Memory、Review、Mock Report 均按当前用户过滤。
- 不在日志、Trace、OTel 或观测页输出完整 prompt、简历、文档、用户回答、模型响应或 API Key。
- Function Calling / MCP 不允许执行高风险或需要用户确认的命令类 Tool。
- POC / optional 能力不可用时不影响默认 openai-compatible 主链路。
