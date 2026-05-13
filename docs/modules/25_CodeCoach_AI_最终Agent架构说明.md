# CodeCoach AI 最终 Agent 架构说明

## 1. 定位

CodeCoach AI 已收束为“统一 Agent Runtime 驱动的 Java AI Agent 面试训练平台”。业务核心仍是项目训练、八股训练、简历训练、模拟面试和复盘中心；Agent 工程化主链路统一围绕 Runtime、Tool Registry、RAG Pipeline、Memory、AiModelGateway 和 Observability 展开。

本项目借鉴 OnCall Agent / AI-Meeting 类项目的工程化表达，包括 RAG、Function Calling、ReAct/Plan-Execute、多 Agent、SSE、MCP、长期记忆、Single-flight 和可观测，但不复制第三方代码。

## 2. 分层架构

### Frontend Interaction Layer

- ChatGPT App 式 Guide 输入框、token 流式训练输出、Action Card、语音输入预留。
- Review Center、Mock Interview Room、Mock Interview Report、`/dev/observability`。
- Mock Interview Report 已展示阶段评分、雷达维度和摘要级问答回放。

### Business Agent Layer

- Guide Agent：结合能力画像、最近训练、简历风险、长期记忆和只读 ToolTrace 给出下一步建议。
- Review Agent：Retriever / Evaluator / Coach 多 Agent 复盘，生成雷达图、问答回放、风险回答、nextActions。
- Mock Interview Agent：Plan-Execute 面试流程，报告生成阶段记录 Retriever / Evaluator / Coach role steps。
- Resume / Training Agent：简历分析、项目训练、八股训练统一通过 Gateway、Memory、RAG 和 Single-flight 能力进入 Agent 主链路。

### Agent Runtime Layer

- `agent_run` / `agent_step` 是统一执行追踪底座。
- traceId 贯通 AgentRun、AgentStep、ToolTrace、AiCall、RagTrace、Memory、MCP。
- Guide、Review、Mock Interview 关键步骤均记录 AgentStep。

### Tool / Function Layer

- `AgentToolRegistry` 是唯一 Tool 安全边界。
- Function Calling 只允许低风险只读工具：`GET_ABILITY_SUMMARY`、`GET_RECENT_TRAINING_SUMMARY`、`GET_RESUME_RISK_SUMMARY`、`GET_USER_MEMORY_SUMMARY`、`SEARCH_KNOWLEDGE`、`SEARCH_USER_DOCUMENTS`、`GET_MOCK_INTERVIEW_SUMMARY`、`GET_REPORT_REPLAY_DATA`。
- Function Calling / MCP 调用仍回到 `AgentToolExecutor`，不绕过风险等级、确认机制和用户隔离。

### AI Model Gateway Layer

- `AiModelGateway` 是新 AI 调用统一入口。
- 默认 provider：`openai-compatible`。
- 可选 provider：`spring-ai`、`spring-ai-alibaba`，通过 profile 和配置开启。
- Guide answer compose、Review Coach/Evaluator、Resume analysis、Project Training、Question Training 已走 Gateway，并写入 `ai_call_log` 的 provider、requestType、promptVersion、traceId。
- 流式训练链路通过 Gateway `streamChat` 入口保留原有 token 输出体验。

### RAG Pipeline Layer

- 统一入口：`RagRetrievalService`。
- 支持 Query Rewrite、Simple Rerank、Context Block 构建、RagTrace、RagEvaluation、用户文档权限过滤。
- Guide 知识查询、Review 检索、Mock Interview 问题生成均走统一 Pipeline。

### Memory Layer

- 结构化 `user_memory` 保留。
- Semantic Memory 通过 Embedding + Qdrant 建索引，Qdrant 失败时回退关键词/结构化记忆。
- Guide、Review、Mock Interview Plan、Mock Interview Report、nextActions 均读取或强化 Memory。
- 初版 Governance：查看 summary、归档 memory、标记 inaccurate、lastReinforcedAt/weight 强化。

### Observability Layer

- `/dev/observability` 汇总 AgentRun、AgentStep、ToolTrace、AiCall、RagTrace、Single-flight Trace、错误摘要、慢调用 TopN。
- 新增 FunctionCall、MCP、Multi-Agent role step 可通过 ToolTrace / AgentStep / traceId 查看。
- Single-flight Trace 只记录 requestKey 摘要、锁动作、命中/失败状态、耗时和降级原因，不记录 Prompt、简历、文档或用户回答原文。
- OpenTelemetry exporter 为可选 profile 配置，默认关闭，不影响主应用。

### Infrastructure Layer

- MySQL：业务状态、长期会话、报告、Agent/RAG/AI Trace。
- Redis：Review、Mock Interview Report、Project/Question Training Report、Resume Analysis、RAG Index 等高成本任务的 Single-flight、生成中状态、流式/问答锁、本地 fallback。
- Qdrant：RAG 向量检索与 Semantic Memory。
- OSS / PDFBox / Security JWT / Docker Compose / CI 保持工程化配套。

## 3. 稳定性与隐私

- 默认主链路不依赖 Spring AI、Alibaba、MCP 或 OTel。
- 高风险 Tool 不允许 Function Calling 自动执行。
- 所有用户数据按登录态 userId 查询，前端不传 userId。
- Prompt、简历、文档、用户回答、模型响应默认只保存摘要，不在观测页展示完整原文。
- Redis / Qdrant / Embedding / LLM 失败均有降级或失败可重试路径。
