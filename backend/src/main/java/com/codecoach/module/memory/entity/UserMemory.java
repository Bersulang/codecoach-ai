package com.codecoach.module.memory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("user_memory")
public class UserMemory {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("memory_type")
    private String memoryType;

    @TableField("memory_key")
    private String memoryKey;

    @TableField("memory_value")
    private String memoryValue;

    @TableField("source_type")
    private String sourceType;

    @TableField("source_id")
    private Long sourceId;

    @TableField("confidence")
    private String confidence;

    @TableField("weight")
    private Integer weight;

    @TableField("status")
    private String status;

    @TableField("source_count")
    private Integer sourceCount;

    @TableField("source_summary")
    private String sourceSummary;

    @TableField("last_reinforced_at")
    private LocalDateTime lastReinforcedAt;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
