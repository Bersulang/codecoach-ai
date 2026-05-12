# 面试复盘 Agent 设计

## 1. 文档说明

本文档用于设计 CodeCoach AI 的面试复盘 Agent 模块。

当前 CodeCoach AI 已经完成：

```text
项目拷打训练
八股问答训练
知识学习模块
能力画像模块
RAG 基础能力
知识文章 RAG
项目档案 RAG
用户文档 RAG
简历驱动训练
token 级流式输出
训练报告
智能学习推荐
评分 Rubric 设计
```

接下来需要构建一个更具 AI 产品特色的能力：

```text
面试复盘 Agent
```

它不是一个泛化聊天机器人，也不是一个万能规划助手，而是一个围绕用户训练数据、简历风险点、能力画像和知识库内容进行复盘、归因、建议和下一步行动规划的垂直 Agent。

---

## 2. 模块定位

面试复盘 Agent 的定位是：

```text
面向 Java 后端求职训练场景的个人面试复盘与下一步行动建议 Agent。
```

它的核心任务是：

```text
读取用户最近训练表现、简历风险点、能力画像、知识库内容和历史报告，帮助用户总结问题、定位短板、解释原因，并给出下一步训练动作。
```

它不是：

```text
万能聊天 Agent
通用学习规划 Agent
自动投递求职 Agent
简历代写 Agent
闲聊机器人
```

它必须围绕 CodeCoach AI 已有数据工作。

---

## 3. 为什么需要复盘 Agent

当前系统已经可以：

```text
训练
生成报告
沉淀能力画像
推荐学习内容
```

但用户仍然可能不知道：

```text
我最近整体表现怎么样？
我为什么总是在某类问题上答不好？
项目拷打和八股问答之间有什么关联？
我的简历风险点有没有被训练覆盖？
我下一次训练应该练什么？
我这周应该优先补项目还是补八股？
```

复盘 Agent 的价值是把分散数据串起来：

```text
训练报告
+ 能力画像
+ 简历风险点
+ 用户文档
+ 知识文章
+ RAG 检索
-> 复盘总结
-> 问题归因
-> 下一步训练动作
```

---

## 4. 产品核心目标

复盘 Agent 要解决的问题不是“告诉用户努力学习”，而是给出具体、可行动、基于数据的复盘。

用户使用它时，希望看到：

```text
最近训练表现总结
高频薄弱点
反复暴露的问题
项目表达风险
八股知识短板
简历高风险点
下一步训练建议
推荐学习材料
推荐专项训练入口
```

最终目标是让用户感觉：

```text
这个 Agent 真的看过我的训练记录，并知道我下一步该怎么练。
```

---

## 5. Agent 的边界

第一版复盘 Agent 不做太泛化。

## 5.1 第一版做

```text
基于最近训练数据生成复盘
基于能力画像归纳短板
基于简历分析识别训练优先级
基于 RAG 推荐知识文章
给出下一步训练动作
生成一份复盘卡片
```

## 5.2 第一版不做

```text
不做万能对话
不做长期自动任务
不做自动发送提醒
不做复杂多步工具链
不做联网搜索
不做投递建议
不做简历代写
不做日程管理
不做全自动训练计划
```

---

## 6. Agent 与普通 AI 调用的区别

普通 AI 调用通常是：

```text
当前问题
+ 当前上下文
-> AI 生成回答
```

复盘 Agent 应该是：

```text
读取多类用户数据
-> 汇总关键证据
-> 识别模式
-> 分析原因
-> 检索相关知识
-> 生成下一步行动
```

它不是只把 prompt 写长，而是要有明确的“工具输入”。

---

## 7. Agent 可用数据源

复盘 Agent 第一版可使用以下数据源。

---

## 7.1 训练报告

来源：

```text
interview_report
question_training_report
```

用途：

```text
分析最近项目训练表现
分析最近八股训练表现
提取 strengths / weaknesses / suggestions
统计低分报告
识别重复问题
```

---

## 7.2 能力画像

来源：

```text
user_ability_snapshot
insights 聚合接口
```

用途：

```text
识别低分维度
识别下降趋势
识别高频 weaknessTags
判断用户当前训练状态
```

---

## 7.3 简历分析结果

来源：

```text
resume_profile
resume_analysis
```

用途：

```text
识别简历项目风险
识别高频追问点
判断训练是否覆盖简历风险
推荐项目拷打方向
```

---

## 7.4 用户文档

来源：

```text
user_document
USER_UPLOAD RAG
```

用途：

```text
检索用户简历
检索 README / 项目文档
检索学习笔记
补充复盘上下文
```

---

## 7.5 系统知识文章

来源：

```text
knowledge_article
KNOWLEDGE_ARTICLE RAG
```

用途：

```text
推荐学习材料
补充薄弱知识点
生成复习建议
```

---

## 8. Agent 输入

第一版复盘 Agent 可以支持用户选择复盘范围。

建议支持：

```text
最近 7 天
最近 30 天
最近 5 次训练
全部训练概览
```

默认：

```text
最近 7 天或最近 10 次训练
```

用户也可以选择复盘类型：

```text
综合复盘
项目训练复盘
八股训练复盘
简历风险复盘
```

第一版可以先做：

```text
综合复盘
```

---

## 9. Agent 输出

复盘 Agent 输出应结构化，而不是一大段聊天文本。

建议输出：

```text
整体结论
关键发现
主要问题
问题原因
下一步行动
推荐学习内容
推荐训练入口
风险提醒
```

---

## 10. 复盘结果结构

建议复盘结果包含以下模块。

---

## 10.1 overallSummary

整体总结。

示例：

```text
你最近的训练主要集中在 Redis 和项目表达。八股部分能够说出关键词，但在场景适配和异常处理上仍然偏弱；项目训练中能描述技术栈，但个人贡献和工程权衡表达不够具体。
```

---

## 10.2 keyFindings

关键发现列表。

示例：

```json
[
  {
    "title": "Redis 相关问题反复暴露短板",
    "description": "最近多次训练中都出现逻辑过期、缓存一致性和分布式锁异常场景表达不完整的问题。",
    "severity": "HIGH"
  }
]
```

---

## 10.3 recurringWeaknesses

反复出现的薄弱点。

示例：

```json
[
  {
    "keyword": "逻辑过期",
    "count": 3,
    "relatedDimension": "Redis",
    "evidence": "多次报告中提到对逻辑过期适用场景表达不完整。"
  }
]
```

---

## 10.4 causeAnalysis

原因分析。

示例：

```text
你的问题不只是概念没背熟，而是缺少“方案适用场景 + 副作用 + 项目表达”的组合表达。你能说出互斥锁和逻辑过期，但没有说明为什么在热点数据场景更适合逻辑过期，也没有说明旧数据容忍问题。
```

---

## 10.5 nextActions

下一步行动。

示例：

```json
[
  {
    "type": "LEARN",
    "title": "复习 Redis 缓存击穿中的逻辑过期方案",
    "reason": "这是你最近反复暴露的薄弱点。",
    "targetPath": "/learn/articles/1"
  },
  {
    "type": "TRAIN",
    "title": "进行一次 Redis 缓存击穿专项八股训练",
    "reason": "复习后需要验证是否能完整表达。",
    "targetPath": "/questions?topicId=21"
  }
]
```

---

## 10.6 resumeRisks

简历相关风险。

示例：

```json
[
  {
    "title": "RAG 项目亮点容易被深挖",
    "description": "简历中提到 RAG、Qdrant、Embedding 和用户文档增强训练，需要准备好切片策略、向量维度选择、权限过滤和失败降级。",
    "suggestedTraining": "进入 CodeCoach AI 项目拷打训练"
  }
]
```

---

## 10.7 confidence

复盘可信度。

取值：

```text
LOW
MEDIUM
HIGH
```

判断依据：

```text
训练次数
有效回答数
报告质量
是否有能力画像
是否有简历分析
是否有足够历史数据
```

如果数据不足，要明确说明：

```text
当前训练数据较少，复盘结果只能作为初步参考。
```

---

## 11. Agent 工具设计

第一版不需要复杂 Agent 框架，但要有“工具思想”。

可以把内部数据能力抽象为工具。

建议工具：

```text
RecentTrainingReportTool
AbilitySnapshotTool
ResumeRiskTool
RagKnowledgeSearchTool
UserDocumentSearchTool
RecommendationTool
```

实际实现可以是 Service 方法，不一定需要独立 Agent 框架。

---

## 12. 工具一：RecentTrainingReportTool

职责：

```text
获取用户最近训练报告
```

输入：

```text
userId
timeRange
limit
trainingType
```

输出：

```text
项目训练报告摘要
八股训练报告摘要
分数
weaknesses
suggestions
createdAt
```

---

## 13. 工具二：AbilitySnapshotTool

职责：

```text
获取用户能力画像摘要
```

输出：

```text
低分维度
下降维度
高频 weaknessTags
最近训练趋势
```

---

## 14. 工具三：ResumeRiskTool

职责：

```text
获取用户最新简历风险点
```

输出：

```text
高风险项目
高风险技术栈
推荐追问题
表达优化建议
```

---

## 15. 工具四：RagKnowledgeSearchTool

职责：

```text
根据薄弱点检索系统知识文章
```

输出：

```text
相关文章
相关知识 chunk
targetPath
```

---

## 16. 工具五：UserDocumentSearchTool

职责：

```text
检索用户私有文档
```

用途：

```text
项目复盘
简历风险复盘
用户笔记复盘
```

---

## 17. Agent 编排流程

第一版复盘 Agent 流程：

```text
用户点击生成复盘
-> 后端获取复盘范围
-> 查询最近训练报告
-> 查询能力画像摘要
-> 查询最新简历风险点
-> 根据 weaknessTags 检索知识文章
-> 根据项目风险检索用户文档
-> 汇总证据
-> 调用 AI 生成结构化复盘
-> 保存复盘结果
-> 前端展示复盘卡片
```

---

## 18. 是否需要持久化

建议持久化。

原因：

```text
复盘结果生成成本较高
用户可能多次查看
可以形成历史复盘记录
方便后续对比
```

建议新增表：

```text
agent_review_report
```

---

## 19. agent_review_report 表设计

建议字段：

```text
id
user_id
review_type
time_range
status
summary
key_findings JSON
recurring_weaknesses JSON
cause_analysis TEXT
next_actions JSON
resume_risks JSON
confidence
source_snapshot JSON
error_message
created_at
updated_at
```

说明：

```text
source_snapshot 用于记录本次复盘使用了哪些数据源，例如报告数量、能力快照数量、简历分析 ID 等。
```

---

## 20. review_type 设计

取值：

```text
COMPREHENSIVE
PROJECT
QUESTION
RESUME
```

第一版先实现：

```text
COMPREHENSIVE
```

---

## 21. status 设计

取值：

```text
PENDING
GENERATED
FAILED
```

---

## 22. confidence 设计

取值：

```text
LOW
MEDIUM
HIGH
```

---

## 23. 后端接口规划

建议接口：

```http
POST /api/agent/reviews
```

生成复盘。

请求：

```json
{
  "reviewType": "COMPREHENSIVE",
  "range": "LAST_7_DAYS"
}
```

响应：

```json
{
  "id": 1,
  "status": "GENERATED"
}
```

---

```http
GET /api/agent/reviews
```

获取复盘历史。

---

```http
GET /api/agent/reviews/{id}
```

获取复盘详情。

---

```http
POST /api/agent/reviews/latest/regenerate
```

重新生成最新复盘，可选。

---

## 24. 前端页面规划

新增页面：

```text
/agent-review
```

页面名称：

```text
复盘 Agent
```

副标题：

```text
基于你的训练报告、能力画像、简历风险点和知识库内容，生成下一步训练建议。
```

---

## 25. 页面结构

建议包含：

```text
顶部说明
生成复盘按钮
复盘范围选择
最新复盘卡片
关键发现
反复薄弱点
原因分析
下一步行动
简历风险提醒
复盘历史
```

---

## 26. 复盘范围选择

第一版可以提供：

```text
最近 7 天
最近 30 天
最近 10 次训练
```

如果实现复杂，第一版默认最近 7 天即可。

---

## 27. 空状态

如果用户训练数据不足：

```text
还没有足够的训练数据
完成几次项目拷打或八股问答后，复盘 Agent 会帮你总结反复出现的问题和下一步训练方向。
```

提供按钮：

```text
去项目拷打
开始八股问答
```

---

## 28. 下一步行动卡片

行动类型：

```text
LEARN
TRAIN_PROJECT
TRAIN_QUESTION
REVIEW_RESUME
UPLOAD_DOCUMENT
```

每个行动需要：

```text
标题
原因
目标路径
优先级
```

示例：

```text
优先复习 Redis 缓存击穿
原因：最近 3 次训练都暴露逻辑过期表达不完整
按钮：开始学习
```

---

## 29. 复盘 Agent 的产品语气

语气要像教练，而不是评委。

要求：

```text
直接
具体
克制
不羞辱用户
不虚假鼓励
不制造焦虑
给出行动
```

避免：

```text
你非常优秀
你已经掌握得很好
继续努力即可
```

更好的表达：

```text
你能说出关键词，但还没有形成完整面试表达。下一步应重点补“适用场景 + 副作用 + 项目例子”。
```

---

## 30. 与训练计划的区别

当前暂时跳过训练计划。

复盘 Agent 不输出完整日程表。

它只输出：

```text
你最近的问题是什么
为什么会出现
下一步最应该做什么
```

后续训练计划模块再把这些行动扩展成 7 天计划。

---

## 31. 与能力画像的区别

能力画像偏数据展示：

```text
维度分数
趋势
薄弱点
推荐文章
```

复盘 Agent 偏解释和决策：

```text
这些问题之间有什么关系
为什么你反复出错
下一步优先做什么
```

能力画像回答：

```text
你哪里弱。
```

复盘 Agent 回答：

```text
为什么弱，以及下一步怎么练。
```

---

## 32. 与报告页的区别

单次报告回答：

```text
这一次训练表现如何。
```

复盘 Agent 回答：

```text
最近一段时间训练表现有什么模式。
```

---

## 33. AI 输出结构

建议 AI 输出 JSON：

```json
{
  "summary": "...",
  "keyFindings": [],
  "recurringWeaknesses": [],
  "causeAnalysis": "...",
  "nextActions": [],
  "resumeRisks": [],
  "confidence": "MEDIUM"
}
```

---

## 34. Prompt 设计原则

Prompt 必须要求：

```text
只能基于提供的数据分析
不要编造用户没有做过的训练
数据不足时必须说明
建议必须可执行
每个关键结论最好有证据
不要输出空泛鼓励
不要泄露隐私
```

---

## 35. 数据不足处理

如果数据不足，例如：

```text
训练报告少于 2 条
没有能力画像
没有简历分析
```

Agent 不应该假装已经充分了解用户。

应输出：

```text
当前数据较少，只能给出初步复盘。
建议先完成至少 1 次项目拷打和 1 次八股训练。
```

---

## 36. 权限与隐私

复盘 Agent 使用大量用户私有数据。

必须保证：

```text
只能读取当前用户数据
不允许前端传 userId
不打印完整简历
不打印完整用户文档
不打印完整 prompt
不泄露 AI / OSS API Key
```

---

## 37. 日志要求

可以记录：

```text
userId
reviewId
reviewType
range
报告数量
能力快照数量
是否包含简历分析
生成耗时
成功失败状态
```

禁止记录：

```text
完整简历
完整文档
完整训练回答
完整 prompt
API Key
Authorization Header
```

---

## 38. MVP 范围

第一版做：

后端：

```text
agent_review_report 表
生成综合复盘接口
复盘历史接口
复盘详情接口
读取最近训练报告
读取能力画像
读取简历风险点
RAG 检索相关知识文章
AI 生成结构化复盘
```

前端：

```text
/agent-review 页面
生成复盘按钮
最新复盘展示
关键发现
原因分析
下一步行动
复盘历史
Dashboard 入口
头像菜单入口
```

第一版不做：

```text
定时复盘
消息提醒
自动任务
多 Agent 协作
长期训练计划
日历
联网搜索
投递建议
```

---

## 39. 阶段完成标准

完成后应满足：

```text
用户可以生成一次综合复盘
复盘能读取最近训练表现
复盘能结合能力画像
复盘能结合简历风险点
复盘能推荐学习或训练动作
数据不足时有明确提示
复盘结果可以再次查看
用户只能访问自己的复盘
不影响已有训练和 RAG
```

---

## 40. 简历表达方向

模块完成后可以描述为：

```text
设计并实现面向 Java 后端求职训练场景的面试复盘 Agent，聚合用户训练报告、能力画像、简历风险点和 RAG 知识检索结果，生成结构化复盘结论、问题归因和下一步训练建议，实现从单次训练报告到长期训练决策的升级。
```

进一步可以描述为：

```text
复盘 Agent 通过内部工具化服务读取最近训练报告、能力快照和简历分析结果，并结合知识库 RAG 检索生成下一步行动卡片，支持用户从复盘结果直接跳转到知识学习、八股训练和项目拷打。
```

---

## 41. 总结

复盘 Agent 是 CodeCoach AI 从“训练工具”升级为“AI 教练”的关键一步。

它不是为了炫技做一个万能 Agent，而是围绕现有业务数据做垂直闭环：

```text
训练
-> 报告
-> 能力画像
-> 简历风险
-> 复盘 Agent
-> 下一步行动
-> 再训练
```

第一版重点不是复杂规划，而是让用户明确知道：

```text
我最近的问题是什么？
为什么反复出现？
下一步最应该练什么？
```