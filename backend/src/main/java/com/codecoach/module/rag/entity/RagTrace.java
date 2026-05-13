package com.codecoach.module.rag.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("rag_trace")
public class RagTrace {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("trace_id")
    private String traceId;

    @TableField("user_id")
    private Long userId;

    @TableField("query_text")
    private String query;

    @TableField("rewritten_query")
    private String rewrittenQuery;

    @TableField("source_types")
    private String sourceTypes;

    @TableField("top_k")
    private Integer topK;

    @TableField("hit_count")
    private Integer hitCount;

    @TableField("selected_chunk_ids")
    private String selectedChunkIds;

    @TableField("avg_score")
    private Double avgScore;

    @TableField("context_chars")
    private Integer contextChars;

    @TableField("success")
    private Integer success;

    @TableField("fallback_reason")
    private String fallbackReason;

    @TableField("latency_ms")
    private Long latencyMs;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
