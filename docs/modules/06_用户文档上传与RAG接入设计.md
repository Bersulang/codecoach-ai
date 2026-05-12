# 用户文档上传与 RAG 接入设计

## 1. 文档说明

本文档用于设计 CodeCoach AI 的用户文档上传与 RAG 接入模块。

当前 CodeCoach AI 已经完成：

```text
项目拷打训练
八股问答训练
知识学习模块
能力画像模块
RAG 基础能力
知识文章 RAG
项目档案 RAG
八股训练 RAG 增强
项目训练 RAG 增强
token 级流式输出
Aliyun OSS 配置
头像上传能力
```

下一阶段需要让用户上传自己的简历、README、项目设计文档、学习笔记，并让这些用户私有文档成为 AI 面试训练的上下文来源。

---

## 2. 模块定位

用户文档上传模块不是普通网盘，也不是通用文件管理系统。

它的定位是：

```text
面向 AI 面试训练场景的用户私有上下文增强模块。
```

它的核心作用是：

```text
让用户上传的真实材料参与 RAG 检索，从而增强项目拷打、简历追问、项目表达优化和个性化学习推荐。
```

它服务于：

```text
项目拷打训练
简历项目追问
项目表达优化
后续 Agent 面试复盘
个性化学习推荐
```

它不是：

```text
通用网盘
企业文档管理系统
多人协作知识库
在线文档编辑器
文件分享系统
```

---

## 3. 为什么需要用户文档

当前项目训练主要依赖用户手动填写的项目档案。

项目档案通常包含：

```text
项目名称
项目描述
技术栈
负责模块
项目亮点
项目难点
```

这些字段已经可以支持基础项目拷打，但真实面试中，面试官往往会根据更具体的材料追问，例如：

```text
简历中的项目描述
README 中的模块说明
接口文档中的业务流程
数据库设计文档中的表结构
项目总结中的技术难点
学习笔记中的知识理解
```

如果系统只能读取用户手填字段，AI 追问会受到上下文限制。

用户文档上传可以让 AI 使用更丰富、更真实的上下文，从而提升训练效果。

---

## 4. 核心价值

用户文档上传模块带来的核心价值：

1. 让项目拷打更贴近用户真实项目；
2. 让简历中的项目经历可以被 AI 深度追问；
3. 让 README、设计文档、学习笔记参与 RAG 检索；
4. 让 AI 反馈不再只依赖简短项目字段；
5. 为后续简历优化、项目复盘 Agent、个性化学习推荐打基础。

---

## 5. 核心使用场景

## 5.1 简历驱动项目拷打

用户上传简历后，系统解析其中的项目经历。

后续 AI 可以围绕简历内容进行追问，例如：

```text
你在简历中提到使用 Redis 做缓存优化，具体优化了哪些接口？
你说项目支持高并发访问，你们做了哪些限流或削峰措施？
你提到负责订单模块，订单状态流转是如何设计的？
```

这会比普通项目档案追问更加真实。

---

## 5.2 README / 项目文档增强项目训练

用户上传项目 README 或设计文档后，项目训练可以检索这些内容。

例如项目 README 中写了：

```text
系统使用 Redis Stream 实现异步订单处理。
```

AI 可以追问：

```text
你为什么选择 Redis Stream，而不是 Kafka 或 RabbitMQ？
Redis Stream 消费组异常时如何处理？
消息重复消费如何保证幂等？
```

---

## 5.3 学习笔记增强八股训练

用户可以上传自己的学习笔记。

例如：

```text
JVM 学习笔记
Redis 面试题总结
MySQL 索引优化笔记
```

后续八股训练可以同时参考：

```text
系统知识文章
用户个人笔记
历史训练报告
```

从而让训练更贴合用户自己的知识体系。

---

## 5.4 能力画像智能推荐增强

当前能力画像已经可以沉淀：

```text
weaknessTags
低分维度
最近训练证据
训练趋势
```

用户文档接入后，系统可以进一步检索用户自己的笔记。

例如：

```text
用户薄弱点：MySQL MVCC
系统知识文章：MySQL MVCC 面试表达指南
用户笔记：MySQL 事务隔离级别笔记
```

系统可以推荐：

```text
你最近在 MVCC 和快照读问题上多次暴露薄弱点，建议先复习你的 MySQL 笔记中事务隔离级别部分，再学习系统知识卡片并进行一次 MySQL 专项训练。
```

---

## 6. 支持文件类型

## 6.1 第一版支持

第一版建议支持：

```text
.txt
.md
.pdf
```

原因：

1. TXT 解析最简单；
2. Markdown 可以复用现有 Markdown 切片能力；
3. PDF 覆盖简历和常见项目文档；
4. 三者足够支撑 MVP。

---

## 6.2 后续支持

后续可以扩展：

```text
.docx
.pptx
.xlsx
图片 OCR
```

但第一版暂不做。

原因：

1. Office 文档解析复杂度更高；
2. 图片 OCR 需要额外模型或服务；
3. 初期重点是打通上传、解析、切片、索引、检索链路；
4. 不要让文档解析复杂度拖慢 RAG 主线。

---

## 7. 总体链路

用户文档上传与 RAG 接入链路：

```text
用户上传文件
-> 文件保存到 OSS
-> user_document 保存元数据
-> 文本解析
-> 文档切片
-> rag_document
-> rag_chunk
-> embedding
-> Qdrant
-> rag_embedding
-> 项目训练 / 八股训练 / 推荐场景检索
```

整体分层：

```text
文件存储层：Aliyun OSS
元数据层：user_document
解析层：DocumentParseService
切片层：DocumentChunkService / MarkdownChunkService
RAG 元数据层：rag_document / rag_chunk / rag_embedding
向量层：Qdrant
应用层：项目训练 / 八股训练 / 成长洞察推荐
```

---

## 8. 存储设计

## 8.1 原始文件存储

原始文件存储在 Aliyun OSS。

OSS key 建议：

```text
documents/{userId}/{uuid}.{ext}
```

例如：

```text
documents/1/8d31dff0-72f1-4c32-8d6e-57c0f74a9e1a.pdf
```

不要使用用户原始文件名作为 OSS key。

原因：

1. 避免中文文件名兼容问题；
2. 避免文件名泄露隐私；
3. 避免路径注入；
4. 避免重名覆盖。

---

## 8.2 数据库存储

数据库保存文件元数据，不直接保存完整文件内容。

元数据包括：

```text
用户 ID
关联项目 ID
文件标题
原始文件名
文件类型
文件大小
OSS key
文件 URL
解析状态
索引状态
错误信息
创建时间
更新时间
逻辑删除标记
```

---

## 8.3 RAG 存储

解析后的文本不直接作为一个大字段进入 AI。

需要先切片，再进入 RAG：

```text
user_document
-> rag_document
-> rag_chunk
-> rag_embedding
-> Qdrant vector
```

---

## 9. 数据模型设计

第一版建议新增表：

```text
user_document
```

---

## 10. user_document 表设计

建议 SQL：

```sql
CREATE TABLE IF NOT EXISTS user_document (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户文档ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    project_id BIGINT DEFAULT NULL COMMENT '关联项目ID，可为空',
    title VARCHAR(255) NOT NULL COMMENT '文档标题',
    original_filename VARCHAR(255) NOT NULL COMMENT '原始文件名',
    file_type VARCHAR(32) NOT NULL COMMENT '文件类型：TXT / MARKDOWN / PDF',
    file_size BIGINT NOT NULL DEFAULT 0 COMMENT '文件大小，单位字节',
    oss_key VARCHAR(512) NOT NULL COMMENT 'OSS对象Key',
    file_url VARCHAR(1024) DEFAULT NULL COMMENT '文件访问URL',
    parse_status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '解析状态：PENDING / PARSED / FAILED',
    index_status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '索引状态：PENDING / INDEXED / FAILED',
    error_message VARCHAR(512) DEFAULT NULL COMMENT '错误信息',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted_at DATETIME DEFAULT NULL COMMENT '删除时间',
    KEY idx_user_id (user_id),
    KEY idx_project_id (project_id),
    KEY idx_user_project (user_id, project_id),
    KEY idx_file_type (file_type),
    KEY idx_parse_status (parse_status),
    KEY idx_index_status (index_status),
    KEY idx_user_created (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户文档表';
```

---

## 11. user_document 字段说明

| 字段 | 说明 |
|---|---|
| user_id | 文档所属用户 |
| project_id | 关联项目，可为空 |
| title | 文档标题 |
| original_filename | 用户上传时的原始文件名 |
| file_type | 标准化后的文件类型 |
| file_size | 文件大小 |
| oss_key | OSS 对象 Key |
| file_url | 文件访问 URL |
| parse_status | 文本解析状态 |
| index_status | RAG 索引状态 |
| error_message | 解析或索引失败原因 |
| is_deleted | 逻辑删除标记 |

---

## 12. file_type 设计

第一版支持：

```text
TXT
MARKDOWN
PDF
```

后续扩展：

```text
DOCX
PPTX
XLSX
IMAGE
```

文件类型应由后端根据后缀和 MIME 共同判断，不完全信任前端传值。

---

## 13. parse_status 设计

取值：

```text
PENDING
PARSED
FAILED
```

含义：

```text
PENDING：等待解析
PARSED：解析成功
FAILED：解析失败
```

---

## 14. index_status 设计

取值：

```text
PENDING
INDEXED
FAILED
```

含义：

```text
PENDING：等待索引
INDEXED：已进入 RAG
FAILED：索引失败
```

---

## 15. 与 rag_document 的关系

用户上传文档进入 RAG 时：

```text
rag_document.owner_type = USER
rag_document.owner_id = userId
rag_document.user_id = userId
rag_document.source_type = USER_UPLOAD
rag_document.source_id = user_document.id
rag_document.title = user_document.title
rag_document.source_path = user_document.oss_key
```

---

## 16. RAG metadata 设计

用户文档切片 metadata 示例：

```json
{
  "sourceType": "USER_UPLOAD",
  "userDocumentId": 1,
  "projectId": 3,
  "fileType": "PDF",
  "originalFilename": "resume.pdf",
  "title": "Java 后端简历",
  "section": "项目经历",
  "pageNo": 2,
  "ownerType": "USER",
  "userId": 1
}
```

必须包含：

```text
sourceType
userDocumentId
title
fileType
ownerType
userId
```

如果关联项目，还应包含：

```text
projectId
```

如果是 PDF，可以包含：

```text
pageNo
```

---

## 17. 上传接口设计

## 17.1 上传用户文档

```http
POST /api/user-documents
```

请求：

```text
multipart/form-data
file: 文件
projectId: 可选
title: 可选
```

响应：

```json
{
  "id": 1,
  "title": "CodeCoach AI README",
  "originalFilename": "README.md",
  "fileType": "MARKDOWN",
  "fileSize": 12345,
  "parseStatus": "PARSED",
  "indexStatus": "INDEXED",
  "createdAt": "2026-05-12 10:00:00"
}
```

要求：

1. 需要登录；
2. userId 从登录态获取；
3. 不允许前端传 userId；
4. projectId 可选；
5. 如果传 projectId，需要校验项目属于当前用户；
6. 文件上传到 OSS；
7. 上传成功后保存 user_document；
8. 第一版可以同步解析和索引；
9. 失败时返回友好错误；
10. 不泄露 OSS 内部信息。

---

## 18. 文档列表接口

```http
GET /api/user-documents
```

查询参数：

```text
projectId 可选
fileType 可选
```

返回当前用户的文档列表。

响应示例：

```json
[
  {
    "id": 1,
    "title": "CodeCoach AI README",
    "originalFilename": "README.md",
    "fileType": "MARKDOWN",
    "fileSize": 12345,
    "parseStatus": "PARSED",
    "indexStatus": "INDEXED",
    "createdAt": "2026-05-12 10:00:00"
  }
]
```

要求：

1. 只返回当前用户文档；
2. 不返回其他用户文档；
3. 不返回敏感 OSS 配置信息；
4. 默认按 created_at DESC 排序；
5. 过滤 is_deleted = 0。

---

## 19. 文档详情接口

```http
GET /api/user-documents/{id}
```

返回文档元信息。

第一版不直接返回完整文档内容。

原因：

1. 文件可能很长；
2. 文档内容可能包含隐私；
3. 当前模块重点是 RAG 训练增强，不是文档预览器。

---

## 20. 删除文档接口

```http
DELETE /api/user-documents/{id}
```

删除策略：

```text
逻辑删除 user_document
清理或禁用对应 RAG 索引
尽力删除 Qdrant vector
```

第一版建议：

1. user_document.is_deleted = 1；
2. rag_document.status = DISABLED；
3. 删除 rag_embedding；
4. 删除 rag_chunk；
5. 删除 Qdrant vector；
6. OSS 原文件可以先保留，后续做彻底清理策略。

---

## 21. 重新索引接口

```http
POST /api/user-documents/{id}/reindex
```

用途：

1. 解析逻辑升级后重新索引；
2. 索引失败后重试；
3. 手动修复 Qdrant 与 MySQL 不一致。

要求：

1. 需要登录；
2. 只能重建自己的文档；
3. 重建前清理旧 RAG 数据；
4. 重新解析文本；
5. 重新切片、embedding、写入 Qdrant；
6. 更新 parse_status 和 index_status。

---

## 22. 文本解析策略

第一版解析能力：

```text
TXT：直接读取文本
MD：读取 Markdown 原文
PDF：使用 PDFBox 提取文本
```

---

## 23. TXT 解析

TXT 解析规则：

1. 使用 UTF-8；
2. 读取完整文本；
3. 去除不可见控制字符；
4. 文本为空则解析失败；
5. 文件过大时限制最大字符数。

---

## 24. Markdown 解析

Markdown 解析规则：

1. 保留标题结构；
2. 保留列表、代码块等文本；
3. 可复用 MarkdownChunkService；
4. 不需要转 HTML；
5. 不需要渲染预览。

Markdown 文档适合：

```text
README
学习笔记
项目总结
接口说明
```

---

## 25. PDF 解析

PDF 第一版使用 PDFBox 提取文本。

注意事项：

1. 普通文本 PDF 可以解析；
2. 扫描版 PDF 可能解析不到文字；
3. 不做 OCR；
4. 解析不到文字时给出友好提示；
5. 不保证 PDF 排版完全还原；
6. 可以按页记录 pageNo。

PDF 适合：

```text
简历
项目说明书
课程报告
面试资料
```

---

## 26. 文档切片策略

不同文件类型使用不同切片策略。

---

## 27. Markdown 切片

Markdown 使用已有 MarkdownChunkService 或扩展能力。

策略：

1. 按 `##` 标题切分；
2. 保留文档标题；
3. 保留章节标题；
4. 长章节按段落拆分；
5. metadata 记录 section。

---

## 28. TXT 切片

TXT 按段落切片。

策略：

1. 按空行拆分段落；
2. 多个短段落合并；
3. 控制 chunk 长度；
4. 长段落硬切；
5. metadata.section 可以使用 “全文” 或 “段落 N”。

---

## 29. PDF 切片

PDF 可以按页和段落组合切片。

策略：

1. 先按页提取文本；
2. 每页文本再按段落拆分；
3. metadata 记录 pageNo；
4. section 可使用 “第 N 页”；
5. 控制 chunk 长度。

---

## 30. 切片长度控制

建议第一版保持和知识文章 RAG 类似：

```text
maxChunkChars = 1200
minChunkChars = 120
overlapChars = 100
```

过短 chunk 不进入索引，除非整篇文档很短。

---

## 31. RAG 索引策略

上传文档后，需要进入 RAG。

索引流程：

```text
user_document
-> 读取 OSS 文件
-> 文本解析
-> 文本切片
-> 创建 rag_document
-> 创建 rag_chunk
-> 调用 EmbeddingService
-> 写入 Qdrant
-> 创建 rag_embedding
-> 更新 index_status
```

---

## 32. 同步还是异步

第一版推荐：

```text
上传后同步解析和索引
```

原因：

1. 当前是个人项目；
2. 文档数量不会太大；
3. 实现简单；
4. 用户可以立即使用文档增强训练。

缺点：

```text
上传接口可能较慢
```

后续如果文档变大或用户变多，可以改成：

```text
上传后进入 PENDING
后台异步解析索引
前端轮询状态
```

---

## 33. RAG sourceType 设计

用户文档进入 RAG 时：

```text
sourceType = USER_UPLOAD
ownerType = USER
```

Qdrant payload 必须包含：

```text
sourceType
ownerType
userId
userDocumentId
projectId
fileType
title
section
```

---

## 34. 项目训练如何使用用户文档

当前项目训练已经支持项目档案 RAG。

接入用户文档后，项目训练检索范围变成：

```text
PROJECT
USER_UPLOAD
```

当训练 session 关联 projectId 时，检索：

```json
{
  "sourceTypes": ["PROJECT", "USER_UPLOAD"],
  "filter": {
    "userId": 1,
    "projectId": 3
  }
}
```

注意：

1. userId 不能来自前端；
2. userId 必须从登录态获取；
3. projectId 来自 session；
4. 只能检索当前用户文档；
5. 没有用户文档时仍可使用 PROJECT RAG；
6. RAG 失败时项目训练降级为原逻辑。

---

## 35. 八股训练如何使用用户文档

第一版可以暂不接八股训练用户文档。

后续可选策略：

```text
系统知识文章
+ 用户学习笔记
```

检索条件：

```json
{
  "sourceTypes": ["KNOWLEDGE_ARTICLE", "USER_UPLOAD"],
  "filter": {
    "userId": 1,
    "fileType": "MARKDOWN"
  }
}
```

适合用户上传：

```text
Redis 笔记
JVM 笔记
MySQL 笔记
```

第一版优先把用户文档用于项目训练。

---

## 36. 简历如何使用

简历可以作为 USER_UPLOAD 文档。

第一版不单独增加简历解析逻辑，只作为普通 PDF 上传。

后续可以增加字段：

```text
document_type
```

取值：

```text
RESUME
PROJECT_DOC
NOTE
OTHER
```

如果加入 document_type，则简历驱动训练可以更精准。

第一版可以先不加，避免过度设计。

---

## 37. 权限设计

权限是该模块的重点。

必须保证：

1. 用户只能上传自己的文档；
2. 用户只能查看自己的文档；
3. 用户只能删除自己的文档；
4. 用户只能重新索引自己的文档；
5. 用户只能检索自己的 USER_UPLOAD 文档；
6. 前端不能传 userId；
7. userId 必须从 UserContext 获取；
8. Qdrant payload 必须带 userId；
9. RagRetrievalService 必须强制 userId 过滤；
10. 不允许用户通过 filter.userId 越权。

---

## 38. OSS 安全设计

上传 OSS 时：

1. 文件名使用 UUID；
2. 路径包含 userId；
3. 不直接使用原始文件名作为 key；
4. 限制文件类型；
5. 限制文件大小；
6. 不打印 OSS accessKeySecret；
7. 不返回内部 bucket 配置；
8. 上传失败返回友好错误；
9. 原始文件名只存在数据库中用于展示。

---

## 39. 文件校验设计

上传时需要校验：

```text
文件不能为空
文件大小不能超过限制
后缀必须在白名单
MIME 类型必须合理
文件类型和后缀要匹配
```

第一版大小限制建议：

```text
10MB
```

允许后缀：

```text
.txt
.md
.pdf
```

允许 MIME：

```text
text/plain
text/markdown
application/pdf
application/octet-stream 仅谨慎兼容
```

---

## 40. 日志安全

禁止日志记录：

```text
OSS accessKeySecret
完整文档内容
完整简历内容
完整向量
Authorization Header
用户敏感信息
```

可以记录：

```text
userId
documentId
fileType
fileSize
parseStatus
indexStatus
chunkCount
latencyMs
错误类型
```

---

## 41. 错误处理

常见错误：

```text
文件为空
文件过大
文件类型不支持
OSS 上传失败
PDF 解析失败
解析后文本为空
Embedding 失败
Qdrant 写入失败
RAG 元数据保存失败
```

处理原则：

1. 上传失败：不保存 user_document；
2. 解析失败：保存文档，但 parse_status=FAILED；
3. 索引失败：保存文档，但 index_status=FAILED；
4. 用户可重新索引；
5. 错误信息控制在 512 字以内；
6. 不暴露内部堆栈。

---

## 42. 前端页面规划

建议新增页面：

```text
/documents
```

页面名称：

```text
我的文档
```

定位：

```text
管理用于 AI 面试训练的简历、README、项目文档和学习笔记。
```

---

## 43. /documents 页面结构

页面包含：

1. 页面头部；
2. 上传区域；
3. 文档列表；
4. 状态标签；
5. 删除按钮；
6. 重新索引按钮；
7. 空状态。

---

## 44. 上传区域

上传区域文案：

```text
上传简历、README、项目设计文档或学习笔记，让 AI 在训练时参考你的真实材料。
```

支持：

```text
拖拽上传
点击选择文件
选择关联项目，可选
填写标题，可选
```

第一版普通上传即可，不强制拖拽。

---

## 45. 文档列表

每个文档卡片展示：

```text
标题
原始文件名
文件类型
文件大小
关联项目
解析状态
索引状态
创建时间
```

操作：

```text
重新索引
删除
```

---

## 46. 状态展示

parse_status：

```text
PENDING：解析中
PARSED：解析完成
FAILED：解析失败
```

index_status：

```text
PENDING：等待索引
INDEXED：已加入 RAG
FAILED：索引失败
```

---

## 47. Dashboard 入口

Dashboard 可以增加入口：

```text
我的文档
上传简历、README 和项目文档，让 AI 训练更贴近你的真实经历。
```

按钮：

```text
管理文档 -> /documents
```

---

## 48. 头像菜单入口

头像菜单可以增加：

```text
我的文档 -> /documents
```

建议菜单顺序：

```text
个人中心
工作台
成长洞察
我的文档
退出登录
```

如果菜单太长，可以放到工作台。

---

## 49. 项目详情页入口

项目详情页或项目编辑页可以增加：

```text
项目文档
```

用户可以上传当前项目的 README 或设计文档。

上传时自动带：

```text
projectId
```

第一版可选，不是必须。

---

## 50. MVP 范围

第一版后端做：

1. user_document 表；
2. OSS 上传文档；
3. 文档列表接口；
4. 文档详情接口；
5. 文档删除接口；
6. 文档重新索引接口；
7. TXT / MD / PDF 解析；
8. 用户文档 RAG 索引；
9. 项目训练检索 USER_UPLOAD。

第一版前端做：

1. /documents 页面；
2. 上传文档；
3. 文档列表；
4. 删除文档；
5. 重新索引；
6. Dashboard 入口；
7. 头像菜单入口。

第一版暂不做：

1. OCR；
2. DOCX；
3. PPTX；
4. XLSX；
5. 在线预览；
6. 文档编辑；
7. 分享；
8. 多人协作；
9. Agent；
10. 简历结构化抽取；
11. 自动简历优化。

---

## 51. 开发任务拆分

## 51.1 DOC-001 user_document 表结构

目标：

```text
新增 user_document 表
```

只做 SQL。

---

## 51.2 DOC-002 文档上传 OSS 接口

目标：

```text
上传文件到 OSS
保存 user_document 元数据
限制文件类型和大小
```

---

## 51.3 DOC-003 文档文本解析服务

目标：

```text
TXT 解析
MD 解析
PDF 解析
解析状态更新
```

---

## 51.4 DOC-004 用户文档 RAG 索引服务

目标：

```text
解析文本切片
创建 rag_document
创建 rag_chunk
调用 embedding
写入 Qdrant
创建 rag_embedding
更新 index_status
```

---

## 51.5 DOC-005 文档管理接口

目标：

```text
列表
详情
删除
重新索引
```

---

## 51.6 DOC-FE-001 文档管理页面

目标：

```text
/documents 页面
上传
列表
删除
重新索引
状态展示
```

---

## 51.7 DOC-006 项目训练接入 USER_UPLOAD RAG

目标：

```text
项目训练检索 PROJECT + USER_UPLOAD
让项目文档参与项目拷打训练
```

---

## 51.8 DOC-007 项目详情页上传入口

目标：

```text
在项目详情页上传项目文档
自动关联 projectId
```

可放到后续。

---

## 52. 与 RAG 模块的关系

用户文档模块复用现有 RAG 能力：

```text
EmbeddingService
VectorStoreService
RagIndexService 部分能力
RagRetrievalService
RagConstants
RAG metadata 结构
```

需要注意：

1. 不要重新造一套向量写入逻辑；
2. 尽量复用重建索引逻辑；
3. USER_UPLOAD 和 PROJECT / KNOWLEDGE_ARTICLE 使用同一套 RAG 元数据表；
4. 权限过滤必须比系统知识更严格。

---

## 53. 与项目训练的关系

项目训练未来的上下文来源：

```text
project 表结构化字段
USER_UPLOAD 中关联当前 projectId 的文档
历史项目训练报告
```

第一版接入：

```text
project
+ USER_UPLOAD
```

---

## 54. 与能力画像的关系

能力画像可以使用用户文档作为推荐依据。

例如：

```text
用户薄弱点：Redis 分布式锁
用户笔记：Redis 锁机制.md
系统文章：Redis 分布式锁面试表达指南
```

后续可以生成更个性化建议。

第一版不接能力画像用户文档推荐，只保留扩展空间。

---

## 55. 与 Agent 的关系

用户文档上传是未来 Agent 的基础。

例如未来的“面试复盘 Agent”可以使用：

```text
简历
项目文档
训练报告
能力画像
知识文章
```

生成：

```text
下一周训练计划
项目表达优化建议
简历项目风险点
面试前冲刺清单
```

但当前不做 Agent。

---

## 56. 安全风险

用户文档可能包含敏感信息，例如：

```text
手机号
邮箱
学校
公司
项目代码
业务数据
简历隐私
```

因此必须注意：

1. 不在日志打印完整内容；
2. 不允许越权访问；
3. 不允许越权检索；
4. 不返回其他用户文档；
5. 不把用户文档混入系统公共知识；
6. Qdrant payload 必须带 userId；
7. 检索 USER_UPLOAD 必须强制 userId 过滤。

---

## 57. 性能风险

上传文档可能导致：

```text
解析耗时
embedding 耗时
Qdrant 写入耗时
接口等待时间长
```

第一版文档小，可以同步处理。

后续可以优化：

```text
异步任务
上传后轮询状态
失败重试
批量 embedding
索引队列
```

---

## 58. 成本控制

用户文档 embedding 会产生成本。

控制策略：

1. 限制文件大小；
2. 限制支持类型；
3. 避免重复索引；
4. 删除文档时清理向量；
5. 重新索引需要用户主动触发；
6. 后续可限制每个用户文档数量；
7. 后续可限制每日索引次数。

---

## 59. 阶段完成标准

用户文档上传第一版完成后应满足：

1. 用户可以上传 TXT / MD / PDF；
2. 文件保存到 OSS；
3. 文档元数据保存到 user_document；
4. 文档可以解析出文本；
5. 文档可以切片；
6. 文档可以 embedding；
7. 文档可以写入 Qdrant；
8. rag_document.source_type = USER_UPLOAD；
9. 用户只能看到自己的文档；
10. 用户只能检索自己的文档；
11. 项目训练可以检索项目关联文档；
12. 删除文档后不再参与检索；
13. 不影响知识文章 RAG；
14. 不影响项目档案 RAG；
15. 不影响八股训练；
16. 不影响能力画像。

---

## 60. 简历表达方向

该模块完成后，可以在简历中描述为：

```text
设计并实现用户文档上传与 RAG 接入模块，支持用户上传简历、README、项目文档和学习笔记，文件存储至阿里云 OSS，系统解析文本后进行切片、Embedding 和 Qdrant 向量索引，并在项目训练中结合用户私有文档进行上下文增强追问。
```

也可以进一步描述：

```text
模块通过 user_document 管理用户私有文件元数据，通过 rag_document / rag_chunk / rag_embedding 统一接入 RAG 索引体系，并基于 userId、projectId、sourceType 实现用户私有文档的权限隔离和检索过滤。
```

---

## 61. 总结

用户文档上传与 RAG 接入模块是 CodeCoach AI 继续增强 AI 产品属性的重要阶段。

它会让系统从：

```text
基于用户手填项目档案训练
```

升级为：

```text
基于用户真实简历、README、项目文档和学习笔记训练
```

第一版应聚焦：

```text
上传
解析
切片
索引
检索
项目训练增强
```

暂时不要做复杂文档管理、OCR、Office 解析、Agent 或简历优化。

当前阶段的核心目标是打通：

```text
用户私有文档
-> OSS
-> 文本解析
-> RAG 索引
-> 项目训练上下文增强
```