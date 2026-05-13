# AI / RAG / Agent 可观测性设计

## 1. 文档说明

本文档用于设计 CodeCoach AI 的 AI / RAG / Agent 可观测性模块。

当前 CodeCoach AI 已经完成：

```text
AI 大模型调用日志
RAG 检索
用户文档知识库
Qdrant 向量库
Tool / Action 调用协议
Agent Runtime
AgentRun / AgentStep
ReAct 式 Guide Agent
Plan-Execute 模拟面试 Agent
agent_tool_trace
token 级流式输出
```

当前系统已经具备 AI Agent 项目的核心执行链路，但还缺少一个统一的可观测入口。

也就是说，系统已经能做：

```text
模型调用
RAG 检索
Tool 调用
Agent 执行
Trace 记录
```

但还不能很好地回答：

```text
这次 Agent 为什么这样回答？
它调用了哪些工具？
RAG 命中了哪些内容？
哪一步最耗时？
哪次 AI 调用失败了？
哪个 Agent 最不稳定？
```

因此需要新增 AI / RAG / Agent 可观测性模块。

---

## 2. 模块定位

AI / RAG / Agent 可观测性模块不是普通用户功能。

它的定位是：

```text
面向开发者、项目演示和后续调试的 AI Agent 执行观测模块。
```

它服务于：

```text
开发调试
面试展示
性能分析
RAG 命中分析
Agent Trace 回放
Tool 调用排查
AI 调用成本和耗时分析
```

它不是：

```text
普通用户训练页
后台管理系统
运营数据大屏
商业 BI 系统
```

第一版可以只面向开发者使用。

---

## 3. 为什么需要可观测性

一个真正的 AI Agent 项目不能只有最终回答。

它还需要能解释：

```text
Agent 做了什么
用了哪些上下文
调用了哪些工具
RAG 有没有命中
LLM 调用是否成功
失败时降级到哪里
```

如果没有可观测性，项目很容易被认为是：

```text
Prompt + API 调用
```

有了可观测性，可以证明系统是：

```text
Agent Runtime
+ Tool Calling
+ RAG
+ Trace
+ 可调试执行链路
```

---

## 4. 核心目标

第一版可观测性模块需要支持：

```text
AI 调用日志查看
Agent Run 查看
Agent Step 查看
Tool Trace 查看
RAG 检索结果摘要查看
耗时统计
成功失败统计
错误原因查看
单次 Agent 执行链路回放
```

---

## 5. 可观测对象

需要观测的对象包括：

```text
LLM Call
Embedding Call
RAG Retrieval
Agent Run
Agent Step
Tool Call
Streaming Response
Report Generation
```

第一版优先观测：

```text
Agent Run
Agent Step
Tool Call
AI Call Log
RAG Retrieval Summary
```

---

## 6. 数据来源

当前已有或应复用的数据来源：

```text
ai_call_log
agent_run
agent_step
agent_tool_trace
rag_document
rag_chunk
rag_embedding
interview_report
question_training_report
mock_interview_report
```

如果已有 RAG 检索 trace 表，优先复用。

如果没有，第一版可以在相关查询结果中记录轻量摘要。

---

## 7. 页面规划

新增页面：

```text
/dev/observability
```

或：

```text
/observability
```

建议第一版使用：

```text
/dev/observability
```

表示这是开发者观测页面，不是普通用户页面。

页面名称：

```text
AI 可观测性
```

页面副标题：

```text
查看 AI 调用、RAG 检索、Tool 调用和 Agent 执行链路。
```

---

## 8. 页面结构

页面包含：

```text
顶部指标卡片
Agent Run 列表
AI 调用列表
Tool 调用列表
Agent Trace 详情
错误日志摘要
耗时分析
```

第一版可以做成简洁页面，不需要复杂图表。

---

## 9. 顶部指标卡片

展示：

```text
今日 Agent Runs
今日 AI Calls
今日 Tool Calls
平均 Agent 耗时
平均 LLM 耗时
失败次数
```

如果没有今日统计，也可以展示最近 24 小时或最近 100 条。

---

## 10. Agent Run 列表

展示字段：

```text
runId
agentType
status
latencyMs
createdAt
inputSummary
outputSummary
```

支持：

```text
按 agentType 过滤
按 status 过滤
点击查看详情
```

---

## 11. Agent Run 详情

详情页或右侧抽屉展示：

```text
AgentRun 基本信息
AgentStep 时间线
Tool 调用
AI 调用摘要
错误信息
最终回答摘要
```

重点是时间线。

示例：

```text
CONTEXT_BUILD 32ms
TOOL_INTENT 120ms
TOOL_EXECUTE GET_ABILITY_SUMMARY 48ms
OBSERVATION 12ms
TOOL_EXECUTE SEARCH_KNOWLEDGE 133ms
RESPONSE_COMPOSE 980ms
```

---

## 12. Agent Step 时间线

每个 Step 展示：

```text
stepType
stepName
toolName
status
latencyMs
inputSummary
outputSummary
errorCode
createdAt
```

不要展示：

```text
完整 prompt
完整模型响应
完整文档内容
完整用户回答
```

---

## 13. Tool 调用列表

展示：

```text
traceId
runId
agentType
toolName
toolType
success
latencyMs
errorCode
createdAt
```

支持：

```text
按 toolName 过滤
按 success 过滤
按 agentType 过滤
```

---

## 14. Tool 调用详情

展示：

```text
输入摘要
输出摘要
风险等级
执行模式
是否成功
错误原因
耗时
关联 AgentRun
```

用于证明：

```text
Agent 调用了受控 Tool
Tool 调用经过白名单和权限校验
```

---

## 15. AI 调用列表

从 `ai_call_log` 展示：

```text
provider
modelName
requestType
promptVersion
latencyMs
success
errorCode
createdAt
```

不要展示完整 prompt。

可以展示：

```text
请求类型
模型
耗时
成功失败
错误码
```

---

## 16. RAG 检索观测

RAG 可观测需要回答：

```text
查了什么
命中了什么
命中分数如何
用了哪些 sourceType
是否过滤 userId / projectId
是否失败降级
```

第一版可以展示轻量摘要：

```text
querySummary
sourceTypes
topK
hitCount
latencyMs
success
createdAt
```

如果当前没有 RAG trace 表，可以作为后续增强。

第一版至少在 AgentStep / ToolTrace 中记录 RAG 工具输出摘要。

---

## 17. 错误分析

页面应能看到：

```text
最近失败的 AgentRun
最近失败的 ToolCall
最近失败的 AI Call
错误码分布
```

第一版可以简单列表展示。

---

## 18. 耗时分析

至少展示：

```text
Agent 平均耗时
LLM 平均耗时
Tool 平均耗时
最慢的 10 次 AgentRun
最慢的 10 次 ToolCall
```

这对排查性能非常有价值。

---

## 19. 权限控制

可观测性页面不应面向所有普通用户。

第一版可选策略：

```text
仅开发环境展示
仅管理员可访问
仅当前用户查看自己的 Trace
```

由于当前项目可能没有管理员体系，第一版建议：

```text
只允许当前用户查看自己的 AgentRun / ToolTrace / AI Call。
```

如果是本地开发，可以先放在 Dashboard 入口之外，只通过 URL 访问。

---

## 20. 隐私和安全

必须保证：

```text
不展示完整简历
不展示完整文档
不展示完整训练回答
不展示完整 Prompt
不展示完整模型响应
不展示 API Key
不展示 Authorization Header
不展示 OSS Secret
不展示 embedding vector
```

所有展示内容必须是摘要。

---

## 21. 与 Agent Runtime 的关系

可观测性依赖：

```text
agent_run
agent_step
agent_tool_trace
```

Agent Runtime 负责写入。

Observability 页面负责读取和展示。

不要让 Observability 反向影响 Agent 执行逻辑。

---

## 22. 与面试展示的关系

这个模块非常适合面试讲解。

可以展示：

```text
Guide Agent 如何调用能力画像工具
ReAct Loop 如何产生 Observation
模拟面试 Plan-Execute 如何推进阶段
RAG 是否命中用户文档
Tool 调用是否成功
```

这能证明项目不是简单调用大模型 API。

---

## 23. MVP 范围

第一版做：

```text
后端查询接口
AgentRun 列表
AgentRun 详情
AgentStep 时间线
ToolTrace 列表
AI Call 列表
顶部统计卡片
简单过滤
前端页面展示
```

第一版不做：

```text
复杂图表
成本统计
Token 精确计费
链路拓扑图
Prometheus / Grafana
LangSmith 集成
OpenTelemetry 全链路追踪
外部告警系统
```

---

## 24. 后续增强方向

后续可以做：

```text
RAG Trace 表
LLM Token 用量统计
模型成本估算
Agent Run 可视化时间轴
RAG 命中质量评估
Prompt 版本对比
失败自动归因
可观测性接入 OpenTelemetry
```

---

## 25. 阶段完成标准

完成后应满足：

```text
能查看最近 AgentRun
能查看 AgentStep 时间线
能查看 ToolCall 记录
能查看 AI Call 记录
能看到成功失败和耗时
能打开一次 Guide Agent 的执行链路
不会泄露隐私原文
不影响主业务
```

---

## 26. 简历表达方向

完成后可以描述为：

```text
设计并实现 AI / RAG / Agent 可观测性模块，基于 AgentRun、AgentStep、ToolTrace 和 AI Call Log 构建 Agent 执行链路追踪能力，支持查看 Agent 上下文构建、工具选择、工具执行、Observation 汇总和最终响应生成过程，提升 RAG 与 Agent 调试效率。
```

进一步可以描述为：

```text
通过可观测性页面展示不同 Agent 的执行时间线、Tool 调用结果、LLM 调用耗时和失败原因，辅助排查 RAG 命中不足、工具调用失败和模型响应超时等问题，使项目从简单 AI API 调用升级为可调试、可追踪的 AI Agent 系统。
```

---

## 27. 总结

AI / RAG / Agent 可观测性是 CodeCoach AI 变成真正 Agent 项目的关键支撑。

没有可观测性，Agent 很难解释和调试。

有了可观测性，系统可以回答：

```text
Agent 为什么这样回答？
它用了哪些工具？
RAG 查到了什么？
哪一步失败了？
哪里最耗时？
```

这也是面试中最能体现工程深度的部分之一。