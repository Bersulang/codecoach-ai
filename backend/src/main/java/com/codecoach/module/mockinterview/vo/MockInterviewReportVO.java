package com.codecoach.module.mockinterview.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MockInterviewReportVO {

    private Long reportId;

    private Long sessionId;

    private String interviewType;

    private String targetRole;

    private String difficulty;

    private Integer totalScore;

    private String sampleSufficiency;

    private String summary;

    private List<StagePerformanceVO> stagePerformances;

    private List<String> strengths;

    private List<String> weaknesses;

    private List<String> highRiskAnswers;

    private Integer followUpPressureScore;

    private Integer projectCredibilityScore;

    private Integer technicalFoundationScore;

    private List<String> nextActions;

    private List<String> recommendedLearning;

    private List<String> recommendedTraining;

    private List<String> weaknessTags;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StagePerformanceVO {

        private String stage;

        private String stageName;

        private Integer score;

        private String comment;
    }
}
