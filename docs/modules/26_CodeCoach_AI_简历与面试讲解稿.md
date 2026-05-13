# CodeCoach AI 简历与面试讲解稿

## 1. 简历项目描述

CodeCoach AI 是一个面向 Java 后端求职者的 AI Agent 面试训练平台，基于 Java 21、Spring Boot 3、React、MySQL、Redis、Qdrant 和 OpenAI-Compatible LLM 构建。项目将项目拷打、八股问答、简历风险分析、用户文档 RAG、真实模拟面试和综合复盘统一到自研 Agent Runtime 下，通过 Tool Registry、RAG Pipeline、Semantic Memory、Multi-Agent Review、Single-flight 和 Observability 构建可追踪、可降级、可扩展的 AI Agent 工程主链路。

## 2. 简历 Bullet

- 设计并落地自研 Agent Runtime，抽象 AgentRun / AgentStep / ToolTrace / RagTrace / AiCallLog，贯通 Guide、Review、Mock Interview 等核心训练流程，实现 traceId 级链路追踪。
- 将模型调用收束到 AiModelGateway，默认支持 OpenAI-Compatible，并通过 profile 可选接入 Spring AI / Spring AI Alibaba Provider，保留 fallback 和 ai_call_log 审计。
- 构建安全 Tool Registry 与 Function Calling Adapter，只开放低风险只读工具，所有模型 tool_call 均回到 Tool Executor，避免绕过用户确认和风险等级。
- 统一用户文档、项目档案、知识文章的 RAG Pipeline，支持 Query Rewrite、Rerank、上下文压缩、权限过滤、RagTrace 和 RagEvaluation。
- 引入结构化 Memory + Semantic Memory，支持 Review 后记忆强化、Mock Interview Plan 弱点选择、Guide 长期建议和 Qdrant 失败后的结构化回退。
- 实现 Review Center 的 Retriever / Evaluator / Coach 多 Agent 复盘，并扩展到模拟面试报告生成，输出阶段分析、雷达维度、问答回放和 nextActions。
- 使用 Redis Single-flight / 生成锁治理 Review、模拟面试报告、训练报告、简历分析和 RAG 索引等高成本任务，降低重复点击导致的重复 LLM / Embedding 调用；同时记录 Single-flight Trace，便于在观测中心排查缓存命中、锁竞争和失败重试。
- 建设 `/dev/observability` Agent 观测中心，按 traceId、agentType、status 查看 AgentRun、AgentStep、ToolTrace、RagTrace、AiCall、错误摘要和慢调用。

## 3. 面试讲解稿

我做 CodeCoach AI 时没有把它只当成“调大模型 API 的训练网站”，而是把它拆成了一个 Agent 工程平台。

第一层是业务 Agent，包括 Guide Agent、Review Agent、Mock Interview Agent、Resume Agent 和 Training Agent。它们不直接散落调用模型，而是进入 Agent Runtime，创建 AgentRun 和 AgentStep。

第二层是能力中台。模型统一通过 AiModelGateway，默认 OpenAI-Compatible，同时 Spring AI / Alibaba 作为可选 Provider。工具统一通过 Tool Registry，Function Calling 和 MCP 都不能绕过 Tool Executor。RAG 统一走 RagRetrievalService，保证 Query Rewrite、Rerank、权限过滤和 RagTrace 一致。Memory 分结构化记忆和语义记忆，既能做长期训练建议，也能在 Qdrant 不可用时回退。

第三层是稳定性和观测。高成本任务使用 Redis Single-flight，Agent、Tool、LLM、RAG、Memory、Single-flight 都带 traceId 或可查询 trace，观测中心可以定位一次复盘或模拟面试报告里 Retriever、Evaluator、Coach 每一步做了什么，也能看到报告生成是否命中缓存、是否发生锁竞争。

这个设计的重点是：框架只是 Provider，Agent Runtime 和 Tool Registry 才是项目的核心安全边界。

## 4. 常见追问

### 为什么没有直接用 Spring AI Agent 替代 Runtime？

因为项目需要业务级 trace、Tool 风险控制、用户确认、Memory 治理和训练报告结构化。Spring AI 更适合作为模型和工具生态适配层，不适合替代业务 Agent Runtime。

### Function Calling 如何保证安全？

Tool Schema 从 Tool Registry 导出，白名单只包含低风险查询类工具。模型返回 tool_call 后仍交给 Tool Executor，Executor 会检查 riskLevel、executionMode、confirmation 和 userId 权限。

### RAG 如何保证用户隔离？

RagRetrievalService 会移除前端传入的 userId，并根据 sourceType 自动注入 ownerType 和当前登录 userId。用户文档、项目档案只检索当前用户数据，系统知识文章才允许公共读取。

### Memory 会不会保存隐私原文？

不会保存完整简历、文档或回答，只保存摘要化的 weakness、risk、goal、nextAction 等结构化记忆。Semantic Memory 的向量 payload 也是摘要。

### 这个项目和 OnCall Agent / AI-Meeting 对齐在哪里？

对齐的是工程形态：RAG、Prompt、Tool/Function Calling、ReAct/Plan-Execute、多 Agent、SSE、MCP、长期记忆、Single-flight、长会话恢复和可观测。业务上 CodeCoach AI 保持面试训练特色。

## 5. 不应夸大的点

- Spring AI / Spring AI Alibaba 是可选 Provider，不是默认生产依赖。
- MCP 当前是低风险 MCP-style Server / endpoint，不默认公网开放。
- OTel exporter 是可选启用，默认仍以站内 observability 为主。
- 模拟面试的提问流式链路保留原稳定实现，Gateway streaming 是预留接口。
- 没有引入摄像头、神态分析、后端 TTS/ASR 等高风险或无关能力。
