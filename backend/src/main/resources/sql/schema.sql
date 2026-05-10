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