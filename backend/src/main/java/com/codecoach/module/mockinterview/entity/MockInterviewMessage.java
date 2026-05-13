package com.codecoach.module.mockinterview.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("mock_interview_message")
public class MockInterviewMessage {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("session_id")
    private Long sessionId;

    @TableField("user_id")
    private Long userId;

    @TableField("role")
    private String role;

    @TableField("message_type")
    private String messageType;

    @TableField("stage")
    private String stage;

    @TableField("content")
    private String content;

    @TableField("round_no")
    private Integer roundNo;

    @TableField("score")
    private Integer score;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
