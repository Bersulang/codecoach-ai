package com.codecoach.module.agent.service.impl;

import com.codecoach.module.agent.model.AgentReviewContext;
import com.codecoach.module.agent.model.AgentReviewResult;
import com.codecoach.module.agent.service.AiAgentReviewService;
import java.util.Map;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@Profile("mock")
public class MockAiAgentReviewServiceImpl implements AiAgentReviewService {

    @Override
    public AgentReviewResult generateReview(AgentReviewContext context) {
        AgentReviewResult result = new AgentReviewResult();
        boolean hasData = context != null && (
                hasUsefulJson(context.getProjectReportsJson())
                        || hasUsefulJson(context.getQuestionReportsJson())
                        || hasUsefulJson(context.getAbilitySnapshotsJson())
        );
        result.setSummary(hasData
                ? "最近训练已经暴露出一些可复盘的问题：需要把八股概念、项目表达和简历风险点串起来，优先补齐反复出现的薄弱维度。"
                : "当前训练数据较少，建议先完成一次项目拷打和一次八股训练后再生成更可靠的复盘。");
        AgentReviewResult.ScoreOverview score = new AgentReviewResult.ScoreOverview();
        score.setScore(hasData ? 68 : null);
        score.setLevel(hasData ? "LIMITED" : "LOW_DATA");
        score.setExplanation(hasData ? "基于最近训练报告、能力画像和长期记忆的保守估计。" : "样本不足，暂不生成综合分数。");
        result.setScoreOverview(score);
        result.setRadarDimensions(hasData
                ? List.of(radar("技术基础", 68, "八股训练仍有知识链路缺口"),
                radar("项目表达", 62, "项目回答偏技术栈罗列"),
                radar("简历可信度", 70, "需要用项目训练验证简历风险"),
                radar("工程思维", 66, "异常处理和指标化表达不足"),
                radar("追问应对", 64, "连续追问下结构容易松散"),
                radar("表达结构", 72, "能回答但框架感不稳定"))
                : List.of());
        result.setKeyFindings(hasData
                ? List.of("近期薄弱点主要集中在知识点展开和项目表达证据不足。", "下一步应把学习材料和专项训练结合起来，而不是只看报告。")
                : List.of("训练报告和能力画像样本不足，暂时无法形成稳定模式判断。"));
        result.setRecurringWeaknesses(hasData
                ? List.of("回答结构不完整", "项目技术细节和工程权衡证据不足")
                : List.of("暂无足够训练数据识别反复薄弱点"));
        result.setHighRiskAnswers(hasData ? List.of(highRisk()) : List.of());
        result.setStagePerformance(hasData ? List.of(stage("TECHNICAL_FUNDAMENTAL", "技术基础", 68), stage("PROJECT_DEEP_DIVE", "项目深挖", 62)) : List.of());
        result.setQaReplay(hasData ? List.of(replay()) : List.of());
        result.setCauseAnalysis(hasData
                ? List.of("八股知识点缺少定义、原理、场景和取舍的完整链路。", "项目回答容易停留在技术栈罗列，缺少个人贡献和结果证明。")
                : List.of("系统缺少最近训练报告、能力快照或简历分析，因此只能给出低置信度建议。"));
        result.setResumeRisks(hasUsefulJson(context == null ? null : context.getResumeRisksJson())
                ? List.of("简历风险点需要通过项目拷打训练验证，优先覆盖高风险项目。")
                : List.of("上传并分析简历后，复盘会结合简历风险点。"));
        result.setNextActions(List.of(
                action("LEARN", "先补齐一个高频薄弱知识点", "复盘需要先把反复出现的知识短板补成标准表达。", 1, "/learn"),
                action("TRAIN_QUESTION", "完成一次八股专项训练", "用连续追问验证概念、原理和场景表达是否补齐。", 2, "/questions"),
                action("TRAIN_PROJECT", "完成一次项目拷打训练", "把技术点放回个人贡献、工程权衡和异常场景里表达。", 3, "/projects")
        ));
        result.setRecommendedArticles(List.of(recommend("知识学习：补齐高频薄弱点", "与近期薄弱点最相关。", "/learn")));
        result.setRecommendedTrainings(List.of(recommend("项目拷打训练", "验证项目细节和简历可信度。", "/projects"), recommend("真实模拟面试", "验证完整面试节奏。", "/mock-interviews")));
        result.setMemoryUpdates(hasData ? List.of("强化弱点：回答结构不完整", "强化项目风险：项目技术细节和工程权衡证据不足") : List.of());
        result.setConfidence(hasData ? "MEDIUM" : "LOW");
        result.setSampleQuality(hasData ? "LIMITED" : "INSUFFICIENT");
        return result;
    }

    private AgentReviewResult.NextAction action(String type, String title, String reason, int priority, String targetPath) {
        AgentReviewResult.NextAction action = new AgentReviewResult.NextAction();
        action.setType(type);
        action.setTitle(title);
        action.setReason(reason);
        action.setPriority(priority);
        action.setTargetPath(targetPath);
        action.setToolName(switch (type) {
            case "LEARN" -> "SEARCH_KNOWLEDGE";
            case "TRAIN_PROJECT" -> "START_PROJECT_TRAINING";
            default -> "START_QUESTION_TRAINING";
        });
        return action;
    }

    private AgentReviewResult.RadarDimension radar(String name, int score, String evidence) {
        AgentReviewResult.RadarDimension item = new AgentReviewResult.RadarDimension();
        item.setName(name);
        item.setScore(score);
        item.setEvidence(evidence);
        return item;
    }

    private AgentReviewResult.HighRiskAnswer highRisk() {
        AgentReviewResult.HighRiskAnswer item = new AgentReviewResult.HighRiskAnswer();
        item.setQuestion("请结合项目说明 Redis 缓存一致性怎么保证？");
        item.setAnswerSummary("回答提到 Redis 和 MySQL，但缺少更新策略、失败补偿和指标。");
        item.setRiskType("技术关键词堆叠但无细节");
        item.setRiskLevel("HIGH");
        item.setReason("简历或项目中出现缓存能力时，面试官通常会追问一致性、异常和监控。");
        item.setBetterDirection("按业务场景、读写链路、失败补偿、监控指标四段表达。");
        item.setRelatedAction(action("TRAIN_PROJECT", "专项练一次缓存项目表达", "把缓存方案讲清楚到异常处理和指标。", 1, "/projects"));
        return item;
    }

    private AgentReviewResult.StagePerformance stage(String stage, String name, int score) {
        AgentReviewResult.StagePerformance item = new AgentReviewResult.StagePerformance();
        item.setStage(stage);
        item.setStageName(name);
        item.setScore(score);
        item.setComment(score < 65 ? "需要优先补齐证据和表达结构。" : "有基础，但稳定性不足。");
        item.setWeaknessTags(List.of("表达结构", "工程细节"));
        return item;
    }

    private AgentReviewResult.QaReplayItem replay() {
        AgentReviewResult.QaReplayItem item = new AgentReviewResult.QaReplayItem();
        item.setSourceType("MOCK_INTERVIEW");
        item.setQuestion("项目里你如何处理缓存异常？");
        item.setAnswerSummary("能说明用了缓存，但缺少降级、重试和监控。");
        item.setAiFollowUp("如果 Redis 不可用，接口如何保证可用性？");
        item.setQuality("LOW");
        item.setMainProblems(List.of("异常处理不足", "没有指标"));
        item.setSuggestedExpression("先说核心链路，再补充失败场景、降级策略和监控指标。");
        return item;
    }

    private AgentReviewResult.Recommendation recommend(String title, String reason, String path) {
        AgentReviewResult.Recommendation item = new AgentReviewResult.Recommendation();
        item.setTitle(title);
        item.setReason(reason);
        item.setTargetPath(path);
        item.setSourceType("TRAINING");
        item.setMetadata(Map.of());
        return item;
    }

    private boolean hasUsefulJson(String json) {
        return StringUtils.hasText(json) && !"[]".equals(json.trim()) && !"{}".equals(json.trim());
    }
}
