package com.codecoach.module.mockinterview.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.codecoach.common.exception.BusinessException;
import com.codecoach.common.result.PageResult;
import com.codecoach.common.result.ResultCode;
import com.codecoach.module.ai.service.AiTokenStreamHandler;
import com.codecoach.module.insight.constant.AbilityDimensionCodes;
import com.codecoach.module.insight.entity.UserAbilitySnapshot;
import com.codecoach.module.insight.mapper.UserAbilitySnapshotMapper;
import com.codecoach.module.mockinterview.dto.MockInterviewAnswerRequest;
import com.codecoach.module.mockinterview.dto.MockInterviewCreateRequest;
import com.codecoach.module.mockinterview.dto.MockInterviewPageRequest;
import com.codecoach.module.mockinterview.entity.MockInterviewMessage;
import com.codecoach.module.mockinterview.entity.MockInterviewReport;
import com.codecoach.module.mockinterview.entity.MockInterviewSession;
import com.codecoach.module.mockinterview.mapper.MockInterviewMessageMapper;
import com.codecoach.module.mockinterview.mapper.MockInterviewReportMapper;
import com.codecoach.module.mockinterview.mapper.MockInterviewSessionMapper;
import com.codecoach.module.mockinterview.service.MockInterviewSessionService;
import com.codecoach.module.mockinterview.vo.MockInterviewAnswerResponse;
import com.codecoach.module.mockinterview.vo.MockInterviewCreateResponse;
import com.codecoach.module.mockinterview.vo.MockInterviewFinishResponse;
import com.codecoach.module.mockinterview.vo.MockInterviewHistoryVO;
import com.codecoach.module.mockinterview.vo.MockInterviewMessageVO;
import com.codecoach.module.mockinterview.vo.MockInterviewReportVO;
import com.codecoach.module.mockinterview.vo.MockInterviewSessionDetailVO;
import com.codecoach.module.project.entity.Project;
import com.codecoach.module.project.mapper.ProjectMapper;
import com.codecoach.module.rag.config.RagProperties;
import com.codecoach.module.rag.constant.RagConstants;
import com.codecoach.module.rag.model.RagRetrievedChunk;
import com.codecoach.module.rag.model.RagSearchRequest;
import com.codecoach.module.rag.model.RagSearchResponse;
import com.codecoach.module.rag.service.RagRetrievalService;
import com.codecoach.module.report.quality.ReportQualityAssessment;
import com.codecoach.module.report.quality.ReportQualityEvaluator;
import com.codecoach.module.report.quality.ReportTrainingType;
import com.codecoach.module.resume.entity.ResumeProfile;
import com.codecoach.module.resume.mapper.ResumeProfileMapper;
import com.codecoach.security.UserContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class MockInterviewSessionServiceImpl implements MockInterviewSessionService {

    private static final Logger log = LoggerFactory.getLogger(MockInterviewSessionServiceImpl.class);
    private static final int NOT_FOUND_CODE = 6101;
    private static final int FINISHED_CODE = 6102;
    private static final int NOT_DELETED = 0;
    private static final int DELETED = 1;
    private static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    private static final String STATUS_FINISHED = "FINISHED";
    private static final String ROLE_USER = "USER";
    private static final String ROLE_ASSISTANT = "ASSISTANT";
    private static final String TYPE_AI_QUESTION = "AI_QUESTION";
    private static final String TYPE_USER_ANSWER = "USER_ANSWER";
    private static final String DEFAULT_TYPE = "COMPREHENSIVE_TECHNICAL";
    private static final String DEFAULT_DIFFICULTY = "NORMAL";
    private static final int DEFAULT_MAX_ROUND = 6;
    private static final int MAX_CONTEXT_CHARS = 1800;
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };
    private static final TypeReference<List<MockInterviewReportVO.StagePerformanceVO>> STAGE_LIST =
            new TypeReference<>() {
            };

    private final MockInterviewSessionMapper sessionMapper;
    private final MockInterviewMessageMapper messageMapper;
    private final MockInterviewReportMapper reportMapper;
    private final ProjectMapper projectMapper;
    private final ResumeProfileMapper resumeProfileMapper;
    private final RagRetrievalService ragRetrievalService;
    private final RagProperties ragProperties;
    private final ReportQualityEvaluator reportQualityEvaluator;
    private final UserAbilitySnapshotMapper abilitySnapshotMapper;
    private final ObjectMapper objectMapper;

    public MockInterviewSessionServiceImpl(
            MockInterviewSessionMapper sessionMapper,
            MockInterviewMessageMapper messageMapper,
            MockInterviewReportMapper reportMapper,
            ProjectMapper projectMapper,
            ResumeProfileMapper resumeProfileMapper,
            RagRetrievalService ragRetrievalService,
            RagProperties ragProperties,
            ReportQualityEvaluator reportQualityEvaluator,
            UserAbilitySnapshotMapper abilitySnapshotMapper,
            ObjectMapper objectMapper
    ) {
        this.sessionMapper = sessionMapper;
        this.messageMapper = messageMapper;
        this.reportMapper = reportMapper;
        this.projectMapper = projectMapper;
        this.resumeProfileMapper = resumeProfileMapper;
        this.ragRetrievalService = ragRetrievalService;
        this.ragProperties = ragProperties;
        this.reportQualityEvaluator = reportQualityEvaluator;
        this.abilitySnapshotMapper = abilitySnapshotMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public MockInterviewCreateResponse createSession(MockInterviewCreateRequest request) {
        Long userId = UserContext.getCurrentUserId();
        Project project = validateProject(userId, request.getProjectId());
        ResumeProfile resume = validateResume(userId, request.getResumeId());
        String type = normalizeType(request.getInterviewType());
        String difficulty = normalizeDifficulty(request.getDifficulty());
        int maxRound = normalizeMaxRound(request.getMaxRound(), type);
        LocalDateTime now = LocalDateTime.now();

        MockInterviewSession session = new MockInterviewSession();
        session.setUserId(userId);
        session.setInterviewType(type);
        session.setTargetRole(request.getTargetRole().trim());
        session.setDifficulty(difficulty);
        session.setProjectId(project == null ? null : project.getId());
        session.setResumeId(resume == null ? null : resume.getId());
        session.setStatus(STATUS_IN_PROGRESS);
        session.setCurrentRound(1);
        session.setMaxRound(maxRound);
        session.setCurrentStage(resolveStage(1, maxRound, type));
        session.setStartedAt(now);
        session.setIsDeleted(NOT_DELETED);
        sessionMapper.insert(session);

        MockInterviewMessage first = newAssistantQuestion(
                session,
                1,
                session.getCurrentStage(),
                buildQuestion(session, project, resume, Collections.emptyList(), "")
        );
        messageMapper.insert(first);
        return new MockInterviewCreateResponse(session.getId(), toMessageVO(first));
    }

    @Override
    public PageResult<MockInterviewHistoryVO> pageSessions(MockInterviewPageRequest request) {
        Long userId = UserContext.getCurrentUserId();
        long pageNum = request == null || request.getPageNum() == null ? 1L : Math.max(1L, request.getPageNum());
        long pageSize = request == null || request.getPageSize() == null ? 10L : Math.min(100L, Math.max(1L, request.getPageSize()));
        LambdaQueryWrapper<MockInterviewSession> wrapper = new LambdaQueryWrapper<MockInterviewSession>()
                .eq(MockInterviewSession::getUserId, userId)
                .eq(MockInterviewSession::getIsDeleted, NOT_DELETED)
                .eq(request != null && StringUtils.hasText(request.getStatus()), MockInterviewSession::getStatus, request == null ? null : request.getStatus())
                .orderByDesc(MockInterviewSession::getCreatedAt);
        Page<MockInterviewSession> page = sessionMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        Map<Long, Long> reportIds = getReportIds(page.getRecords());
        List<MockInterviewHistoryVO> records = page.getRecords().stream()
                .map(session -> new MockInterviewHistoryVO(
                        session.getId(),
                        session.getInterviewType(),
                        session.getTargetRole(),
                        session.getDifficulty(),
                        session.getStatus(),
                        session.getCurrentRound(),
                        session.getMaxRound(),
                        session.getCurrentStage(),
                        session.getTotalScore(),
                        reportIds.get(session.getId()),
                        session.getCreatedAt()
                ))
                .toList();
        return new PageResult<>(records, page.getTotal(), page.getCurrent(), page.getSize(), page.getPages());
    }

    @Override
    public MockInterviewSessionDetailVO getSessionDetail(Long sessionId) {
        MockInterviewSession session = requireOwnedSession(sessionId);
        Project project = session.getProjectId() == null ? null : projectMapper.selectById(session.getProjectId());
        ResumeProfile resume = session.getResumeId() == null ? null : resumeProfileMapper.selectById(session.getResumeId());
        List<MockInterviewMessageVO> messages = listMessages(sessionId).stream().map(this::toMessageVO).toList();
        MockInterviewReport report = getReportBySessionId(sessionId);
        return new MockInterviewSessionDetailVO(
                session.getId(),
                session.getInterviewType(),
                session.getTargetRole(),
                session.getDifficulty(),
                session.getProjectId(),
                project == null ? null : project.getName(),
                session.getResumeId(),
                resume == null ? null : resume.getTitle(),
                session.getStatus(),
                session.getCurrentRound(),
                session.getMaxRound(),
                session.getCurrentStage(),
                report == null ? null : report.getId(),
                messages
        );
    }

    @Override
    public MockInterviewAnswerResponse submitAnswer(Long sessionId, MockInterviewAnswerRequest request) {
        return submitAnswerInternal(sessionId, request, null);
    }

    @Override
    public MockInterviewAnswerResponse submitAnswerStream(
            Long sessionId,
            MockInterviewAnswerRequest request,
            AiTokenStreamHandler streamHandler
    ) {
        return submitAnswerInternal(sessionId, request, streamHandler);
    }

    @Transactional
    protected MockInterviewAnswerResponse submitAnswerInternal(
            Long sessionId,
            MockInterviewAnswerRequest request,
            AiTokenStreamHandler streamHandler
    ) {
        MockInterviewSession session = requireOwnedSession(sessionId);
        if (!STATUS_IN_PROGRESS.equals(session.getStatus())) {
            throw new BusinessException(FINISHED_CODE, "模拟面试已结束");
        }
        String answer = request.getAnswer().trim();
        Integer roundNo = session.getCurrentRound();
        LocalDateTime now = LocalDateTime.now();
        MockInterviewMessage userAnswer = new MockInterviewMessage();
        userAnswer.setSessionId(session.getId());
        userAnswer.setUserId(session.getUserId());
        userAnswer.setRole(ROLE_USER);
        userAnswer.setMessageType(TYPE_USER_ANSWER);
        userAnswer.setStage(session.getCurrentStage());
        userAnswer.setContent(answer);
        userAnswer.setRoundNo(roundNo);
        userAnswer.setScore(scoreAnswer(answer));
        userAnswer.setCreatedAt(now);
        messageMapper.insert(userAnswer);

        boolean finished = roundNo >= session.getMaxRound();
        if (finished) {
            MockInterviewFinishResponse finish = finishSessionWithReport(session, now);
            return new MockInterviewAnswerResponse(toMessageVO(userAnswer), null, true, finish.getReportId(), finish.getTotalScore(), session.getCurrentStage());
        }

        List<MockInterviewMessage> history = listMessages(sessionId);
        Project project = session.getProjectId() == null ? null : projectMapper.selectById(session.getProjectId());
        ResumeProfile resume = session.getResumeId() == null ? null : resumeProfileMapper.selectById(session.getResumeId());
        int nextRound = roundNo + 1;
        String nextStage = resolveStage(nextRound, session.getMaxRound(), session.getInterviewType());
        String nextQuestionText = buildQuestion(session, project, resume, history, answer);
        streamText(streamHandler, nextQuestionText);
        MockInterviewMessage nextQuestion = newAssistantQuestion(session, nextRound, nextStage, nextQuestionText);
        messageMapper.insert(nextQuestion);

        session.setCurrentRound(nextRound);
        session.setCurrentStage(nextStage);
        sessionMapper.updateById(session);
        return new MockInterviewAnswerResponse(toMessageVO(userAnswer), toMessageVO(nextQuestion), false, null, null, nextStage);
    }

    @Override
    @Transactional
    public MockInterviewFinishResponse finishSession(Long sessionId) {
        MockInterviewSession session = requireOwnedSession(sessionId);
        MockInterviewReport existing = getReportBySessionId(sessionId);
        if (existing != null) {
            return new MockInterviewFinishResponse(existing.getId(), sessionId, existing.getTotalScore());
        }
        return finishSessionWithReport(session, LocalDateTime.now());
    }

    @Override
    public MockInterviewReportVO getReport(Long sessionId) {
        MockInterviewSession session = requireOwnedSession(sessionId);
        MockInterviewReport report = getReportBySessionId(sessionId);
        if (report == null) {
            throw new BusinessException(NOT_FOUND_CODE, "模拟面试报告不存在");
        }
        return toReportVO(report, session);
    }

    private MockInterviewFinishResponse finishSessionWithReport(MockInterviewSession session, LocalDateTime now) {
        MockInterviewReport existing = getReportBySessionId(session.getId());
        if (existing != null) {
            markFinished(session, existing.getTotalScore(), now);
            return new MockInterviewFinishResponse(existing.getId(), session.getId(), existing.getTotalScore());
        }
        List<MockInterviewMessage> messages = listMessages(session.getId());
        List<String> answers = messages.stream()
                .filter(message -> TYPE_USER_ANSWER.equals(message.getMessageType()))
                .map(MockInterviewMessage::getContent)
                .filter(StringUtils::hasText)
                .toList();
        ReportQualityAssessment quality = reportQualityEvaluator.assess(answers, session.getMaxRound(), ReportTrainingType.PROJECT);
        int baseScore = answers.isEmpty()
                ? 0
                : (int) Math.round(messages.stream()
                .filter(message -> TYPE_USER_ANSWER.equals(message.getMessageType()))
                .map(MockInterviewMessage::getScore)
                .filter(score -> score != null)
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0));
        int totalScore = reportQualityEvaluator.applyScoreCap(baseScore, quality);
        List<String> weaknessTags = buildWeaknessTags(messages, quality);
        MockInterviewReport report = new MockInterviewReport();
        report.setSessionId(session.getId());
        report.setUserId(session.getUserId());
        report.setProjectId(session.getProjectId());
        report.setResumeId(session.getResumeId());
        report.setTotalScore(totalScore);
        report.setSampleSufficiency(quality.getSampleSufficiency().name());
        report.setSummary(buildSummary(totalScore, quality));
        report.setStagePerformances(toJson(buildStagePerformances(messages, session)));
        report.setStrengths(toJson(buildStrengths(messages, quality)));
        report.setWeaknesses(toJson(mergeFront(quality.getDeductionReasons(), buildWeaknesses(messages))));
        report.setHighRiskAnswers(toJson(buildHighRiskAnswers(messages)));
        report.setNextActions(toJson(mergeFront(quality.getNextActions(), buildNextActions(weaknessTags))));
        report.setRecommendedLearning(toJson(buildLearningRecommendations(weaknessTags)));
        report.setRecommendedTraining(toJson(List.of("八股综合训练", "项目拷打训练", "简历项目追问训练", "面试复盘 Agent")));
        report.setWeaknessTags(toJson(weaknessTags));
        report.setCreatedAt(now);
        reportMapper.insert(report);
        markFinished(session, totalScore, now);
        if (!quality.isLowConfidence()) {
            createAbilitySnapshots(report, session);
        }
        return new MockInterviewFinishResponse(report.getId(), session.getId(), totalScore);
    }

    private void markFinished(MockInterviewSession session, Integer totalScore, LocalDateTime now) {
        session.setStatus(STATUS_FINISHED);
        session.setEndedAt(now);
        session.setTotalScore(totalScore);
        session.setCurrentStage("WRAP_UP");
        sessionMapper.updateById(session);
    }

    private String buildQuestion(
            MockInterviewSession session,
            Project project,
            ResumeProfile resume,
            List<MockInterviewMessage> history,
            String latestAnswer
    ) {
        String stage = resolveStage(session.getCurrentRound(), session.getMaxRound(), session.getInterviewType());
        if (history != null && !history.isEmpty()) {
            int nextRound = Math.min(session.getCurrentRound() + 1, session.getMaxRound());
            stage = resolveStage(nextRound, session.getMaxRound(), session.getInterviewType());
        }
        String projectName = project == null ? "你最熟悉的一个 Java 后端项目" : "「" + project.getName() + "」";
        String resumeHint = resume == null ? "如果没有简历材料，就按你真实经历回答" : "结合你简历中的项目经历";
        String ragHint = retrieveRagContext(session, stage, project, resume, latestAnswer);
        String pressure = isWeakAnswer(latestAnswer) ? "你刚才的回答偏浅，我会换个角度确认真实掌握程度。" : "";
        String question = switch (stage) {
            case "OPENING" -> "我们开始一场 Java 后端技术一面。请你先做一个 1 到 2 分钟自我介绍，重点讲目标岗位、技术栈和最能代表你的项目。";
            case "RESUME_PROJECT" -> pressure + resumeHint + "，请你介绍一个最核心的项目经历：项目解决什么业务问题，你负责哪些模块，哪些地方最容易被追问？";
            case "TECHNICAL_FUNDAMENTAL" -> pressure + "从 Java 后端基础来看，请你结合 " + projectName + " 讲一个你实际用过或必须掌握的技术点，例如并发、JVM、MySQL、Redis 或 Spring，并说明它的原理和适用边界。";
            case "PROJECT_DEEP_DIVE" -> pressure + "我们深挖 " + projectName + "。请你选一个核心链路，说明请求流转、数据一致性、异常补偿和性能瓶颈分别怎么处理。";
            case "SCENARIO_DESIGN" -> pressure + "假设 " + projectName + " 的核心接口突然流量涨到平时 10 倍，你会怎样设计限流、缓存、异步化、数据库保护和降级方案？";
            default -> "最后一个问题：请你总结本场面试里自己最有把握和最没把握的点，并向面试官反问一个和团队工程实践相关的问题。";
        };
        if (StringUtils.hasText(ragHint)) {
            return question + "\n\n我会参考你已有材料中的线索继续追问，例如："
                    + summarizeContext(ragHint)
                    + "。请不要复述材料，请用自己的话回答。";
        }
        return question;
    }

    private String retrieveRagContext(MockInterviewSession session, String stage, Project project, ResumeProfile resume, String answer) {
        if (ragProperties == null || !Boolean.TRUE.equals(ragProperties.getEnabled())) {
            return "";
        }
        try {
            RagSearchRequest request = new RagSearchRequest();
            request.setQuery(String.join(" ", session.getTargetRole(), stage, nullToEmpty(project == null ? null : project.getName()), nullToEmpty(answer)));
            request.setTopK(4);
            request.setSourceTypes(sourceTypesForStage(stage));
            Map<String, Object> filter = new LinkedHashMap<>();
            filter.put("userId", session.getUserId());
            if (project != null) {
                filter.put("projectId", project.getId());
            }
            if (resume != null) {
                filter.put("resumeId", resume.getId());
            }
            request.setFilter(filter);
            RagSearchResponse response = ragRetrievalService.search(request);
            List<RagRetrievedChunk> chunks = response == null ? List.of() : response.getChunks();
            if (chunks == null || chunks.isEmpty()) {
                return "";
            }
            return ragRetrievalService.buildContextBlock(chunks, MAX_CONTEXT_CHARS);
        } catch (Exception exception) {
            log.warn("Mock interview RAG fallback. sessionId={}, stage={}", session.getId(), stage);
            return "";
        }
    }

    private List<String> sourceTypesForStage(String stage) {
        return switch (stage) {
            case "TECHNICAL_FUNDAMENTAL" -> List.of(RagConstants.SOURCE_TYPE_KNOWLEDGE_ARTICLE);
            case "PROJECT_DEEP_DIVE", "RESUME_PROJECT" -> List.of(RagConstants.SOURCE_TYPE_PROJECT, RagConstants.SOURCE_TYPE_USER_UPLOAD);
            case "SCENARIO_DESIGN" -> List.of(RagConstants.SOURCE_TYPE_PROJECT, RagConstants.SOURCE_TYPE_KNOWLEDGE_ARTICLE, RagConstants.SOURCE_TYPE_USER_UPLOAD);
            default -> List.of(RagConstants.SOURCE_TYPE_PROJECT, RagConstants.SOURCE_TYPE_USER_UPLOAD);
        };
    }

    private String resolveStage(int round, int maxRound, String type) {
        if ("RESUME_PROJECT_DEEP_DIVE".equals(type)) {
            if (round == 1) {
                return "OPENING";
            }
            if (round >= maxRound) {
                return "WRAP_UP";
            }
            return round <= Math.max(3, maxRound / 2) ? "RESUME_PROJECT" : "PROJECT_DEEP_DIVE";
        }
        if ("BA_GU_COMPREHENSIVE".equals(type)) {
            if (round == 1) {
                return "OPENING";
            }
            if (round >= maxRound) {
                return "WRAP_UP";
            }
            return round < maxRound - 1 ? "TECHNICAL_FUNDAMENTAL" : "SCENARIO_DESIGN";
        }
        double p = (double) round / Math.max(maxRound, 1);
        if (round == 1) {
            return "OPENING";
        }
        if (p <= 0.34d) {
            return "RESUME_PROJECT";
        }
        if (p <= 0.55d) {
            return "TECHNICAL_FUNDAMENTAL";
        }
        if (p <= 0.75d) {
            return "PROJECT_DEEP_DIVE";
        }
        if (round < maxRound) {
            return "SCENARIO_DESIGN";
        }
        return "WRAP_UP";
    }

    private int scoreAnswer(String answer) {
        if (!StringUtils.hasText(answer)) {
            return 0;
        }
        return switch (reportQualityEvaluator.classifyAnswer(answer, ReportTrainingType.PROJECT)) {
            case NO_ANSWER, INVALID -> 8;
            case VERY_WEAK -> 22;
            case PARTIAL -> 45;
            case BASIC -> 65;
            case GOOD -> 80;
            case EXCELLENT -> 90;
        };
    }

    private void streamText(AiTokenStreamHandler handler, String text) {
        if (handler == null || !StringUtils.hasText(text)) {
            return;
        }
        int index = 0;
        while (index < text.length()) {
            int end = Math.min(text.length(), index + 8);
            handler.onDelta(text.substring(index, end));
            index = end;
        }
    }

    private List<MockInterviewReportVO.StagePerformanceVO> buildStagePerformances(
            List<MockInterviewMessage> messages,
            MockInterviewSession session
    ) {
        Map<String, List<MockInterviewMessage>> grouped = messages.stream()
                .filter(message -> TYPE_USER_ANSWER.equals(message.getMessageType()))
                .collect(Collectors.groupingBy(MockInterviewMessage::getStage, LinkedHashMap::new, Collectors.toList()));
        List<String> stages = List.of("OPENING", "RESUME_PROJECT", "TECHNICAL_FUNDAMENTAL", "PROJECT_DEEP_DIVE", "SCENARIO_DESIGN", "WRAP_UP");
        List<MockInterviewReportVO.StagePerformanceVO> result = new ArrayList<>();
        for (String stage : stages) {
            List<MockInterviewMessage> stageAnswers = grouped.getOrDefault(stage, List.of());
            int score = stageAnswers.isEmpty() ? 0 : (int) Math.round(stageAnswers.stream().map(MockInterviewMessage::getScore).filter(v -> v != null).mapToInt(Integer::intValue).average().orElse(0));
            if (!stageAnswers.isEmpty() || "COMPREHENSIVE_TECHNICAL".equals(session.getInterviewType())) {
                result.add(new MockInterviewReportVO.StagePerformanceVO(stage, stageName(stage), score, stageComment(stage, score, stageAnswers.size())));
            }
        }
        return result;
    }

    private String stageComment(String stage, int score, int count) {
        if (count == 0) {
            return "本阶段样本不足，暂不能判断稳定表现。";
        }
        if (score < 40) {
            return stageName(stage) + "回答偏弱，缺少可验证的经历、原理或工程细节。";
        }
        if (score < 70) {
            return stageName(stage) + "有基础表达，但追问下的结构、例子和取舍还不够扎实。";
        }
        return stageName(stage) + "表达相对完整，能够支撑进一步追问。";
    }

    private List<String> buildStrengths(List<MockInterviewMessage> messages, ReportQualityAssessment quality) {
        if (quality.isLowConfidence()) {
            return List.of("本次样本不足，暂不沉淀稳定优势。");
        }
        return List.of("能够完成多阶段技术面试流程", "部分回答能结合项目或工程场景展开", "具备继续进行专项训练和复盘的材料基础");
    }

    private List<String> buildWeaknesses(List<MockInterviewMessage> messages) {
        List<String> weaknesses = new ArrayList<>();
        if (averageStage(messages, "PROJECT_DEEP_DIVE") < 60) {
            weaknesses.add("项目深挖阶段对链路、异常和性能指标说明不够具体。");
        }
        if (averageStage(messages, "TECHNICAL_FUNDAMENTAL") < 60) {
            weaknesses.add("技术基础阶段需要补充原理、适用边界和常见追问。");
        }
        if (averageStage(messages, "SCENARIO_DESIGN") < 60) {
            weaknesses.add("场景设计阶段需要加强工程权衡、降级策略和数据一致性表达。");
        }
        if (weaknesses.isEmpty()) {
            weaknesses.add("后续重点提升回答量化程度，例如指标、数据规模和优化前后对比。");
        }
        return weaknesses;
    }

    private List<String> buildHighRiskAnswers(List<MockInterviewMessage> messages) {
        return messages.stream()
                .filter(message -> TYPE_USER_ANSWER.equals(message.getMessageType()))
                .filter(message -> message.getScore() == null || message.getScore() < 50)
                .map(message -> "第 " + message.getRoundNo() + " 轮（" + stageName(message.getStage()) + "）：回答样本偏弱，容易被继续追问个人贡献、原理和落地细节。")
                .limit(5)
                .toList();
    }

    private List<String> buildWeaknessTags(List<MockInterviewMessage> messages, ReportQualityAssessment quality) {
        Set<String> tags = new LinkedHashSet<>();
        if (quality.getValidAnswerCount() < 3) {
            tags.add("样本不足");
        }
        if (averageStage(messages, "RESUME_PROJECT") < 60) {
            tags.add("简历项目表达");
        }
        if (averageStage(messages, "TECHNICAL_FUNDAMENTAL") < 60) {
            tags.add("技术基础");
        }
        if (averageStage(messages, "PROJECT_DEEP_DIVE") < 60) {
            tags.add("项目深挖");
        }
        if (averageStage(messages, "SCENARIO_DESIGN") < 60) {
            tags.add("场景设计");
        }
        tags.add("沟通表达");
        return tags.stream().limit(8).toList();
    }

    private List<String> buildNextActions(List<String> weaknessTags) {
        List<String> actions = new ArrayList<>();
        if (weaknessTags.contains("简历项目表达")) {
            actions.add("回到简历训练，重写 2 分钟项目介绍并补齐风险点。");
        }
        if (weaknessTags.contains("技术基础")) {
            actions.add("进入知识学习，优先补 Java 并发、JVM、MySQL、Redis、Spring 的核心原理。");
        }
        if (weaknessTags.contains("项目深挖")) {
            actions.add("选择关联项目做一次项目拷打，重点练链路、异常、数据一致性和指标。");
        }
        actions.add("生成一次复盘 Agent，把本场报告和近期训练合并看重复问题。");
        return actions.stream().distinct().limit(6).toList();
    }

    private List<String> buildLearningRecommendations(List<String> weaknessTags) {
        List<String> items = new ArrayList<>();
        if (weaknessTags.contains("技术基础")) {
            items.addAll(List.of("JUC 线程池", "MySQL 索引与事务", "Redis 缓存一致性", "Spring 事务传播"));
        }
        if (weaknessTags.contains("场景设计")) {
            items.addAll(List.of("限流降级", "幂等性", "分布式事务", "消息可靠性"));
        }
        if (items.isEmpty()) {
            items.addAll(List.of("项目表达结构", "技术选型解释", "追问应对"));
        }
        return items.stream().distinct().limit(8).toList();
    }

    private String buildSummary(int score, ReportQualityAssessment quality) {
        if (quality.isLowConfidence()) {
            return "本场模拟面试样本不足或回答质量偏低，系统已按低质量回答规则限制分数。当前更适合把它作为诊断入口，而不是稳定能力结论。";
        }
        if (score >= 80) {
            return "本场模拟面试整体表现较稳，能够撑住多阶段追问。下一步建议加强量化指标和更高压的场景设计追问。";
        }
        if (score >= 60) {
            return "本场模拟面试有基础支撑，但在项目深挖、技术原理和工程权衡之间切换还不够顺滑。";
        }
        return "本场模拟面试暴露出较明显风险，回答容易停留在关键词和经历描述，需要优先补齐结构化表达和核心原理。";
    }

    private void createAbilitySnapshots(MockInterviewReport report, MockInterviewSession session) {
        List<SnapshotCommand> commands = List.of(
                new SnapshotCommand("MOCK_INTERVIEW_OVERALL", "综合模拟面试表现", "真实模拟面试", report.getTotalScore()),
                new SnapshotCommand(AbilityDimensionCodes.PROJECT_BACKGROUND, "简历项目表达", "真实模拟面试", averageScoreOrTotal(report, session, "RESUME_PROJECT")),
                new SnapshotCommand(AbilityDimensionCodes.TECHNICAL_CLARITY, "技术基础", "真实模拟面试", averageScoreOrTotal(report, session, "TECHNICAL_FUNDAMENTAL")),
                new SnapshotCommand(AbilityDimensionCodes.ARCHITECTURE_UNDERSTANDING, "项目深挖", "真实模拟面试", averageScoreOrTotal(report, session, "PROJECT_DEEP_DIVE")),
                new SnapshotCommand(AbilityDimensionCodes.ENGINEERING_TRADEOFF, "场景设计", "真实模拟面试", averageScoreOrTotal(report, session, "SCENARIO_DESIGN")),
                new SnapshotCommand(AbilityDimensionCodes.EXPRESSION_STRUCTURE, "沟通表达", "真实模拟面试", report.getTotalScore())
        );
        for (SnapshotCommand command : commands) {
            UserAbilitySnapshot snapshot = new UserAbilitySnapshot();
            snapshot.setUserId(report.getUserId());
            snapshot.setSourceType("MOCK_INTERVIEW_REPORT");
            snapshot.setSourceId(report.getId());
            snapshot.setDimensionCode(command.code());
            snapshot.setDimensionName(command.name());
            snapshot.setCategory(command.category());
            snapshot.setScore(command.score());
            snapshot.setDifficulty(session.getDifficulty());
            snapshot.setEvidence(report.getSummary());
            snapshot.setWeaknessTags(report.getWeaknessTags());
            snapshot.setCreatedAt(LocalDateTime.now());
            try {
                abilitySnapshotMapper.insert(snapshot);
            } catch (Exception exception) {
                log.warn("Failed to create mock interview ability snapshot. reportId={}, dimension={}", report.getId(), command.code());
            }
        }
    }

    private int averageScoreOrTotal(MockInterviewReport report, MockInterviewSession session, String stage) {
        int score = averageStage(listMessages(session.getId()), stage);
        return score <= 0 ? report.getTotalScore() : score;
    }

    private int averageStage(List<MockInterviewMessage> messages, String stage) {
        return (int) Math.round(messages.stream()
                .filter(message -> TYPE_USER_ANSWER.equals(message.getMessageType()))
                .filter(message -> stage.equals(message.getStage()))
                .map(MockInterviewMessage::getScore)
                .filter(score -> score != null)
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0));
    }

    private MockInterviewSession requireOwnedSession(Long sessionId) {
        Long userId = UserContext.getCurrentUserId();
        MockInterviewSession session = sessionMapper.selectById(sessionId);
        if (session == null || Integer.valueOf(DELETED).equals(session.getIsDeleted())) {
            throw new BusinessException(NOT_FOUND_CODE, "模拟面试不存在");
        }
        if (!userId.equals(session.getUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        return session;
    }

    private Project validateProject(Long userId, Long projectId) {
        if (projectId == null) {
            return null;
        }
        Project project = projectMapper.selectById(projectId);
        if (project == null || Integer.valueOf(DELETED).equals(project.getIsDeleted())) {
            throw new BusinessException(2001, "项目不存在");
        }
        if (!userId.equals(project.getUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        return project;
    }

    private ResumeProfile validateResume(Long userId, Long resumeId) {
        if (resumeId == null) {
            return null;
        }
        ResumeProfile resume = resumeProfileMapper.selectById(resumeId);
        if (resume == null || Integer.valueOf(DELETED).equals(resume.getIsDeleted())) {
            throw new BusinessException(5101, "简历不存在");
        }
        if (!userId.equals(resume.getUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        return resume;
    }

    private List<MockInterviewMessage> listMessages(Long sessionId) {
        return messageMapper.selectList(new LambdaQueryWrapper<MockInterviewMessage>()
                .eq(MockInterviewMessage::getSessionId, sessionId)
                .orderByAsc(MockInterviewMessage::getCreatedAt)
                .orderByAsc(MockInterviewMessage::getId));
    }

    private MockInterviewReport getReportBySessionId(Long sessionId) {
        return reportMapper.selectOne(new LambdaQueryWrapper<MockInterviewReport>()
                .eq(MockInterviewReport::getSessionId, sessionId)
                .last("LIMIT 1"));
    }

    private Map<Long, Long> getReportIds(List<MockInterviewSession> sessions) {
        if (sessions == null || sessions.isEmpty()) {
            return Map.of();
        }
        List<Long> sessionIds = sessions.stream().map(MockInterviewSession::getId).toList();
        return reportMapper.selectList(new LambdaQueryWrapper<MockInterviewReport>()
                        .in(MockInterviewReport::getSessionId, sessionIds))
                .stream()
                .collect(Collectors.toMap(MockInterviewReport::getSessionId, MockInterviewReport::getId, (a, b) -> a));
    }

    private MockInterviewMessage newAssistantQuestion(MockInterviewSession session, Integer round, String stage, String content) {
        MockInterviewMessage message = new MockInterviewMessage();
        message.setSessionId(session.getId());
        message.setUserId(session.getUserId());
        message.setRole(ROLE_ASSISTANT);
        message.setMessageType(TYPE_AI_QUESTION);
        message.setStage(stage);
        message.setContent(content);
        message.setRoundNo(round);
        message.setCreatedAt(LocalDateTime.now());
        return message;
    }

    private MockInterviewMessageVO toMessageVO(MockInterviewMessage message) {
        return new MockInterviewMessageVO(
                message.getId(),
                message.getRole(),
                message.getMessageType(),
                message.getStage(),
                message.getContent(),
                message.getRoundNo(),
                message.getScore(),
                message.getCreatedAt()
        );
    }

    private MockInterviewReportVO toReportVO(MockInterviewReport report, MockInterviewSession session) {
        return new MockInterviewReportVO(
                report.getId(),
                report.getSessionId(),
                session.getInterviewType(),
                session.getTargetRole(),
                session.getDifficulty(),
                report.getTotalScore(),
                report.getSampleSufficiency(),
                report.getSummary(),
                readJson(report.getStagePerformances(), STAGE_LIST),
                readJson(report.getStrengths(), STRING_LIST),
                readJson(report.getWeaknesses(), STRING_LIST),
                readJson(report.getHighRiskAnswers(), STRING_LIST),
                averageStage(listMessages(session.getId()), "PROJECT_DEEP_DIVE"),
                averageStage(listMessages(session.getId()), "RESUME_PROJECT"),
                averageStage(listMessages(session.getId()), "TECHNICAL_FUNDAMENTAL"),
                readJson(report.getNextActions(), STRING_LIST),
                readJson(report.getRecommendedLearning(), STRING_LIST),
                readJson(report.getRecommendedTraining(), STRING_LIST),
                readJson(report.getWeaknessTags(), STRING_LIST),
                report.getCreatedAt()
        );
    }

    private String normalizeType(String value) {
        if (!StringUtils.hasText(value)) {
            return DEFAULT_TYPE;
        }
        return Set.of(DEFAULT_TYPE, "RESUME_PROJECT_DEEP_DIVE", "BA_GU_COMPREHENSIVE").contains(value.trim())
                ? value.trim()
                : DEFAULT_TYPE;
    }

    private String normalizeDifficulty(String value) {
        if (!StringUtils.hasText(value)) {
            return DEFAULT_DIFFICULTY;
        }
        return Set.of("EASY", "NORMAL", "HARD").contains(value.trim()) ? value.trim() : DEFAULT_DIFFICULTY;
    }

    private int normalizeMaxRound(Integer maxRound, String type) {
        int value = maxRound == null ? DEFAULT_MAX_ROUND : maxRound;
        return Math.max(4, Math.min(12, value));
    }

    private String stageName(String stage) {
        return switch (stage) {
            case "OPENING" -> "开场自我介绍";
            case "RESUME_PROJECT" -> "简历项目追问";
            case "TECHNICAL_FUNDAMENTAL" -> "技术基础";
            case "PROJECT_DEEP_DIVE" -> "项目深挖";
            case "SCENARIO_DESIGN" -> "场景设计";
            case "WRAP_UP" -> "总结反问";
            default -> stage;
        };
    }

    private boolean isWeakAnswer(String answer) {
        return StringUtils.hasText(answer) && scoreAnswer(answer) < 50;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String summarizeContext(String context) {
        if (!StringUtils.hasText(context)) {
            return "项目背景、技术栈或学习材料";
        }
        String normalized = context.replaceAll("[\\r\\n]+", " ").replaceAll("\\s+", " ").trim();
        return normalized.length() > 120 ? normalized.substring(0, 120) + "..." : normalized;
    }

    private List<String> mergeFront(List<String> priorityItems, List<String> originalItems) {
        List<String> result = new ArrayList<>();
        if (priorityItems != null) {
            result.addAll(priorityItems);
        }
        if (originalItems != null) {
            result.addAll(originalItems);
        }
        return result.stream().filter(StringUtils::hasText).map(String::trim).distinct().limit(8).toList();
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? List.of() : value);
        } catch (JsonProcessingException exception) {
            return "[]";
        }
    }

    private <T> T readJson(String json, TypeReference<T> typeReference) {
        if (!StringUtils.hasText(json)) {
            try {
                return objectMapper.readValue("[]", typeReference);
            } catch (Exception exception) {
                return null;
            }
        }
        try {
            return objectMapper.readValue(json, typeReference);
        } catch (Exception exception) {
            try {
                return objectMapper.readValue("[]", typeReference);
            } catch (Exception ignored) {
                return null;
            }
        }
    }

    private record SnapshotCommand(String code, String name, String category, Integer score) {
    }
}
