package com.codecoach.module.mockinterview.vo;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MockInterviewPlanVO {

    private String planId;

    private Integer totalRounds;

    private String coverageSummary;

    private List<StageVO> stages;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StageVO {

        private String stage;

        private String stageName;

        private String objective;

        private Integer suggestedRounds;

        private List<String> focusPoints;

        private List<String> ragSources;

        private List<String> scoringDimensions;
    }
}
