package com.codecoach.module.report.quality;

import java.util.List;

public class ReportQualityAssessment {

    private final int answerCount;

    private final int validAnswerCount;

    private final AnswerQualityLevel answerQuality;

    private final SampleSufficiency sampleSufficiency;

    private final int scoreCap;

    private final List<String> deductionReasons;

    private final List<String> nextActions;

    public ReportQualityAssessment(
            int answerCount,
            int validAnswerCount,
            AnswerQualityLevel answerQuality,
            SampleSufficiency sampleSufficiency,
            int scoreCap,
            List<String> deductionReasons,
            List<String> nextActions
    ) {
        this.answerCount = answerCount;
        this.validAnswerCount = validAnswerCount;
        this.answerQuality = answerQuality;
        this.sampleSufficiency = sampleSufficiency;
        this.scoreCap = scoreCap;
        this.deductionReasons = deductionReasons;
        this.nextActions = nextActions;
    }

    public int getAnswerCount() {
        return answerCount;
    }

    public int getValidAnswerCount() {
        return validAnswerCount;
    }

    public AnswerQualityLevel getAnswerQuality() {
        return answerQuality;
    }

    public SampleSufficiency getSampleSufficiency() {
        return sampleSufficiency;
    }

    public int getScoreCap() {
        return scoreCap;
    }

    public List<String> getDeductionReasons() {
        return deductionReasons;
    }

    public List<String> getNextActions() {
        return nextActions;
    }

    public boolean isLowConfidence() {
        return SampleSufficiency.INSUFFICIENT.equals(sampleSufficiency)
                || validAnswerCount == 0
                || answerQuality.ordinal() <= AnswerQualityLevel.VERY_WEAK.ordinal();
    }
}
