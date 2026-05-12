package com.codecoach.module.report.quality;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ReportQualityEvaluator {

    private static final int DEFAULT_SCORE_CAP = 100;

    private static final Pattern NO_ANSWER_PATTERN = Pattern.compile(
            "^(我)?(真)?(不|没|沒有|没有)(太|怎么|咋|很)?(知道|清楚|会|會|懂|理解|了解|学过|學過|接触过|接觸過|看过|研究过|确定).*$"
                    + "|^(不会|不會|不知道|不清楚|不懂|没学过|沒有學過|没了解|不了解|不会答|不会回答|忘了|没印象|不太会|不太懂|不确定)$"
    );

    private static final Pattern INVALID_PATTERN = Pattern.compile("^[\\p{Punct}\\p{IsPunctuation}\\s\\d]+$");

    public ReportQualityAssessment assess(List<String> answers, int maxRound, ReportTrainingType trainingType) {
        List<String> safeAnswers = answers == null ? List.of() : answers.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .toList();
        int answerCount = safeAnswers.size();
        Map<AnswerQualityLevel, Integer> counts = new EnumMap<>(AnswerQualityLevel.class);
        for (AnswerQualityLevel level : AnswerQualityLevel.values()) {
            counts.put(level, 0);
        }

        List<AnswerQualityLevel> levels = safeAnswers.stream()
                .map(answer -> classifyAnswer(answer, trainingType))
                .peek(level -> counts.put(level, counts.get(level) + 1))
                .toList();

        int validAnswerCount = (int) levels.stream()
                .filter(level -> level.ordinal() >= AnswerQualityLevel.PARTIAL.ordinal())
                .count();
        AnswerQualityLevel overallQuality = resolveOverallQuality(levels);
        SampleSufficiency sampleSufficiency = resolveSampleSufficiency(answerCount, validAnswerCount);
        int scoreCap = resolveScoreCap(answerCount, validAnswerCount, maxRound, counts, overallQuality, sampleSufficiency);
        List<String> deductionReasons = buildDeductionReasons(
                answerCount,
                validAnswerCount,
                maxRound,
                counts,
                overallQuality,
                sampleSufficiency,
                trainingType
        );
        List<String> nextActions = buildNextActions(overallQuality, sampleSufficiency, trainingType);

        return new ReportQualityAssessment(
                answerCount,
                validAnswerCount,
                overallQuality,
                sampleSufficiency,
                scoreCap,
                deductionReasons,
                nextActions
        );
    }

    public AnswerQualityLevel classifyAnswer(String answer, ReportTrainingType trainingType) {
        String normalized = normalize(answer);
        if (!StringUtils.hasText(normalized)) {
            return AnswerQualityLevel.NO_ANSWER;
        }
        if (isNoAnswer(normalized)) {
            return AnswerQualityLevel.NO_ANSWER;
        }
        if (isInvalid(normalized)) {
            return AnswerQualityLevel.INVALID;
        }
        int length = normalized.length();
        if (isStackOnlyProjectAnswer(normalized, trainingType)) {
            return AnswerQualityLevel.VERY_WEAK;
        }
        if (length < 12) {
            return AnswerQualityLevel.VERY_WEAK;
        }
        if (length < 35 || countMeaningfulSeparators(normalized) == 0) {
            return AnswerQualityLevel.PARTIAL;
        }
        if (length < 90) {
            return AnswerQualityLevel.BASIC;
        }
        if (length < 180) {
            return AnswerQualityLevel.GOOD;
        }
        return AnswerQualityLevel.EXCELLENT;
    }

    public int applyScoreCap(Integer aiScore, ReportQualityAssessment assessment) {
        int safeScore = aiScore == null ? 0 : Math.max(0, Math.min(100, aiScore));
        if (assessment == null) {
            return safeScore;
        }
        return Math.min(safeScore, assessment.getScoreCap());
    }

    private boolean isNoAnswer(String normalized) {
        String compact = normalized.replaceAll("[，。,.！!？?、\\s]", "");
        return NO_ANSWER_PATTERN.matcher(compact).matches();
    }

    private boolean isInvalid(String normalized) {
        if (INVALID_PATTERN.matcher(normalized).matches()) {
            return true;
        }
        long meaningfulChars = normalized.chars()
                .filter(ch -> Character.isLetterOrDigit(ch) || Character.UnicodeScript.of(ch) == Character.UnicodeScript.HAN)
                .count();
        if (meaningfulChars < 2) {
            return true;
        }
        return normalized.length() >= 6 && normalized.chars().distinct().count() <= 2;
    }

    private boolean isStackOnlyProjectAnswer(String normalized, ReportTrainingType trainingType) {
        if (!ReportTrainingType.PROJECT.equals(trainingType)) {
            return false;
        }
        String lower = normalized.toLowerCase(Locale.ROOT);
        boolean hasStack = lower.contains("redis")
                || lower.contains("mysql")
                || lower.contains("spring")
                || lower.contains("boot")
                || lower.contains("mq")
                || lower.contains("java");
        boolean hasContribution = lower.contains("负责")
                || lower.contains("实现")
                || lower.contains("设计")
                || lower.contains("优化")
                || lower.contains("解决")
                || lower.contains("权衡")
                || lower.contains("因为")
                || lower.contains("效果");
        return hasStack && !hasContribution && normalized.length() <= 40;
    }

    private AnswerQualityLevel resolveOverallQuality(List<AnswerQualityLevel> levels) {
        if (levels == null || levels.isEmpty()) {
            return AnswerQualityLevel.NO_ANSWER;
        }
        return levels.stream()
                .sorted(Comparator.comparingInt(Enum::ordinal))
                .toList()
                .get(levels.size() / 2);
    }

    private SampleSufficiency resolveSampleSufficiency(int answerCount, int validAnswerCount) {
        if (answerCount < 2 || validAnswerCount == 0) {
            return SampleSufficiency.INSUFFICIENT;
        }
        if (answerCount < 4 || validAnswerCount < 3) {
            return SampleSufficiency.LIMITED;
        }
        return SampleSufficiency.ENOUGH;
    }

    private int resolveScoreCap(
            int answerCount,
            int validAnswerCount,
            int maxRound,
            Map<AnswerQualityLevel, Integer> counts,
            AnswerQualityLevel overallQuality,
            SampleSufficiency sampleSufficiency
    ) {
        int cap = DEFAULT_SCORE_CAP;
        if (answerCount == 0 || validAnswerCount == 0) {
            cap = Math.min(cap, 10);
        }
        if (dominates(counts, answerCount, AnswerQualityLevel.INVALID)) {
            cap = Math.min(cap, 10);
        }
        if (dominates(counts, answerCount, AnswerQualityLevel.NO_ANSWER)) {
            cap = Math.min(cap, 15);
        }
        if (dominates(counts, answerCount, AnswerQualityLevel.VERY_WEAK)) {
            cap = Math.min(cap, 25);
        }
        if (validAnswerCount < 2) {
            cap = Math.min(cap, 25);
        }
        if (answerCount < 2) {
            cap = Math.min(cap, 25);
        }
        if (SampleSufficiency.INSUFFICIENT.equals(sampleSufficiency)) {
            cap = Math.min(cap, 25);
        } else if (SampleSufficiency.LIMITED.equals(sampleSufficiency)) {
            cap = Math.min(cap, 60);
        }
        if (AnswerQualityLevel.VERY_WEAK.equals(overallQuality)) {
            cap = Math.min(cap, 25);
        }
        if (maxRound > 0) {
            double completion = (double) answerCount / (double) maxRound;
            if (completion < 0.4d) {
                cap = Math.min(cap, 30);
            } else if (completion < 0.6d) {
                cap = Math.min(cap, 45);
            }
        }
        return cap;
    }

    private boolean dominates(Map<AnswerQualityLevel, Integer> counts, int answerCount, AnswerQualityLevel level) {
        if (answerCount <= 0) {
            return AnswerQualityLevel.NO_ANSWER.equals(level);
        }
        return counts.getOrDefault(level, 0) * 2 >= answerCount;
    }

    private List<String> buildDeductionReasons(
            int answerCount,
            int validAnswerCount,
            int maxRound,
            Map<AnswerQualityLevel, Integer> counts,
            AnswerQualityLevel overallQuality,
            SampleSufficiency sampleSufficiency,
            ReportTrainingType trainingType
    ) {
        List<String> reasons = new ArrayList<>();
        if (answerCount == 0 || validAnswerCount == 0 || dominates(counts, answerCount, AnswerQualityLevel.NO_ANSWER)) {
            reasons.add("当前回答没有展示有效知识点，无法证明已掌握相关内容。");
        }
        if (dominates(counts, answerCount, AnswerQualityLevel.INVALID)) {
            reasons.add("主要回答与问题无关或缺少可评估内容。");
        }
        if (AnswerQualityLevel.VERY_WEAK.equals(overallQuality)) {
            reasons.add("回答内容过少，缺少概念解释、原因分析和具体例子。");
        }
        if (ReportTrainingType.PROJECT.equals(trainingType)
                && overallQuality.ordinal() <= AnswerQualityLevel.PARTIAL.ordinal()) {
            reasons.add("项目表达缺少个人贡献、技术细节和工程权衡，不能只罗列技术栈。");
        }
        if (ReportTrainingType.QUESTION.equals(trainingType)
                && overallQuality.ordinal() <= AnswerQualityLevel.PARTIAL.ordinal()) {
            reasons.add("八股回答只覆盖零散关键词，缺少核心原理、适用场景和追问展开。");
        }
        if (!SampleSufficiency.ENOUGH.equals(sampleSufficiency)) {
            reasons.add("本次训练样本不足，分数仅供参考。建议完成更多轮训练后再查看能力趋势。");
        }
        if (maxRound > 0 && answerCount < maxRound) {
            reasons.add("训练完成度较低，系统已对最终分数应用上限。");
        }
        return reasons.stream().distinct().toList();
    }

    private List<String> buildNextActions(
            AnswerQualityLevel overallQuality,
            SampleSufficiency sampleSufficiency,
            ReportTrainingType trainingType
    ) {
        List<String> actions = new ArrayList<>();
        if (overallQuality.ordinal() <= AnswerQualityLevel.NO_ANSWER.ordinal()) {
            actions.add("先学习基础概念，用自己的话写出定义、原因和典型场景后再训练。");
        } else if (overallQuality.ordinal() <= AnswerQualityLevel.PARTIAL.ordinal()) {
            actions.add("把回答补成“定义/背景 -> 原理或实现 -> 场景 -> 风险和取舍”的四段结构。");
        }
        if (ReportTrainingType.PROJECT.equals(trainingType)) {
            actions.add("补充一次真实项目案例：你负责什么、为什么这样设计、遇到什么问题、结果如何。");
            actions.add("重新训练时至少完成 3 轮追问，重点练个人贡献和异常场景。");
        } else {
            actions.add("整理一版标准答案，至少覆盖概念准确性、核心原理、方案完整度和常见追问。");
            actions.add("重新训练时至少完成 3 轮追问，重点练原理解释和场景适配。");
        }
        if (!SampleSufficiency.ENOUGH.equals(sampleSufficiency)) {
            actions.add("完成更多轮回答后再观察能力画像趋势。");
        }
        return actions.stream().distinct().limit(4).toList();
    }

    private int countMeaningfulSeparators(String normalized) {
        int count = 0;
        for (String marker : List.of("，", "。", "；", ",", ".", ";", "因为", "所以", "首先", "其次", "然后", "但是")) {
            if (normalized.contains(marker)) {
                count++;
            }
        }
        return count;
    }

    private String normalize(String answer) {
        if (answer == null) {
            return "";
        }
        return answer.trim().replaceAll("\\s+", " ");
    }
}
