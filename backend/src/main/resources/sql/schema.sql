CREATE TABLE user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    username VARCHAR(64) NOT NULL COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '加密后的密码',
    nickname VARCHAR(64) DEFAULT NULL COMMENT '用户昵称',
    avatar_url VARCHAR(512) DEFAULT NULL COMMENT '头像地址',
    role VARCHAR(32) NOT NULL DEFAULT 'USER' COMMENT '角色：USER/ADMIN',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1正常，0禁用',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_username (username),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

CREATE TABLE project (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '项目ID',
    user_id BIGINT NOT NULL COMMENT '所属用户ID',
    name VARCHAR(128) NOT NULL COMMENT '项目名称',
    description TEXT NOT NULL COMMENT '项目描述',
    tech_stack TEXT NOT NULL COMMENT '技术栈',
    role TEXT DEFAULT NULL COMMENT '负责模块',
    highlights TEXT DEFAULT NULL COMMENT '项目亮点',
    difficulties TEXT DEFAULT NULL COMMENT '项目难点',
    status VARCHAR(32) NOT NULL DEFAULT 'NORMAL' COMMENT '状态：NORMAL/DELETED',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted_at DATETIME DEFAULT NULL COMMENT '删除时间',
    KEY idx_user_id (user_id),
    KEY idx_user_deleted_created (user_id, is_deleted, created_at),
    KEY idx_user_name (user_id, name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目档案表';

CREATE TABLE interview_session (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '训练会话ID',
    user_id BIGINT NOT NULL COMMENT '所属用户ID',
    project_id BIGINT NOT NULL COMMENT '项目ID',
    target_role VARCHAR(128) NOT NULL COMMENT '目标岗位',
    difficulty VARCHAR(32) NOT NULL DEFAULT 'NORMAL' COMMENT '难度：EASY/NORMAL/HARD',
    status VARCHAR(32) NOT NULL DEFAULT 'IN_PROGRESS' COMMENT '状态：IN_PROGRESS/FINISHED/FAILED',
    current_round INT NOT NULL DEFAULT 0 COMMENT '当前轮次',
    max_round INT NOT NULL DEFAULT 5 COMMENT '最大轮次',
    started_at DATETIME DEFAULT NULL COMMENT '开始时间',
    ended_at DATETIME DEFAULT NULL COMMENT '结束时间',
    total_score INT DEFAULT NULL COMMENT '总评分',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_user_id (user_id),
    KEY idx_project_id (project_id),
    KEY idx_user_status_created (user_id, status, created_at),
    KEY idx_user_project_created (user_id, project_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='训练会话表';

CREATE TABLE interview_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '消息ID',
    session_id BIGINT NOT NULL COMMENT '训练会话ID',
    user_id BIGINT NOT NULL COMMENT '所属用户ID',
    role VARCHAR(32) NOT NULL COMMENT '消息角色：USER/ASSISTANT/SYSTEM',
    message_type VARCHAR(32) NOT NULL COMMENT '消息类型',
    content TEXT NOT NULL COMMENT '消息内容',
    round_no INT NOT NULL DEFAULT 1 COMMENT '所属轮次',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY idx_session_id (session_id),
    KEY idx_user_id (user_id),
    KEY idx_session_round_created (session_id, round_no, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='训练消息表';

CREATE TABLE interview_report (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '报告ID',
    session_id BIGINT NOT NULL COMMENT '训练会话ID',
    user_id BIGINT NOT NULL COMMENT '所属用户ID',
    project_id BIGINT NOT NULL COMMENT '项目ID',
    total_score INT NOT NULL COMMENT '总评分',
    summary TEXT NOT NULL COMMENT '总体评价',
    strengths JSON DEFAULT NULL COMMENT '优点列表',
    weaknesses JSON DEFAULT NULL COMMENT '薄弱点列表',
    suggestions JSON DEFAULT NULL COMMENT '改进建议列表',
    qa_review JSON DEFAULT NULL COMMENT '问答复盘',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_session_id (session_id),
    KEY idx_user_id (user_id),
    KEY idx_project_id (project_id),
    KEY idx_user_created (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='训练报告表';

CREATE TABLE ai_call_log (
                             id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '日志ID',

                             user_id BIGINT DEFAULT NULL COMMENT '用户ID',
                             project_id BIGINT DEFAULT NULL COMMENT '项目ID',
                             session_id BIGINT DEFAULT NULL COMMENT '训练会话ID',

                             provider VARCHAR(64) NOT NULL COMMENT '模型服务商',
                             model_name VARCHAR(128) NOT NULL COMMENT '模型名称',
                             request_type VARCHAR(64) NOT NULL COMMENT '调用类型',
                             prompt_version VARCHAR(64) DEFAULT NULL COMMENT 'Prompt版本',

                             prompt_tokens INT DEFAULT NULL COMMENT '输入Token数',
                             completion_tokens INT DEFAULT NULL COMMENT '输出Token数',
                             total_tokens INT DEFAULT NULL COMMENT '总Token数',

                             latency_ms BIGINT DEFAULT NULL COMMENT '调用耗时毫秒',
                             success TINYINT NOT NULL DEFAULT 1 COMMENT '是否成功：1成功，0失败',

                             status_code INT DEFAULT NULL COMMENT '模型接口HTTP状态码',
                             error_code VARCHAR(128) DEFAULT NULL COMMENT '模型接口错误码',
                             error_message TEXT DEFAULT NULL COMMENT '错误信息',
                             request_id VARCHAR(128) DEFAULT NULL COMMENT '模型服务请求ID',

                             raw_response MEDIUMTEXT DEFAULT NULL COMMENT '模型原始响应',
                             created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

                             KEY idx_user_id (user_id),
                             KEY idx_project_id (project_id),
                             KEY idx_session_id (session_id),
                             KEY idx_request_type (request_type),
                             KEY idx_success_created (success, created_at),
                             KEY idx_model_created (provider, model_name, created_at),
                             KEY idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI调用日志表';

CREATE TABLE knowledge_topic (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '知识点ID',
    category VARCHAR(64) NOT NULL COMMENT '知识分类，例如 Redis/JVM/MySQL',
    name VARCHAR(128) NOT NULL COMMENT '知识点名称',
    description TEXT DEFAULT NULL COMMENT '知识点描述',
    difficulty VARCHAR(32) NOT NULL DEFAULT 'NORMAL' COMMENT '默认难度：EASY/NORMAL/HARD',
    interview_focus TEXT DEFAULT NULL COMMENT '常见追问方向',
    tags VARCHAR(512) DEFAULT NULL COMMENT '标签，逗号分隔',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序值',
    status VARCHAR(32) NOT NULL DEFAULT 'ENABLED' COMMENT '状态：ENABLED/DISABLED',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted_at DATETIME DEFAULT NULL COMMENT '删除时间',
    UNIQUE KEY uk_category_name (category, name),
    KEY idx_category (category),
    KEY idx_status (status),
    KEY idx_sort_order (sort_order),
    KEY idx_category_status_deleted (category, status, is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识点目录表';

CREATE TABLE knowledge_article (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '文章ID',
    topic_id BIGINT NOT NULL COMMENT '知识点ID',
    title VARCHAR(128) NOT NULL COMMENT '文章标题',
    summary VARCHAR(512) DEFAULT NULL COMMENT '文章摘要',
    content_path VARCHAR(512) NOT NULL COMMENT 'Markdown正文路径',
    source_type VARCHAR(32) NOT NULL DEFAULT 'ORIGINAL' COMMENT '来源类型：ORIGINAL/AI_ASSISTED/EXTERNAL_REFERENCE',
    source_name VARCHAR(128) DEFAULT NULL COMMENT '来源名称',
    source_url VARCHAR(512) DEFAULT NULL COMMENT '参考链接',
    version VARCHAR(32) NOT NULL DEFAULT 'v1' COMMENT '内容版本',
    status VARCHAR(32) NOT NULL DEFAULT 'PUBLISHED' COMMENT '状态：DRAFT/PUBLISHED/DISABLED',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted_at DATETIME DEFAULT NULL COMMENT '删除时间',
    UNIQUE KEY uk_topic_title (topic_id, title),
    KEY idx_topic_id (topic_id),
    KEY idx_status (status),
    KEY idx_sort_order (sort_order),
    KEY idx_topic_status_deleted (topic_id, status, is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识文章表';

CREATE TABLE question_training_session (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '八股训练会话ID',
    user_id BIGINT NOT NULL COMMENT '所属用户ID',
    topic_id BIGINT NOT NULL COMMENT '知识点ID',
    target_role VARCHAR(128) NOT NULL COMMENT '目标岗位',
    difficulty VARCHAR(32) NOT NULL DEFAULT 'NORMAL' COMMENT '难度：EASY/NORMAL/HARD',
    status VARCHAR(32) NOT NULL DEFAULT 'IN_PROGRESS' COMMENT '状态：IN_PROGRESS/FINISHED/FAILED',
    current_round INT NOT NULL DEFAULT 1 COMMENT '当前轮次',
    max_round INT NOT NULL DEFAULT 5 COMMENT '最大轮次',
    started_at DATETIME DEFAULT NULL COMMENT '开始时间',
    ended_at DATETIME DEFAULT NULL COMMENT '结束时间',
    total_score INT DEFAULT NULL COMMENT '总评分',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted_at DATETIME DEFAULT NULL COMMENT '删除时间',
    KEY idx_user_id (user_id),
    KEY idx_topic_id (topic_id),
    KEY idx_status (status),
    KEY idx_user_status_created (user_id, status, created_at),
    KEY idx_user_topic_created (user_id, topic_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='八股问答训练会话表';

CREATE TABLE question_training_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '八股训练消息ID',
    session_id BIGINT NOT NULL COMMENT '八股训练会话ID',
    user_id BIGINT NOT NULL COMMENT '所属用户ID',
    role VARCHAR(32) NOT NULL COMMENT '消息角色：USER/ASSISTANT/SYSTEM',
    message_type VARCHAR(64) NOT NULL COMMENT '消息类型：AI_QUESTION/USER_ANSWER/AI_FEEDBACK/AI_REFERENCE_ANSWER/AI_FOLLOW_UP/SYSTEM_NOTICE',
    content TEXT NOT NULL COMMENT '消息内容',
    round_no INT NOT NULL DEFAULT 1 COMMENT '所属轮次',
    score INT DEFAULT NULL COMMENT '本轮得分',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY idx_session_id (session_id),
    KEY idx_user_id (user_id),
    KEY idx_session_round (session_id, round_no),
    KEY idx_session_created (session_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='八股问答训练消息表';

CREATE TABLE question_training_report (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '八股训练报告ID',
    session_id BIGINT NOT NULL COMMENT '八股训练会话ID',
    user_id BIGINT NOT NULL COMMENT '所属用户ID',
    topic_id BIGINT NOT NULL COMMENT '知识点ID',
    total_score INT DEFAULT NULL COMMENT '总评分',
    summary TEXT DEFAULT NULL COMMENT '总体评价',
    strengths JSON DEFAULT NULL COMMENT '优点列表',
    weaknesses JSON DEFAULT NULL COMMENT '薄弱点列表',
    suggestions JSON DEFAULT NULL COMMENT '改进建议列表',
    qa_review JSON DEFAULT NULL COMMENT '问答复盘',
    knowledge_gaps JSON DEFAULT NULL COMMENT '知识薄弱点',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_session_id (session_id),
    KEY idx_user_id (user_id),
    KEY idx_topic_id (topic_id),
    KEY idx_user_created (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='八股问答训练报告表';

CREATE TABLE user_ability_snapshot (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '能力快照ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    source_type VARCHAR(32) NOT NULL COMMENT '来源类型：PROJECT_REPORT / QUESTION_REPORT',
    source_id BIGINT NOT NULL COMMENT '来源报告ID',
    dimension_code VARCHAR(64) NOT NULL COMMENT '能力维度编码',
    dimension_name VARCHAR(128) NOT NULL COMMENT '能力维度名称',
    category VARCHAR(64) DEFAULT NULL COMMENT '知识分类或能力分类',
    score INT DEFAULT NULL COMMENT '能力分数',
    difficulty VARCHAR(32) DEFAULT NULL COMMENT '训练难度',
    evidence TEXT DEFAULT NULL COMMENT '证据摘要',
    weakness_tags JSON DEFAULT NULL COMMENT '薄弱点标签',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_source_dimension (source_type, source_id, dimension_code),
    KEY idx_user_id (user_id),
    KEY idx_source (source_type, source_id),
    KEY idx_dimension (dimension_code),
    KEY idx_user_dimension_created (user_id, dimension_code, created_at),
    KEY idx_user_created (user_id, created_at),
    KEY idx_user_source_created (user_id, source_type, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户能力快照表';

INSERT IGNORE INTO knowledge_topic
    (category, name, description, difficulty, interview_focus, tags, sort_order)
VALUES
    ('Java 基础', 'HashMap', 'HashMap 是 Java 常用哈希表集合，面试中重点考察底层结构、扩容机制和并发风险。', 'NORMAL', '常见追问包括数组链表红黑树结构、hash 扰动、扩容流程、线程不安全原因以及与 ConcurrentHashMap 的区别。', 'Java,集合,HashMap', 101),
    ('Java 基础', 'ArrayList', 'ArrayList 是基于动态数组实现的 List，适合考察集合基础和扩容原理。', 'EASY', '常见追问包括初始容量、扩容倍数、随机访问效率、删除插入复杂度以及与 LinkedList 的区别。', 'Java,集合,ArrayList', 102),
    ('Java 基础', '泛型', '泛型用于在编译期提供类型约束，减少类型转换和运行时类型错误。', 'NORMAL', '常见追问包括类型擦除、通配符、上下界、泛型方法以及泛型数组限制。', 'Java,泛型,类型擦除', 103),
    ('Java 基础', '反射', '反射允许程序在运行时获取类信息并动态访问字段、方法和构造器。', 'NORMAL', '常见追问包括反射使用场景、性能开销、破坏封装、注解解析以及框架中的应用。', 'Java,反射,框架', 104),
    ('Java 基础', '动态代理', '动态代理用于在运行期生成代理对象，是 AOP、RPC 等能力的重要基础。', 'HARD', '常见追问包括 JDK 动态代理、CGLIB、代理对象生成过程、方法拦截和 Spring AOP 选择策略。', 'Java,动态代理,AOP', 105),

    ('JVM', 'JVM 内存区域', 'JVM 内存区域描述 Java 程序运行时不同内存空间的职责和生命周期。', 'NORMAL', '常见追问包括堆、方法区、虚拟机栈、本地方法栈、程序计数器、线程私有与共享区域。', 'JVM,内存模型,运行时数据区', 201),
    ('JVM', '类加载机制', '类加载机制描述 class 文件从加载到可用的全过程。', 'NORMAL', '常见追问包括加载、验证、准备、解析、初始化阶段，以及主动引用和被动引用。', 'JVM,类加载,ClassLoader', 202),
    ('JVM', '双亲委派模型', '双亲委派模型是 Java 类加载器的委派加载机制，用于保证核心类库安全和类型一致性。', 'NORMAL', '常见追问包括委派流程、为什么需要双亲委派、如何打破双亲委派以及典型框架场景。', 'JVM,类加载,双亲委派', 203),
    ('JVM', '垃圾回收算法', '垃圾回收算法用于识别和回收不再使用的对象，影响应用吞吐和停顿时间。', 'NORMAL', '常见追问包括标记清除、复制、标记整理、分代收集、可达性分析和 GC Roots。', 'JVM,GC,垃圾回收', 204),
    ('JVM', 'G1 垃圾回收器', 'G1 是面向服务端应用的低停顿垃圾回收器，按 Region 管理堆内存。', 'HARD', '常见追问包括 Region、Remembered Set、Mixed GC、停顿预测、适用场景和调优参数。', 'JVM,G1,GC调优', 205),

    ('JUC', '线程池', '线程池用于复用线程、控制并发资源并管理异步任务执行。', 'NORMAL', '常见追问包括核心参数、任务提交流程、拒绝策略、队列选择、线程池隔离和生产参数配置。', 'JUC,线程池,并发', 301),
    ('JUC', 'synchronized', 'synchronized 是 Java 内置锁机制，用于保证临界区的互斥访问和可见性。', 'NORMAL', '常见追问包括锁对象、锁升级、偏向锁、轻量级锁、重量级锁以及 wait/notify。', 'JUC,锁,synchronized', 302),
    ('JUC', 'volatile', 'volatile 用于保证变量可见性和禁止指令重排，但不保证复合操作原子性。', 'NORMAL', '常见追问包括 JMM、内存屏障、可见性、有序性、单例双重检查和不适用场景。', 'JUC,volatile,JMM', 303),
    ('JUC', 'CAS', 'CAS 是无锁并发中的比较并交换操作，是很多原子类的底层基础。', 'HARD', '常见追问包括 ABA 问题、自旋开销、Unsafe、Atomic 类和高并发下的适用边界。', 'JUC,CAS,无锁', 304),
    ('JUC', 'AQS', 'AQS 是构建锁和同步器的基础框架，维护同步状态和等待队列。', 'HARD', '常见追问包括 state、CLH 队列、独占共享模式、ReentrantLock、Semaphore 和 CountDownLatch 实现思路。', 'JUC,AQS,同步器', 305),

    ('MySQL', '索引', '索引用于提升查询效率，是 MySQL 面试中最核心的知识点之一。', 'NORMAL', '常见追问包括索引类型、联合索引、最左前缀、索引失效、覆盖索引和回表。', 'MySQL,索引,查询优化', 401),
    ('MySQL', 'B+ 树', 'B+ 树是 InnoDB 索引的核心数据结构，适合范围查询和磁盘页访问。', 'NORMAL', '常见追问包括 B+ 树与 B 树区别、叶子节点链表、树高、页分裂和范围查询效率。', 'MySQL,B+树,InnoDB', 402),
    ('MySQL', '事务', '事务保证一组数据库操作满足 ACID 特性，是业务一致性的基础。', 'NORMAL', '常见追问包括 ACID、隔离级别、脏读、不可重复读、幻读和事务失效场景。', 'MySQL,事务,ACID', 403),
    ('MySQL', 'MVCC', 'MVCC 通过多版本机制提升并发读写能力，是 InnoDB 事务隔离的重要实现。', 'HARD', '常见追问包括 ReadView、undo log、版本链、快照读、当前读和不同隔离级别差异。', 'MySQL,MVCC,事务隔离', 404),
    ('MySQL', '锁机制', 'MySQL 锁机制用于控制并发访问，影响事务隔离和性能。', 'HARD', '常见追问包括行锁、表锁、间隙锁、临键锁、意向锁、死锁排查和加锁规则。', 'MySQL,锁,InnoDB', 405),

    ('Redis', '缓存穿透', '缓存穿透指请求查询不存在的数据，导致请求绕过缓存持续打到数据库。', 'NORMAL', '常见追问包括空值缓存、布隆过滤器、参数校验、恶意请求防护和误判问题。', 'Redis,缓存,缓存穿透', 501),
    ('Redis', '缓存击穿', '缓存击穿指热点 key 失效瞬间大量请求打到数据库。', 'NORMAL', '常见追问包括互斥锁、逻辑过期、热点 key 续期、锁粒度和高并发下锁竞争。', 'Redis,缓存,缓存击穿', 502),
    ('Redis', '缓存雪崩', '缓存雪崩指大量缓存同时失效或 Redis 故障导致数据库压力骤增。', 'NORMAL', '常见追问包括过期时间随机化、多级缓存、限流降级、预热和 Redis 高可用。', 'Redis,缓存,缓存雪崩', 503),
    ('Redis', '缓存一致性', '缓存一致性关注数据库和缓存之间数据更新顺序与最终一致。', 'HARD', '常见追问包括先删缓存还是先更新数据库、延迟双删、消息队列、binlog 订阅和失败补偿。', 'Redis,缓存一致性,数据一致性', 504),
    ('Redis', '分布式锁', 'Redis 分布式锁用于跨进程协调共享资源访问。', 'HARD', '常见追问包括 SET NX EX、锁续期、误删锁、Lua 脚本、Redisson、主从切换和 RedLock。', 'Redis,分布式锁,并发控制', 505),

    ('Spring', 'IOC', 'IOC 通过容器管理对象创建和依赖关系，是 Spring 框架的核心思想。', 'NORMAL', '常见追问包括依赖注入、BeanFactory、ApplicationContext、BeanDefinition 和循环依赖。', 'Spring,IOC,容器', 601),
    ('Spring', 'AOP', 'AOP 用于将日志、事务、权限等横切逻辑从业务代码中解耦。', 'NORMAL', '常见追问包括切点、通知、代理实现、JDK/CGLIB、调用失效和应用场景。', 'Spring,AOP,代理', 602),
    ('Spring', 'Bean 生命周期', 'Bean 生命周期描述 Spring Bean 从实例化到销毁的完整过程。', 'NORMAL', '常见追问包括实例化、属性填充、初始化、后置处理器、Aware 接口和销毁回调。', 'Spring,Bean生命周期,容器', 603),
    ('Spring', '事务传播', '事务传播定义方法嵌套调用时事务如何复用、挂起或新建。', 'HARD', '常见追问包括 REQUIRED、REQUIRES_NEW、NESTED、事务失效、异常回滚和代理调用边界。', 'Spring,事务,事务传播', 604),
    ('Spring', 'Spring Boot 自动配置', 'Spring Boot 自动配置根据 classpath 和条件注解自动装配常用组件。', 'NORMAL', '常见追问包括 starter、自动配置类、条件注解、配置优先级和自定义 starter。', 'Spring Boot,自动配置,starter', 605),

    ('MQ', '消息可靠性', '消息可靠性关注消息从生产到消费全过程不丢失。', 'HARD', '常见追问包括生产确认、持久化、消费确认、重试、补偿、事务消息和消息堆积。', 'MQ,可靠性,消息队列', 701),
    ('MQ', '重复消费', '重复消费是消息系统常见问题，通常需要业务幂等保障。', 'NORMAL', '常见追问包括幂等键、去重表、唯一索引、状态机、Redis 去重和失败重试。', 'MQ,重复消费,幂等', 702),
    ('MQ', '顺序消息', '顺序消息用于保证同一业务维度的消息按顺序处理。', 'NORMAL', '常见追问包括局部顺序、分区路由、单队列消费、并发限制和乱序补偿。', 'MQ,顺序消息,消息队列', 703),
    ('MQ', '死信队列', '死信队列用于承接无法正常消费的异常消息，便于排查和补偿。', 'NORMAL', '常见追问包括死信产生原因、重试次数、告警、人工补偿和隔离策略。', 'MQ,死信队列,异常处理', 704),
    ('MQ', '削峰填谷', '削峰填谷通过异步队列缓冲突发流量，保护下游系统。', 'NORMAL', '常见追问包括异步化、消费速率、积压处理、限流降级和最终一致性。', 'MQ,削峰填谷,异步', 705),

    ('计算机网络', 'TCP 三次握手', 'TCP 三次握手用于建立可靠连接并同步双方初始序列号。', 'EASY', '常见追问包括三次握手流程、为什么不是两次、SYN 队列、半连接和 SYN Flood。', '网络,TCP,三次握手', 801),
    ('计算机网络', 'TCP 四次挥手', 'TCP 四次挥手用于可靠释放连接，处理双方独立关闭。', 'EASY', '常见追问包括挥手流程、TIME_WAIT、CLOSE_WAIT、为什么需要四次和端口占用问题。', '网络,TCP,四次挥手', 802),
    ('计算机网络', 'HTTP 与 HTTPS', 'HTTP 与 HTTPS 是 Web 通信基础，区别主要体现在安全传输和证书校验。', 'NORMAL', '常见追问包括 TLS 握手、对称加密、非对称加密、证书链、性能开销和 HTTP/2。', '网络,HTTP,HTTPS', 803),
    ('计算机网络', 'Cookie Session Token', 'Cookie、Session、Token 是 Web 登录态和认证授权中的常见机制。', 'NORMAL', '常见追问包括状态保存位置、跨域、安全属性、JWT、Session 共享和刷新机制。', '网络,认证,Cookie,Session,Token', 804),

    ('操作系统', '进程与线程', '进程是资源分配单位，线程是 CPU 调度单位，是并发编程的基础概念。', 'EASY', '常见追问包括资源隔离、线程共享内容、上下文切换、协程和多进程多线程适用场景。', '操作系统,进程,线程', 901),
    ('操作系统', '上下文切换', '上下文切换指 CPU 在不同执行单元之间保存和恢复运行状态。', 'NORMAL', '常见追问包括切换成本、触发原因、线程数过多影响、减少切换的工程手段。', '操作系统,上下文切换,性能', 902),
    ('操作系统', '虚拟内存', '虚拟内存为进程提供独立连续的地址空间，并通过页表映射到物理内存。', 'NORMAL', '常见追问包括分页、页表、缺页中断、交换空间、内存映射和局部性原理。', '操作系统,虚拟内存,内存管理', 903),
    ('操作系统', 'IO 多路复用', 'IO 多路复用允许单线程监听多个 IO 事件，是高并发网络编程的重要基础。', 'HARD', '常见追问包括 select、poll、epoll、水平触发、边缘触发、Reactor 模型和 Netty。', '操作系统,IO多路复用,网络编程', 904),

    ('分布式', '分布式事务', '分布式事务用于处理跨服务或跨资源的数据一致性问题。', 'HARD', '常见追问包括 2PC、TCC、Saga、本地消息表、事务消息、最终一致性和补偿机制。', '分布式,事务,一致性', 1001),
    ('分布式', 'CAP 和 BASE', 'CAP 和 BASE 描述分布式系统在一致性、可用性和分区容错之间的权衡。', 'NORMAL', '常见追问包括 CAP 三要素、取舍案例、BASE 思想、强一致和最终一致。', '分布式,CAP,BASE', 1002),
    ('分布式', '幂等性', '幂等性用于保证重复请求或重复消息不会造成重复业务效果。', 'NORMAL', '常见追问包括唯一请求号、去重表、唯一索引、状态机、Token 机制和接口重试。', '分布式,幂等,重复请求', 1003),
    ('分布式', '限流降级', '限流降级用于在高流量或故障场景下保护系统核心链路。', 'HARD', '常见追问包括令牌桶、漏桶、滑动窗口、熔断、降级策略、热点参数限流和灰度控制。', '分布式,限流,降级', 1004),
    ('分布式', '分布式 ID', '分布式 ID 用于在多节点环境下生成全局唯一标识。', 'NORMAL', '常见追问包括雪花算法、号段模式、数据库自增、时钟回拨、趋势递增和业务可读性。', '分布式,ID,雪花算法', 1005);

INSERT IGNORE INTO knowledge_article
    (topic_id, title, summary, content_path, source_type, version, status, sort_order)
SELECT
    kt.id,
    'Redis 缓存击穿面试表达指南',
    '围绕缓存击穿的定义、解决方案、常见追问和项目表达进行面试导向整理。',
    'knowledge/redis/cache-breakdown.md',
    'AI_ASSISTED',
    'v1',
    'PUBLISHED',
    1
FROM knowledge_topic kt
WHERE kt.category = 'Redis'
  AND kt.name = '缓存击穿'
  AND kt.is_deleted = 0
LIMIT 1;

INSERT IGNORE INTO knowledge_article
    (topic_id, title, summary, content_path, source_type, version, status, sort_order)
SELECT
    kt.id,
    'Redis 缓存穿透面试表达指南',
    '围绕缓存穿透的定义、空值缓存、布隆过滤器和恶意请求防护进行面试导向整理。',
    'knowledge/redis/cache-penetration.md',
    'AI_ASSISTED',
    'v1',
    'PUBLISHED',
    2
FROM knowledge_topic kt
WHERE kt.category = 'Redis'
  AND kt.name = '缓存穿透'
  AND kt.is_deleted = 0
LIMIT 1;

INSERT IGNORE INTO knowledge_article
    (topic_id, title, summary, content_path, source_type, version, status, sort_order)
SELECT
    kt.id,
    'Redis 分布式锁面试表达指南',
    '围绕 Redis 分布式锁的加锁释放、续期、误删锁和异常场景进行面试导向整理。',
    'knowledge/redis/distributed-lock.md',
    'AI_ASSISTED',
    'v1',
    'PUBLISHED',
    3
FROM knowledge_topic kt
WHERE kt.category = 'Redis'
  AND kt.name = '分布式锁'
  AND kt.is_deleted = 0
LIMIT 1;

INSERT IGNORE INTO knowledge_article
    (topic_id, title, summary, content_path, source_type, version, status, sort_order)
SELECT
    kt.id,
    'MySQL 索引面试表达指南',
    '围绕 MySQL 索引类型、联合索引、回表、覆盖索引和索引失效进行面试导向整理。',
    'knowledge/mysql/index.md',
    'AI_ASSISTED',
    'v1',
    'PUBLISHED',
    4
FROM knowledge_topic kt
WHERE kt.category = 'MySQL'
  AND kt.name = '索引'
  AND kt.is_deleted = 0
LIMIT 1;

INSERT IGNORE INTO knowledge_article
    (topic_id, title, summary, content_path, source_type, version, status, sort_order)
SELECT
    kt.id,
    'MySQL MVCC 面试表达指南',
    '围绕 MVCC 的版本链、ReadView、快照读、当前读和隔离级别差异进行面试导向整理。',
    'knowledge/mysql/mvcc.md',
    'AI_ASSISTED',
    'v1',
    'PUBLISHED',
    5
FROM knowledge_topic kt
WHERE kt.category = 'MySQL'
  AND kt.name = 'MVCC'
  AND kt.is_deleted = 0
LIMIT 1;

INSERT IGNORE INTO knowledge_article
    (topic_id, title, summary, content_path, source_type, version, status, sort_order)
SELECT
    kt.id,
    'JVM 内存区域面试表达指南',
    '围绕 JVM 运行时数据区、线程私有与共享区域、堆栈关系和常见追问进行面试导向整理。',
    'knowledge/jvm/memory-area.md',
    'AI_ASSISTED',
    'v1',
    'PUBLISHED',
    6
FROM knowledge_topic kt
WHERE kt.category = 'JVM'
  AND kt.name = 'JVM 内存区域'
  AND kt.is_deleted = 0
LIMIT 1;

INSERT IGNORE INTO knowledge_article
    (topic_id, title, summary, content_path, source_type, version, status, sort_order)
SELECT
    kt.id,
    'JVM 垃圾回收面试表达指南',
    '围绕垃圾回收算法、可达性分析、分代收集和常见 GC 追问进行面试导向整理。',
    'knowledge/jvm/gc.md',
    'AI_ASSISTED',
    'v1',
    'PUBLISHED',
    7
FROM knowledge_topic kt
WHERE kt.category = 'JVM'
  AND kt.name = '垃圾回收算法'
  AND kt.is_deleted = 0
LIMIT 1;

INSERT IGNORE INTO knowledge_article
    (topic_id, title, summary, content_path, source_type, version, status, sort_order)
SELECT
    kt.id,
    'JUC 线程池面试表达指南',
    '围绕线程池核心参数、任务提交流程、拒绝策略、队列选择和生产配置进行面试导向整理。',
    'knowledge/juc/thread-pool.md',
    'AI_ASSISTED',
    'v1',
    'PUBLISHED',
    8
FROM knowledge_topic kt
WHERE kt.category = 'JUC'
  AND kt.name = '线程池'
  AND kt.is_deleted = 0
LIMIT 1;

INSERT IGNORE INTO knowledge_article
    (topic_id, title, summary, content_path, source_type, version, status, sort_order)
SELECT
    kt.id,
    'Spring AOP 面试表达指南',
    '围绕 Spring AOP 的代理机制、切面模型、调用失效和典型业务场景进行面试导向整理。',
    'knowledge/spring/aop.md',
    'AI_ASSISTED',
    'v1',
    'PUBLISHED',
    9
FROM knowledge_topic kt
WHERE kt.category = 'Spring'
  AND kt.name = 'AOP'
  AND kt.is_deleted = 0
LIMIT 1;

INSERT IGNORE INTO knowledge_article
    (topic_id, title, summary, content_path, source_type, version, status, sort_order)
SELECT
    kt.id,
    'Spring 事务传播面试表达指南',
    '围绕 Spring 事务传播行为、事务失效、异常回滚和代理边界进行面试导向整理。',
    'knowledge/spring/transaction-propagation.md',
    'AI_ASSISTED',
    'v1',
    'PUBLISHED',
    10
FROM knowledge_topic kt
WHERE kt.category = 'Spring'
  AND kt.name = '事务传播'
  AND kt.is_deleted = 0
LIMIT 1;
