# Agent 架构升级路线

## 1. 文档说明

本文档用于规划 CodeCoach AI 从“AI 功能型应用”升级为“AI Agent 面试训练平台”的技术路线。

当前 CodeCoach AI 已经完成：

```text
项目拷打训练
八股问答训练
真实模拟面试
简历驱动训练
用户文档 RAG
知识文章 RAG
项目档案 RAG
token 级流式输出
产品内 AI Guide
面试复盘 Agent
能力画像
评分 Rubric
语音输入
```

但从架构视角看，当前项目仍然存在明显问题：

```text
AI 能力分散在各业务模块
部分模块仍然是普通 Prompt 调用
Guide action 还不是完整 Tool Calling
缺少统一 Agent Runtime
缺少 Agent Trace
缺少统一 Tool 协议
缺少任务规划和执行分离
缺少长期 Memory
缺少多 Agent 协作机制
```

因此，下一阶段需要从架构上升级。

目标不是为了堆技术名词，而是让 CodeCoach AI 真正具备 Agent 项目的技术特征。

---

## 2. 项目重新定位

CodeCoach AI 的新定位：

```text
面向 Java 后端求职场景的 AI Agent 面试训练平台。
```

它不是通用 Agent 平台，而是垂直领域 Agent 应用。

核心目标：

```text
围绕简历、项目、用户文档、知识库、训练报告和能力画像，构建可调用工具、可追踪过程、可规划任务、可持续复盘的 AI 面试训练系统。
```

---

## 3. 为什么需要架构升级

当前项目已经有很多 AI 功能，但如果架构继续分散，会出现问题：

```text
每个模块各写各的 Prompt
每个模块各自调用 AI
工具调用不可复用
Agent 行为不可追踪
无法解释 Agent 为什么给出建议
无法沉淀长期记忆
无法演进到 ReAct / Plan-Execute / Multi-Agent
面试时容易被认为只是“调 API”
```

因此要把项目从：

```text
业务模块内直接调用 LLM
```

升级为：

```text
业务场景调用 Agent
Agent 基于上下文选择 Tool
Tool 执行业务能力
Trace 记录全过程
结果回写业务系统
```

---

## 4. 升级原则

架构升级必须遵循：

```text
产品场景驱动
工具调用受控
权限优先
可观测优先
不为了技术而技术
不一次性重构全部模块
逐步替换普通 AI 调用
```

不要做：

```text
为了写简历强行上 Multi-Agent
为了 ReAct 让系统变得不可控
让 AI 随意调用任意接口
让 AI 生成任意 URL
为了 MCP 重构所有工具
让 Agent 直接修改敏感数据
```

---

## 5. 目标 Agent 技术架构

目标架构：

```text
Frontend
  |
  | Chat / Action / Streaming
  v
Agent API Layer
  |
  v
Agent Runtime
  |
  +--> Context Builder
  +--> Planner
  +--> Tool Selector
  +--> Tool Executor
  +--> Observation Collector
  +--> Response Composer
  +--> Trace Recorder
  |
  v
Tool Registry
  |
  +--> Training Tools
  +--> Resume Tools
  +--> RAG Tools
  +--> Review Tools
  +--> Insight Tools
  +--> Navigation Tools
  |
  v
Business Services
  |
  +--> Project Training
  +--> Question Training
  +--> Mock Interview
  +--> Resume Analysis
  +--> RAG Retrieval
  +--> Agent Review
  +--> Ability Snapshot
```

---

## 6. Agent Runtime 定位

Agent Runtime 是后续所有 Agent 的统一运行底座。

它负责：

```text
接收用户请求
构建上下文
选择工具
执行工具
收集结果
生成回答
记录 Trace
处理失败降级
```

第一版不需要复杂推理循环，但要预留演进空间。

---

## 7. Tool Registry 定位

Tool Registry 是站内工具注册中心。

它维护：

```text
工具名称
工具描述
工具入参
工具出参
权限要求
是否需要确认
是否可自动执行
是否高风险
执行函数
前端展示方式
```

工具不是任意 API，而是受控业务能力。

---

## 8. Tool 类型设计

第一批工具应来自真实产品需求。

建议包括：

```text
NavigationTool
StartQuestionTrainingTool
StartProjectTrainingTool
StartMockInterviewTool
GenerateAgentReviewTool
AnalyzeResumeTool
CreateProjectFromResumeTool
SearchKnowledgeTool
SearchUserDocumentTool
GetAbilitySummaryTool
GetRecentTrainingSummaryTool
GetResumeRiskSummaryTool
```

---

## 9. Tool 调用安全边界

工具必须分级。

## 9.1 低风险工具

```text
页面跳转
查询摘要
RAG 检索
获取推荐
```

可以直接执行。

## 9.2 中风险工具

```text
创建训练会话
生成复盘
分析简历
生成项目草稿
```

需要用户点击确认。

## 9.3 高风险工具

```text
删除文档
删除项目
修改用户资料
覆盖训练结果
```

第一阶段不允许 Agent 执行。

---

## 10. Tool Action 和 Function Calling 的关系

当前 Guide action 是 action card，不是模型原生 Function Calling。

升级方向：

```text
第一阶段：内部 Tool 协议
第二阶段：让 LLM 输出 tool intent
第三阶段：后端映射为站内 Tool
第四阶段：如果模型支持，再接入原生 function calling / tool_calls
```

这样可以避免一开始被模型 provider 绑定。

---

## 11. ReAct 的落地方式

ReAct 不应该直接暴露给用户。

产品中的 ReAct 应表现为：

```text
Agent 观察用户状态
选择一个工具
拿到工具结果
再决定下一步
最终给用户回答和行动卡片
```

内部可以记录：

```text
step
thought_summary
action
observation
```

但不要把模型完整推理链展示给用户。

---

## 12. Plan-Execute 的落地方式

Plan-Execute 最适合真实模拟面试。

模拟面试不是随机问问题，而应该：

```text
先生成面试计划
再按阶段执行
根据用户回答动态调整
最后生成报告
```

计划示例：

```text
1. 自我介绍
2. 简历项目概述
3. 项目职责追问
4. 项目技术细节
5. 相关八股知识
6. 异常场景设计
7. 总结反问
```

---

## 13. Multi-Agent 的落地方式

Multi-Agent 不应该一开始泛化。

适合 CodeCoach AI 的 Multi-Agent：

```text
Interviewer Agent：负责追问
Evaluator Agent：负责评分和质量判断
Coach Agent：负责复盘和建议
Retriever Agent：负责 RAG 检索上下文
Guide Agent：负责站内导航和 action
```

第一阶段不需要完全拆分进程，只需要逻辑分工清晰。

---

## 14. Memory 的落地方式

Memory 不等于简单存聊天记录。

CodeCoach AI 的 Memory 应该是：

```text
用户目标岗位
用户常见薄弱点
高频失败问题
已掌握知识点
简历风险点
项目表达风险
训练偏好
最近行动建议
```

Memory 来源：

```text
训练报告
能力画像
复盘 Agent
模拟面试报告
简历分析
用户文档
```

Memory 应用于：

```text
Guide Agent
复盘 Agent
模拟面试 Agent
训练推荐
Prompt 个性化
```

---

## 15. MCP 的落地方式

MCP 暂时不是第一优先级。

后续可以把站内工具包装为 MCP Server：

```text
start_question_training
start_project_training
search_knowledge
search_user_document
generate_review
analyze_resume
create_project_from_resume
```

MCP 的价值：

```text
让外部 Agent 客户端也能调用 CodeCoach AI 的训练能力
让项目更贴近最新 Agent 工具协议
```

但在内部 Tool 协议稳定前，不建议先做 MCP。

---

## 16. 可观测性目标

Agent 项目必须可观测。

需要记录：

```text
agent_run
agent_step
tool_call
rag_trace
llm_call
final_response
```

可观测性回答这些问题：

```text
Agent 为什么给出这个建议？
调用了哪些工具？
RAG 命中了哪些 chunk？
Tool 是否成功？
AI 响应耗时多少？
失败在哪里？
```

---

## 17. 推荐升级路线

## 17.1 TOOL-001：统一 Tool / Action 调用协议

目标：

```text
把当前 Guide action card 升级为通用 Tool / Action 协议。
```

产出：

```text
Tool Registry
Tool 定义
Tool 执行结果
权限校验
前端 action 执行适配
```

这是最优先任务。

---

## 17.2 AGENT-CORE-001：Agent Runtime 与 Trace

目标：

```text
建立统一 Agent 运行框架。
```

产出：

```text
AgentRun
AgentStep
ToolCall
TraceId
AgentContext
AgentResponse
```

---

## 17.3 GUIDE-003：Guide Agent 升级

目标：

```text
把产品内 AI Guide 从“回答 + action”升级为 Tool-Calling Guide Agent。
```

能力：

```text
识别用户意图
查询用户摘要
调用推荐工具
调用导航工具
调用训练创建工具
返回可执行 action
记录 trace
```

---

## 17.4 MOCK-002：模拟面试 Plan-Execute

目标：

```text
让模拟面试先规划，再执行。
```

能力：

```text
面试计划生成
阶段目标
每阶段问题策略
动态调整
报告归因
```

---

## 17.5 REVIEW-002：复盘 Agent ReAct 化

目标：

```text
让复盘 Agent 使用工具循环收集证据。
```

工具：

```text
查询最近报告
查询能力画像
查询简历风险
RAG 检索知识文章
生成 next actions
```

---

## 17.6 MEMORY-001：长期训练记忆

目标：

```text
沉淀用户长期训练画像，供 Agent 个性化使用。
```

Memory 包括：

```text
目标岗位
薄弱点
已掌握点
简历风险
项目表达问题
训练偏好
```

---

## 17.7 OBS-001：AI / RAG / Agent 可观测性

目标：

```text
构建面向开发者和面试讲解的观测面板。
```

展示：

```text
LLM 调用次数
RAG 检索次数
Tool 调用次数
Agent Trace
响应耗时
失败原因
```

---

## 17.8 MCP-001：MCP 工具协议适配

目标：

```text
将 CodeCoach AI 站内工具暴露为 MCP Server。
```

放在后期。

---

## 18. 简历技术表达升级

当前可以写：

```text
基于 Java 21 + Spring Boot 3 + React 19 构建 AI Agent 面试训练平台，接入 OpenAI-Compatible 大模型、Zhipu Embedding 与 Qdrant 向量库，设计用户私有知识库 RAG 链路，支持文档解析、向量检索、token 级流式输出、简历分析、模拟面试、复盘 Agent 与产品内 Guide Agent。
```

升级 TOOL / Agent Runtime 后可以写：

```text
设计并实现面向业务场景的 Agent Runtime 与受控 Tool Calling 机制，将项目训练、八股训练、简历分析、复盘生成、RAG 检索等站内能力封装为可调用工具，通过 Tool Registry、权限校验、Action 白名单和 Agent Trace 实现 Agent 工具调用的安全执行与可观测。
```

---

## 19. 面试讲解升级

可以这样讲：

```text
一开始这个项目只是多个 AI 训练功能，后来我发现如果每个模块都直接拼 Prompt 调模型，会很难维护，也不够 Agent 化。

所以我把系统升级成 Agent 架构：底层是 LLM、Embedding、RAG 和业务数据；中间是 Tool Registry，把站内能力抽象成工具；上层是不同场景 Agent，比如 Guide Agent、模拟面试 Agent、复盘 Agent。

Agent 不直接调用任意接口，而是先输出工具意图，后端通过白名单映射成受控 Tool，再做权限校验和执行，并记录 Agent Trace。这样既保留了 Agent 的智能决策能力，又保证了业务安全和可观测性。
```

---

## 20. 不要过度包装

不能说已经实现：

```text
完整 Function Calling
完整 ReAct
完整 Multi-Agent
完整 MCP
复杂 Agent 编排
长期 Memory
```

除非后续确实完成。

当前应说：

```text
已实现 Agent 化雏形
正在升级统一 Tool Calling 和 Agent Runtime
```

---

## 21. 阶段完成标准

完成 Agent 架构升级后，应满足：

```text
站内能力可以被统一 Tool 协议描述
Agent 可以通过受控 Tool 执行动作
工具调用有权限校验
工具调用有 Trace
Guide Agent 不再只是返回普通 action
复盘 Agent 可以逐步演进到工具循环
模拟面试可以逐步演进到 Plan-Execute
后续可以平滑接入 Function Calling / ReAct / MCP
```

---

## 22. 总结

CodeCoach AI 的下一阶段不应该继续堆页面，而应该升级 Agent 技术底座。

最关键的第一步是：

```text
统一 Tool / Action 调用协议
```

因为它是：

```text
Function Calling 的前置
ReAct 的前置
Agent Runtime 的前置
MCP 的前置
可观测性的前置
```

只有先把站内能力工具化，CodeCoach AI 才能真正从“AI 功能集合”升级为“AI Agent 面试训练平台”。