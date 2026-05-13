package com.codecoach.module.ai.service.impl;

import com.codecoach.module.ai.dto.InterviewContext;
import com.codecoach.module.ai.service.AiInterviewService;
import com.codecoach.module.ai.vo.FeedbackAndQuestionResult;
import com.codecoach.module.ai.vo.ReportGenerateResult;
import com.codecoach.module.project.entity.Project;

import java.util.ArrayList;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@Profile("mock")
public class MockAiInterviewServiceImpl implements AiInterviewService {

    private static final String DEFAULT_PROJECT_NAME = "这个项目";

    private static final String DEFAULT_TECH_STACK = "项目中使用的技术栈";

    private static final String DEFAULT_TARGET_ROLE = "目标岗位";

    @Override
    public String generateFirstQuestion(Project project, String targetRole, String difficulty) {
        String projectName = getProjectName(project);
        return "请你先介绍一下「" + projectName + "」这个项目的背景、核心业务流程，以及你主要负责的模块。";
    }

    @Override
    public FeedbackAndQuestionResult generateFeedbackAndNextQuestion(InterviewContext context) {
        Project project = context.getProject();
        String projectName = getProjectName(project);
        String techStack = getTechStack(project);
        int roundNo = getRoundNo(context);

        String feedback = "你的回答已经覆盖了「" + projectName + "」的一部分核心信息。"
                + "建议继续结合具体职责、关键技术取舍和线上问题处理，把回答从“做了什么”推进到“为什么这样做、效果如何”。";
        String nextQuestion = switch (roundNo) {
            case 1 -> "请继续说明「" + projectName + "」的核心业务流程：一次用户请求从进入系统到完成核心业务动作，主要会经过哪些模块？";
            case 2 -> "你提到了技术栈「" + techStack + "」，请说明这些技术分别解决了什么问题，以及当时为什么选择它们。";
            case 3 -> "如果「" + projectName + "」在核心链路中出现异常，例如数据库写入失败、缓存不一致或消息处理失败，你会如何发现、补偿和恢复？";
            case 4 -> "当「" + projectName + "」面对高并发访问时，你会从限流、缓存、异步化、数据库压力控制等方面做哪些设计？";
            case 5 -> "如果现在让你重新优化「" + projectName + "」，你会优先改进哪些架构、性能或工程质量问题？";
            default -> "请结合「" + projectName + "」补充一个你认为最能体现技术深度的设计点，并说明它带来的收益。";
        };

        return new FeedbackAndQuestionResult(feedback, nextQuestion);
    }

    @Override
    public ReportGenerateResult generateReport(InterviewContext context) {
        Project project = context.getProject();
        String projectName = getProjectName(project);
        String techStack = getTechStack(project);
        String targetRole = getTargetRole(context);

        List<String> strengths = List.of(
                "能够围绕「" + projectName + "」说明项目背景和主要业务流程",
                "能够结合技术栈「" + techStack + "」描述部分技术实现",
                "回答中体现了一定的项目参与度和岗位相关经验"
        );
        List<String> weaknesses = List.of(
                "对异常场景、边界条件和补偿方案的说明还可以更具体",
                "对技术选型背后的取舍、性能指标和落地效果表达还不够量化"
        );
        List<String> suggestions = List.of(
                "准备一版 2 分钟项目介绍，覆盖背景、流程、职责、技术难点和结果",
                "围绕 " + targetRole + " 常问的高并发、缓存一致性、幂等和故障恢复继续补充案例",
                "用数据描述项目收益，例如响应时间、吞吐量、稳定性或研发效率的变化"
        );

        return new ReportGenerateResult(
                78,
                "本次训练中，你能够围绕「" + projectName + "」进行基本说明，"
                        + "也能提到 " + techStack + " 等技术点。后续需要继续强化异常场景、高并发设计和技术取舍的表达。",
                strengths,
                weaknesses,
                suggestions,
                buildQaReview(context)
        );
    }

    private List<ReportGenerateResult.QaReviewItem> buildQaReview(InterviewContext context) {
        List<InterviewContext.QaRecord> qaRecords = context.getQaRecords();
        if (qaRecords == null || qaRecords.isEmpty()) {
            List<ReportGenerateResult.QaReviewItem> defaultReview = new ArrayList<>();
            defaultReview.add(new ReportGenerateResult.QaReviewItem(
                    context.getCurrentQuestion(),
                    context.getUserAnswer(),
                    "回答整体方向正确，建议补充更具体的技术细节、异常处理和项目收益。"
            ));
            return defaultReview;
        }

        return qaRecords.stream()
                .map(record -> new ReportGenerateResult.QaReviewItem(
                        record.getQuestion(),
                        record.getAnswer(),
                        record.getFeedback()
                ))
                .toList();
    }

    private String getProjectName(Project project) {
        if (project == null || !StringUtils.hasText(project.getName())) {
            return DEFAULT_PROJECT_NAME;
        }
        return project.getName();
    }

    private String getTechStack(Project project) {
        if (project == null || !StringUtils.hasText(project.getTechStack())) {
            return DEFAULT_TECH_STACK;
        }
        return project.getTechStack();
    }

    private int getRoundNo(InterviewContext context) {
        if (context == null || context.getRoundNo() == null || context.getRoundNo() < 1) {
            return 1;
        }
        return context.getRoundNo();
    }

    private String getTargetRole(InterviewContext context) {
        if (context == null || !StringUtils.hasText(context.getTargetRole())) {
            return DEFAULT_TARGET_ROLE;
        }
        return context.getTargetRole();
    }
}
