package com.codecoach.module.mockinterview.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.codecoach.common.exception.BusinessException;
import com.codecoach.common.result.PageResult;
import com.codecoach.common.result.ResultCode;
import com.codecoach.module.ai.service.AiTokenStreamHandler;
import com.codecoach.module.agent.runtime.dto.AgentStepRecord;
import com.codecoach.module.agent.runtime.enums.AgentRunStatus;
import com.codecoach.module.agent.runtime.enums.AgentStepStatus;
import com.codecoach.module.agent.runtime.enums.AgentStepType;
import com.codecoach.module.agent.runtime.service.AgentTraceService;
import com.codecoach.module.agent.runtime.support.AgentExecutionContext;
import com.codecoach.module.agent.runtime.support.AgentRuntimeContextHolder;
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
import com.codecoach.module.mockinterview.model.InterviewPlan;
import com.codecoach.module.mockinterview.model.InterviewPlanStage;
import com.codecoach.module.mockinterview.service.MockInterviewSessionService;
import com.codecoach.module.mockinterview.vo.MockInterviewAnswerResponse;
import com.codecoach.module.mockinterview.vo.MockInterviewCreateResponse;
import com.codecoach.module.mockinterview.vo.MockInterviewFinishResponse;
import com.codecoach.module.mockinterview.vo.MockInterviewHistoryVO;
import com.codecoach.module.mockinterview.vo.MockInterviewMessageVO;
import com.codecoach.module.mockinterview.vo.MockInterviewPlanVO;
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
import java.util.UUID;
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
    private static final String AGENT_TYPE = "MOCK_INTERVIEW_PLAN_EXECUTE";
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };
    private static final TypeReference<List<MockInterviewReportVO.StagePerformanceVO>> STAGE_LIST =
            new TypeReference<>() {
            };
    private static final TypeReference<InterviewPlan> PLAN_TYPE = new TypeReference<>() {
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
    private final AgentTraceService agentTraceService;
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
            AgentTraceService agentTraceService,
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
        this.agentTraceService = agentTraceService;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public MockInterviewCreateResponse createSession(MockInterviewCreateRequest request) {
        Long userId = UserContext.getCurrentUserId();
        long startTime = System.currentTimeMillis();
        String runId = beginAgentRun(userId, "create mock interview plan, type=" + (request == null ? null : request.getInterviewType()));
        try {
            Project project = validateProject(userId, request.getProjectId());
            ResumeProfile resume = validateResume(userId, request.getResumeId());
            String type = normalizeType(request.getInterviewType());
            String difficulty = normalizeDifficulty(request.getDifficulty());
            int maxRound = normalizeMaxRound(request.getMaxRound(), type);
            LocalDateTime now = LocalDateTime.now();
            InterviewPlan plan = createInterviewPlan(type, request.getTargetRole().trim(), difficulty, maxRound, project, resume);
            recordStep(runId, AgentStepType.PLAN_CREATE, "Create mock interview plan", null,
                    "type=" + type + ", difficulty=" + difficulty + ", rounds=" + maxRound,
                    "planId=" + plan.getPlanId() + ", activeStages=" + activeStages(plan).size());

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
            session.setPlanId(plan.getPlanId());
            session.setPlanJson(toJson(plan));
            session.setCurrentStage(firstExecutableStage(plan));
            session.setStartedAt(now);
            session.setIsDeleted(NOT_DELETED);
            sessionMapper.insert(session);

            String contextSummary = buildContextSummary(project, resume, plan, session.getCurrentStage());
            recordStep(runId, AgentStepType.CONTEXT_BUILD, "Build opening context", null,
                    "stage=" + session.getCurrentStage() + ", projectLinked=" + (project != null) + ", resumeLinked=" + (resume != null),
                    contextSummary);
            QuestionBuildResult questionResult = buildQuestion(session, plan, project, resume, Collections.emptyList(), "", session.getCurrentStage(), AnswerObservation.opening());
            recordStep(runId, AgentStepType.RAG_RETRIEVE, "Retrieve opening context", "rag",
                    "stage=" + session.getCurrentStage() + ", sources=" + String.join(",", sourceTypesForStage(session.getCurrentStage())),
                    "hit=" + questionResult.ragHit() + ", contextChars=" + safeLength(questionResult.ragContext()));
            recordStep(runId, AgentStepType.QUESTION_GENERATE, "Generate first question", null,
                    "stage=" + session.getCurrentStage() + ", round=1",
                    "questionChars=" + safeLength(questionResult.question()));
            MockInterviewMessage first = newAssistantQuestion(session, 1, session.getCurrentStage(), questionResult.question());
            messageMapper.insert(first);
            recordStep(runId, AgentStepType.RESPONSE_COMPOSE, "Compose opening response", null,
                    "sessionId=" + session.getId(),
                    "firstQuestionReady=true");
            finishAgentRun(runId, AgentRunStatus.SUCCEEDED, "created session " + session.getId(), null, null, startTime);
            return new MockInterviewCreateResponse(session.getId(), toMessageVO(first), toPlanVO(plan));
        } catch (RuntimeException exception) {
            finishAgentRun(runId, AgentRunStatus.FAILED, null, "MOCK_INTERVIEW_CREATE_FAILED", exception.getMessage(), startTime);
            throw exception;
        } finally {
            AgentRuntimeContextHolder.clear();
        }
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
        InterviewPlan plan = readPlan(session);
        StageProgress stageProgress = stageProgress(listMessages(sessionId), session.getCurrentStage(), plan);
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
                stageObjective(plan, session.getCurrentStage()),
                stageProgress.completedRounds(),
                stageProgress.suggestedRounds(),
                toPlanVO(plan),
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
        long startTime = System.currentTimeMillis();
        String runId = beginAgentRun(session.getUserId(), "execute mock interview round, sessionId=" + sessionId);
        if (!STATUS_IN_PROGRESS.equals(session.getStatus())) {
            finishAgentRun(runId, AgentRunStatus.FAILED, null, "MOCK_INTERVIEW_FINISHED", "session already finished", startTime);
            AgentRuntimeContextHolder.clear();
            throw new BusinessException(FINISHED_CODE, "模拟面试已结束");
        }
        try {
            InterviewPlan plan = readPlan(session);
            String answer = request.getAnswer().trim();
            Integer roundNo = session.getCurrentRound();
            LocalDateTime now = LocalDateTime.now();
            AnswerObservation observation = observeAnswer(answer);
            MockInterviewMessage userAnswer = new MockInterviewMessage();
            userAnswer.setSessionId(session.getId());
            userAnswer.setUserId(session.getUserId());
            userAnswer.setRole(ROLE_USER);
            userAnswer.setMessageType(TYPE_USER_ANSWER);
            userAnswer.setStage(session.getCurrentStage());
            userAnswer.setContent(answer);
            userAnswer.setRoundNo(roundNo);
            userAnswer.setScore(observation.score());
            userAnswer.setCreatedAt(now);
            messageMapper.insert(userAnswer);
            recordStep(runId, AgentStepType.ANSWER_OBSERVE, "Observe user answer", null,
                    "stage=" + session.getCurrentStage() + ", round=" + roundNo + ", answerChars=" + safeLength(answer),
                    "quality=" + observation.quality() + ", score=" + observation.score() + ", unknown=" + observation.unknown());

            boolean finished = roundNo >= session.getMaxRound();
            if (finished) {
                MockInterviewFinishResponse finish = finishSessionWithReport(session, now, runId);
                finishAgentRun(runId, AgentRunStatus.SUCCEEDED, "finished session " + session.getId(), null, null, startTime);
                return new MockInterviewAnswerResponse(
                        toMessageVO(userAnswer),
                        null,
                        true,
                        finish.getReportId(),
                        finish.getTotalScore(),
                        session.getCurrentStage(),
                        stageObjective(plan, session.getCurrentStage()),
                        stageProgress(listMessages(sessionId), session.getCurrentStage(), plan).completedRounds(),
                        stageProgress(listMessages(sessionId), session.getCurrentStage(), plan).suggestedRounds()
                );
            }

            List<MockInterviewMessage> history = listMessages(sessionId);
            Project project = session.getProjectId() == null ? null : projectMapper.selectById(session.getProjectId());
            ResumeProfile resume = session.getResumeId() == null ? null : resumeProfileMapper.selectById(session.getResumeId());
            recordStep(runId, AgentStepType.CONTEXT_BUILD, "Build execute context", null,
                    "stage=" + session.getCurrentStage() + ", round=" + roundNo + ", historyMessages=" + history.size(),
                    buildContextSummary(project, resume, plan, session.getCurrentStage()));

            int nextRound = roundNo + 1;
            StageDecision decision = decideNextStage(plan, session, history, observation, nextRound);
            recordStep(runId, AgentStepType.STAGE_ADJUST, "Adjust interview stage", null,
                    "currentStage=" + session.getCurrentStage() + ", quality=" + observation.quality(),
                    "nextStage=" + decision.nextStage() + ", reason=" + decision.reason());

            QuestionBuildResult questionResult = buildQuestion(session, plan, project, resume, history, answer, decision.nextStage(), observation);
            recordStep(runId, AgentStepType.RAG_RETRIEVE, "Retrieve stage RAG context", "rag",
                    "stage=" + decision.nextStage() + ", sources=" + String.join(",", sourceTypesForStage(decision.nextStage())),
                    "hit=" + questionResult.ragHit() + ", contextChars=" + safeLength(questionResult.ragContext()));
            recordStep(runId, AgentStepType.QUESTION_GENERATE, "Generate next question", null,
                    "stage=" + decision.nextStage() + ", round=" + nextRound + ", historyAnswers=" + countAnswers(history),
                    "questionChars=" + safeLength(questionResult.question()));
            streamText(streamHandler, questionResult.question());
            MockInterviewMessage nextQuestion = newAssistantQuestion(session, nextRound, decision.nextStage(), questionResult.question());
            messageMapper.insert(nextQuestion);

            session.setCurrentRound(nextRound);
            session.setCurrentStage(decision.nextStage());
            sessionMapper.updateById(session);
            StageProgress nextProgress = stageProgress(listMessages(sessionId), decision.nextStage(), plan);
            recordStep(runId, AgentStepType.RESPONSE_COMPOSE, "Compose streamed response", null,
                    "stage=" + decision.nextStage() + ", round=" + nextRound,
                    "nextQuestionReady=true");
            finishAgentRun(runId, AgentRunStatus.SUCCEEDED, "next question ready", null, null, startTime);
            return new MockInterviewAnswerResponse(
                    toMessageVO(userAnswer),
                    toMessageVO(nextQuestion),
                    false,
                    null,
                    null,
                    decision.nextStage(),
                    stageObjective(plan, decision.nextStage()),
                    nextProgress.completedRounds(),
                    nextProgress.suggestedRounds()
            );
        } catch (RuntimeException exception) {
            finishAgentRun(runId, AgentRunStatus.FAILED, null, "MOCK_INTERVIEW_EXECUTE_FAILED", exception.getMessage(), startTime);
            throw exception;
        } finally {
            AgentRuntimeContextHolder.clear();
        }
    }

    @Override
    @Transactional
    public MockInterviewFinishResponse finishSession(Long sessionId) {
        MockInterviewSession session = requireOwnedSession(sessionId);
        long startTime = System.currentTimeMillis();
        String runId = beginAgentRun(session.getUserId(), "finish mock interview, sessionId=" + sessionId);
        MockInterviewReport existing = getReportBySessionId(sessionId);
        try {
            if (existing != null) {
                finishAgentRun(runId, AgentRunStatus.SUCCEEDED, "existing report " + existing.getId(), null, null, startTime);
                return new MockInterviewFinishResponse(existing.getId(), sessionId, existing.getTotalScore());
            }
            MockInterviewFinishResponse response = finishSessionWithReport(session, LocalDateTime.now(), runId);
            finishAgentRun(runId, AgentRunStatus.SUCCEEDED, "generated report " + response.getReportId(), null, null, startTime);
            return response;
        } catch (RuntimeException exception) {
            finishAgentRun(runId, AgentRunStatus.FAILED, null, "MOCK_INTERVIEW_REPORT_FAILED", exception.getMessage(), startTime);
            throw exception;
        } finally {
            AgentRuntimeContextHolder.clear();
        }
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

    private MockInterviewFinishResponse finishSessionWithReport(MockInterviewSession session, LocalDateTime now, String runId) {
        MockInterviewReport existing = getReportBySessionId(session.getId());
        if (existing != null) {
            markFinished(session, existing.getTotalScore(), now);
            return new MockInterviewFinishResponse(existing.getId(), session.getId(), existing.getTotalScore());
        }
        List<MockInterviewMessage> messages = listMessages(session.getId());
        InterviewPlan plan = readPlan(session);
        recordStep(runId, AgentStepType.REPORT_GENERATE, "Generate mock interview report", null,
                "sessionId=" + session.getId() + ", answerCount=" + countAnswers(messages),
                "planId=" + (plan == null ? null : plan.getPlanId()) + ", stages=" + (plan == null ? 0 : activeStages(plan).size()));
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
        report.setSummary(buildSummary(totalScore, quality, plan, messages));
        report.setStagePerformances(toJson(buildStagePerformances(messages, session, plan)));
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

    private InterviewPlan createInterviewPlan(
            String type,
            String targetRole,
            String difficulty,
            int totalRounds,
            Project project,
            ResumeProfile resume
    ) {
        InterviewPlan plan = new InterviewPlan();
        plan.setPlanId("plan-" + UUID.randomUUID());
        plan.setInterviewType(type);
        plan.setTargetRole(targetRole);
        plan.setDifficulty(difficulty);
        plan.setTotalRounds(totalRounds);
        Map<String, Integer> allocation = allocateStageRounds(type, totalRounds);
        plan.setStages(List.of(
                stage("OPENING", "开场自我介绍", "确认候选人背景、目标岗位、表达结构和最核心项目线索。", allocation.get("OPENING"),
                        List.of("自我介绍结构", "目标岗位匹配", "核心项目线索"), List.of("简历摘要", "项目摘要"), List.of("表达结构", "岗位匹配", "项目选择")),
                stage("RESUME_PROJECT", "简历项目追问", "围绕简历和项目经历确认真实性、个人贡献和可追问风险。", allocation.get("RESUME_PROJECT"),
                        List.of("项目背景", "个人职责", "风险点", "量化结果"), List.of("简历", "用户文档", "项目档案"), List.of("项目可信度", "贡献清晰度", "风险识别")),
                stage("TECHNICAL_FUNDAMENTAL", "技术基础", "验证 Java 后端核心技术原理、边界和实际使用能力。", allocation.get("TECHNICAL_FUNDAMENTAL"),
                        List.of("Java/JUC", "JVM", "MySQL", "Redis", "Spring"), List.of("知识文章", "能力画像薄弱点"), List.of("原理准确性", "边界意识", "追问承压")),
                stage("PROJECT_DEEP_DIVE", "项目深挖", "深挖核心链路、数据一致性、异常补偿、性能指标和工程取舍。", allocation.get("PROJECT_DEEP_DIVE"),
                        List.of("核心链路", "异常补偿", "数据一致性", "性能瓶颈"), List.of("项目档案", "项目文档", "简历项目经历"), List.of("链路完整度", "工程落地", "指标意识")),
                stage("SCENARIO_DESIGN", "场景设计", "用真实工程场景考察系统设计、容量保护、降级和权衡能力。", allocation.get("SCENARIO_DESIGN"),
                        List.of("高并发", "限流降级", "缓存", "异步化", "数据库保护"), List.of("项目上下文", "知识文章"), List.of("方案完整性", "取舍解释", "风险控制")),
                stage("WRAP_UP", "总结反问", "观察候选人的复盘能力、自我认知和对团队工程实践的关注。", allocation.get("WRAP_UP"),
                        List.of("自我复盘", "反问质量", "行动计划"), List.of("本场面试摘要"), List.of("复盘能力", "沟通成熟度", "下一步意识"))
        ));
        personalizePlan(plan, project, resume);
        return plan;
    }

    private InterviewPlanStage stage(
            String stage,
            String stageName,
            String objective,
            Integer suggestedRounds,
            List<String> focusPoints,
            List<String> ragSources,
            List<String> scoringDimensions
    ) {
        InterviewPlanStage item = new InterviewPlanStage();
        item.setStage(stage);
        item.setStageName(stageName);
        item.setObjective(objective);
        item.setSuggestedRounds(suggestedRounds == null ? 0 : suggestedRounds);
        item.setFocusPoints(focusPoints);
        item.setRagSources(ragSources);
        item.setScoringDimensions(scoringDimensions);
        return item;
    }

    private Map<String, Integer> allocateStageRounds(String type, int totalRounds) {
        Map<String, Integer> allocation = new LinkedHashMap<>();
        allocation.put("OPENING", 1);
        allocation.put("RESUME_PROJECT", 1);
        allocation.put("TECHNICAL_FUNDAMENTAL", 1);
        allocation.put("PROJECT_DEEP_DIVE", 1);
        allocation.put("SCENARIO_DESIGN", 1);
        allocation.put("WRAP_UP", 1);
        int remaining = totalRounds - allocation.values().stream().mapToInt(Integer::intValue).sum();
        List<String> priority = switch (type) {
            case "RESUME_PROJECT_DEEP_DIVE" -> List.of("RESUME_PROJECT", "PROJECT_DEEP_DIVE", "RESUME_PROJECT", "PROJECT_DEEP_DIVE", "SCENARIO_DESIGN", "TECHNICAL_FUNDAMENTAL");
            case "BA_GU_COMPREHENSIVE" -> List.of("TECHNICAL_FUNDAMENTAL", "TECHNICAL_FUNDAMENTAL", "SCENARIO_DESIGN", "PROJECT_DEEP_DIVE", "RESUME_PROJECT");
            default -> List.of("RESUME_PROJECT", "TECHNICAL_FUNDAMENTAL", "PROJECT_DEEP_DIVE", "SCENARIO_DESIGN", "PROJECT_DEEP_DIVE", "TECHNICAL_FUNDAMENTAL");
        };
        int index = 0;
        while (remaining > 0 && !priority.isEmpty()) {
            String stage = priority.get(index % priority.size());
            allocation.put(stage, allocation.get(stage) + 1);
            remaining--;
            index++;
        }
        return allocation;
    }

    private void personalizePlan(InterviewPlan plan, Project project, ResumeProfile resume) {
        if (plan == null || plan.getStages() == null) {
            return;
        }
        String projectName = project == null ? null : project.getName();
        String resumeTitle = resume == null ? null : resume.getTitle();
        for (InterviewPlanStage stage : plan.getStages()) {
            if ("RESUME_PROJECT".equals(stage.getStage()) && StringUtils.hasText(resumeTitle)) {
                stage.setObjective(stage.getObjective() + " 当前简历：" + resumeTitle + "。");
            }
            if (("PROJECT_DEEP_DIVE".equals(stage.getStage()) || "SCENARIO_DESIGN".equals(stage.getStage()))
                    && StringUtils.hasText(projectName)) {
                stage.setObjective(stage.getObjective() + " 重点项目：" + projectName + "。");
            }
        }
    }

    private StageDecision decideNextStage(
            InterviewPlan plan,
            MockInterviewSession session,
            List<MockInterviewMessage> history,
            AnswerObservation observation,
            int nextRound
    ) {
        String currentStage = session.getCurrentStage();
        int completedInCurrent = countStageAnswers(history, currentStage);
        int suggested = Math.max(1, suggestedRounds(plan, currentStage));
        if (observation.weak() && !"WRAP_UP".equals(currentStage)
                && canSpendFollowUp(plan, currentStage, nextRound, session.getMaxRound())) {
            return new StageDecision(currentStage, observation.unknown() ? "UNKNOWN_SWITCH_ANGLE" : "WEAK_ANSWER_FOLLOW_UP");
        }
        if (completedInCurrent < suggested && !"WRAP_UP".equals(currentStage)) {
            return new StageDecision(currentStage, "PLAN_STAGE_REMAINING");
        }
        String nextStage = nextExecutableStage(plan, currentStage);
        return new StageDecision(nextStage == null ? "WRAP_UP" : nextStage, "STAGE_OBJECTIVE_COMPLETED_OR_ROUND_LIMIT");
    }

    private boolean canSpendFollowUp(InterviewPlan plan, String currentStage, int nextRound, int maxRound) {
        int futureRoundsAfterThisQuestion = Math.max(0, maxRound - nextRound);
        int pendingFutureStages = pendingStagesAfter(plan, currentStage);
        return futureRoundsAfterThisQuestion >= pendingFutureStages;
    }

    private String firstExecutableStage(InterviewPlan plan) {
        return activeStages(plan).stream().findFirst().orElse("OPENING");
    }

    private String nextExecutableStage(InterviewPlan plan, String currentStage) {
        List<String> active = activeStages(plan);
        int index = active.indexOf(currentStage);
        if (index < 0) {
            return active.stream().findFirst().orElse("WRAP_UP");
        }
        if (index + 1 >= active.size()) {
            return "WRAP_UP";
        }
        return active.get(index + 1);
    }

    private List<String> activeStages(InterviewPlan plan) {
        if (plan == null || plan.getStages() == null) {
            return List.of("OPENING", "RESUME_PROJECT", "TECHNICAL_FUNDAMENTAL", "PROJECT_DEEP_DIVE", "SCENARIO_DESIGN", "WRAP_UP");
        }
        return plan.getStages().stream()
                .filter(stage -> stage.getSuggestedRounds() != null && stage.getSuggestedRounds() > 0)
                .map(InterviewPlanStage::getStage)
                .toList();
    }

    private int pendingStagesAfter(InterviewPlan plan, String currentStage) {
        List<String> active = activeStages(plan);
        int index = active.indexOf(currentStage);
        if (index < 0 || index + 1 >= active.size()) {
            return 0;
        }
        return active.size() - index - 1;
    }

    private QuestionBuildResult buildQuestion(
            MockInterviewSession session,
            InterviewPlan plan,
            Project project,
            ResumeProfile resume,
            List<MockInterviewMessage> history,
            String latestAnswer,
            String stage,
            AnswerObservation observation
    ) {
        InterviewPlanStage planStage = findStage(plan, stage);
        String projectName = project == null ? "你最熟悉的一个 Java 后端项目" : "「" + project.getName() + "」";
        String resumeHint = resume == null ? "如果没有简历材料，就按你真实经历回答" : "结合你简历中的项目经历";
        String ragHint = retrieveRagContext(session, stage, project, resume, latestAnswer);
        String pressure = observation != null && observation.weak()
                ? (observation.unknown() ? "你刚才表示不确定，我会换一个更基础的角度确认。" : "你刚才的回答偏浅，我会继续追问真实掌握程度。")
                : "";
        String planGoal = planStage == null ? "" : "本阶段我关注的是：" + planStage.getObjective() + "。";
        String historyHint = buildHistoryHint(history);
        String abilityHint = "TECHNICAL_FUNDAMENTAL".equals(stage) ? buildAbilityWeaknessHint(session.getUserId()) : "";
        String question = switch (stage) {
            case "OPENING" -> "我们开始一场 Java 后端技术一面。请你先做一个 1 到 2 分钟自我介绍，重点讲目标岗位、技术栈和最能代表你的项目。";
            case "RESUME_PROJECT" -> pressure + resumeHint + "，请你介绍一个最核心的项目经历：项目解决什么业务问题，你负责哪些模块，你的个人贡献是什么？";
            case "TECHNICAL_FUNDAMENTAL" -> pressure + "从 Java 后端基础来看，请你结合 " + projectName + " 讲一个你实际用过或必须掌握的技术点，例如并发、JVM、MySQL、Redis 或 Spring，并说明它的原理、适用边界和常见风险。";
            case "PROJECT_DEEP_DIVE" -> pressure + "我们深挖 " + projectName + "。请你选一个核心链路，说明请求流转、数据一致性、异常补偿和性能瓶颈分别怎么处理。";
            case "SCENARIO_DESIGN" -> pressure + "假设 " + projectName + " 的核心接口突然流量涨到平时 10 倍，你会怎样设计限流、缓存、异步化、数据库保护和降级方案？";
            default -> "最后一个问题：请你总结本场面试里自己最有把握和最没把握的点，并向面试官反问一个和团队工程实践相关的问题。";
        };
        if (StringUtils.hasText(planGoal) && !"OPENING".equals(stage)) {
            question = planGoal + question;
        }
        if (StringUtils.hasText(historyHint) && !"OPENING".equals(stage)) {
            question = question + "\n\n基于前面回答，我会顺着这个线索问：" + historyHint;
        }
        if (StringUtils.hasText(abilityHint)) {
            question = question + "\n\n结合你最近的能力画像，我会优先确认：" + abilityHint + "。";
        }
        if (StringUtils.hasText(ragHint)) {
            return new QuestionBuildResult(question + "\n\n我会参考你已有材料中的线索继续追问，例如："
                    + summarizeContext(ragHint)
                    + "。请不要复述材料，请用自己的话回答。", ragHint, true);
        }
        return new QuestionBuildResult(question, ragHint, false);
    }

    private String retrieveRagContext(MockInterviewSession session, String stage, Project project, ResumeProfile resume, String answer) {
        if (ragProperties == null || !Boolean.TRUE.equals(ragProperties.getEnabled())) {
            return "";
        }
        List<String> sourceTypes = sourceTypesForStage(stage);
        if (sourceTypes.isEmpty()) {
            return "";
        }
        try {
            RagSearchRequest request = new RagSearchRequest();
            request.setQuery(String.join(" ", session.getTargetRole(), stage, nullToEmpty(project == null ? null : project.getName()), nullToEmpty(answer)));
            request.setTopK(4);
            request.setSourceTypes(sourceTypes);
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
            case "WRAP_UP" -> List.of();
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
        if (isUnknownAnswer(answer)) {
            return 5;
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
            MockInterviewSession session,
            InterviewPlan plan
    ) {
        Map<String, List<MockInterviewMessage>> grouped = messages.stream()
                .filter(message -> TYPE_USER_ANSWER.equals(message.getMessageType()))
                .collect(Collectors.groupingBy(MockInterviewMessage::getStage, LinkedHashMap::new, Collectors.toList()));
        List<String> stages = plan == null ? List.of("OPENING", "RESUME_PROJECT", "TECHNICAL_FUNDAMENTAL", "PROJECT_DEEP_DIVE", "SCENARIO_DESIGN", "WRAP_UP")
                : plan.getStages().stream().map(InterviewPlanStage::getStage).toList();
        List<MockInterviewReportVO.StagePerformanceVO> result = new ArrayList<>();
        for (String stage : stages) {
            List<MockInterviewMessage> stageAnswers = grouped.getOrDefault(stage, List.of());
            int score = stageAnswers.isEmpty() ? 0 : (int) Math.round(stageAnswers.stream().map(MockInterviewMessage::getScore).filter(v -> v != null).mapToInt(Integer::intValue).average().orElse(0));
            int suggestedRounds = suggestedRounds(plan, stage);
            if (!stageAnswers.isEmpty() || suggestedRounds > 0 || "COMPREHENSIVE_TECHNICAL".equals(session.getInterviewType())) {
                int followUpCount = Math.max(0, stageAnswers.size() - Math.min(1, suggestedRounds));
                result.add(new MockInterviewReportVO.StagePerformanceVO(
                        stage,
                        stageName(stage),
                        score,
                        stageComment(stage, score, stageAnswers.size()),
                        suggestedRounds,
                        stageAnswers.size(),
                        completionStatus(stageAnswers.size(), suggestedRounds),
                        followUpCount,
                        stageDeductionReasons(stage, score, stageAnswers.size())
                ));
            }
        }
        return result;
    }

    private String completionStatus(int completedRounds, int suggestedRounds) {
        if (completedRounds == 0) {
            return "NOT_STARTED";
        }
        if (suggestedRounds > 0 && completedRounds > suggestedRounds) {
            return "FOLLOWED_UP";
        }
        if (suggestedRounds > 0 && completedRounds < suggestedRounds) {
            return "EARLY_ENDED";
        }
        return "COMPLETED";
    }

    private List<String> stageDeductionReasons(String stage, int score, int count) {
        if (count == 0) {
            return List.of("本阶段回答样本不足，报告不做高置信判断。");
        }
        List<String> reasons = new ArrayList<>();
        if (score < 40) {
            reasons.add("回答有效信息不足，缺少可验证细节。");
        } else if (score < 70) {
            reasons.add("回答有基础，但结构、例子或工程取舍不足。");
        }
        if ("PROJECT_DEEP_DIVE".equals(stage) && score < 70) {
            reasons.add("项目链路、异常补偿或性能指标不够具体。");
        }
        if ("TECHNICAL_FUNDAMENTAL".equals(stage) && score < 70) {
            reasons.add("技术原理和适用边界需要补强。");
        }
        if ("SCENARIO_DESIGN".equals(stage) && score < 70) {
            reasons.add("场景设计需要补充容量、降级和一致性权衡。");
        }
        return reasons.isEmpty() ? List.of("本阶段未发现明显扣分项。") : reasons;
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

    private String buildSummary(int score, ReportQualityAssessment quality, InterviewPlan plan, List<MockInterviewMessage> messages) {
        String planPrefix = plan == null
                ? "本场面试按多阶段技术面流程推进。"
                : "本场面试按 Plan-Execute 计划推进，覆盖" + planCoverage(plan) + "。";
        String riskiestStage = riskiestStage(messages);
        if (quality.isLowConfidence()) {
            return planPrefix + "样本不足或回答质量偏低，系统已按低质量回答规则限制分数。暴露最多问题的阶段：" + riskiestStage + "。当前更适合把它作为诊断入口，而不是稳定能力结论。";
        }
        if (score >= 80) {
            return planPrefix + "整体表现较稳，能够撑住多阶段追问。暴露最多问题的阶段：" + riskiestStage + "。下一步建议加强量化指标和更高压的场景设计追问。";
        }
        if (score >= 60) {
            return planPrefix + "有基础支撑，但在项目深挖、技术原理和工程权衡之间切换还不够顺滑。暴露最多问题的阶段：" + riskiestStage + "。";
        }
        return planPrefix + "暴露出较明显风险，回答容易停留在关键词和经历描述。暴露最多问题的阶段：" + riskiestStage + "，需要优先补齐结构化表达和核心原理。";
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

    private AnswerObservation observeAnswer(String answer) {
        int score = scoreAnswer(answer);
        boolean unknown = isUnknownAnswer(answer);
        boolean weak = score < 50;
        String quality;
        if (unknown) {
            quality = "UNKNOWN";
        } else if (score < 30) {
            quality = "VERY_WEAK";
        } else if (score < 50) {
            quality = "SHALLOW";
        } else if (score < 70) {
            quality = "BASIC";
        } else if (score < 85) {
            quality = "GOOD";
        } else {
            quality = "EXCELLENT";
        }
        return new AnswerObservation(score, quality, unknown, weak);
    }

    private boolean isUnknownAnswer(String answer) {
        if (!StringUtils.hasText(answer)) {
            return true;
        }
        String normalized = answer.trim().toLowerCase();
        return normalized.length() <= 20 && (normalized.contains("不知道")
                || normalized.contains("不清楚")
                || normalized.contains("不会")
                || normalized.contains("没了解")
                || normalized.contains("不了解")
                || normalized.contains("不太会")
                || normalized.contains("i don't know")
                || normalized.contains("dont know"));
    }

    private InterviewPlan readPlan(MockInterviewSession session) {
        if (session != null && StringUtils.hasText(session.getPlanJson())) {
            try {
                return objectMapper.readValue(session.getPlanJson(), PLAN_TYPE);
            } catch (Exception exception) {
                log.warn("Failed to parse mock interview plan, sessionId={}", session.getId());
            }
        }
        if (session == null) {
            return null;
        }
        return createInterviewPlan(
                normalizeType(session.getInterviewType()),
                session.getTargetRole(),
                normalizeDifficulty(session.getDifficulty()),
                normalizeMaxRound(session.getMaxRound(), session.getInterviewType()),
                null,
                null
        );
    }

    private MockInterviewPlanVO toPlanVO(InterviewPlan plan) {
        if (plan == null) {
            return null;
        }
        List<MockInterviewPlanVO.StageVO> stages = plan.getStages() == null ? List.of() : plan.getStages().stream()
                .map(stage -> new MockInterviewPlanVO.StageVO(
                        stage.getStage(),
                        stage.getStageName(),
                        stage.getObjective(),
                        stage.getSuggestedRounds(),
                        stage.getFocusPoints(),
                        stage.getRagSources(),
                        stage.getScoringDimensions()
                ))
                .toList();
        return new MockInterviewPlanVO(plan.getPlanId(), plan.getTotalRounds(), planCoverage(plan), stages);
    }

    private InterviewPlanStage findStage(InterviewPlan plan, String stage) {
        if (plan == null || plan.getStages() == null) {
            return null;
        }
        return plan.getStages().stream()
                .filter(item -> stage.equals(item.getStage()))
                .findFirst()
                .orElse(null);
    }

    private int suggestedRounds(InterviewPlan plan, String stage) {
        InterviewPlanStage planStage = findStage(plan, stage);
        return planStage == null || planStage.getSuggestedRounds() == null ? 0 : planStage.getSuggestedRounds();
    }

    private String stageObjective(InterviewPlan plan, String stage) {
        InterviewPlanStage planStage = findStage(plan, stage);
        return planStage == null ? stageName(stage) : planStage.getObjective();
    }

    private StageProgress stageProgress(List<MockInterviewMessage> messages, String stage, InterviewPlan plan) {
        return new StageProgress(countStageAnswers(messages, stage), suggestedRounds(plan, stage));
    }

    private int countStageAnswers(List<MockInterviewMessage> messages, String stage) {
        if (messages == null) {
            return 0;
        }
        return (int) messages.stream()
                .filter(message -> TYPE_USER_ANSWER.equals(message.getMessageType()))
                .filter(message -> stage.equals(message.getStage()))
                .count();
    }

    private int countAnswers(List<MockInterviewMessage> messages) {
        if (messages == null) {
            return 0;
        }
        return (int) messages.stream()
                .filter(message -> TYPE_USER_ANSWER.equals(message.getMessageType()))
                .count();
    }

    private String planCoverage(InterviewPlan plan) {
        if (plan == null || plan.getStages() == null) {
            return "开场、简历项目、技术基础、项目深挖、场景设计、总结";
        }
        return plan.getStages().stream()
                .filter(stage -> stage.getSuggestedRounds() != null && stage.getSuggestedRounds() > 0)
                .map(InterviewPlanStage::getStageName)
                .collect(Collectors.joining("、"));
    }

    private String riskiestStage(List<MockInterviewMessage> messages) {
        return List.of("RESUME_PROJECT", "TECHNICAL_FUNDAMENTAL", "PROJECT_DEEP_DIVE", "SCENARIO_DESIGN", "OPENING", "WRAP_UP").stream()
                .filter(stage -> countStageAnswers(messages, stage) > 0)
                .min((left, right) -> Integer.compare(averageStage(messages, left), averageStage(messages, right)))
                .map(this::stageName)
                .orElse("样本不足");
    }

    private String buildHistoryHint(List<MockInterviewMessage> history) {
        if (history == null || history.isEmpty()) {
            return "";
        }
        return history.stream()
                .filter(message -> TYPE_USER_ANSWER.equals(message.getMessageType()))
                .reduce((first, second) -> second)
                .map(message -> {
                    if (message.getScore() == null || message.getScore() < 50) {
                        return "刚才的回答还缺少细节，请补充一个真实例子或关键指标。";
                    }
                    return "刚才已经有基础描述，请进一步讲清楚原理、边界和取舍。";
                })
                .orElse("");
    }

    private String buildContextSummary(Project project, ResumeProfile resume, InterviewPlan plan, String stage) {
        return "stage=" + stage
                + ", plan=" + (plan == null ? "fallback" : plan.getPlanId())
                + ", projectLinked=" + (project != null)
                + ", resumeLinked=" + (resume != null)
                + ", objective=" + stageObjective(plan, stage);
    }

    private String buildAbilityWeaknessHint(Long userId) {
        if (userId == null) {
            return "";
        }
        try {
            return abilitySnapshotMapper.selectList(new LambdaQueryWrapper<UserAbilitySnapshot>()
                            .eq(UserAbilitySnapshot::getUserId, userId)
                            .orderByDesc(UserAbilitySnapshot::getCreatedAt)
                            .last("LIMIT 5"))
                    .stream()
                    .filter(snapshot -> snapshot.getScore() == null || snapshot.getScore() < 70)
                    .map(UserAbilitySnapshot::getDimensionName)
                    .filter(StringUtils::hasText)
                    .distinct()
                    .limit(3)
                    .collect(Collectors.joining("、"));
        } catch (RuntimeException exception) {
            log.warn("Failed to read ability hints for mock interview, userId={}", userId);
            return "";
        }
    }

    private int safeLength(String value) {
        return value == null ? 0 : value.length();
    }

    private String beginAgentRun(Long userId, String inputSummary) {
        String runId = UUID.randomUUID().toString();
        String traceId = UUID.randomUUID().toString();
        AgentRuntimeContextHolder.set(new AgentExecutionContext(runId, traceId, userId, AGENT_TYPE));
        try {
            agentTraceService.createRun(runId, traceId, userId, AGENT_TYPE, inputSummary);
        } catch (RuntimeException exception) {
            log.warn("Failed to create mock interview agent run, runId={}", runId);
        }
        return runId;
    }

    private void finishAgentRun(
            String runId,
            AgentRunStatus status,
            String outputSummary,
            String errorCode,
            String errorMessage,
            long startTime
    ) {
        try {
            agentTraceService.finishRun(runId, status, outputSummary, errorCode, errorMessage, System.currentTimeMillis() - startTime);
        } catch (RuntimeException exception) {
            log.warn("Failed to finish mock interview agent run, runId={}", runId);
        }
    }

    private void recordStep(
            String runId,
            AgentStepType type,
            String stepName,
            String toolName,
            String inputSummary,
            String outputSummary
    ) {
        if (!StringUtils.hasText(runId)) {
            return;
        }
        AgentStepRecord record = new AgentStepRecord();
        record.setStepType(type);
        record.setStepName(stepName);
        record.setToolName(toolName);
        record.setInputSummary(inputSummary);
        record.setOutputSummary(outputSummary);
        record.setStatus(AgentStepStatus.SUCCEEDED);
        record.setLatencyMs(0);
        agentTraceService.recordStep(runId, record);
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
        InterviewPlan plan = readPlan(session);
        return new MockInterviewReportVO(
                report.getId(),
                report.getSessionId(),
                session.getInterviewType(),
                session.getTargetRole(),
                session.getDifficulty(),
                report.getTotalScore(),
                report.getSampleSufficiency(),
                report.getSummary(),
                toPlanVO(plan),
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
        return Math.max(6, Math.min(12, value));
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

    private record QuestionBuildResult(String question, String ragContext, boolean ragHit) {
    }

    private record AnswerObservation(Integer score, String quality, boolean unknown, boolean weak) {
        private static AnswerObservation opening() {
            return new AnswerObservation(0, "OPENING", false, false);
        }
    }

    private record StageDecision(String nextStage, String reason) {
    }

    private record StageProgress(Integer completedRounds, Integer suggestedRounds) {
    }
}
