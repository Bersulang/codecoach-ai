# ReAct 式 Agent 执行循环设计

## 1. 文档说明

本文档用于设计 CodeCoach AI 的 ReAct 式 Agent 执行循环。

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
统一 Tool / Action 协议
Agent Runtime
AgentRun / AgentStep Trace
token 级流式输出
```

当前系统已经具备 Agent 化基础：

```text
Tool Registry
Tool Executor
Tool Trace
Agent Runtime
Agent Step
Guide Agent
```

但目前大部分 Agent 行为仍然偏：

```text
一次输入
一次工具建议
一次响应
```

下一阶段需要升级为：

```text
Agent 能够基于上下文选择工具，执行工具，观察结果，再生成最终回答。
```

这就是 ReAct 思想在 CodeCoach AI 中的第一版落地。

---

## 2. 模块定位

ReAct 式 Agent 执行循环不是新增业务页面。

它是 Agent Runtime 的下一层能力，用于让 Agent 具备：

```text
Reasoning
Action
Observation
Final Response
```

在 CodeCoach AI 中，不能把模型完整推理链暴露给用户，也不能保存完整 chain-of-thought。

因此，本项目中的 ReAct 落地方式是：

```text
隐藏内部推理
记录 thought summary
执行受控 Tool
记录 observation
生成用户可见回答
```

也就是说，我们实现的是：

```text
可控、可观测、业务安全的 ReAct 式工具循环
```

而不是把模型自由思考和自由调用 API 暴露出来。

---

## 3. 为什么需要 ReAct 式执行循环

当前系统如果只是：

```text
LLM 生成 answer + actions
```

那仍然容易被认为是：

```text
Prompt + API 调用
```

真正的 Agent 项目需要体现：

```text
Agent 不只是生成文本
Agent 可以使用工具
Agent 可以根据工具结果调整回答
Agent 的过程可追踪
Agent 的工具调用是受控的
```

例如用户问 Guide Agent：

```text
我下一步该练什么？
```

普通做法：

```text
直接让 LLM 根据用户摘要回答。
```

Agent 做法：

```text
1. 查询能力画像摘要
2. 查询最近训练摘要
3. 查询最新复盘建议
4. 如果发现 Redis 薄弱，则检索 Redis 相关文章
5. 生成下一步建议和可执行 action
```

这才像 Agent。

---

## 4. 核心目标

第一版 ReAct 式执行循环需要支持：

```text
多步 Agent Step
Tool Intent 生成
Tool 选择
Tool 执行
Observation 汇总
最终回答生成
Trace 记录
失败降级
最大步数限制
工具白名单控制
```

第一版重点接入：

```text
Guide Agent
```

后续再接入：

```text
Review Agent
Mock Interview Agent
Resume Agent
```

---

## 5. ReAct 在本项目中的定义

标准 ReAct 常见形式：

```text
Thought
Action
Observation
Thought
Action
Observation
Final Answer
```

CodeCoach AI 中的安全形式：

```text
Reason Summary
Tool Intent
Tool Execution
Observation Summary
Response Compose
```

说明：

```text
不展示完整 Thought
不保存完整模型推理链
只保存可审计的摘要
所有 Tool 必须来自 Tool Registry
所有 Tool 执行必须经过权限校验
```

---

## 6. 目标执行流程

以 Guide Agent 为例：

```text
用户输入：我下一步该做什么？
-> 创建 AgentRun
-> 构建 AgentContext
-> LLM / 规则判断需要哪些信息
-> 选择 GET_ABILITY_SUMMARY
-> 执行 Tool
-> 得到 Observation：Redis、项目表达偏弱
-> 选择 GET_RECENT_TRAINING_SUMMARY
-> 执行 Tool
-> 得到 Observation：最近项目训练较少
-> 选择 SEARCH_KNOWLEDGE
-> 执行 Tool
-> 得到 Observation：Redis 缓存击穿文章
-> 生成最终回答
-> 返回 action cards
```

用户看到：

```text
你最近 Redis 和项目表达偏弱。建议先复习 Redis 缓存击穿，再做一次项目拷打。
```

用户不会看到完整推理链。

---

## 7. ReAct Step 类型

建议在现有 AgentStep 基础上支持：

```text
REASON
TOOL_INTENT
TOOL_EXECUTE
OBSERVATION
RESPONSE_COMPOSE
FALLBACK
```

如果当前已有：

```text
CONTEXT_BUILD
INTENT_DETECT
TOOL_SELECT
TOOL_EXECUTE
OBSERVATION
RESPONSE_COMPOSE
```

可以复用，不必强行新增枚举。

---

## 8. Tool Intent

Tool Intent 是 Agent 想要调用的工具意图。

结构建议：

```json
{
  "toolName": "GET_ABILITY_SUMMARY",
  "reason": "需要了解用户最近薄弱维度",
  "params": {}
}
```

注意：

```text
toolName 必须能在 Tool Registry 中匹配
params 必须校验
未知 toolName 必须拒绝
```

---

## 9. Observation

Observation 是 Tool 执行结果摘要。

示例：

```json
{
  "toolName": "GET_ABILITY_SUMMARY",
  "success": true,
  "summary": "用户最近 Redis、项目表达维度偏弱。"
}
```

Observation 会进入下一步响应生成。

禁止 Observation 包含：

```text
完整简历
完整文档
完整训练回答
完整向量
完整 Prompt
API Key
Authorization Header
```

---

## 10. 最大步数限制

ReAct 循环必须限制最大步数。

第一版建议：

```text
Guide Agent 最大 3 个 Tool Step
Review Agent 最大 5 个 Tool Step
Mock Interview Agent 最大 3 个 Tool Step
```

防止：

```text
无限循环
成本失控
延迟过长
用户等待太久
```

---

## 11. 工具调用权限

Agent 不能自动执行所有工具。

工具执行分三类：

```text
AUTO_EXECUTE
SUGGEST_ONLY
EXECUTE_AFTER_CONFIRM
```

在 ReAct 循环中：

```text
AUTO_EXECUTE 可以直接执行
SUGGEST_ONLY 只返回 action card
EXECUTE_AFTER_CONFIRM 只能作为 action card 返回，等待用户点击确认
```

例如：

```text
GET_ABILITY_SUMMARY：可以自动执行
SEARCH_KNOWLEDGE：可以自动执行
START_PROJECT_TRAINING：不能自动执行，只能建议
GENERATE_AGENT_REVIEW：不能自动执行，只能建议或用户确认
DELETE_PROJECT：禁止 Agent 执行
```

---

## 12. 第一版适合自动执行的工具

第一版可以允许 Agent 自动执行：

```text
GET_ABILITY_SUMMARY
GET_RECENT_TRAINING_SUMMARY
GET_RESUME_RISK_SUMMARY
SEARCH_KNOWLEDGE
SEARCH_USER_DOCUMENTS
```

这些工具只读或低风险。

---

## 13. 第一版不允许自动执行的工具

第一版不允许 Agent 自动执行：

```text
START_PROJECT_TRAINING
START_QUESTION_TRAINING
START_MOCK_INTERVIEW
GENERATE_AGENT_REVIEW
ANALYZE_RESUME
CREATE_PROJECT_FROM_RESUME
```

这些可以作为 action card 返回，由用户点击确认。

---

## 14. 高风险工具禁止

第一版禁止：

```text
删除项目
删除文档
删除简历
修改用户资料
覆盖报告
清空训练记录
```

即使 AI 输出了这类 intent，也必须拒绝。

---

## 15. Guide Agent ReAct 化

Guide Agent 是第一批最适合升级的 Agent。

升级前：

```text
用户问题
-> 规则 / AI
-> answer + actions
```

升级后：

```text
用户问题
-> Agent Runtime
-> Context Build
-> Tool Intent
-> 执行低风险查询工具
-> Observation
-> Response Compose
-> answer + actions
```

---

## 16. Guide Agent 示例

用户问：

```text
我不知道下一步该干什么
```

Agent 执行：

```text
GET_ABILITY_SUMMARY
GET_RECENT_TRAINING_SUMMARY
GET_RESUME_RISK_SUMMARY
```

如果发现：

```text
没有训练记录
有简历但未分析
没有项目
```

回答：

```text
你现在还缺少训练材料。建议先从简历训练开始，分析你的项目经历，再一键生成项目档案，最后进入项目拷打。
```

Actions：

```text
GO_RESUMES
GO_DOCUMENTS
GO_PROJECTS
```

---

## 17. Review Agent 后续 ReAct 化

复盘 Agent 非常适合 ReAct。

流程：

```text
查询最近训练报告
查询能力画像
查询简历风险点
检索相关知识文章
生成复盘结论
```

但第一版可以先不改 Review Agent，等 Guide Agent ReAct 化稳定后再迁移。

---

## 18. Mock Interview 后续 Plan-Execute 化

真实模拟面试更适合 Plan-Execute，而不是普通 ReAct。

流程：

```text
先生成面试计划
按阶段执行
根据回答动态调整
最后生成报告
```

因此 Mock Interview 不应优先接入 ReAct 循环，而应后续接入 Plan-Execute。

---

## 19. 与 Function Calling 的关系

当前不强制依赖模型原生 Function Calling。

第一版可以使用：

```text
LLM 输出 JSON tool intents
后端解析
Tool Registry 校验
Tool Executor 执行
```

后续如果模型支持标准 tool_calls，可以替换 Tool Intent 解析层。

架构应支持平滑迁移：

```text
JSON Tool Intent
-> Model Native Function Calling
-> Spring AI Tool Calling
```

---

## 20. 与 Spring AI 的关系

Spring AI / Spring AI Alibaba 可以用于：

```text
ChatClient
Tool Calling
Advisor
VectorStore
Agent Graph
```

但 CodeCoach AI 的业务 Agent Runtime 不应完全依赖 Spring AI。

正确关系：

```text
CodeCoach Agent Runtime
-> Tool Registry
-> AI Provider Adapter
-> Spring AI / 自研模型调用
```

Spring AI 是底层能力增强。

Agent Runtime 是业务 Agent 架构核心。

---

## 21. Trace 设计

每次 ReAct 执行应记录：

```text
AgentRun
AgentStep: CONTEXT_BUILD
AgentStep: REASON / INTENT_DETECT
AgentStep: TOOL_SELECT
AgentStep: TOOL_EXECUTE
AgentStep: OBSERVATION
AgentStep: RESPONSE_COMPOSE
ToolTrace
```

每一步记录：

```text
stepType
stepName
toolName
inputSummary
outputSummary
latencyMs
status
errorCode
```

---

## 22. 隐私和安全

ReAct Trace 不能记录完整推理链和隐私原文。

必须做到：

```text
只记录 thought summary
只记录 input summary
只记录 output summary
只记录 observation summary
不保存完整模型推理
不保存完整 prompt
不保存完整用户文档
```

---

## 23. 前端展示

第一版不需要展示 ReAct 步骤给普通用户。

前端仍然看到：

```text
助手回答
行动卡片
```

后续开发者模式或观测面板可以展示：

```text
本次 Agent 调用了哪些工具
每个工具耗时
是否成功
```

---

## 24. 失败降级

如果 Tool 调用失败：

```text
记录失败 step
跳过该 observation
继续尝试其他可用信息
最终回答中说明数据不足
```

如果 LLM 调用失败：

```text
降级为规则回答
返回基础 actions
记录 FALLBACK step
```

如果所有工具都失败：

```text
返回通用导航建议
不影响页面主功能
```

---

## 25. 验收标准

完成后应满足：

```text
Guide Agent 能基于用户问题自动调用低风险查询工具
Agent 能基于 Tool Observation 生成回答
中风险工具只作为 action card 返回，不自动执行
AgentRun / AgentStep 能记录完整过程
未知 Tool 不执行
高风险 Tool 不执行
LLM 失败可降级
前端体验不被破坏
```

---

## 26. 简历表达方向

完成后可以描述为：

```text
在自研 Agent Runtime 和 Tool Registry 基础上，实现 ReAct 式工具执行循环。Agent 可根据用户意图生成 Tool Intent，自动调用低风险查询工具获取能力画像、训练摘要、简历风险和知识检索结果，并基于 Observation 生成下一步训练建议；中高风险工具通过 Action Card 交由用户确认执行，保障工具调用安全可控。
```

---

## 27. 总结

ReAct 式 Agent 执行循环是 CodeCoach AI 从“Tool Action 系统”升级为“真正 Agent 架构”的关键一步。

这一步完成后，系统将具备：

```text
可选择工具
可执行工具
可观察结果
可生成最终响应
可追踪过程
可安全控制
```

这比简单 Prompt 调用更接近真实 AI Agent 项目。