package com.codecoach.module.mockinterview.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("mock_interview_report")
public class MockInterviewReport {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("session_id")
    private Long sessionId;

    @TableField("user_id")
    private Long userId;

    @TableField("project_id")
    private Long projectId;

    @TableField("resume_id")
    private Long resumeId;

    @TableField("total_score")
    private Integer totalScore;

    @TableField("sample_sufficiency")
    private String sampleSufficiency;

    @TableField("summary")
    private String summary;

    @TableField("stage_performances")
    private String stagePerformances;

    @TableField("strengths")
    private String strengths;

    @TableField("weaknesses")
    private String weaknesses;

    @TableField("high_risk_answers")
    private String highRiskAnswers;

    @TableField("next_actions")
    private String nextActions;

    @TableField("recommended_learning")
    private String recommendedLearning;

    @TableField("recommended_training")
    private String recommendedTraining;

    @TableField("weakness_tags")
    private String weaknessTags;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
