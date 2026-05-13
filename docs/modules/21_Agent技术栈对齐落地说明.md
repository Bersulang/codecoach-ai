# Agent 技术栈对齐落地说明

本次改造目标是把现有自研 Agent 架构补齐为更接近主流 Java AI Agent 项目的技术栈，同时保持主业务默认链路不变。

## Spring AI POC

- 默认构建不引入 Spring AI，避免依赖下载或自动配置影响主应用。
- `spring-ai-poc` Maven profile 引入 `org.springframework.ai:spring-ai-bom:1.1.0` 和 `spring-ai-starter-model-openai`。
- `spring-ai-alibaba-poc` Maven profile 引入 `com.alibaba.cloud.ai:spring-ai-alibaba-bom:1.1.0.0` 和 `spring-ai-alibaba-starter-dashscope`，适配当前 Spring Boot 3.4.x。
- `ai.spring-ai.enabled=true` 后启用 `SpringAiModelGateway`。
- Dev 接口：`POST /api/dev/spring-ai/chat`。

## AiModelGateway

- 新增 `AiModelGateway` 抽象，覆盖 `chat`、`streamChat`、`structuredChat`、`parseToolIntent`。
- 默认主链路仍使用既有 OpenAI-Compatible 服务；新网关先作为旁路和 POC。
- `OpenAiCompatibleModelGateway` 统一写入 `AiCallLog.traceId`。

## Function Calling

- `ToolSchemaExporter` 将低风险只读 Tool 导出为 OpenAI function schema。
- `FunctionCallParser` 解析 `tool_calls`。
- `FunctionCallExecutorAdapter` 将 function call 映射回 `AgentToolExecutor`。
- Dev 接口：`/api/dev/function-calling/tools`、`/api/dev/function-calling/poc/ability-summary`。

## RAG Pipeline

- `RuleBasedRagQueryRewriteService` 负责 Query Rewrite。
- `SimpleRagRerankService` 负责规则 Rerank。
- `RagTrace` 记录 query、rewrittenQuery、sourceTypes、topK、hitCount、selectedChunkIds、latencyMs、success、fallbackReason。
- `RagEvaluationResult` 返回 hitCount、avgScore、emptyHit、sourceType distribution、contextChars。

## Semantic Memory

- `UserSemanticMemoryService` 将摘要化 `user_memory` 写入向量库，payload 只保存 memoryId、类型和摘要。
- `UserMemoryService.semanticSearch` 先走向量召回，失败时回退结构化关键词检索。
- `GET_USER_MEMORY_SUMMARY` 支持 `query` 参数返回 semantic recall。

## Multi-Agent

- `MultiAgentCoordinator` 在复盘 Agent 场景创建 `AGENT_REVIEW_MULTI` run。
- Retriever Agent 负责收集训练、简历、RAG 上下文。
- Evaluator Agent 负责生成/校验复盘结构。
- Coach Agent 负责落库、记忆沉淀和最终建议。

## MCP POC

- `McpToolAdapter` 复用 Function Calling 的工具导出和执行。
- 默认 `MCP_POC_ENABLED=false`，本地开发限制默认开启。
- 只暴露低风险只读工具。

## Trace

- `TraceContextHolder` 统一获取当前 Agent trace 或创建标准 traceId。
- AgentRun、AgentStep、ToolTrace、AiCall、RagTrace、Memory Search 可共享 traceId。
- `OpenTelemetryStubConfig` 预留 OTel exporter 接入点，不强制接 Jaeger/Grafana。
