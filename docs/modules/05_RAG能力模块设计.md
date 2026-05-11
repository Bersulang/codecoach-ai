# RAG 能力模块设计

## 1. 文档说明

本文档用于设计 CodeCoach AI 的 RAG 能力模块。

当前 CodeCoach AI 已经完成：

```text
项目拷打训练
八股问答训练
项目训练报告
八股训练报告
训练历史
知识学习模块
能力画像模块
用户能力快照
官网与前端产品化体验
```

当前产品已经具备较完整的训练与成长闭环：

```text
项目档案 / 知识点
-> AI 训练
-> 训练报告
-> 能力画像
-> 知识学习
-> 再次训练
```

下一阶段需要引入 RAG 能力，让系统不再只依赖当前 prompt，而是可以从知识文章、用户项目档案、训练报告、能力画像和后续用户上传文档中检索相关上下文，从而增强 AI 追问、反馈、报告和学习推荐的质量。

---

## 2. 模块定位

CodeCoach AI 的 RAG 不是通用文档问答系统。

它不是：

```text
上传文档后随便问问题
企业知识库问答
通用搜索引擎
聊天机器人
```

它的定位是：

```text
面向 Java 后端求职训练场景的上下文增强与智能推荐能力。
```

RAG 模块的核心作用是：

```text
为 AI 面试训练、知识学习推荐、项目表达优化和能力画像分析提供可检索的上下文。
```

它服务于 CodeCoach AI 的主线：

```text
训练更精准
反馈更具体
报告更有依据
推荐更个性化
```

---

## 3. 为什么需要 RAG

当前系统已经有 AI 训练能力，但主要依赖 prompt 和当前会话上下文。

这种方式存在几个问题：

1. 模型无法长期记住用户资料；
2. 项目训练主要依赖用户手动填写的项目字段；
3. 八股问答无法充分利用知识文章库；
4. 训练报告中的薄弱点无法自动匹配最适合的学习内容；
5. 历史训练数据没有充分参与后续训练；
6. 简历、README、项目设计文档暂时无法被系统化利用；
7. AI 反馈可能泛化，不够贴合用户真实情况。

RAG 可以解决这些问题：

1. 检索知识文章，为八股训练提供知识依据；
2. 检索项目档案，为项目拷打提供真实上下文；
3. 检索历史训练报告，让后续训练更有针对性；
4. 检索用户薄弱点，为学习推荐提供依据；
5. 检索简历和项目文档，生成更贴近真实经历的追问；
6. 控制 prompt 上下文质量，减少无关信息；
7. 让 AI 从“泛泛回答”变成“基于用户资料和知识库回答”。

---

## 4. RAG 使用边界

第一版 RAG 必须控制范围。

### 4.1 第一版不做

第一版不做：

```text
通用文档问答助手
企业知识库问答
多人协作文档问答
自动爬取互联网资料
复杂 Agent 工作流
知识库在线编辑器
简历智能改写全流程
大型文件管理系统
```

原因：

1. 这些功能会让项目范围过大；
2. 容易偏离面试训练主线；
3. 会增加权限、安全、存储和成本复杂度；
4. 当前阶段更需要先打通 RAG 基础链路。

---

### 4.2 第一版只做

第一版只做：

```text
知识文章切片
知识文章向量化
向量存储
相似度检索
检索上下文注入八股训练
为智能学习推荐预留能力
```

也就是说，第一版 RAG 的核心目标是：

```text
让系统知识文章可以被检索，并增强八股训练效果。
```

---

## 5. 核心应用场景

RAG 模块未来会服务多个场景。

---

## 6. 场景一：八股问答训练增强

当前八股问答训练流程是：

```text
选择知识点
-> AI 提问
-> 用户回答
-> AI 反馈
-> 参考答案
-> 下一轮追问
-> 生成报告
```

接入 RAG 后：

```text
选择知识点
-> 检索相关知识文章片段
-> AI 基于知识片段提问
-> 用户回答
-> AI 基于知识片段反馈
-> AI 给出更准确参考答案
-> 生成更有依据的报告
```

示例：

用户选择：

```text
Redis / 缓存击穿
```

系统检索：

```text
Redis 缓存击穿面试表达指南
Redis 缓存穿透面试表达指南
Redis 分布式锁面试表达指南
```

AI 可以基于检索结果追问：

```text
互斥锁方案在高并发下会带来什么问题？
逻辑过期方案为什么适合热点数据？
缓存击穿和缓存穿透的本质区别是什么？
```

这样比只依赖 prompt 更稳定。

---

## 7. 场景二：项目拷打训练增强

当前项目拷打依赖用户填写的项目档案。

项目档案字段包括：

```text
项目名称
项目描述
技术栈
负责模块
项目亮点
项目难点
```

未来接入 RAG 后，可以检索：

```text
项目档案
项目 README
项目设计文档
历史项目训练报告
用户简历中的项目描述
```

AI 可以生成更贴近真实经历的追问：

```text
你在项目里用了 Redis 分布式锁，那么锁超时和业务执行时间不一致时怎么处理？
你说使用 MQ 做异步解耦，那消息重复消费如何保证幂等？
你项目中 MySQL 慢查询是如何定位和优化的？
```

这样项目拷打会更像真实技术面试。

---

## 8. 场景三：智能学习推荐

当前能力画像可以沉淀：

```text
用户薄弱点
低分能力维度
最近训练趋势
报告 summary
weaknessTags
```

接入 RAG 后，可以结合知识文章检索：

```text
用户薄弱点
+ 知识文章片段
+ 历史训练表现
-> 推荐学习文章
-> 推荐专项训练
```

示例：

能力画像发现用户多次暴露：

```text
逻辑过期
缓存一致性
分布式锁异常场景
```

RAG 检索到：

```text
Redis 缓存击穿面试表达指南
Redis 分布式锁面试表达指南
```

系统可以推荐：

```text
建议先学习 Redis 缓存击穿中的逻辑过期方案，再进行一次 Redis 缓存一致性专项训练。
```

---

## 9. 场景四：简历 / 项目文档增强

后续可以支持用户上传：

```text
简历 PDF
项目 README
项目设计文档
学习笔记
课程项目说明
```

系统可以解析、切片、向量化这些文档。

用途：

1. 生成更真实的项目追问；
2. 检查简历项目描述是否经得起追问；
3. 根据简历内容生成模拟面试；
4. 帮助用户优化项目表达；
5. 根据项目文档生成训练素材。

这一部分属于后续高级阶段，不在第一版 RAG 中实现。

---

## 10. 数据来源设计

RAG 可检索的数据来源分为系统数据和用户数据。

---

## 11. 系统知识文章

来源：

```text
knowledge_article
backend/src/main/resources/knowledge/*.md
```

内容包括：

```text
Redis 缓存击穿
Redis 缓存穿透
Redis 分布式锁
MySQL 索引
MySQL MVCC
JVM 内存区域
JVM 垃圾回收
JUC 线程池
Spring AOP
Spring 事务传播
```

用途：

1. 八股训练增强；
2. 参考答案增强；
3. 报告生成增强；
4. 学习推荐；
5. 后续知识路径推荐。

这是第一版 RAG 的优先数据源。

---

## 12. 用户项目档案

来源：

```text
project
```

字段包括：

```text
项目名称
项目描述
技术栈
负责模块
项目亮点
项目难点
创建时间
更新时间
```

用途：

1. 项目拷打训练增强；
2. 项目表达优化；
3. 项目相关知识点推荐；
4. 简历项目描述增强。

这一部分可以在知识文章 RAG 完成后接入。

---

## 13. 用户训练报告

来源：

```text
interview_report
question_training_report
user_ability_snapshot
```

内容包括：

```text
summary
strengths
weaknesses
suggestions
knowledgeGaps
qaReview
totalScore
difficulty
topic
project
```

用途：

1. 发现用户长期薄弱点；
2. 避免后续训练重复泛问；
3. 让 AI 更关注用户曾经答不好的地方；
4. 生成个性化学习推荐；
5. 支持能力画像增强。

---

## 14. 用户上传文档

来源：

```text
简历 PDF
项目 README
项目设计文档
学习笔记
```

用途：

1. 简历驱动追问；
2. 项目文档理解；
3. 项目经验抽取；
4. 面试问题生成；
5. 简历表达优化。

第一版暂不实现，但数据模型需要预留 `USER_UPLOAD` 类型。

---

## 15. RAG 总体架构

整体链路：

```text
数据源
-> 文档抽象 rag_document
-> 文本切片 rag_chunk
-> Embedding 生成
-> 向量库写入
-> 检索 topK chunks
-> prompt 上下文注入
-> AI 生成问题 / 反馈 / 推荐
```

模块分层：

```text
内容层：
knowledge_article / project / report / upload

文档层：
rag_document

切片层：
rag_chunk

向量层：
Qdrant / vector store

检索层：
RagRetrievalService

应用层：
八股训练 / 项目训练 / 学习推荐 / 简历增强
```

---

## 16. 核心数据模型

第一版建议新增：

```text
rag_document
rag_chunk
rag_embedding
```

其中 `rag_embedding` 是否必须取决于向量库使用方式。

如果使用 Qdrant，可以 MySQL 存储 `vector_id`，实际向量存储在 Qdrant。

---

## 17. rag_document 表设计

`rag_document` 表用于表示一个可检索文档。

例如：

```text
一篇知识文章
一个项目档案
一份训练报告
一个用户上传文件
```

建议字段：

```sql
CREATE TABLE rag_document (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'RAG文档ID',
    owner_type VARCHAR(32) NOT NULL COMMENT '归属类型：SYSTEM / USER',
    owner_id BIGINT DEFAULT NULL COMMENT '归属ID，SYSTEM可为空，USER为用户ID',
    user_id BIGINT DEFAULT NULL COMMENT '用户ID，系统文档为空',
    title VARCHAR(255) NOT NULL COMMENT '文档标题',
    source_type VARCHAR(64) NOT NULL COMMENT '来源类型',
    source_id BIGINT DEFAULT NULL COMMENT '来源ID',
    source_path VARCHAR(512) DEFAULT NULL COMMENT '来源路径',
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING / INDEXED / FAILED / DISABLED',
    chunk_count INT NOT NULL DEFAULT 0 COMMENT '切片数量',
    error_message VARCHAR(512) DEFAULT NULL COMMENT '错误信息',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_owner (owner_type, owner_id),
    KEY idx_user_id (user_id),
    KEY idx_source (source_type, source_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='RAG文档表';
```

---

## 18. rag_document 字段说明

| 字段 | 说明 |
|---|---|
| owner_type | SYSTEM 或 USER |
| owner_id | 所属用户或系统标识 |
| user_id | 用户私有文档对应用户 ID |
| title | 文档标题 |
| source_type | 来源类型 |
| source_id | 来源数据 ID |
| source_path | 文件或 classpath 路径 |
| status | 索引状态 |
| chunk_count | 切片数量 |
| error_message | 索引失败原因 |

---

## 19. owner_type 设计

`owner_type` 取值：

```text
SYSTEM
USER
```

含义：

```text
SYSTEM：系统公共知识，例如 knowledge_article
USER：用户私有数据，例如项目文档、简历、训练报告
```

权限规则：

```text
SYSTEM 文档所有登录用户可检索
USER 文档只能所属用户检索
```

---

## 20. source_type 设计

`source_type` 建议取值：

```text
KNOWLEDGE_ARTICLE
PROJECT
INTERVIEW_REPORT
QUESTION_REPORT
USER_UPLOAD
```

含义：

```text
KNOWLEDGE_ARTICLE：知识文章
PROJECT：项目档案
INTERVIEW_REPORT：项目训练报告
QUESTION_REPORT：八股训练报告
USER_UPLOAD：用户上传文档
```

第一版重点支持：

```text
KNOWLEDGE_ARTICLE
```

---

## 21. rag_chunk 表设计

`rag_chunk` 表用于存储文本切片。

建议字段：

```sql
CREATE TABLE rag_chunk (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'RAG切片ID',
    document_id BIGINT NOT NULL COMMENT '文档ID',
    user_id BIGINT DEFAULT NULL COMMENT '用户ID，系统文档为空',
    chunk_index INT NOT NULL COMMENT '切片序号',
    content TEXT NOT NULL COMMENT '切片内容',
    token_count INT DEFAULT NULL COMMENT 'Token数量估算',
    metadata JSON DEFAULT NULL COMMENT '元数据',
    embedding_status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '向量状态：PENDING / EMBEDDED / FAILED',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY idx_document_id (document_id),
    KEY idx_user_id (user_id),
    KEY idx_embedding_status (embedding_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='RAG切片表';
```

---

## 22. rag_chunk metadata 设计

`metadata` 用于保存切片来源信息。

知识文章切片 metadata 示例：

```json
{
  "sourceType": "KNOWLEDGE_ARTICLE",
  "articleId": 1,
  "topicId": 21,
  "category": "Redis",
  "topicName": "缓存击穿",
  "title": "Redis 缓存击穿面试表达指南",
  "section": "面试标准回答"
}
```

项目档案切片 metadata 示例：

```json
{
  "sourceType": "PROJECT",
  "projectId": 3,
  "projectName": "CodeCoach AI",
  "section": "技术栈"
}
```

训练报告切片 metadata 示例：

```json
{
  "sourceType": "QUESTION_REPORT",
  "reportId": 8,
  "topicId": 21,
  "category": "Redis",
  "section": "weaknesses"
}
```

---

## 23. rag_embedding 表设计

如果使用 Qdrant 存向量，MySQL 只需要记录向量库中的 `vector_id`。

建议字段：

```sql
CREATE TABLE rag_embedding (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'RAG向量记录ID',
    chunk_id BIGINT NOT NULL COMMENT '切片ID',
    vector_id VARCHAR(128) NOT NULL COMMENT '向量库ID',
    embedding_model VARCHAR(128) NOT NULL COMMENT 'Embedding模型',
    vector_store VARCHAR(64) NOT NULL DEFAULT 'QDRANT' COMMENT '向量库存储',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_chunk_model (chunk_id, embedding_model),
    UNIQUE KEY uk_vector_id (vector_id),
    KEY idx_chunk_id (chunk_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='RAG向量记录表';
```

---

## 24. 向量库选择

第一版推荐使用：

```text
Qdrant
```

原因：

1. Docker 部署简单；
2. 比 Milvus 轻量；
3. 对个人项目友好；
4. 支持向量相似度搜索；
5. 支持 metadata payload；
6. 后续可以扩展过滤条件。

不推荐第一版使用：

```text
Milvus
Elasticsearch
MySQL 存向量
自己手写 cosine 检索
```

原因：

```text
Milvus 太重
Elasticsearch 引入成本较高
MySQL 不适合向量检索
手写检索不适合作为工程项目亮点
```

---

## 25. Qdrant 集合设计

集合名称建议：

```text
codecoach_rag_chunks
```

每个 point：

```text
id = vector_id
vector = embedding 向量
payload = metadata + chunkId + documentId + ownerType + userId + sourceType
```

payload 示例：

```json
{
  "chunkId": 1001,
  "documentId": 101,
  "ownerType": "SYSTEM",
  "userId": null,
  "sourceType": "KNOWLEDGE_ARTICLE",
  "articleId": 1,
  "topicId": 21,
  "category": "Redis",
  "topicName": "缓存击穿",
  "title": "Redis 缓存击穿面试表达指南"
}
```

检索时可以按 payload 过滤：

```text
sourceType = KNOWLEDGE_ARTICLE
category = Redis
ownerType = SYSTEM
userId = 当前用户
```

---

## 26. Embedding 模型策略

Embedding 来源可以使用 OpenAI-compatible API 或其他兼容服务。

配置建议：

```yaml
ai:
  embedding:
    provider: openai-compatible
    base-url: ${EMBEDDING_BASE_URL}
    api-key: ${EMBEDDING_API_KEY}
    model: ${EMBEDDING_MODEL}
    timeout-seconds: 60
```

注意：

1. API Key 放在本地忽略配置；
2. 不提交密钥；
3. 支持后续切换 embedding 模型；
4. 记录 embedding 调用日志；
5. embedding 失败不影响主训练流程；
6. 静态知识文章只在内容变更时重新 embedding。

---

## 27. Embedding 调用日志

建议复用或扩展现有 AI 调用日志体系。

需要记录：

```text
userId
provider
modelName
requestType
latencyMs
success
errorCode
errorMessage
createdAt
```

requestType 可新增：

```text
RAG_EMBEDDING
RAG_RETRIEVAL
```

注意：

1. 不记录 API Key；
2. 不记录完整敏感文档内容；
3. 可以记录 chunkId / documentId；
4. 错误信息要可排查；
5. 不要把用户简历全文打入日志。

---

## 28. 文本切片策略

切片质量直接影响 RAG 效果。

第一版切片策略不需要过度复杂，但必须保证语义完整。

---

## 29. 知识文章切片策略

知识文章是 Markdown。

推荐切片方式：

```text
按 Markdown 标题层级切片
```

规则：

1. 以 `#` 和 `##` 分割主要章节；
2. 保留文章标题；
3. 每个切片包含当前章节标题；
4. 如果章节过长，再按段落拆分；
5. 每个 chunk 约 300 到 800 tokens；
6. metadata 记录 section 标题；
7. 不要把一个知识点切得太碎。

示例：

```text
文章：Redis 缓存击穿
切片 1：一句话解释
切片 2：面试标准回答
切片 3：核心原理
切片 4：常见追问
切片 5：项目表达模板
```

---

## 30. 项目档案切片策略

项目档案不是 Markdown，但可以转成结构化文本。

切片可以按字段组织：

```text
项目基本信息
项目描述
技术栈
负责模块
项目亮点
项目难点
```

示例：

```text
标题：项目技术栈
内容：该项目使用 Spring Boot、React、MySQL、Redis、JWT...
```

metadata 记录：

```json
{
  "sourceType": "PROJECT",
  "projectId": 3,
  "section": "techStack"
}
```

---

## 31. 训练报告切片策略

训练报告可按模块切片：

```text
summary
strengths
weaknesses
suggestions
knowledgeGaps
qaReview
```

用途：

1. 后续训练时检索用户曾经薄弱点；
2. 生成学习推荐；
3. 生成能力画像解释；
4. 让 AI 避免重复泛泛提问。

---

## 32. 检索策略

第一版检索输入可以来自：

```text
当前训练问题
当前用户回答
topicName
category
projectName
weaknessTags
report summary
```

检索输出：

```text
topK chunks
content
score
metadata
sourceType
title
```

默认 topK：

```text
3 到 5
```

不要一次检索过多内容，避免 prompt 过长。

---

## 33. 典型过滤策略

### 33.1 八股训练

八股训练优先检索：

```text
sourceType = KNOWLEDGE_ARTICLE
category = 当前 topic.category
topicId = 当前 topicId
```

如果 topicId 命中内容不足，可以放宽到 category。

---

### 33.2 项目训练

项目训练优先检索：

```text
sourceType = PROJECT
userId = 当前用户
projectId = 当前项目
```

后续再检索：

```text
INTERVIEW_REPORT
USER_UPLOAD
```

---

### 33.3 智能学习推荐

学习推荐检索：

```text
sourceType = KNOWLEDGE_ARTICLE
query = weaknessTags + low dimension + report summary
```

过滤可选：

```text
category in 用户薄弱分类
```

---

## 34. Prompt 注入策略

RAG 检索结果不能无脑塞进 prompt。

需要构造清晰上下文块。

示例：

```text
【检索到的相关知识】
1. 来源：Redis 缓存击穿面试表达指南 / 面试标准回答
内容：...

2. 来源：Redis 分布式锁面试表达指南 / 易错点
内容：...

【用户历史薄弱点】
- 逻辑过期方案表达不完整
- 分布式锁异常场景理解不足

【要求】
请优先基于上述上下文进行追问和反馈。
如果上下文与当前问题无关，可以忽略。
不要编造上下文中不存在的用户经历。
```

---

## 35. Prompt 注入原则

必须遵守：

1. 检索内容只作为参考；
2. 不要让 AI 盲目信任不相关内容；
3. 不要注入过多 chunk；
4. 每个 chunk 标明来源；
5. 系统知识和用户私有资料要分开；
6. 用户私有资料不能泄露给其他用户；
7. 上下文不足时允许 AI 说明不足；
8. Prompt 要保持结构清晰。

---

## 36. RAG 应用阶段规划

### 36.1 RAG-APP-001：知识文章索引

目标：

```text
将 knowledge_article Markdown 文件切片并写入向量库。
```

这是第一版最优先做的应用。

---

### 36.2 RAG-APP-002：八股训练增强

目标：

```text
在八股问答生成反馈、参考答案、追问时检索相关知识片段。
```

第一版可以只在 `generateFeedbackAndNextQuestion` 中接入。

---

### 36.3 RAG-APP-003：智能学习推荐

目标：

```text
根据 user_ability_snapshot 的 weaknessTags 检索知识文章，推荐学习内容。
```

可以用于：

```text
/insights
/dashboard
/report
```

---

### 36.4 RAG-APP-004：项目训练增强

目标：

```text
将项目档案、项目报告和用户上传文档纳入检索，让项目拷打更精准。
```

---

### 36.5 RAG-APP-005：用户上传文档

目标：

```text
支持用户上传简历、README、项目文档，并用于训练增强。
```

这是后续高级功能，不放在第一版。

---

## 37. 第一版 MVP 范围

RAG 第一版只做以下内容。

### 37.1 后端

1. `rag_document` 表；
2. `rag_chunk` 表；
3. `rag_embedding` 表；
4. Qdrant 本地配置；
5. Embedding API 接入；
6. Markdown 知识文章切片；
7. 知识文章索引服务；
8. 检索服务；
9. 检索调试接口；
10. 八股训练反馈阶段注入 RAG 上下文。

---

### 37.2 前端

第一版前端可以非常少。

可选：

1. 在 `/insights` 展示“智能推荐即将接入”；
2. 后续展示 RAG 推荐文章；
3. 暂不做 RAG 管理页面。

RAG 第一版主要是后端能力。

---

### 37.3 暂不做

1. 文件上传；
2. 简历解析；
3. 项目文档解析；
4. 项目训练 RAG；
5. 复杂 Agent；
6. 多轮规划；
7. 管理后台；
8. 用户知识库管理；
9. 在线编辑知识文章；
10. 自动爬虫。

---

## 38. 后端模块规划

建议新增包：

```text
com.codecoach.module.rag
```

子包：

```text
entity
mapper
service
service.impl
controller
model
support
constant
```

核心类规划：

```text
RagDocument
RagChunk
RagEmbedding

RagDocumentMapper
RagChunkMapper
RagEmbeddingMapper

RagIndexService
RagChunkService
EmbeddingService
VectorStoreService
RagRetrievalService

MarkdownChunkService
QdrantVectorStoreService
OpenAiCompatibleEmbeddingService
```

---

## 39. 核心服务职责

### 39.1 RagIndexService

负责索引数据源。

职责：

1. 索引知识文章；
2. 创建 rag_document；
3. 调用切片服务；
4. 保存 rag_chunk；
5. 调用 embedding 服务；
6. 写入向量库；
7. 更新索引状态。

---

### 39.2 MarkdownChunkService

负责 Markdown 切片。

职责：

1. 按标题切分；
2. 控制 chunk 长度；
3. 生成 metadata；
4. 保留章节标题；
5. 返回 chunk 列表。

---

### 39.3 EmbeddingService

负责生成文本向量。

职责：

1. 调用 embedding API；
2. 支持配置模型；
3. 处理超时；
4. 记录调用日志；
5. 返回 float vector。

---

### 39.4 VectorStoreService

负责和向量库交互。

职责：

1. 创建 collection；
2. upsert vector；
3. search vector；
4. delete vector；
5. metadata filter。

---

### 39.5 RagRetrievalService

负责业务侧检索。

职责：

1. 接收 query；
2. 生成 query embedding；
3. 调用向量库搜索；
4. 过滤权限；
5. 返回检索片段；
6. 格式化 prompt context。

---

## 40. 接口规划

### 40.1 索引所有知识文章

```http
POST /api/rag/index/knowledge-articles
```

用途：

```text
开发阶段手动触发索引全部知识文章。
```

返回：

```json
{
  "indexedCount": 10,
  "failedCount": 0
}
```

---

### 40.2 索引单篇知识文章

```http
POST /api/rag/index/knowledge-articles/{articleId}
```

用途：

```text
文章更新后重新索引单篇文章。
```

---

### 40.3 检索调试接口

```http
POST /api/rag/search
```

请求：

```json
{
  "query": "Redis 缓存击穿如何回答",
  "sourceTypes": ["KNOWLEDGE_ARTICLE"],
  "topK": 5
}
```

响应：

```json
{
  "chunks": [
    {
      "content": "缓存击穿是指...",
      "score": 0.82,
      "sourceType": "KNOWLEDGE_ARTICLE",
      "title": "Redis 缓存击穿面试表达指南",
      "metadata": {
        "category": "Redis",
        "topicName": "缓存击穿"
      }
    }
  ]
}
```

---

## 41. 接口权限设计

第一版：

```text
索引接口：登录用户可访问，后续改成管理员或内部接口
搜索接口：登录用户可访问
```

未来生产化时：

```text
索引接口应限制管理员或内部调用
用户私有数据检索必须按 userId 过滤
```

---

## 42. 降级策略

RAG 不能影响主训练流程。

如果 RAG 检索失败：

```text
记录 warn
继续使用原 prompt
不阻断训练
不向用户暴露底层错误
```

如果 embedding 失败：

```text
标记 chunk 为 FAILED
记录错误
允许后续重试
```

如果 Qdrant 不可用：

```text
RAG 功能降级
主训练功能继续可用
```

---

## 43. 成本控制

RAG 引入 embedding 成本和 token 成本。

需要控制：

1. 静态知识文章只在变更时重新 embedding；
2. 不要每次训练都重新 embedding 知识文章；
3. 查询 embedding 可以按 query 生成；
4. topK 默认 3 到 5；
5. prompt 注入内容限制总长度；
6. 长文档切片后分批 embedding；
7. embedding 调用失败可重试，但要限制次数；
8. 记录 embedding 调用日志。

---

## 44. 安全设计

RAG 会处理用户私有数据，需要提前设计安全边界。

### 44.1 系统知识

```text
owner_type = SYSTEM
user_id = null
```

所有登录用户可检索。

---

### 44.2 用户私有数据

```text
owner_type = USER
user_id = 当前用户 ID
```

只能当前用户检索。

---

### 44.3 检索权限

所有检索必须满足：

```text
SYSTEM 文档
OR 当前 userId 的 USER 文档
```

禁止用户通过接口传 userId 检索他人数据。

---

### 44.4 日志安全

禁止记录：

```text
API Key
Authorization Header
完整简历内容
完整用户上传文档
敏感个人信息
```

可以记录：

```text
documentId
chunkId
sourceType
latency
success
errorCode
```

---

## 45. 配置规划

建议新增配置：

```yaml
rag:
  enabled: true
  top-k: 5
  max-context-chars: 4000
  vector-store:
    provider: qdrant
    url: http://localhost:6333
    collection: codecoach_rag_chunks
  embedding:
    provider: openai-compatible
    base-url: ${EMBEDDING_BASE_URL}
    api-key: ${EMBEDDING_API_KEY}
    model: ${EMBEDDING_MODEL}
    timeout-seconds: 60
```

注意：

```text
真实 API Key 必须放在 application-local.yml 或环境变量中。
不要提交到 Git。
```

---

## 46. Qdrant 本地部署建议

`docker-compose` 可增加：

```yaml
qdrant:
  image: qdrant/qdrant:latest
  container_name: codecoach-qdrant
  ports:
    - "6333:6333"
    - "6334:6334"
  volumes:
    - qdrant_data:/qdrant/storage
```

注意：

```text
开发阶段使用本地 Qdrant。
生产阶段需要持久化 volume。
```

---

## 47. 与知识学习模块的关系

知识学习模块是内容层。

RAG 模块是检索增强层。

关系：

```text
knowledge_article
-> Markdown 正文
-> rag_document
-> rag_chunk
-> embedding
-> vector store
-> retrieval
```

知识文章可以独立展示。

RAG 会让知识文章进一步参与：

```text
八股训练
智能推荐
报告增强
```

---

## 48. 与能力画像模块的关系

能力画像提供用户侧信号。

例如：

```text
weaknessTags
low score dimensions
recent trend
latestEvidence
```

RAG 提供知识侧内容。

例如：

```text
Redis 缓存击穿文章片段
JVM GC 面试表达片段
MySQL MVCC 常见追问片段
```

两者结合：

```text
用户薄弱点
+ 知识库检索
+ AI 排序
-> 个性化学习推荐
```

---

## 49. 与项目训练模块的关系

项目训练可以使用 RAG 检索：

```text
项目档案
项目历史报告
用户上传项目文档
相关知识文章
```

这样 AI 可以提出更贴近用户项目的追问。

第一版暂不接项目训练 RAG。

后续阶段再做。

---

## 50. 与八股训练模块的关系

八股训练是第一版 RAG 应用重点。

接入点：

```text
生成第一题
提交回答后的反馈
参考答案生成
下一轮追问
报告生成
```

第一版优先接入：

```text
提交回答后的反馈和下一轮追问
```

原因：

1. 这个环节最需要知识依据；
2. 用户回答已经提供上下文；
3. 检索 query 更明确；
4. 改动范围相对可控。

---

## 51. 八股训练 RAG 检索 Query 设计

可构造 query：

```text
topicName + category + currentQuestion + userAnswer
```

示例：

```text
Redis 缓存击穿。问题：什么是缓存击穿？用户回答：热点 key 过期后大量请求打到数据库...
```

过滤：

```text
sourceType = KNOWLEDGE_ARTICLE
topicId = 当前 topicId
```

如果结果太少：

```text
放宽到 category = Redis
```

---

## 52. RAG Prompt 上下文示例

```text
【相关知识片段】
来源：Redis 缓存击穿面试表达指南 / 面试标准回答
内容：缓存击穿是指热点 key 在过期瞬间，大量请求直接访问数据库...

来源：Redis 缓存击穿面试表达指南 / 常见追问
内容：常见追问包括互斥锁的锁竞争、逻辑过期的旧数据问题...

【当前训练信息】
知识点：Redis 缓存击穿
难度：NORMAL
当前问题：请解释什么是缓存击穿
用户回答：...

【要求】
请基于当前用户回答和相关知识片段，给出反馈、参考答案和下一轮追问。
如果知识片段与用户回答无关，可以忽略。
不要编造用户没有提到的项目经历。
```

---

## 53. 开发任务拆分

### RAG-001 RAG 数据模型设计

新增：

```text
rag_document
rag_chunk
rag_embedding
```

---

### RAG-002 Qdrant 本地环境与配置

新增：

```text
docker-compose qdrant
RagProperties
VectorStore 配置
```

---

### RAG-003 Embedding 服务接入

实现：

```text
EmbeddingService
OpenAiCompatibleEmbeddingService
embedding 配置
embedding 调用日志
```

---

### RAG-004 Markdown 切片服务

实现：

```text
MarkdownChunkService
按标题切分 Markdown
生成 chunk metadata
```

---

### RAG-005 知识文章索引服务

实现：

```text
索引所有知识文章
索引单篇知识文章
写入 rag_document
写入 rag_chunk
写入 Qdrant
写入 rag_embedding
```

---

### RAG-006 检索服务与调试接口

实现：

```text
RagRetrievalService
POST /api/rag/search
```

---

### RAG-007 八股训练接入 RAG 上下文

实现：

```text
八股反馈 / 参考答案 / 追问阶段注入检索知识片段
RAG 失败自动降级
```

---

### RAG-008 能力画像智能学习推荐

实现：

```text
基于 weaknessTags 检索知识文章
AI 生成推荐学习路径
在 /insights 展示推荐
```

---

### RAG-009 项目档案接入 RAG

实现：

```text
项目档案切片
项目训练检索
项目拷打增强
```

---

### RAG-010 用户文档上传与索引

实现：

```text
简历 / README / 项目文档上传
解析
切片
embedding
检索
```

---

## 54. 第一阶段开发顺序建议

强烈建议按以下顺序：

```text
RAG-001 数据模型
RAG-002 Qdrant 环境
RAG-003 Embedding 接入
RAG-004 Markdown 切片
RAG-005 知识文章索引
RAG-006 检索调试接口
RAG-007 八股训练接入
```

不要一开始就做：

```text
用户上传文档
简历解析
Agent
复杂学习推荐
项目训练 RAG
```

---

## 55. 阶段完成标准

RAG 第一版完成后，应满足：

1. 知识文章可以被切片；
2. 切片可以生成 embedding；
3. embedding 可以写入 Qdrant；
4. MySQL 可以记录 document / chunk / vector_id；
5. 用户可以通过 query 检索相关文章片段；
6. 检索接口支持 sourceType 和 topK；
7. 八股训练反馈阶段可以注入检索上下文；
8. RAG 检索失败时自动降级为普通 AI 调用；
9. 不影响已有项目训练；
10. 不影响八股训练原有主流程；
11. 不泄露用户私有数据；
12. 为后续智能推荐和文档上传打基础。

---

## 56. 风险与注意事项

### 56.1 RAG 容易做大

RAG 容易扩展成：

```text
文档系统
搜索系统
Agent 系统
推荐系统
知识库系统
```

第一版必须限制边界。

---

### 56.2 检索质量决定效果

如果切片粗糙、metadata 不完整、query 构造不好，RAG 效果会很差。

所以第一版应重点关注：

```text
知识文章切片质量
metadata 完整性
query 构造
topK 控制
prompt 注入格式
```

---

### 56.3 不要影响主训练流程

RAG 是增强能力，不是主流程依赖。

必须保证：

```text
RAG 失败
-> 训练继续
-> 使用普通 prompt
-> 记录 warn
```

---

### 56.4 成本控制

Embedding 和 prompt 注入都会增加成本。

需要控制：

```text
只索引变更内容
限制 topK
限制上下文长度
避免重复 embedding
```

---

## 57. 简历表达方向

RAG 第一版完成后，可以在简历中描述为：

```text
设计并实现面向 AI 面试训练场景的 RAG 检索增强模块，将 Markdown 知识文章切片并向量化存入 Qdrant，支持按知识点、分类和用户回答检索相关知识片段，并注入八股问答训练 Prompt，提升 AI 反馈、参考答案和追问生成的准确性。
```

进一步可以描述为：

```text
RAG 模块采用 MySQL 存储文档与切片元数据，Qdrant 存储向量索引，Embedding 服务支持 OpenAI-compatible 模型配置。系统通过 sourceType、topicId、category 和 userId 实现检索过滤，为后续用户项目文档、简历解析和个性化学习推荐预留扩展能力。
```

---

## 58. 总结

RAG 模块是 CodeCoach AI 从“调用大模型的训练产品”升级为“具备上下文增强和个性化推荐能力的 AI 产品”的关键阶段。

它不是为了做通用知识库问答，而是服务于：

```text
更精准的项目追问
更可靠的八股反馈
更个性化的学习推荐
更真实的简历 / 项目文档训练
```

第一版应聚焦知识文章 RAG：

```text
knowledge_article Markdown
-> 切片
-> embedding
-> Qdrant
-> 检索
-> 八股训练增强
```

在这个基础上，再逐步扩展到能力画像推荐、项目档案增强和用户上传文档。