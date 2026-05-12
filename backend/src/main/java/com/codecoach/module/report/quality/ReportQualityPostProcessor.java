package com.codecoach.module.report.quality;

import com.codecoach.module.ai.model.QuestionReportGenerateResult;
import com.codecoach.module.ai.vo.ReportGenerateResult;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ReportQualityPostProcessor {

    private final ReportQualityEvaluator reportQualityEvaluator;

    public ReportQualityPostProcessor(ReportQualityEvaluator reportQualityEvaluator) {
        this.reportQualityEvaluator = reportQualityEvaluator;
    }

    public ReportQualityAssessment processProjectReport(
            ReportGenerateResult result,
            List<String> userAnswers,
            int maxRound
    ) {
        ReportQualityAssessment assessment = reportQualityEvaluator.assess(userAnswers, maxRound, ReportTrainingType.PROJECT);
        if (result == null) {
            return assessment;
        }
        int finalScore = reportQualityEvaluator.applyScoreCap(result.getTotalScore(), assessment);
        result.setTotalScore(finalScore);
        if (assessment.isLowConfidence()) {
            result.setSummary(buildLowQualitySummary(assessment, ReportTrainingType.PROJECT));
            result.setStrengths(buildLowQualityStrengths(assessment));
        } else if (!StringUtils.hasText(result.getSummary())) {
            result.setSummary("本次训练已完成基础评估，分数基于实际回答、追问表现和项目表达可信度生成。");
        }
        result.setWeaknesses(mergeFront(assessment.getDeductionReasons(), result.getWeaknesses()));
        result.setSuggestions(mergeFront(assessment.getNextActions(), result.getSuggestions()));
        return assessment;
    }

    public ReportQualityAssessment processQuestionReport(
            QuestionReportGenerateResult result,
            List<String> userAnswers,
            int maxRound
    ) {
        ReportQualityAssessment assessment = reportQualityEvaluator.assess(userAnswers, maxRound, ReportTrainingType.QUESTION);
        if (result == null) {
            return assessment;
        }
        int finalScore = reportQualityEvaluator.applyScoreCap(result.getTotalScore(), assessment);
        result.setTotalScore(finalScore);
        if (assessment.isLowConfidence()) {
            result.setSummary(buildLowQualitySummary(assessment, ReportTrainingType.QUESTION));
            result.setStrengths(buildLowQualityStrengths(assessment));
        } else if (!StringUtils.hasText(result.getSummary())) {
            result.setSummary("本次训练已完成基础评估，分数基于实际回答、连续追问表现和表达结构生成。");
        }
        result.setWeaknesses(mergeFront(assessment.getDeductionReasons(), result.getWeaknesses()));
        result.setSuggestions(mergeFront(assessment.getNextActions(), result.getSuggestions()));
        if (assessment.isLowConfidence()) {
            result.setKnowledgeGaps(mergeFront(
                    List.of("基础概念未有效展示", "核心原理未展开", "场景适配能力无法判断"),
                    result.getKnowledgeGaps()
            ));
        }
        return assessment;
    }

    private String buildLowQualitySummary(ReportQualityAssessment assessment, ReportTrainingType trainingType) {
        if (assessment.getValidAnswerCount() == 0) {
            return ReportTrainingType.PROJECT.equals(trainingType)
                    ? "本次训练没有展示有效项目表达内容，系统无法判断你的项目背景、个人贡献和技术深度。最终分数已按低质量回答规则进行限制。"
                    : "本次训练没有展示有效知识点，系统无法判断你对该八股知识点的掌握情况。最终分数已按低质量回答规则进行限制。";
        }
        String prefix = ReportTrainingType.PROJECT.equals(trainingType)
                ? "本次项目训练样本有限，当前回答只能说明部分表达线索，尚不足以形成完整项目能力评估。"
                : "本次八股训练样本有限，当前回答只能说明部分关键词掌握情况，尚不足以形成完整知识能力评估。";
        return prefix + "系统已根据有效回答数、回答质量和训练完成度限制最终分数。";
    }

    private List<String> buildLowQualityStrengths(ReportQualityAssessment assessment) {
        if (assessment.getValidAnswerCount() == 0) {
            return List.of("本次暂无可确认的能力优势，建议先补齐基础内容后重新训练。");
        }
        return List.of("本次仅能确认你开始尝试回答，但证据不足，暂不沉淀为稳定能力优势。");
    }

    private List<String> mergeFront(List<String> priorityItems, List<String> originalItems) {
        List<String> merged = new ArrayList<>();
        if (priorityItems != null) {
            priorityItems.stream().filter(StringUtils::hasText).map(String::trim).forEach(merged::add);
        }
        if (originalItems != null) {
            originalItems.stream().filter(StringUtils::hasText).map(String::trim).forEach(merged::add);
        }
        return merged.stream().distinct().limit(8).toList();
    }
}
