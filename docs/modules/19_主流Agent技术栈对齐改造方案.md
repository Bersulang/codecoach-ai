# 主流 Agent 技术栈对齐改造方案

## 1. 文档说明

本文档用于规划 CodeCoach AI 从“自研 Agent 架构”进一步对齐主流 AI Agent 技术栈。

当前 CodeCoach AI 已经完成：

```text
Tool / Action 协议
Agent Runtime
AgentRun / AgentStep Trace
Guide Agent ReAct
模拟面试 Plan-Execute
Memory
AI / RAG / Agent Observability
用户文档 RAG
项目档案 RAG
知识文章 RAG
简历驱动训练
真实模拟面试
token 级流式输出
```

当前项目已经具备明显 Agent 化特征，但如果目标是投递大模型应用开发 / AI Agent 应用开发岗位，还需要进一步对齐主流技术栈：

```text
Spring AI / Spring AI Alibaba
Tool Calling / Function Calling
RAG Pipeline 增强
Semantic Memory
Multi-Agent 协作
MCP
OpenTelemetry / 标准 Trace
```

本次改造的目标不是为了堆技术名词，而是让项目架构真正具备主流 Agent 项目的工程表达能力。

---

## 2. 改造目标

本阶段目标：

```text
让 CodeCoach AI 从“自研 Agent 能力”升级为“对齐主流 Java AI Agent 技术栈的工程化项目”。
```

具体目标：

```text
引入 Spring AI / Spring AI Alibaba POC
抽象 AI Model Gateway
将 Tool Registry 适配为 Function Calling 能力
升级 RAG Pipeline
引入语义 Memory
形成 Multi-Agent 协作雏形
提供 MCP Server POC
为 OpenTelemetry / 标准 Trace 做准备
```

---

## 3. 改造原则

必须遵守：

```text
不推倒重写
不破坏现有主链路
不为了框架而框架
优先 POC，再 Adapter，再局部迁移
保留自研 Agent Runtime 的业务控制权
保留 Tool 风险等级、权限校验、用户确认机制
所有新技术必须服务于真实业务场景
```

---

## 4. 为什么不能直接全量替换

当前主链路已经包含：

```text
训练状态机
RAG 检索
报告生成
评分 Rubric
能力画像
Memory
Agent Trace
Tool 风险控制
用户私有数据权限隔离
```

如果直接全量换成某个框架，风险包括：

```text
破坏 token 流式输出
破坏已有 RAG 权限过滤
破坏报告和评分链路
破坏 Guide / 模拟面试 / 复盘 Agent
引入大量不稳定依赖
导致项目不可控
```

因此采用：

```text
POC -> Adapter -> 局部替换 -> 逐步迁移
```

---

## 5. 目标架构

升级后的目标架构：

```text
Frontend
  |
  | ChatGPT App 式交互 / Streaming / Action Card
  v
Business Agent Layer
  |
  +-- Guide Agent
  +-- Review Agent
  +-- Mock Interview Agent
  +-- Resume Agent
  +-- Training Agent
  |
  v
CodeCoach Agent Runtime
  |
  +-- ReAct Loop
  +-- Plan-Execute
  +-- AgentRun / AgentStep
  +-- Memory
  +-- Observability
  |
  v
Tool / Function Layer
  |
  +-- Tool Registry
  +-- Tool Executor
  +-- Tool Risk Level
  +-- Tool Confirmation
  +-- Function Calling Adapter
  +-- MCP Adapter
  |
  v
AI Gateway Layer
  |
  +-- OpenAI-Compatible Gateway
  +-- Spring AI Gateway
  +-- Spring AI Alibaba Gateway
  |
  v
RAG / Memory Layer
  |
  +-- Query Rewrite
  +-- Hybrid Retrieval
  +-- Rerank
  +-- RAG Evaluation
  +-- Vector Memory
  +-- Qdrant
  |
  v
Infrastructure
  |
  +-- MySQL
  +-- Redis
  +-- Aliyun OSS
  +-- Qdrant
  +-- OpenTelemetry / Trace
```

---

## 6. 改造模块一：Spring AI / Spring AI Alibaba POC

### 6.1 目标

引入 Spring AI / Spring AI Alibaba POC，使项目具备主流 Java AI 框架适配能力。

第一版不替换主链路。

### 6.2 POC 范围

```text
新增独立 dev 接口
使用 Spring AI ChatClient 完成一次模型调用
支持配置隔离
记录调用耗时
不影响现有 OpenAI-Compatible 调用
```

### 6.3 价值

```text
对齐 Java AI 岗位关键词
验证依赖兼容性
为后续 ChatClient / Tool Calling / Advisor / Graph 适配做准备
```

---

## 7. 改造模块二：AI Model Gateway Adapter

### 7.1 目标

统一模型调用入口。

从：

```text
各模块直接调用当前 AI Service
```

升级为：

```text
业务 Agent -> AiModelGateway -> Provider 实现
```

### 7.2 适配实现

```text
OpenAiCompatibleGateway
SpringAiGateway
SpringAiAlibabaGateway，后续
```

### 7.3 支持能力

```text
chat
streamChat
structuredOutput
toolIntentParse
embedding，后续可拆
```

### 7.4 价值

```text
避免模型调用散落在业务模块
便于后续切换 Spring AI
便于做灰度和影子调用
便于统一日志和错误处理
```

---

## 8. 改造模块三：Function Calling Adapter

### 8.1 当前问题

当前 Tool Calling 主要是：

```text
LLM 输出 JSON tool intent
后端解析
Tool Registry 校验
Tool Executor 执行
```

这已经是 Agent Tool Calling 的雏形，但还不是模型原生 Function Calling。

### 8.2 目标

新增 Function Calling Adapter。

要求：

```text
保留现有 Tool Registry
保留权限校验
保留风险等级
保留用户确认
将 Tool 定义转换为模型可识别的 function schema
支持模型 tool_calls 解析
支持回退到 JSON intent
```

### 8.3 不允许

```text
不允许模型直接调用任意 API
不允许模型绕过 Tool Registry
不允许模型自动执行中高风险工具
```

### 8.4 价值

```text
让项目从“自研 JSON 工具意图”升级为“兼容模型原生 Function Calling 的 Tool Calling 架构”
```

---

## 9. 改造模块四：RAG Pipeline 增强

### 9.1 当前 RAG 能力

已有：

```text
文档上传
文本解析
切片
Embedding
Qdrant 检索
MySQL 元数据
Prompt 注入
用户权限过滤
```

### 9.2 缺失能力

```text
Query Rewrite
Hybrid Retrieval
Rerank
RAG Evaluation
RAG Trace
命中质量分析
```

### 9.3 本阶段目标

新增标准化 RAG Pipeline：

```text
Query Analyze
Query Rewrite
Retrieve
Rerank
Context Compress
Prompt Context Build
RagTrace
```

### 9.4 优先实现

```text
Query Rewrite
简单 Rerank
RAG Trace
RAG Evaluation 初版
```

### 9.5 价值

```text
让 RAG 从“向量搜索 + 拼上下文”升级为完整 RAG Pipeline
```

---

## 10. 改造模块五：Semantic Memory

### 10.1 当前 Memory

当前 Memory 偏结构化业务记忆：

```text
WEAKNESS
RESUME_RISK
PROJECT_RISK
NEXT_ACTION
MASTERED
```

### 10.2 目标

引入语义 Memory。

流程：

```text
Memory 文本摘要
-> Embedding
-> 写入 Qdrant 或 memory vector collection
-> Agent 查询时通过语义检索召回相关 Memory
```

### 10.3 适用场景

```text
Guide Agent 个性化建议
模拟面试优先追问长期薄弱点
复盘 Agent 长期归因
训练推荐
```

### 10.4 价值

```text
让 Memory 从结构化标签升级为可语义检索的长期记忆系统
```

---

## 11. 改造模块六：Multi-Agent 协作雏形

### 11.1 当前状态

当前多 Agent 主要是逻辑分层：

```text
Guide Agent
Review Agent
Mock Interview Agent
Resume Agent
Training Agent
```

还没有真正 Agent-to-Agent 协作。

### 11.2 目标

在模拟面试或复盘场景中实现 Multi-Agent 协作雏形。

推荐第一版：

```text
Interviewer Agent
Evaluator Agent
Coach Agent
Retriever Agent
```

### 11.3 第一版协作方式

不要做复杂分布式 Multi-Agent。

采用 Runtime 内部协作：

```text
Interviewer Agent 生成问题
Evaluator Agent 评估回答质量
Retriever Agent 提供 RAG 上下文
Coach Agent 生成最终复盘建议
```

### 11.4 价值

```text
让项目从“多个 AI 模块”升级为“多 Agent 分工协作”
```

---

## 12. 改造模块七：MCP Server POC

### 12.1 目标

将部分站内 Tool 暴露为 MCP Server POC。

### 12.2 第一批 MCP Tool

只暴露低风险或开发场景工具：

```text
get_ability_summary
search_knowledge
get_recent_training_summary
get_resume_risk_summary
```

暂不暴露：

```text
delete_project
delete_document
modify_profile
clear_history
```

中风险工具需要谨慎：

```text
start_question_training
generate_review
analyze_resume
```

### 12.3 价值

```text
对齐当前 Agent 工具协议生态
让外部 MCP Client 可调用 CodeCoach 的部分训练上下文能力
```

### 12.4 安全原则

```text
只允许白名单工具
需要认证
限制用户数据范围
限制返回摘要
不返回完整文档和 Prompt
记录 MCP Tool Trace
```

---

## 13. 改造模块八：OpenTelemetry / 标准 Trace 预埋

### 13.1 当前状态

当前可观测性偏自研：

```text
agent_run
agent_step
agent_tool_trace
ai_call_log
/dev/observability
```

### 13.2 目标

为标准化 Trace 做准备。

第一版可以：

```text
统一 traceId
统一 span 命名
为 AgentRun / ToolCall / LLMCall / RAGCall 建立映射
预留 OpenTelemetry 接入点
```

### 13.3 不强制第一版完成

```text
不一定马上接 Prometheus / Grafana
不一定马上接 Jaeger
不一定马上做完整 OTel exporter
```

### 13.4 价值

```text
让自研可观测逐步对齐标准可观测体系
```

---

## 14. 分阶段实施顺序

### 阶段一：标准框架 POC

```text
SPRINGAI-POC-001
AI Model Gateway Adapter
```

### 阶段二：Tool Calling 标准化

```text
Function Calling Adapter
Tool Schema Export
Tool Calls Parser
```

### 阶段三：RAG Pipeline 标准化

```text
Query Rewrite
Rerank
RAG Trace
RAG Evaluation
```

### 阶段四：Memory 语义化

```text
Semantic Memory Embedding
Memory Vector Retrieval
Memory Tool 升级
```

### 阶段五：Multi-Agent 协作

```text
Interviewer Agent
Evaluator Agent
Retriever Agent
Coach Agent
```

### 阶段六：MCP POC

```text
MCP Server
MCP Tool WhiteList
MCP Trace
```

### 阶段七：标准 Trace

```text
OpenTelemetry 预埋
TraceId 贯通
Span 命名规范
```

---

## 15. 简历表达升级

完成本阶段后，项目可描述为：

```text
基于 Java 21 + Spring Boot 3 构建 AI Agent 面试训练平台，接入 Spring AI / Spring AI Alibaba POC 并设计 AI Model Gateway 适配层；在自研 Agent Runtime、Tool Registry、Memory 和 Observability 基础上，实现 ReAct、Plan-Execute、Function Calling Adapter、多源 RAG Pipeline、Semantic Memory 和 Multi-Agent 协作雏形。
```

---

## 16. 面试讲解思路

可以这样讲：

```text
最开始这个项目是一个 AI 面试训练系统，后来我把它逐步改造成 Agent 架构。

底层不是让业务直接调用模型，而是抽象了 AI Model Gateway，可以兼容当前 OpenAI-Compatible 调用和 Spring AI ChatClient。

中间层是 Agent Runtime，负责 AgentRun、AgentStep、ReAct Loop、Plan-Execute、Memory 和 Observability。

工具层是 Tool Registry，每个 Tool 有风险等级、执行模式和权限校验，并逐步适配模型原生 Function Calling 和 MCP。

RAG 层从简单向量检索升级为 Query Rewrite、Rerank、RAG Trace 和 Evaluation。

最后在模拟面试和复盘场景中引入 Multi-Agent 分工，例如 Interviewer、Evaluator、Retriever 和 Coach。
```

---

## 17. 不要过度包装

在未完成前不能写：

```text
已全面接入 Spring AI Alibaba
已完整实现 MCP
已实现生产级 Multi-Agent
已接入完整 OpenTelemetry
```

应该写：

```text
已完成自研 Agent Runtime 和 Tool Registry
正在通过 Adapter 方式接入 Spring AI / Spring AI Alibaba
已完成 Function Calling / MCP / Multi-Agent 的 POC 或适配规划
```

---

## 18. 阶段完成标准

本阶段最终应满足：

```text
项目具备主流 Java AI Agent 技术栈表达
Spring AI POC 可运行
AI Model Gateway 可扩展
Tool Registry 可导出 Function Schema
RAG Pipeline 有 Query Rewrite / Rerank / Trace
Memory 支持语义检索
至少一个场景具备 Multi-Agent 协作雏形
至少一组低风险工具可通过 MCP POC 暴露
TraceId 能贯通 Agent / Tool / LLM / RAG
```

---

## 19. 总结

本阶段不是为了炫技，而是为了让 CodeCoach AI 的技术架构从：

```text
自研 AI 应用
```

升级为：

```text
对齐主流技术栈的 Java AI Agent 项目
```

核心改造方向：

```text
Spring AI / Spring AI Alibaba
Function Calling
RAG Pipeline
Semantic Memory
Multi-Agent
MCP
OpenTelemetry
```

这些能力将显著增强项目的简历价值和面试可讲性。