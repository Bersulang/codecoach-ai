package com.codecoach.module.mockinterview.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("mock_interview_session")
public class MockInterviewSession {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("interview_type")
    private String interviewType;

    @TableField("target_role")
    private String targetRole;

    @TableField("difficulty")
    private String difficulty;

    @TableField("project_id")
    private Long projectId;

    @TableField("resume_id")
    private Long resumeId;

    @TableField("status")
    private String status;

    @TableField("current_round")
    private Integer currentRound;

    @TableField("max_round")
    private Integer maxRound;

    @TableField("plan_id")
    private String planId;

    @TableField("plan_json")
    private String planJson;

    @TableField("current_stage")
    private String currentStage;

    @TableField("started_at")
    private LocalDateTime startedAt;

    @TableField("ended_at")
    private LocalDateTime endedAt;

    @TableField("total_score")
    private Integer totalScore;

    @TableField("is_deleted")
    private Integer isDeleted;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
