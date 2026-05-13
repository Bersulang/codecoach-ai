# CodeCoach AI Agent 项目架构与技术栈

## 1. 项目重新定位

CodeCoach AI 当前已经从最早的“AI 项目拷打训练工具”演进为面向 Java 后端求职场景的 AI Agent 面试训练平台雏形。它不是通用聊天机器人，而是围绕“简历材料、项目档案、知识文章、用户文档、训练报告、能力画像、下一步行动”形成训练闭环。

更准确的一句话定位：

> CodeCoach AI 是一个面向 Java 后端求职者的 AI Agent 面试训练平台，基于 RAG、用户私有知识库、Prompt 工程、token 级流式输出、训练状态机和受控 action 引导，实现项目拷打、八股问答、真实模拟面试、简历驱动训练、能力画像和复盘建议闭环。

需要注意：当前项目具备 Agent 化雏形，但还没有完整实现 Function Calling、ReAct、Plan-Execute、Multi-Agent、MCP、长期 Memory 和复杂 Agent 编排。简历和面试中应把它描述为“Agent 化面试训练平台”或“具备 Agent 能力雏形的 AI 面试训练平台”，不要强行包装为已经完整落地的多智能体系统。

## 2. 当前实际技术架构

### 2.1 总体架构

```text
React 19 + TypeScript + Vite 前端
  |
  | HTTP / JSON / NDJSON streaming
  v
Spring Boot 3.4 + Java 21 后端单体
  |
  | MyBatis-Plus
  v
MySQL 8
  |
  | RedisTemplate
  v
Redis 7
  |
  | REST API
  +--> OpenAI-Compatible Chat Completions
  +--> Zhipu embedding-3
  +--> Qdrant Vector Store
  +--> Aliyun OSS
```

项目采用前后端分离的模块化单体架构。后端按 `auth`、`project`、`interview`、`question`、`mockinterview`、`resume`、`document`、`rag`、`agent`、`guide`、`insight`、`report`、`ai` 等包拆分业务边界；前端按页面、API、组件、类型和工具函数组织。

### 2.2 后端技术栈

| 类别 | 当前实际使用 |
|---|---|
| 语言 | Java 21 |
| 框架 | Spring Boot 3.4.13、Spring MVC、Spring Security |
| ORM | MyBatis-Plus 3.5.16 |
| 数据库 | MySQL 8，核心业务表、训练消息、报告、AI 日志、RAG 元数据都落 MySQL |
| 缓存/锁 | Redis 7，当前用于八股训练回答锁、报告生成锁、文档索引锁等防重复处理 |
| 鉴权 | Spring Security + JWT + BCrypt |
| OSS | Aliyun OSS，用于用户文档上传存储 |
| API 文档 | springdoc-openapi |
| AI 调用方式 | OpenAI-Compatible Chat Completions，使用 `RestClient` 和 `java.net.http.HttpClient` 调用 |
| AI 日志 | `ai_call_log` 表记录 provider、model、request_type、token、耗时、状态、错误、request_id、可选 raw_response |
| RAG 组件 | `RagIndexService`、`RagRetrievalService`、`MarkdownChunkService`、`ProjectChunkService`、`UserDocumentChunkService` |
| 向量库 | Qdrant，`rag_embedding` 只保存 vector_id 和模型等元数据 |
| Embedding | Zhipu `embedding-3`，默认 512 维 |
| 文档解析 | PDFBox 解析 PDF，UTF-8 文本解析 TXT/Markdown |
| 流式输出 | 后端对前端输出 `application/x-ndjson`；后端对模型消费 OpenAI-Compatible stream `data:` 行 |

### 2.3 前端技术栈

| 类别 | 当前实际使用 |
|---|---|
| 框架 | React 19.2.5 + TypeScript |
| 构建工具 | Vite 8 |
| 路由 | React Router 7 |
| UI | Ant Design 6 + 自定义 CSS |
| HTTP | Axios，统一封装 token、业务错误和 401 跳转 |
| 状态管理 | 项目依赖 Zustand，但当前主要通过组件状态、localStorage 和 API 数据驱动 |
| 流式读取 | `fetch` + `ReadableStream.getReader()` + `TextDecoder` 解析 NDJSON |
| Markdown | `react-markdown` |
| 语音输入 | 浏览器 `SpeechRecognition` / `webkitSpeechRecognition`，前端把识别文本写入输入框 |

### 2.4 AI / Agent 技术栈

| 类别 | 当前实际使用 |
|---|---|
| LLM Provider | OpenAI-Compatible，可通过环境变量配置 base-url、api-key、model |
| Embedding Provider | Zhipu `embedding-3` |
| RAG | 知识文章、项目档案、用户上传文档可索引和检索；复盘、八股训练、模拟面试等场景会读取 RAG 上下文 |
| Prompt | `backend/src/main/resources/prompts` 管理面试、八股、报告、复盘 Prompt；部分模块内联 Prompt |
| Tool / Action | Guide 模块返回白名单 action card，由前端执行站内跳转；不是模型原生 tool call |
| Agent 类型 | 复盘 Agent、产品内 Guide Agent、模拟面试状态机、简历驱动训练入口具备 Agent 雏形 |
| Memory / 上下文 | 会话消息、训练报告、能力快照、简历分析、用户文档、项目档案作为业务上下文；未实现独立长期记忆系统 |
| SSE / token stream | 模型侧读取 OpenAI-Compatible SSE；产品侧对前端输出 NDJSON token delta |
| 用户私有知识库 | `user_document` + `rag_document/rag_chunk/rag_embedding` + Qdrant payload，按用户隔离 |
| 简历分析 | 上传文档后由 LLM 结构化分析简历，提取技能、项目经历、风险点、推荐追问，并可生成项目档案草稿 |
| 复盘 Agent | 聚合项目报告、八股报告、能力快照、简历风险和 RAG 文章，生成结构化复盘和 next actions |
| 产品 Guide Agent | 聚合当前页面、项目数、文档数、简历数、近期训练、低分维度、最近复盘、简历风险，输出回答和 action card |

## 3. 当前 AI Agent 能力分层

推荐使用下面的分层来描述项目，比简单地把所有 AI 调用都叫 Agent 更准确。

### 3.1 基础 AI 调用层

负责统一接入大模型、Prompt 渲染、JSON 解析、错误处理、token 流、AI 调用日志。

代表模块：

- `AiInterviewService` / `OpenAiCompatibleAiInterviewServiceImpl`
- `AiQuestionPracticeService` / `OpenAiCompatibleQuestionPracticeServiceImpl`
- `AiResumeAnalysisService`
- `AiAgentReviewService`
- `AiCallLogService`
- `PromptTemplateService`

### 3.2 RAG 能力层

负责文档切片、Embedding、向量写入、向量检索、上下文拼接和权限过滤。

代表模块：

- `RagIndexServiceImpl`
- `RagRetrievalServiceImpl`
- `QdrantVectorStoreServiceImpl`
- `ZhipuEmbeddingServiceImpl`
- `MarkdownChunkServiceImpl`
- `ProjectChunkServiceImpl`
- `UserDocumentServiceImpl`

### 3.3 训练 Agent 层

负责面试训练的多轮状态推进、追问、反馈、报告生成和能力沉淀。

当前包括：

- 项目拷打训练：多轮项目面试追问，结合项目、历史消息、RAG 上下文和报告生成。
- 八股问答训练：围绕知识点多轮问答、RAG 知识注入、反馈、参考答案和报告。
- 真实模拟面试：按开场、简历项目、技术基础、项目深挖、场景设计、收尾阶段推进。当前主要是规则状态机 + RAG + 评分规则，不是每轮都由 LLM 动态规划。

### 3.4 分析 Agent 层

负责把训练数据、简历风险、能力画像和知识推荐转成结构化分析。

当前包括：

- 简历分析：LLM 单次结构化分析，提取风险点和项目经历。
- 评分 Rubric：`ReportQualityEvaluator`、`ReportQualityPostProcessor` 做回答质量判断、低质量样本拦截、分数上限控制。
- 能力画像：`user_ability_snapshot` 从项目报告、八股报告、模拟面试报告沉淀维度分数。
- 复盘 Agent：聚合多个数据源并生成复盘结论和下一步行动，Agent 特征最明显。

### 3.5 产品 Guide Agent 层

负责产品内训练导航和受控 action 推荐。

当前实现是“规则优先 + LLM 兜底 + action 白名单”：

- 规则识别项目、八股、简历、文档、弱点、复盘、模拟面试等意图。
- 登录用户会聚合项目数、文档数、简历数、近 30 天训练、低分维度、最近复盘、简历风险。
- LLM 只返回 JSON 格式的 answer 和 action 名称。
- 后端通过 `GuideAction` 枚举过滤 action，前端通过 `isAllowedAction` 再次校验。

### 3.6 用户上下文层

这是 Agent 能力的核心数据底座：

- 项目档案；
- 简历档案与简历项目经历；
- 用户上传文档；
- 知识文章；
- 项目/八股/模拟面试会话和消息；
- 项目/八股/模拟面试报告；
- 能力画像快照；
- 复盘 Agent 历史结果；
- AI 调用日志。

## 4. Agent 与普通 AI 调用的边界

### 4.1 更接近 Agent 的模块

| 模块 | 为什么更接近 Agent |
|---|---|
| 复盘 Agent | 聚合项目报告、八股报告、能力快照、简历风险、RAG 知识文章，形成结构化诊断和 next actions；具备跨数据源分析和行动建议 |
| 产品 Guide Agent | 识别用户意图，读取当前页面和用户训练状态，返回受控 action card；具备有限的感知、决策、行动映射 |
| 项目拷打训练 | 多轮上下文推进，结合项目档案、历史回答、RAG 上下文，生成追问/反馈/报告；属于训练 Agent 雏形 |
| 八股训练 | 围绕知识主题多轮问答，结合 RAG 知识片段、回答质量和轮次状态输出反馈和追问；属于专项训练 Agent 雏形 |
| 真实模拟面试 | 有阶段状态机、用户上下文、RAG 检索、评分和报告闭环；但当前问题生成偏规则化，Agent 程度弱于项目/八股训练 |
| 简历驱动训练 | 从简历分析中提取风险点和项目经历，转成训练入口和项目档案草稿；属于 Agent 工作流入口雏形 |

### 4.2 普通 LLM 调用或弱 Agent 模块

| 模块 | 当前性质 |
|---|---|
| 简历单次分析 | 主要是一次 LLM JSON 结构化抽取，不具备循环规划或工具调用 |
| 项目报告生成 | 基于会话上下文的一次报告生成，属于普通生成/评估调用 |
| 八股报告生成 | 基于问答历史的一次报告生成，属于普通生成/评估调用 |
| 首题生成 | 基于项目或知识点上下文生成第一题，属于普通 LLM 调用 |
| Guide action card | 是受控 action 映射，不是 OpenAI Function Calling |
| 模拟面试报告 | 当前主要由规则评分和质量评估生成，部分推荐是规则拼装，不是多 Agent 协作 |

## 5. 与主流 AI Agent 技术栈对照

| 技术点 | 当前是否实现 | 当前实现位置 | 是否适合写简历 | 后续优化方向 |
|---|---|---|---|---|
| RAG | 已实现 | `module/rag`，训练、复盘、模拟面试调用 `RagRetrievalService` | 适合 | 增加重排、召回评估、检索可观测性 |
| 文档切片 | 已实现 | `MarkdownChunkServiceImpl`、`ProjectChunkServiceImpl`、`UserDocumentChunkServiceImpl` | 适合 | 引入语义切片、标题层级、chunk overlap 配置 |
| Embedding | 已实现 | `ZhipuEmbeddingServiceImpl`，默认 `embedding-3` 512 维 | 适合 | 支持多 provider、批量 embedding、失败重试 |
| 向量数据库 | 已实现 | Qdrant，`QdrantVectorStoreServiceImpl` | 适合 | 增加 collection migration、payload schema 管理 |
| 用户私有知识库 | 已实现 | `user_document`、`rag_document`、Qdrant payload、`ownerType/userId` 过滤 | 适合 | 加入文档版本、权限审计、文档级可见性 |
| Prompt 工程 | 已实现 | `resources/prompts`、部分 service 内联 Prompt | 适合 | Prompt 版本化、灰度和自动评测 |
| token 级流式输出 | 已实现 | 模型侧 SSE，前端侧 NDJSON delta；`NdjsonStreamWriter`、`readNdjsonStream` | 适合 | 统一为标准 SSE 或保留 NDJSON 并补充断线处理 |
| 多轮对话 | 已实现 | `interview_message`、`question_training_message`、`mock_interview_message` | 适合 | 抽象统一会话上下文窗口和裁剪策略 |
| Tool / Action 调用 | 部分实现 | Guide `GuideAction` 白名单 + 前端 action card | 可以谨慎写 | 统一 Tool/Action 协议，增加参数校验、审计和执行结果回传 |
| Function Calling | 未实现 | 未发现 `tools/tool_calls/tool_choice` 调用 | 不建议写已实现 | 将 Guide action 升级为模型原生 tool call 或兼容协议 |
| ReAct | 未实现 | 当前无 Thought-Action-Observation 循环 | 不建议写已实现 | Guide Agent 可先试点 ReAct，但隐藏推理过程，仅记录 action trace |
| Plan-Execute | 未实现 | 模拟面试有阶段状态机，但没有 LLM 规划-执行拆分 | 不建议写已实现 | 给模拟面试生成阶段计划、执行、动态调整 |
| Multi-Agent | 未实现 | 多模块并存，但没有 Agent 间通信/协作协议 | 不建议写已实现 | 拆分面试官、评分、复盘、推荐 Agent，并定义编排器 |
| Memory | 部分实现 | 业务上下文和历史报告可作为记忆；无独立长期记忆模块 | 可写“业务上下文记忆” | 建立长期训练记忆、偏好、薄弱点演化和召回策略 |
| MCP | 未实现 | 未发现 MCP server/client 接入 | 不建议写 | 后续适配 MCP，把站内训练 API 暴露为工具 |
| Agent 可观测性 | 部分实现 | `ai_call_log` 有 AI 调用日志；缺少 Agent trace/RAG trace | 可写 AI 调用日志，不宜写完整观测 | 增加 trace_id、prompt、retrieval、tool action、latency 看板 |
| 权限隔离 | 已实现 | JWT、UserContext、业务表 user_id 校验、RAG owner/user 过滤 | 适合 | 增加对象级审计、RAG payload 双重校验、管理端权限 |
| AI 评分 Rubric | 已实现 | `ReportQualityEvaluator`、`ReportQualityPostProcessor`、报告弱点/建议/分数 | 适合 | 标准化 rubric 配置，增加人工校准样本 |
| 语音输入 | 已实现 | 前端 `ChatInputBox` 使用浏览器 SpeechRecognition | 适合 | 后端 ASR、降噪、录音上传、移动端兼容 |
| 简历解析 | 已实现 | 文档上传解析 + `AiResumeAnalysisService` 结构化分析 | 适合 | 支持 DOCX、版式保留、隐私脱敏、风险评分 |
| 模拟面试 | 已实现 | `mockinterview` 会话、阶段、消息、报告、RAG、评分 | 适合 | 接入 LLM 动态追问、Plan-Execute、评分 Agent |

## 6. 已实现能力清单

- 项目拷打训练：项目档案、训练会话、多轮追问、反馈、报告、历史记录。
- 八股问答训练：知识点、文章、训练会话、多轮反馈、参考答案、报告。
- 真实模拟面试：多阶段状态机、RAG 上下文、评分、报告、能力快照。
- 简历驱动训练：文档上传、简历分析、风险点、项目经历提取、项目档案草稿。
- 用户文档上传与 RAG：Aliyun OSS 存储、PDF/TXT/Markdown 解析、切片、Embedding、Qdrant 入库。
- 知识文章 RAG：本地 Markdown 知识文章、知识点目录、索引与检索。
- 项目档案 RAG：项目内容切片并作为用户私有上下文参与检索。
- token 级流式输出：模型流式响应解析，后端 NDJSON 转发，前端增量渲染。
- 面试复盘 Agent：聚合多类训练数据和 RAG 文章生成复盘。
- 产品内 AI Guide：规则意图识别、用户摘要、LLM JSON 建议、action 白名单。
- 能力画像：能力快照、成长洞察、学习推荐。
- 评分 Rubric：回答质量等级、样本充分性、低质量回答分数上限、报告后处理。
- 语音输入：浏览器语音识别。
- AI 调用日志：记录请求类型、模型、token、耗时、错误和 request id。

## 7. 未实现或仅部分实现能力

- 未真正使用 OpenAI Function Calling / Tool Calling。
- 未实现 ReAct 思考-行动-观察循环。
- 未实现 Plan-Execute 模式；模拟面试是阶段状态机，不是 LLM 规划器。
- 未实现 Multi-Agent 协作；“复盘 Agent、Guide Agent、训练 Agent”是模块分工，不是 Agent 间协作。
- 未实现独立长期 Memory；当前是业务数据上下文，不是记忆系统。
- 未接入 MCP。
- Agent 可观测性不足；有 AI 调用日志，但缺 RAG trace、tool trace、agent step trace。
- 工具调用安全控制还停留在 action 白名单和前后端枚举校验，未形成沙箱和审计闭环。
- 复杂任务编排不足；尚无统一 Agent runtime、任务队列、工作流引擎或编排器。

## 8. 适合简历的一句话介绍

### 8.1 简历版，偏技术

CodeCoach AI 是一个面向 Java 后端求职场景的 AI Agent 面试训练平台，基于 Spring Boot、React、OpenAI-Compatible LLM、Zhipu Embedding、Qdrant 和 RAG，实现用户私有知识库、项目/八股/模拟面试训练、token 级流式输出、简历分析、能力画像和复盘 Agent。

### 8.2 面试口述版，偏业务 + 技术

这个项目解决的是 Java 后端求职者“项目讲不清、八股和项目脱节、练完不知道怎么复盘”的问题。我把简历、项目档案、用户文档、知识文章和训练报告统一沉淀成用户上下文，再通过 RAG、流式大模型调用、训练状态机和复盘 Agent，把训练、评分、能力画像和下一步建议串成闭环。

### 8.3 产品介绍版，偏用户价值

CodeCoach AI 是你的 AI 面试训练教练：上传简历和项目材料后，它可以陪你做项目拷打、八股问答和真实模拟面试，并在每次训练后生成报告、定位薄弱点，推荐下一步该练什么。

## 9. 简历项目描述

项目名称：CodeCoach AI - AI Agent 面试训练平台

项目背景：解决 Java 后端求职者在简历项目表达、八股训练、模拟面试和长期复盘中的低效问题，将项目档案、简历、用户文档、知识文章、训练报告和能力画像串成可持续训练闭环。

技术栈：Java 21、Spring Boot 3.4、Spring Security、JWT、MyBatis-Plus、MySQL 8、Redis 7、Aliyun OSS、PDFBox、OpenAI-Compatible Chat Completions、Zhipu embedding-3、Qdrant、React 19、TypeScript、Vite、React Router、Ant Design、Axios、NDJSON Streaming。

项目职责 / 核心工作：

- 设计并实现 RAG 能力层，支持知识文章、项目档案、用户上传文档的切片、Embedding、Qdrant 向量写入和 TopK 检索，并通过 `ownerType + userId` 过滤保障用户私有知识库隔离。
- 封装 OpenAI-Compatible 大模型调用层，覆盖项目追问、八股问答、报告生成、简历分析、复盘 Agent 等请求类型，落库记录模型、token、耗时、状态码、错误信息和 request id。
- 实现 token 级流式训练体验，后端消费模型 SSE `data:` 增量并投影可见字段，再通过 `application/x-ndjson` 向前端输出 `start/stage/delta/done/error` 事件，前端用 `ReadableStream` 增量渲染。
- 建设用户文档知识库，支持 TXT/Markdown/PDF 上传到 Aliyun OSS，使用 PDFBox/UTF-8 解析、切片、向量化和重建索引，并用 Redis 锁避免重复索引。
- 实现简历驱动训练链路，对上传简历做结构化分析，提取技能风险、项目经历、推荐追问，并支持从简历项目经历生成项目档案草稿。
- 实现项目拷打、八股训练和真实模拟面试三类训练流程，沉淀多轮消息、报告、评分、弱点标签和能力快照，为后续复盘和推荐提供数据源。
- 设计复盘 Agent，聚合近期项目报告、八股报告、能力画像、简历风险和 RAG 推荐文章，生成关键发现、反复薄弱点、原因分析和 next actions。
- 设计产品内 Guide Agent，基于当前路由、用户训练摘要和意图识别返回白名单 action card，前后端双重校验 action，避免 AI 生成不可控跳转或操作。

## 10. 面试讲解版本

### 10.1 项目背景怎么讲

可以这样讲：

“我做 CodeCoach AI 是因为 Java 后端求职者常见的问题不是完全没学，而是项目经历、八股知识和面试表达割裂。传统题库只能刷题，不能根据自己的简历、项目和训练历史持续复盘。所以我做了一个 AI Agent 面试训练平台，把用户的项目档案、简历、文档、训练记录和能力画像沉淀成上下文，再让 AI 做多轮追问、反馈、报告和下一步训练建议。”

### 10.2 总体架构怎么讲

“整体是前后端分离的模块化单体。前端是 React + TypeScript + Vite + Ant Design；后端是 Java 21 + Spring Boot + MyBatis-Plus。MySQL 保存用户、项目、会话、消息、报告、能力快照、AI 日志和 RAG 元数据；Redis 做回答和索引防重复锁；Aliyun OSS 存用户上传文档；Qdrant 存向量；大模型通过 OpenAI-Compatible Chat Completions 接入，Embedding 用智谱 embedding-3。”

### 10.3 RAG 链路怎么讲

“RAG 分索引和检索两条链路。索引时，知识文章、项目档案、用户文档会被解析成文本，按 Markdown/项目字段/文档段落切片，调用 embedding-3 生成向量，向量写入 Qdrant，MySQL 保存 rag_document、rag_chunk、rag_embedding 元数据。检索时，先把 query 向量化，带 sourceType、ownerType、userId 等 payload filter 查 Qdrant，再回表校验文档状态和用户权限，最后拼成上下文块注入 Prompt。”

### 10.4 Agent 设计怎么讲

“我没有把所有 LLM 调用都叫 Agent。项目里 Agent 特征最明显的是复盘 Agent 和 Guide Agent。复盘 Agent 会聚合训练报告、能力画像、简历风险和 RAG 文章，输出诊断和 next actions。Guide Agent 会读取当前页面和用户训练摘要，识别意图并返回受控 action card。项目/八股/模拟面试属于训练 Agent 雏形，它们有多轮状态、上下文和报告闭环，但还没有完整 ReAct 或 Function Calling。”

### 10.5 token 流式输出怎么讲

“后端调用模型时使用 OpenAI-Compatible stream，读取 `data:` 行里的 delta content。因为训练接口最终还要返回业务 payload，所以我对前端没有直接透传标准 SSE，而是封装了 NDJSON 事件：start、stage、delta、done、error。前端用 fetch 的 ReadableStream 逐行解析，delta 用于实时展示，done 用于落最终消息和报告状态。”

### 10.6 用户文档权限隔离怎么讲

“权限隔离做了两层：业务层所有文档、项目、训练会话都用当前 JWT 解析出的 userId 校验；RAG 层写入 rag_document 时区分 SYSTEM 和 USER，用户文档和项目档案会写 userId，检索时移除外部传入的 userId/ownerType，再由后端根据 sourceType 和当前登录用户重建 filter。即使 Qdrant 召回了不该看的 chunk，回表时还会再次检查 document 的 ownerType、userId 和 indexed 状态。”

### 10.7 模拟面试怎么讲

“真实模拟面试不是简单聊天，而是一个阶段状态机：开场、简历项目、技术基础、项目深挖、场景设计、收尾。每轮保存消息、阶段和分数，问题会根据阶段、项目、简历、RAG 上下文和上一轮回答生成。结束后生成阶段表现、薄弱点、高风险回答、推荐学习和推荐训练，并沉淀能力快照。当前这一块更偏状态机 + RAG + 规则评分，后续可以升级为 Plan-Execute Agent。”

### 10.8 复盘 Agent 怎么讲

“复盘 Agent 的核心不是单次总结，而是数据聚合。它会拿最近项目训练报告、八股报告、能力快照、最新简历分析风险点，再根据薄弱标签去 RAG 里召回知识文章，最后让 LLM 输出结构化 JSON，包括 summary、keyFindings、recurringWeaknesses、causeAnalysis、resumeRisks 和 nextActions。生成结果会保存 sourceSnapshot，能说明这次复盘用了哪些数据源。”

### 10.9 项目亮点怎么讲

- 不是简单套壳聊天，而是围绕面试训练领域做了用户上下文、RAG、训练状态、报告和复盘闭环。
- RAG 不只接系统知识库，也接用户私有文档和项目档案，并做权限过滤。
- token 级流式输出兼顾实时体验和最终业务 payload。
- 复盘 Agent 和 Guide Agent 把训练数据转成下一步行动，形成产品内闭环。
- 有 AI 调用日志、回答质量 Rubric、能力画像和学习推荐，方便后续做可观测性和个性化推荐。

### 10.10 面试官可能追问什么

- 你们的 RAG 如何保证用户 A 检索不到用户 B 的文档？
- 为什么前端输出用 NDJSON，不直接用标准 SSE？
- 如何处理模型返回 JSON 不稳定？
- 你的 Guide action 和 Function Calling 有什么区别？
- 复盘 Agent 为什么算 Agent？它和普通总结报告有什么区别？
- 模拟面试目前是不是由 LLM 动态追问？如果不是，怎么升级？
- Embedding 维度、Qdrant payload、MySQL 元数据如何对应？
- AI 日志里是否保存 raw response，如何处理隐私？
- Redis 锁解决了哪些重复提交问题？
- 如果要接入 ReAct 或 Tool Calling，你会改哪些模块？

## 11. 后续技术升级路线

按优先级建议如下：

1. `TOOL-001`：统一 Tool / Action 调用协议。把 Guide action、训练入口、复盘 next action 抽象成统一结构，包含 name、description、params schema、permission、executor、audit log。
2. `OBS-001`：建设 AI / RAG / Agent 可观测性。为每次训练生成 trace_id，记录 prompt_version、retrieved_chunks、score、tool_action、latency、error，做基础看板。
3. `PLAN-001`：模拟面试 Agent 接入 Plan-Execute。创建会话时先生成面试计划，执行过程中根据回答动态调整阶段重点。
4. `MEMORY-001`：用户长期训练记忆。把长期弱点、常错知识点、简历风险、表达偏好沉淀为可检索 memory，并控制过期和置信度。
5. `REACT-001`：产品 Guide Agent 接入 ReAct 风格的受控思考-行动-观察循环。内部记录 action trace，但不向用户暴露推理链。
6. `RAG-001`：升级检索质量。增加 rerank、混合检索、召回评估集、chunk 命中解释和无关片段过滤。
7. `MCP-001`：探索 MCP 工具协议适配。将站内训练、报告、文档、复盘能力包装为 MCP tools，方便外部 Agent 调用。
8. `MULTI-AGENT-001`：引入多 Agent 编排。拆分面试官 Agent、评分 Agent、复盘 Agent、推荐 Agent，由编排器统一上下文和执行顺序。
9. `SEC-001`：工具调用安全沙箱。为 action/tool 增加参数白名单、幂等保护、危险操作确认、审计和回滚策略。

## 12. 结论

CodeCoach AI 当前真实技术底座已经覆盖 AI 应用开发中很关键的一批能力：RAG、Embedding、向量数据库、用户私有知识库、Prompt 工程、多轮对话、token 级流式输出、AI 日志、评分 Rubric、简历分析、能力画像和产品内受控 action。它适合重新定义为“AI Agent 面试训练平台”，但表达时要保持边界清晰：当前是 Agent 化平台雏形，最强的 Agent 能力在复盘和 Guide，训练模块具备多轮上下文和状态推进；Function Calling、ReAct、Plan-Execute、Multi-Agent、MCP 和长期 Memory 仍是后续升级方向。
