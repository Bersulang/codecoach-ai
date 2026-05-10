package com.codecoach.module.ai.support;

import org.springframework.util.StringUtils;

public class InterviewDifficultyStrategy {

    private static final String EASY = "EASY";

    private static final String HARD = "HARD";

    private final String difficultyName;

    private final String interviewerStyle;

    private final String questionDepth;

    private final String focusAreas;

    private final String feedbackStyle;

    private final String scoringPolicy;

    private InterviewDifficultyStrategy(
            String difficultyName,
            String interviewerStyle,
            String questionDepth,
            String focusAreas,
            String feedbackStyle,
            String scoringPolicy
    ) {
        this.difficultyName = difficultyName;
        this.interviewerStyle = interviewerStyle;
        this.questionDepth = questionDepth;
        this.focusAreas = focusAreas;
        this.feedbackStyle = feedbackStyle;
        this.scoringPolicy = scoringPolicy;
    }

    public static InterviewDifficultyStrategy from(String difficulty) {
        String normalized = StringUtils.hasText(difficulty) ? difficulty.trim().toUpperCase() : "";
        return switch (normalized) {
            case EASY -> easy();
            case HARD -> hard();
            default -> normal();
        };
    }

    private static InterviewDifficultyStrategy easy() {
        return new InterviewDifficultyStrategy(
                "EASY - 入门引导",
                "偏引导，适合实习初学者，语气专业但不过度压迫。",
                "优先确认项目背景、核心业务流程、本人负责模块，以及基础技术为什么这样用。",
                "业务理解、项目参与度、基础技术概念、接口和数据流转的基本表达。",
                "指出问题时更鼓励，先肯定清楚的部分，再提示需要补充的业务链路或基础概念。",
                "评分更关注表达清晰度、项目参与真实性和基础理解，不因缺少高阶架构细节过度扣分。"
        );
    }

    private static InterviewDifficultyStrategy normal() {
        return new InterviewDifficultyStrategy(
                "NORMAL - 常规面试",
                "常规 Java 后端实习 / 校招面试风格，客观、专业、有一定追问压力。",
                "在项目流程基础上继续追问技术细节、接口设计、数据库设计、缓存、异常场景和实现思路。",
                "技术选型、数据一致性、性能、代码实现思路、常见异常处理和工程落地能力。",
                "客观指出优缺点，既评价表达完整度，也指出技术细节和场景意识上的不足。",
                "综合评估表达清晰度、技术深度、场景意识、问题分析能力和工程实现可信度。"
        );
    }

    private static InterviewDifficultyStrategy hard() {
        return new InterviewDifficultyStrategy(
                "HARD - 深挖压测",
                "偏大厂深挖，严厉但专业，持续追问边界、瓶颈和取舍。",
                "重点深挖高并发、分布式一致性、故障恢复、性能瓶颈、架构权衡和线上治理。",
                "极端场景、边界条件、压测、降级限流、幂等、数据一致性、容量规划和故障恢复。",
                "反馈更严格，明确指出回答漏洞、缺失前提、风险点和没有量化说明的地方。",
                "评分更看重技术深度、系统性、工程权衡和线上问题处理能力；如果只停留在表层描述，不能给高分。"
        );
    }

    public String getDifficultyName() {
        return difficultyName;
    }

    public String getInterviewerStyle() {
        return interviewerStyle;
    }

    public String getQuestionDepth() {
        return questionDepth;
    }

    public String getFocusAreas() {
        return focusAreas;
    }

    public String getFeedbackStyle() {
        return feedbackStyle;
    }

    public String getScoringPolicy() {
        return scoringPolicy;
    }
}
