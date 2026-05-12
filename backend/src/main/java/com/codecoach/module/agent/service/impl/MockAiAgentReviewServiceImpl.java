package com.codecoach.module.agent.service.impl;

import com.codecoach.module.agent.model.AgentReviewContext;
import com.codecoach.module.agent.model.AgentReviewResult;
import com.codecoach.module.agent.service.AiAgentReviewService;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@ConditionalOnProperty(prefix = "ai", name = "provider", havingValue = "mock")
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
        result.setKeyFindings(hasData
                ? List.of("近期薄弱点主要集中在知识点展开和项目表达证据不足。", "下一步应把学习材料和专项训练结合起来，而不是只看报告。")
                : List.of("训练报告和能力画像样本不足，暂时无法形成稳定模式判断。"));
        result.setRecurringWeaknesses(hasData
                ? List.of("回答结构不完整", "项目技术细节和工程权衡证据不足")
                : List.of("暂无足够训练数据识别反复薄弱点"));
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
        result.setConfidence(hasData ? "MEDIUM" : "LOW");
        return result;
    }

    private AgentReviewResult.NextAction action(String type, String title, String reason, int priority, String targetPath) {
        AgentReviewResult.NextAction action = new AgentReviewResult.NextAction();
        action.setType(type);
        action.setTitle(title);
        action.setReason(reason);
        action.setPriority(priority);
        action.setTargetPath(targetPath);
        return action;
    }

    private boolean hasUsefulJson(String json) {
        return StringUtils.hasText(json) && !"[]".equals(json.trim()) && !"{}".equals(json.trim());
    }
}
