# Agent 技术栈整体迁移颗粒度与路线

## 1. 当前状态

本节基于当前代码结构梳理，不把 POC 写成主链路能力。

### 1.1 当前主链路 AI 调用路径

| 场景 | 主调用入口 | 当前实现 | 是否经过 AiModelGateway | 说明 |
| --- | --- | --- | --- | --- |
| 项目训练首问、反馈、报告 | `InterviewSessionServiceImpl` -> `AiInterviewService` | `OpenAiCompatibleAiInterviewServiceImpl` / mock | 否 | 核心链路，含 token 流式输出 |
| 八股训练首问、反馈、报告 | `QuestionSessionServiceImpl` -> `AiQuestionPracticeService` | `OpenAiCompatibleQuestionPracticeServiceImpl` / mock | 否 | 核心链路，含 token 流式输出 |
| 复盘 Agent 总结 | `AgentReviewServiceImpl` -> `AiAgentReviewService` | `OpenAiCompatibleAgentReviewServiceImpl` / mock | 否 | 已接入 Multi-Agent trace，但模型调用仍走旧 service |
| 简历分析 | `ResumeServiceImpl` -> `AiResumeAnalysisService` | `OpenAiCompatibleResumeAnalysisServiceImpl` / mock | 否 | 独立 OpenAI-Compatible 调用，当前未写 `ai_call_log` |
| Guide 兜底 AI 建议 | `GuideChatServiceImpl` -> `AiGuideService` | `OpenAiCompatibleGuideServiceImpl` | 否 | Guide 主体是规则 + Tool，AI 只作为可选兜底 |
| RAG Embedding | `RagRetrievalServiceImpl` / `RagIndexServiceImpl` -> `EmbeddingService` | `ZhipuEmbeddingServiceImpl` | 否 | 暂不纳入 Model Gateway 迁移 |
| Dev Spring AI POC | `DevSpringAiController` -> `AiModelGatewayRouter` | `SpringAiModelGateway` | 是 | POC，默认关闭 |
| Dev Gateway OpenAI POC | `OpenAiCompatibleModelGateway` | 新网关实现 | 是 | Adapter 已存在，主业务未切换 |

### 1.2 仍直接依赖 OpenAI-Compatible 的模块

- `OpenAiCompatibleAiInterviewServiceImpl`：项目训练首问、反馈、流式反馈、项目报告。
- `OpenAiCompatibleQuestionPracticeServiceImpl`：八股首问、反馈、流式反馈、八股报告。
- `OpenAiCompatibleAgentReviewServiceImpl`：复盘 Agent LLM 总结。
- `OpenAiCompatibleResumeAnalysisServiceImpl`：简历分析。
- `OpenAiCompatibleGuideServiceImpl`：Guide 可选 AI 建议。
- `ZhipuEmbeddingServiceImpl`：RAG embedding，虽不是 OpenAI-Compatible Chat，但仍是独立模型调用实现。

### 1.3 已经可以通过 AiModelGateway 的模块

- `DevSpringAiController` 使用 `AiModelGatewayRouter.require("spring-ai")`。
- `OpenAiCompatibleModelGateway`、`SpringAiModelGateway`、`AiModelGatewayRouter` 已存在。
- 主业务模块尚未接入 `AiModelGateway`。

### 1.4 POC / 旁路模块

- Spring AI / Spring AI Alibaba Maven profile。
- `SpringAiModelGateway`。
- `AiModelGateway` 切换与 shadow call 配置。
- Function Calling：`ToolSchemaExporter`、`FunctionCallParser`、`FunctionCallExecutorAdapter`、dev POC。
- MCP：`McpToolAdapter`、`DevMcpController`，默认关闭。
- OTel exporter：`OpenTelemetryStubConfig`，仅预埋。

### 1.5 已进入主链路的模块

- RAG Pipeline：`RagRetrievalServiceImpl` 已统一执行 Query Rewrite、Retrieve、Simple Rerank、RagTrace、Evaluation。
- RagTrace：所有经过 `RagRetrievalService.search` 的检索都会记录。
- Semantic Memory 回退：`UserMemoryService.semanticSearch` 可走向量召回，失败回退关键词；`GET_USER_MEMORY_SUMMARY` 支持 semantic recall。
- Review Multi-Agent trace：`AgentReviewServiceImpl` 使用 `MultiAgentCoordinator` 记录 Retriever / Evaluator / Coach。
- AiCall traceId：`AiCallLogServiceImpl` 会自动补 `TraceContextHolder.getTraceId()`。
- `/dev/observability` 展示 RagTrace 和 AiCall traceId。

### 1.6 已记录 traceId 的模块

- `agent_run.trace_id`：Guide Agent、Mock Interview Agent、Review Multi-Agent。
- `agent_step.trace_id`：通过 `AgentTraceServiceImpl.recordStep` 从 `AgentRuntimeContextHolder` 获取。
- `agent_tool_trace.trace_id`：Tool 执行独立 trace，同时保存 parent trace。
- `ai_call_log.trace_id`：通过 `AiCallLogServiceImpl` 自动补齐；但前提是调用方确实写 `AiCallLog`。
- `rag_trace.trace_id`：通过 `RagRetrievalServiceImpl` 使用 `TraceContextHolder.getOrCreateTraceId()`。
- `AiModelGateway` POC：OpenAI-Compatible / Spring AI 网关会创建 traceId。

### 1.7 尚未接入 AgentRun / AgentStep 的模块

- 项目训练普通会话 `InterviewSessionServiceImpl`：有 RAG 和 Memory，但没有独立 AgentRun/AgentStep。
- 八股训练 `QuestionSessionServiceImpl`：有 RAG 和 Memory，但没有独立 AgentRun/AgentStep。
- 简历分析 `ResumeServiceImpl` / `OpenAiCompatibleResumeAnalysisServiceImpl`：没有 AgentRun/AgentStep。
- 纯 RAG 索引链路 `RagIndexServiceImpl`：属于基础设施任务，当前不记录 AgentRun。
- 知识学习、文档上传、项目档案 CRUD：业务链路不属于 AgentRun。

### 1.8 尚未接入统一 Tool / Function Calling 的模块

- 项目训练、八股训练、模拟面试、简历分析、复盘总结的内部业务调用仍是 service 编排。
- Guide Agent 当前使用自研 Tool Intent + `AgentToolExecutor`，Function Calling 仅 dev POC。
- Command Tool 仍应保持 action card + 用户确认，不迁移为自动 function calling。

### 1.9 尚未接入 RagTrace 的模块

- 已通过 `RagRetrievalService.search` 的入口都会记录 RagTrace，包括训练、Guide Tool 搜索、复盘 RAG、洞察推荐、RAG 搜索接口。
- 不经过 `RagRetrievalService.search` 的索引、embedding 写入、文档解析不记录 RagTrace。
- 如果后续有业务绕过 `RagRetrievalService` 直接访问 `VectorStoreService`，需要禁止或补 trace。

### 1.10 尚未接入 Semantic Memory 的模块

- 已写入结构化 Memory：项目报告、八股报告、模拟面试报告、简历分析、复盘 Agent、能力快照。
- 已支持 semantic search：`UserMemoryService.semanticSearch`、`GET_USER_MEMORY_SUMMARY`。
- 尚未正式影响推荐：Guide Agent、Mock Interview Plan、项目训练、八股训练。
- Review Agent 已在生成后触发 `indexActiveSemanticMemory`，但生成前上下文仍主要依赖结构化证据。

## 2. 目标架构分层

### 2.1 Frontend Interaction Layer

职责：
- ChatGPT App 式输入框。
- token 流式输出。
- action card。
- 语音输入。
- `/dev/observability`。

目标：
- 前端只感知统一 Agent API、stream event、action card、traceId。
- 不直接感知模型 provider、RAG provider、Tool 执行细节。

### 2.2 Business Agent Layer

职责：
- Guide Agent。
- Review Agent。
- Mock Interview Agent。
- Resume Agent。
- Project Training Agent。
- Question Training Agent。

目标：
- 每个业务 Agent 只表达业务意图、上下文构造、输出约束。
- 不直接依赖某个模型 SDK。
- 不自行绕过 Tool / RAG / Memory / Trace 底座。

### 2.3 Agent Runtime Layer

职责：
- AgentRun。
- AgentStep。
- ReAct Loop。
- Plan-Execute。
- Multi-Agent Coordinator。
- Fallback。
- TraceId 贯通。

目标：
- 所有 Agent 场景可观察、可回放、可降级。
- Guide 保留 ReAct。
- Mock Interview 保留 Plan-Execute。
- Review 先作为 Multi-Agent 样板。

### 2.4 Tool / Function Layer

职责：
- Tool Registry。
- Tool Executor。
- Tool Schema Exporter。
- Function Call Parser。
- MCP Adapter。
- ToolTrace。
- Tool 风险等级和确认机制。

目标：
- 模型不能绕过 Tool Registry。
- 低风险只读 Tool 可自动执行。
- 中风险 Tool 必须用户确认。
- 高风险 Tool 禁止。
- MCP 和 Function Calling 共享同一套 Tool 白名单。

### 2.5 AI Model Gateway Layer

职责：
- `OpenAiCompatibleModelGateway`。
- `SpringAiModelGateway`。
- `SpringAiAlibabaGateway`。
- `chat`。
- `streamChat`。
- `structuredChat`。
- `functionCalling`。
- shadow call。

目标：
- 业务模块依赖 Gateway，而不是依赖具体 provider 实现。
- 每个迁移点保留原 OpenAI-Compatible fallback。
- streaming 迁移单独验收，不能影响当前 NDJSON / token 输出协议。

### 2.6 RAG Pipeline Layer

职责：
- Query Rewrite。
- Retrieve。
- Rerank。
- Context Compress。
- RagTrace。
- RagEvaluation。
- 权限过滤。

目标：
- 所有检索入口统一走 `RagRetrievalService`。
- 用户文档、项目档案必须保留 userId / ownerType 过滤。
- 业务模块不自行拼接向量检索。

### 2.7 Memory Layer

职责：
- structured `user_memory`。
- semantic memory。
- memory embedding。
- memory retrieval。
- memory tool。

目标：
- 结构化 Memory 继续作为稳定主链路。
- Semantic Memory 先 shadow recall，再逐步影响推荐。
- 不保存完整隐私原文，只保存摘要。

### 2.8 Observability Layer

职责：
- `ai_call_log`。
- `agent_run`。
- `agent_step`。
- `agent_tool_trace`。
- `rag_trace`。
- traceId。
- OTel 预埋。

目标：
- 所有 LLM / RAG / Tool / Agent 步骤可通过 traceId 串起来。
- 当前自研页面继续工作。
- OTel exporter 后置接入，不阻塞业务迁移。

### 2.9 Infrastructure Layer

职责：
- MySQL。
- Redis。
- Qdrant。
- OSS。
- PDFBox。
- Security / JWT。

目标：
- 基础设施对上层提供稳定接口。
- 安全上下文统一从登录态获取 userId。
- 不让前端传 userId。

## 3. 迁移原则

1. 不一次性替换全部主链路。
2. 先迁移低风险非流式场景。
3. 再迁移非核心报告生成。
4. 再迁移 Guide / Review。
5. 最后考虑项目训练、八股训练、模拟面试等核心流式链路。
6. 每一步都必须可回滚。
7. 每一步都必须保留原 OpenAI-Compatible fallback。
8. 每一步都必须记录 trace。
9. 不允许破坏 RAG 权限过滤。
10. 不允许破坏 token 流式输出。
11. 不允许把 POC 能力假装成生产主链路。

## 4. 迁移颗粒度

### 4.1 Model Gateway 迁移颗粒度

1. 非流式普通 chat：dev compare 或内部 shadow。
2. structured output：报告/分析 JSON 输出。
3. report generation：项目报告、八股报告、复盘总结。
4. guide response compose：先 shadow，再部分接入。
5. review response compose：复盘 Agent 较适合作为首批正式迁移。
6. streaming chat：项目训练、八股训练、模拟面试，最后迁移。
7. tool calling chat：先 Guide 只读 Tool。
8. embedding：暂缓，继续使用 `EmbeddingService` + Zhipu。

### 4.2 RAG Pipeline 迁移颗粒度

1. RagTrace 全面接入：已通过 `RagRetrievalService` 完成。
2. Query Rewrite 默认关闭或场景开关化：当前规则改写已在主链路，需要后续加场景开关。
3. Simple Rerank 默认开启：已完成。
4. Context Compress POC：后续加，不影响当前 context build。
5. RAG Evaluation 指标采集：已有初版指标，后续扩展评测集。
6. LLM Rerank：后置，可 provider 化。

### 4.3 Tool / Function Calling 迁移颗粒度

1. Tool Schema Export：已完成。
2. 只读 Tool 原生 Function Calling POC：已完成 dev POC。
3. Guide Agent function calling shadow：下一步。
4. Guide Agent 局部切换：只切低风险只读 Tool。
5. Review Agent 接入：只用于查询类上下文收集。
6. Command Tool 保持用户确认。
7. MCP 暂只暴露低风险只读 Tool。

### 4.4 Memory 迁移颗粒度

1. 结构化 Memory 保持主链路。
2. Semantic Memory shadow recall。
3. Guide Agent 使用 semantic memory。
4. Mock Interview plan 使用 semantic memory。
5. Review Agent 写入 semantic memory。
6. 后续 memory evaluation。

### 4.5 Multi-Agent 迁移颗粒度

1. Review Agent Multi-Agent 已接入。
2. Mock Interview report generation 接入 Evaluator / Coach。
3. Mock Interview question generation 暂不拆太细。
4. Guide Agent 暂不做 Multi-Agent。
5. 后续抽出共用 Retriever Agent。

### 4.6 Observability 迁移颗粒度

1. traceId 贯通。
2. RagTrace 页面展示。
3. MemoryTrace：可选。
4. FunctionCallTrace：建议先用 ToolTrace + agentType 区分。
5. MCPTrace：建议先用 ToolTrace + `MCP_POC` 区分。
6. OTel exporter 真实接入后置。

## 5. 分阶段路线

### Phase 0：补齐数据库迁移

改造范围：
- 执行或迁移 `schema.sql` 中新增字段和表。

涉及模块：
- `ai_call_log.trace_id`。
- `rag_trace`。
- `agent_run` / `agent_step` / `agent_tool_trace` 已有 runId / traceId。
- `user_memory` 无新增字段，semantic memory 复用 Qdrant payload。

是否影响主链路：
- 不应影响。仅补 schema。

数据库变更：
- 需要。

前端变更：
- 不需要，前端已能展示 RagTrace。

回滚方式：
- 回滚新增 `rag_trace` 表和 `ai_call_log.trace_id` 字段，代码可保留空写兼容；生产回滚需先停止新版本写入。

验收命令：
- `mvn -q -DskipTests package`
- `npm run build`

验收接口：
- `GET /api/dev/observability/rag-traces`
- `GET /api/dev/observability/ai-calls`

风险点：
- 线上库未执行字段会导致 MyBatis 插入失败。

简历价值：
- 能说明 Agent / LLM / RAG traceId 具备统一数据模型。

### Phase 1：AiModelGateway 接入低风险场景

改造范围：
- 选择一个非流式、低风险、输出结构简单的场景做 shadow。
- 推荐先做 Dev-only compare endpoint 或 Guide answer compose shadow。

涉及模块：
- `AiModelGatewayRouter`。
- `OpenAiCompatibleModelGateway`。
- `SpringAiModelGateway`。
- `AiCallLog`。

是否影响主链路：
- 不影响，shadow 不改变用户响应。

数据库变更：
- 不需要，依赖 Phase 0 的 traceId 字段。

前端变更：
- 可不需要；如做 dev compare 页面再补。

回滚方式：
- 关闭 `AI_GATEWAY_SHADOW_SPRING_AI` 或移除 shadow 调用。
- 原业务仍调用旧 service。

验收命令：
- `mvn -q -DskipTests package`
- `mvn -q -Pspring-ai-poc -DskipTests package`
- `mvn -q -Pspring-ai-alibaba-poc -DskipTests package`

验收接口：
- `POST /api/dev/spring-ai/chat`
- 后续新增 `/api/dev/ai-gateway/compare`

风险点：
- provider 配置缺失。
- shadow 调用增加成本和延迟，必须异步或限流。

简历价值：
- 能说明模型网关、provider 隔离、影子调用。

### Phase 2：报告生成类迁移

改造范围：
- 先迁移非流式、失败可重试的 structured output。
- 推荐顺序：复盘总结 -> 项目报告 -> 八股报告 -> 简历分析。

涉及模块：
- `AiAgentReviewService`。
- `AiInterviewService.generateReport`。
- `AiQuestionPracticeService.generateReport`。
- `AiResumeAnalysisService`。
- `AiModelGateway.structuredChat`。

是否影响主链路：
- 影响报告生成，但不影响训练实时对话。

数据库变更：
- 不需要。

前端变更：
- 不需要。

回滚方式：
- 配置切回旧 OpenAI-Compatible service。
- 保留旧 service 实现和原 prompt。

验收命令：
- `mvn -q -DskipTests package`
- 手动生成各类报告。

验收接口：
- 复盘：`POST /api/agent-reviews`
- 项目报告：现有项目训练结束报告接口。
- 八股报告：现有八股训练结束报告接口。
- 简历分析：现有简历分析接口。

风险点：
- JSON shape 不稳定。
- 简历分析目前未写 `ai_call_log`，迁移时应一起补齐日志。

简历价值：
- 能说明 structured output 统一网关、报告类 Agent 迁移。

### Phase 3：Guide Agent Function Calling 迁移

改造范围：
- 只读 Tool 使用模型原生 Function Calling。
- Command Tool 保持 action card + 用户确认。

涉及模块：
- `AgentRuntime`。
- `ToolSchemaExporter`。
- `FunctionCallParser`。
- `FunctionCallExecutorAdapter`。
- `AgentToolExecutor`。

是否影响主链路：
- 初期 shadow 不影响。
- 局部切换后影响 Guide 的只读观察，不影响训练创建。

数据库变更：
- 不需要。

前端变更：
- 不需要，action card 协议保持。

回滚方式：
- 配置关闭 function calling。
- 回退当前 JSON intent / 规则 Tool plan。

验收命令：
- `mvn -q -DskipTests package`
- `npm run build`

验收接口：
- `GET /api/dev/function-calling/tools`
- `POST /api/dev/function-calling/poc/ability-summary`
- Guide 聊天接口。

风险点：
- 模型 hallucinate tool name。
- 参数注入。
- 中高风险 Tool 被误执行。

简历价值：
- 能说明 Function Calling 与内部 Tool Registry 安全对接。

### Phase 4：RAG Pipeline 标准化

改造范围：
- 明确所有 RAG 检索入口只能走 `RagRetrievalService`。
- 增加 Query Rewrite 场景开关。
- 增加 Context Compress POC。

涉及模块：
- `RagRetrievalServiceImpl`。
- `RagQueryRewriteService`。
- `RagRerankService`。
- `RagTraceService`。
- 各业务调用方。

是否影响主链路：
- 当前已影响 RAG 检索结果排序和 trace。
- 后续开关化降低风险。

数据库变更：
- 不需要。

前端变更：
- 不需要，可观测页面已支持。

回滚方式：
- 关闭 Query Rewrite。
- Rerank 降为按原 score 排序。
- 保留旧 context build。

验收命令：
- `mvn -q -DskipTests package`

验收接口：
- `POST /api/rag/search`
- `GET /api/dev/observability/rag-traces`

风险点：
- Rewrite 扩展过度导致召回偏移。
- 用户文档权限过滤不能被改坏。

简历价值：
- 能说明标准 RAG Pipeline、Trace、Eval 指标。

### Phase 5：Semantic Memory 接入 Agent

改造范围：
- Semantic Memory 先 shadow recall。
- Guide / Review / Mock Interview Plan 逐步使用 semantic recall。

涉及模块：
- `UserMemoryService`。
- `UserSemanticMemoryService`。
- `AgentToolConfiguration.GET_USER_MEMORY_SUMMARY`。
- Guide / Review / Mock Interview。

是否影响主链路：
- 初期不影响。
- 正式接入后影响推荐排序和上下文。

数据库变更：
- 不需要 MySQL 字段；依赖 Qdrant collection。

前端变更：
- 不需要。

回滚方式：
- semantic recall 为空时自动回退结构化 Memory。
- 关闭 Agent 使用 semantic recall 的配置。

验收命令：
- `mvn -q -DskipTests package`

验收接口：
- `POST /api/dev/memory/semantic-index`
- `GET /api/dev/memory/semantic-search?query=Redis`
- `GET_USER_MEMORY_SUMMARY` Tool。

风险点：
- Qdrant 不可用。
- 摘要过短召回质量差。
- 不能写入完整隐私原文。

简历价值：
- 能说明结构化长期记忆 + 向量语义记忆双轨架构。

### Phase 6：核心流式链路谨慎迁移

改造范围：
- 项目训练。
- 八股训练。
- 模拟面试。
- 仅在 Gateway streaming 能力稳定后迁移。

涉及模块：
- `OpenAiCompatibleAiInterviewServiceImpl`。
- `OpenAiCompatibleQuestionPracticeServiceImpl`。
- `MockInterviewSessionServiceImpl`。
- 前端 NDJSON stream 消费。

是否影响主链路：
- 高影响，必须最后做。

数据库变更：
- 不需要。

前端变更：
- 理论不需要，必须保持现有 NDJSON / token 协议。

回滚方式：
- 配置回旧 streaming 实现。
- 按场景灰度，不跨场景全量切换。

验收命令：
- `mvn -q -DskipTests package`
- `npm run build`

验收接口：
- `/api/interview-sessions/{sessionId}/answers/stream`
- `/api/question-sessions/{sessionId}/answers/stream`
- `/api/mock-interviews/{sessionId}/answers/stream`

风险点：
- token 顺序、JSON 可见内容过滤、前端流式状态。
- 模型输出结构解析失败。

简历价值：
- 能说明生产级流式 Agent 迁移、兼容前端协议、可回滚灰度。

### Phase 7：MCP 与 OTel

改造范围：
- MCP 保持开发者 POC。
- OTel exporter 最后接。

涉及模块：
- `McpToolAdapter`。
- `DevMcpController`。
- `OpenTelemetryStubConfig`。
- Observability。

是否影响主链路：
- 不影响。

数据库变更：
- 不需要。

前端变更：
- 不需要。

回滚方式：
- `MCP_POC_ENABLED=false`。
- `OTEL_ENABLED=false`。

验收命令：
- `mvn -q -DskipTests package`

验收接口：
- `GET /api/dev/mcp/tools`
- OTel 后续用 collector / Jaeger 验证。

风险点：
- MCP 工具暴露边界。
- OTel exporter 配置导致启动失败，必须条件化。

简历价值：
- 能说明对齐 MCP 生态和标准 Trace，但保持安全边界。

## 6. 当前阶段不能动

1. token 流式训练协议。
2. 项目训练核心状态机。
3. 八股训练核心状态机。
4. 模拟面试房间主交互。
5. RAG 用户权限过滤。
6. 评分 Rubric 低质量回答限制。
7. Tool 风险等级和用户确认机制。
8. 用户文档上传和 OSS 主链路。
9. 生产配置中的 API Key 管理方式。

## 7. 风险评估

| 风险 | 影响 | 控制方式 |
| --- | --- | --- |
| Gateway 迁移导致模型响应结构变化 | 报告解析失败 | 先迁移 structured output，保留旧 service fallback |
| Function Calling 误执行命令 | 用户数据被修改 | 只开放低风险只读 Tool，中风险继续用户确认 |
| RAG Rewrite 召回偏移 | 推荐不准 | 场景开关、RagTrace、Evaluation 指标 |
| Semantic Memory 泄露隐私 | 合规风险 | 只写摘要，不写完整文档/简历原文 |
| Streaming 迁移破坏前端体验 | 核心训练不可用 | 最后迁移，保持 NDJSON 协议，逐场景灰度 |
| MCP 暴露敏感工具 | 数据泄露或误操作 | 默认关闭、本地限制、白名单 |
| OTel exporter 影响启动 | 主应用不可用 | 条件化配置，最后接入 |

## 8. 简历表达

### 8.1 当前已实现版

可以写：

> 在 Java 21 + Spring Boot 3 的 AI 面试训练平台中，设计并落地 Agent 技术栈对齐底座：保留原 OpenAI-Compatible 主链路，同时引入 Spring AI / Spring AI Alibaba POC、AiModelGateway Adapter、Function Calling Adapter、RAG Pipeline（Query Rewrite / Rerank / RagTrace / Evaluation）、Semantic Memory POC、Review Multi-Agent trace、MCP Adapter POC 与 OTel 预埋；其中 RAG Pipeline、RagTrace、复盘 Multi-Agent trace、AiCall traceId 已进入主链路，Spring AI、Function Calling、MCP、OTel exporter 仍为 POC / 预埋。

不要写：

> 已全量迁移 Spring AI / 已生产使用 MCP / 已生产使用原生 Function Calling。

### 8.2 完成主链路迁移第一阶段版

可以写：

> 基于 AiModelGateway 对非流式低风险场景进行灰度迁移，先在 Guide / Review 等非核心链路完成 shadow call、trace 对比和 OpenAI-Compatible fallback；报告类 structured output 逐步通过统一网关生成，实现模型 provider 隔离、统一日志与可回滚迁移。

### 8.3 完成完整 Agent 技术栈对齐版

可以写：

> 将 CodeCoach AI 演进为统一 Agent 技术底座驱动的面试训练平台：业务 Agent 通过 Agent Runtime 统一记录 AgentRun / AgentStep，Tool Registry 同时支持 ReAct JSON intent、原生 Function Calling 和 MCP Adapter，AI Model Gateway 支持 OpenAI-Compatible / Spring AI / Spring AI Alibaba 多 provider 与 shadow call，RAG Pipeline 统一 Query Rewrite / Retrieve / Rerank / Context Compress / Evaluation，Memory 层支持结构化记忆和语义向量召回，Observability 层贯通 LLM / RAG / Tool / Agent trace 并预留 OTel exporter。

## 9. 最终架构图

```text
Frontend Interaction Layer
  - Chat input / token stream / action card / voice / dev observability
  |
  v
Agent API
  - Guide API / Review API / Training API / Dev API
  |
  v
Business Agent Layer
  - Guide Agent
  - Review Agent
  - Mock Interview Agent
  - Resume Agent
  - Project Training Agent
  - Question Training Agent
  |
  v
Agent Runtime Layer
  - AgentRun / AgentStep
  - ReAct Loop
  - Plan-Execute
  - Multi-Agent Coordinator
  - Fallback
  - TraceId propagation
  |
  +-----------------------------+
  |                             |
  v                             v
Tool / Function Layer        AI Model Gateway Layer
  - Tool Registry              - OpenAiCompatibleModelGateway
  - Tool Executor              - SpringAiModelGateway
  - Tool Schema Exporter       - SpringAiAlibabaGateway
  - Function Call Parser       - chat / streamChat / structuredChat
  - MCP Adapter                - functionCalling / shadow call
  - ToolTrace                  |
  |                            |
  +-------------+--------------+
                |
                v
RAG / Memory Layer
  - Query Rewrite / Retrieve / Rerank / Context Compress
  - RagTrace / RagEvaluation / permission filter
  - structured user_memory / semantic memory / memory retrieval
                |
                v
Observability Layer
  - ai_call_log / agent_run / agent_step / agent_tool_trace / rag_trace
  - traceId / OTel exporter hook
                |
                v
Infrastructure Layer
  - MySQL / Redis / Qdrant / OSS / PDFBox / Security JWT
```

## 10. 后续任务清单

1. 执行数据库迁移并验证 `rag_trace`、`ai_call_log.trace_id`。
2. 增加 Dev-only Gateway compare endpoint。
3. 增加 Gateway shadow call 日志模型。
4. 给 Query Rewrite 增加场景开关。
5. 将复盘 Agent LLM 总结迁移到 `AiModelGateway.structuredChat`，保留 fallback。
6. 为简历分析补 `ai_call_log` 与 traceId。
7. Guide Agent 增加 Function Calling shadow。
8. Guide Agent 只读 Tool 局部切换 Function Calling。
9. Mock Interview report generation 接入 Evaluator / Coach。
10. Semantic Memory 接入 Guide shadow recall。
11. Semantic Memory 接入 Mock Interview Plan shadow recall。
12. Context Compress POC。
13. MemoryTrace / FunctionCallTrace 评估是否单独建表。
14. MCP 保持本地 dev POC，不扩展写操作。
15. OTel exporter 在主链路稳定后再接入。
