package com.codecoach.module.resume.service.impl;

import com.codecoach.module.resume.model.ResumeAnalysisResult;
import com.codecoach.module.resume.service.AiResumeAnalysisService;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@ConditionalOnProperty(prefix = "ai", name = "provider", havingValue = "mock")
public class MockAiResumeAnalysisServiceImpl implements AiResumeAnalysisService {

    @Override
    public ResumeAnalysisResult analyze(String resumeText, String targetRole) {
        String role = StringUtils.hasText(targetRole) ? targetRole.trim() : "Java 后端实习";
        ResumeAnalysisResult result = new ResumeAnalysisResult();
        result.setSummary("这份简历具备进入「" + role + "」项目面试的基础素材，但部分项目收益、个人职责边界和技术取舍还需要用真实细节补强。");

        ResumeAnalysisResult.SkillItem java = new ResumeAnalysisResult.SkillItem();
        java.setName("Java / Spring Boot");
        java.setCategory("后端开发");
        java.setRiskLevel("MEDIUM");
        java.setReason("简历通常会写框架使用，但面试会追问 IOC、事务、异常处理和项目中的具体落点。");
        ResumeAnalysisResult.SkillItem redis = new ResumeAnalysisResult.SkillItem();
        redis.setName("Redis");
        redis.setCategory("缓存与性能");
        redis.setRiskLevel("HIGH");
        redis.setReason("如果写了缓存、分布式锁或高并发，面试官容易继续追问一致性、击穿、雪崩和锁失效。");
        result.setSkills(List.of(java, redis));

        ResumeAnalysisResult.ProjectExperienceItem project = new ResumeAnalysisResult.ProjectExperienceItem();
        project.setProjectName(extractProjectName(resumeText));
        project.setDescription("简历中的核心项目经历，需要进一步验证业务背景、核心链路和个人贡献。");
        project.setTechStack(List.of("Spring Boot", "MySQL", "Redis"));
        project.setRole("负责核心接口、数据模型和部分性能优化表达。");
        project.setHighlights(List.of("可以围绕业务链路、异常处理和性能优化展开", "适合作为项目拷打训练入口"));
        project.setRiskPoints(List.of("项目描述偏概括，缺少真实指标", "个人贡献和团队职责边界需要讲清楚"));
        project.setRecommendedQuestions(List.of(
                "请完整讲一下这个项目的核心业务流程，一次请求会经过哪些模块？",
                "你在项目里最有技术含量的设计是什么，为什么不是普通 CRUD？",
                "如果 Redis 缓存和 MySQL 数据不一致，你们如何发现和处理？"
        ));
        result.setProjectExperiences(List.of(project));

        ResumeAnalysisResult.RiskPointItem risk = new ResumeAnalysisResult.RiskPointItem();
        risk.setType("项目描述过泛");
        risk.setLevel("HIGH");
        risk.setEvidence("项目亮点如果只写“优化性能、提升效率”，面试官会追问优化前后指标和你具体做了什么。");
        risk.setSuggestion("补充真实场景、约束、方案取舍和可验证结果，不要编造指标。");
        result.setRiskPoints(List.of(risk));
        result.setInterviewQuestions(List.of(
                "你简历里提到的技术栈分别解决了什么问题？",
                "这个项目最容易出线上问题的环节在哪里？",
                "如果让你重构这个项目，你会先改哪里？"
        ));
        result.setOptimizationSuggestions(List.of(
                "把“负责开发”改成“负责某个模块的设计、接口、数据一致性或异常处理”。",
                "把“性能提升明显”替换为真实可解释的现象、压测口径或线上观察。",
                "每个项目准备一条业务主线、一条技术难点和一条复盘改进。"
        ));
        return result;
    }

    private String extractProjectName(String resumeText) {
        if (!StringUtils.hasText(resumeText)) {
            return "简历项目";
        }
        String[] lines = resumeText.split("\\R");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.length() >= 2 && trimmed.length() <= 32 && trimmed.contains("项目")) {
                return trimmed.replaceAll("^[#\\-\\s]+", "");
            }
        }
        return "简历项目";
    }
}
