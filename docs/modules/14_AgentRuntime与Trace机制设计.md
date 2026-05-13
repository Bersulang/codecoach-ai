# Agent Runtime 与 Trace 机制设计

## 1. 文档说明

本文档用于设计 CodeCoach AI 的 Agent Runtime 与 Trace 机制。

当前 CodeCoach AI 已经完成：

```text
项目拷打训练
八股问答训练
真实模拟面试
简历驱动训练
用户文档 RAG
知识文章 RAG
项目档案 RAG
产品内 AI Guide
面试复盘 Agent
token 级流式输出
统一 Tool / Action 调用协议
agent_tool_trace 工具调用追踪
```

当前系统已经具备 Tool Calling 雏形，但还缺少统一 Agent Runtime。

也就是说，目前系统已经能做到：

```text
Guide 返回 ToolAction
用户点击 Action
后端执行 Tool
记录 Tool Trace
```

但还没有完整做到：

```text
Agent 接收任务
-> 构建上下文
-> 选择工具
-> 执行工具
-> 收集观察结果
-> 生成最终回答
-> 记录完整 Agent Run
```

因此，需要新增 Agent Runtime 与 Trace 机制。

---

## 2. 模块定位

Agent Runtime 是 CodeCoach AI 后续 Agent 化升级的核心底座。

它不是一个新业务页面，也不是一个新训练功能。

它的定位是：

```text
统一管理 Agent 的运行过程、上下文、工具调用、执行步骤、失败降级和可观测记录。
```

它服务于：

```text
产品内 Guide Agent
面试复盘 Agent
真实模拟面试 Agent
简历训练 Agent
未来 ReAct Agent
未来 Plan-Execute Agent
未来 Multi-Agent 协作
未来 MCP 工具适配
```

---

## 3. 为什么需要 Agent Runtime

当前问题：

```text
Guide 有自己的上下文构建逻辑
复盘 Agent 有自己的数据聚合逻辑
模拟面试有自己的阶段推进逻辑
简历训练有自己的 Prompt 调用逻辑
Tool Trace 只能记录单次工具调用
无法完整追踪一次 Agent 任务
```

如果继续分散实现，会导致：

```text
Agent 行为不可解释
工具调用链路不可还原
多个 Agent 之间无法复用上下文
无法演进到 ReAct / Plan-Execute
无法做统一观测面板
面试时仍然容易被认为只是 Prompt 调接口
```

Agent Runtime 的目标是把这些能力统一起来。

---

## 4. Agent Runtime 的核心目标

Agent Runtime 需要支持：

```text
Agent Run 生命周期管理
Agent Step 记录
Tool Call 串联
上下文构建
工具选择
工具执行
观察结果收集
最终回答生成
失败降级
Trace 查询
```

第一版不需要实现复杂 ReAct 循环，但必须为后续演进预留结构。

---

## 5. 目标执行流程

标准 Agent 执行流程：

```text
用户请求
-> 创建 AgentRun
-> 构建 AgentContext
-> 判断 Agent 类型
-> 规则或 AI 生成 tool intent
-> 匹配 Tool Registry
-> 执行 Tool
-> 记录 AgentStep
-> 记录 ToolCall
-> 收集 Observation
-> 生成最终 AgentResponse
-> 更新 AgentRun 状态
-> 返回前端
```

---

## 6. Agent 类型设计

第一版建议支持以下 Agent 类型：

```text
GUIDE
REVIEW
MOCK_INTERVIEW
RESUME
QUESTION_TRAINING
PROJECT_TRAINING
```

第一版重点接入：

```text
GUIDE
```

后续逐步接入：

```text
REVIEW
MOCK_INTERVIEW
```

---

## 7. AgentRun

AgentRun 表示一次完整 Agent 任务。

例如：

```text
用户问 Guide：我下一步该做什么？
```

这应该是一条 AgentRun。

AgentRun 需要记录：

```text
runId
userId
agentType
inputSummary
status
finalAnswerSummary
errorCode
latencyMs
createdAt
updatedAt
```

状态建议：

```text
RUNNING
SUCCEEDED
FAILED
CANCELLED
```

---

## 8. AgentStep

AgentStep 表示 Agent 运行中的一个步骤。

例如：

```text
构建上下文
识别意图
选择工具
执行工具
生成回答
```

每个 Step 需要记录：

```text
stepId
runId
stepType
stepName
inputSummary
outputSummary
status
latencyMs
errorCode
createdAt
```

Step 类型建议：

```text
CONTEXT_BUILD
INTENT_DETECT
LLM_CALL
TOOL_SELECT
TOOL_EXECUTE
OBSERVATION
RESPONSE_COMPOSE
FALLBACK
```

---

## 9. ToolCall 与 AgentStep 的关系

当前已经有：

```text
agent_tool_trace
```

它记录的是工具调用。

Agent Runtime 需要将 ToolCall 和 AgentRun 关联起来。

关系：

```text
AgentRun 1 - N AgentStep
AgentStep 1 - N ToolCall
```

如果当前 agent_tool_trace 表没有 runId，可以后续补充。

第一版可以通过 traceId 关联。

---

## 10. AgentContext

AgentContext 是 Agent 运行时使用的上下文摘要。

它可以包含：

```text
当前路径
用户输入
登录状态
用户摘要
项目数量
文档数量
简历数量
能力画像摘要
最近训练摘要
最新复盘摘要
可用 Tool 列表
```

禁止包含：

```text
完整简历原文
完整用户文档
完整训练回答
完整 Prompt
完整向量
API Key
Authorization Header
```

---

## 11. Observation

Observation 是 Tool 执行后的观察结果。

例如：

```text
GET_ABILITY_SUMMARY 返回 Redis 维度偏弱
SEARCH_KNOWLEDGE 返回 3 篇相关文章
START_QUESTION_TRAINING 创建 sessionId=12
```

Observation 应该是摘要化的，方便 Agent 后续生成回答。

---

## 12. AgentResponse

AgentResponse 是最终返回给前端的结果。

包括：

```text
answer
actions
observations
runId
traceId
status
```

其中：

```text
answer：用户可见回答
actions：可点击行动卡片
observations：可选，用于调试或内部展示
runId：本次 Agent 运行 ID
traceId：全链路追踪 ID
```

---

## 13. 第一版范围

第一版不做复杂 Agent 循环。

第一版重点做：

```text
统一 AgentRun
统一 AgentStep
统一 AgentContext
统一 AgentResponse
Guide Agent 接入 Agent Runtime
Tool 执行关联 AgentRun
Trace 可查询
```

第一版不做：

```text
完整 ReAct
完整 Plan-Execute
完整 Multi-Agent
长期 Memory
MCP
复杂 Agent 编排 DSL
```

---

## 14. Guide Agent 接入方式

当前 Guide 流程可能是：

```text
/api/guide/chat
-> GuideChatService
-> 规则 / AI
-> 返回 answer + actions
```

升级后变为：

```text
/api/guide/chat
-> AgentRuntime.run(GUIDE, input)
-> Build Context
-> Detect Intent
-> Select ToolAction
-> Compose Response
-> Record AgentRun / AgentStep
-> Return AgentResponse
```

这样 Guide 就不再是普通 Controller + Service，而是一个基于 Agent Runtime 的 Agent。

---

## 15. Runtime 与 Tool Registry 的关系

Agent Runtime 不直接写具体业务逻辑。

它调用：

```text
Tool Registry
Tool Executor
Context Builder
LLM Gateway
Trace Recorder
```

Tool Registry 负责：

```text
有哪些 Tool
Tool 能不能执行
Tool 风险等级
Tool 是否需要确认
Tool 参数结构
```

Agent Runtime 负责：

```text
什么时候选择 Tool
如何记录过程
如何组织最终回答
```

---

## 16. Runtime 与 LLM 的关系

第一版 Runtime 可以支持两种模式：

```text
规则模式
LLM 模式
```

规则模式：

```text
明确导航类问题直接规则响应
```

LLM 模式：

```text
个性化建议和复杂意图识别调用大模型
```

无论哪种模式，都应该记录 AgentStep。

例如：

```text
INTENT_DETECT
TOOL_SELECT
RESPONSE_COMPOSE
```

---

## 17. Trace 记录原则

Agent Trace 不能记录敏感原文。

允许记录：

```text
userId
agentType
stepType
toolName
输入摘要
输出摘要
耗时
成功失败
错误码
```

禁止记录：

```text
完整简历
完整用户文档
完整训练回答
完整 Prompt
完整模型响应
完整向量
API Key
Authorization Header
OSS Secret
```

---

## 18. 数据模型建议

第一版可以新增：

```text
agent_run
agent_step
```

如果不想新增太多表，也可以先只新增：

```text
agent_run
```

但建议至少有 `agent_step`，否则无法体现 Agent 执行过程。

---

## 19. agent_run 表建议

字段建议：

```text
id
run_id
user_id
agent_type
status
input_summary
output_summary
error_code
error_message
latency_ms
created_at
updated_at
```

---

## 20. agent_step 表建议

字段建议：

```text
id
run_id
user_id
agent_type
step_type
step_name
tool_name
input_summary
output_summary
status
error_code
latency_ms
created_at
```

---

## 21. 与 agent_tool_trace 的关系

`agent_tool_trace` 继续保留。

后续建议补充：

```text
run_id
step_id
```

这样可以完整串联：

```text
agent_run
-> agent_step
-> agent_tool_trace
```

如果第一版不方便改表，可以先用 traceId 关联。

---

## 22. Runtime API

Agent Runtime 不一定需要直接暴露给前端。

但可以提供内部服务：

```text
AgentRuntime.run(AgentRequest request)
```

输入：

```text
agentType
message
currentPath
frontendContext
```

输出：

```text
answer
actions
runId
traceId
```

---

## 23. 可观测查询接口

第一版可以只写入数据，不做前端面板。

后续 OBS-001 再做页面。

可以先提供调试接口：

```text
GET /api/agent/runs/{runId}
GET /api/agent/runs/{runId}/steps
```

也可以暂时不暴露接口，只保留数据库记录。

---

## 24. 失败降级

Agent Runtime 必须支持失败降级。

场景：

```text
上下文构建失败
LLM 调用失败
Tool 选择失败
Tool 执行失败
Response 组装失败
```

处理原则：

```text
记录失败 Step
更新 AgentRun 为 FAILED 或 PARTIAL
返回友好回答
提供基础 action
不影响主业务
```

---

## 25. Agent Runtime 与后续 ReAct

ReAct 需要循环：

```text
Thought
Action
Observation
Thought
Action
Observation
Final Answer
```

第一版不用暴露 Thought，但可以记录：

```text
stepType = TOOL_SELECT
stepType = TOOL_EXECUTE
stepType = OBSERVATION
stepType = RESPONSE_COMPOSE
```

这样后续可以自然升级。

---

## 26. Agent Runtime 与后续 Plan-Execute

Plan-Execute 需要：

```text
先生成计划
再逐步执行
最后总结
```

第一版可以预留：

```text
stepType = PLAN
stepType = EXECUTE
```

模拟面试后续可以使用。

---

## 27. Agent Runtime 与后续 Multi-Agent

Multi-Agent 需要多个 Agent 共享 Runtime。

例如：

```text
Guide Agent
Review Agent
Interviewer Agent
Evaluator Agent
Retriever Agent
```

共享：

```text
AgentRun
AgentStep
Tool Registry
Trace Recorder
Context Builder
```

---

## 28. Agent Runtime 与 Spring AI

引入 Spring AI / Spring AI Alibaba 时，Runtime 不应该被替换。

正确关系：

```text
CodeCoach Agent Runtime
-> AI Provider Adapter
-> Spring AI / Spring AI Alibaba
-> LLM / Embedding / VectorStore
```

Spring AI 是底层 AI 抽象和生态能力。

Agent Runtime 是业务场景 Agent 编排底座。

---

## 29. 验收标准

第一版完成后应满足：

```text
Guide Agent 通过 Agent Runtime 执行
每次 Guide 请求生成 AgentRun
关键步骤生成 AgentStep
Tool 调用能关联到本次 AgentRun 或 traceId
失败时能记录失败原因
用户可见响应不受影响
不泄露隐私
不破坏现有 Tool 协议
不破坏 Guide 前端体验
```

---

## 30. 简历表达方向

完成后可以描述为：

```text
设计并实现 Agent Runtime 与 Trace 机制，将 Guide Agent、Tool Registry 和站内 Tool 调用纳入统一运行流程，通过 AgentRun、AgentStep 和 ToolTrace 记录 Agent 的上下文构建、意图识别、工具选择、工具执行和响应生成过程，为后续 ReAct、Plan-Execute、Multi-Agent 和 MCP 适配提供可演进底座。
```

---

## 31. 总结

Agent Runtime 是 CodeCoach AI 从“Tool Calling 雏形”升级为“可演进 Agent 架构”的关键一步。

它不是为了新增页面，而是为了让整个 AI 系统具备：

```text
统一运行入口
统一工具调用
统一 Trace
统一失败降级
统一可观测基础
```

完成后，后续每个 Agent 都可以逐步迁移到这个 Runtime 上。