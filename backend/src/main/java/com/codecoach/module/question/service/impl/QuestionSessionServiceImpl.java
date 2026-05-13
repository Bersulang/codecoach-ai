package com.codecoach.module.question.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.codecoach.common.concurrency.SingleFlightService;
import com.codecoach.common.exception.BusinessException;
import com.codecoach.common.result.PageResult;
import com.codecoach.common.result.ResultCode;
import com.codecoach.module.ai.model.QuestionFeedbackResult;
import com.codecoach.module.ai.model.QuestionPracticeContext;
import com.codecoach.module.ai.model.QuestionReportGenerateResult;
import com.codecoach.module.ai.service.AiQuestionPracticeService;
import com.codecoach.module.ai.service.AiTokenStreamHandler;
import com.codecoach.module.insight.service.UserAbilitySnapshotService;
import com.codecoach.module.knowledge.entity.KnowledgeTopic;
import com.codecoach.module.knowledge.mapper.KnowledgeTopicMapper;
import com.codecoach.module.memory.service.UserMemoryService;
import com.codecoach.module.question.dto.QuestionAnswerRequest;
import com.codecoach.module.question.dto.QuestionSessionCreateRequest;
import com.codecoach.module.question.dto.QuestionSessionPageRequest;
import com.codecoach.module.question.entity.QuestionTrainingMessage;
import com.codecoach.module.question.entity.QuestionTrainingReport;
import com.codecoach.module.question.entity.QuestionTrainingSession;
import com.codecoach.module.question.mapper.QuestionTrainingMessageMapper;
import com.codecoach.module.question.mapper.QuestionTrainingReportMapper;
import com.codecoach.module.question.mapper.QuestionTrainingSessionMapper;
import com.codecoach.module.question.service.QuestionSessionService;
import com.codecoach.module.question.vo.QuestionAnswerResponse;
import com.codecoach.module.question.vo.QuestionFinishResponse;
import com.codecoach.module.question.vo.QuestionMessageVO;
import com.codecoach.module.question.vo.QuestionSessionCreateResponse;
import com.codecoach.module.question.vo.QuestionSessionDetailVO;
import com.codecoach.module.question.vo.QuestionSessionHistoryVO;
import com.codecoach.module.rag.config.RagProperties;
import com.codecoach.module.rag.constant.RagConstants;
import com.codecoach.module.rag.model.RagRetrievedChunk;
import com.codecoach.module.rag.model.RagSearchRequest;
import com.codecoach.module.rag.model.RagSearchResponse;
import com.codecoach.module.rag.service.RagRetrievalService;
import com.codecoach.module.report.quality.ReportQualityAssessment;
import com.codecoach.module.report.quality.ReportQualityPostProcessor;
import com.codecoach.security.UserContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

@Service
public class QuestionSessionServiceImpl implements QuestionSessionService {

    private static final Logger log = LoggerFactory.getLogger(QuestionSessionServiceImpl.class);

    private static final int TOPIC_NOT_FOUND_CODE = 5001;

    private static final int SESSION_NOT_FOUND_CODE = 5002;

    private static final int SESSION_ENDED_CODE = 5003;

    private static final int AI_CALL_FAILED_CODE = 3003;

    private static final int ANSWER_PROCESSING_CODE = 3004;

    private static final String STATUS_ENABLED = "ENABLED";

    private static final String STATUS_IN_PROGRESS = "IN_PROGRESS";

    private static final String STATUS_FINISHED = "FINISHED";

    private static final String ROLE_USER = "USER";

    private static final String ROLE_ASSISTANT = "ASSISTANT";

    private static final String MESSAGE_TYPE_AI_QUESTION = "AI_QUESTION";

    private static final String MESSAGE_TYPE_USER_ANSWER = "USER_ANSWER";

    private static final String MESSAGE_TYPE_AI_FEEDBACK = "AI_FEEDBACK";

    private static final String MESSAGE_TYPE_AI_REFERENCE_ANSWER = "AI_REFERENCE_ANSWER";

    private static final String MESSAGE_TYPE_AI_FOLLOW_UP = "AI_FOLLOW_UP";

    private static final String DIFFICULTY_EASY = "EASY";

    private static final String DIFFICULTY_NORMAL = "NORMAL";

    private static final String DIFFICULTY_HARD = "HARD";

    private static final int FIRST_ROUND_NO = 1;

    private static final int MAX_ROUND = 5;

    private static final int NOT_DELETED = 0;

    private static final int DELETED = 1;

    private static final long DEFAULT_PAGE_NUM = 1L;

    private static final long DEFAULT_PAGE_SIZE = 10L;

    private static final long MAX_PAGE_SIZE = 100L;

    private static final Duration ANSWER_LOCK_TTL = Duration.ofSeconds(120);

    private static final String ANSWER_LOCK_KEY_PREFIX = "question:answer:lock:";

    private static final Duration REPORT_LOCK_TTL = Duration.ofMinutes(15);
    private static final Duration REPORT_CACHE_TTL = Duration.ofMinutes(10);

    private static final int RAG_ANSWER_TRUNCATE_LENGTH = 1000;

    private static final int RAG_QUESTION_TRUNCATE_LENGTH = 800;

    private static final int RAG_HISTORY_TRUNCATE_LENGTH = 1500;

    private static final DefaultRedisScript<Long> RELEASE_LOCK_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then "
                    + "return redis.call('del', KEYS[1]) "
                    + "else return 0 end",
            Long.class
    );

    private final KnowledgeTopicMapper knowledgeTopicMapper;

    private final QuestionTrainingSessionMapper questionTrainingSessionMapper;

    private final QuestionTrainingMessageMapper questionTrainingMessageMapper;

    private final QuestionTrainingReportMapper questionTrainingReportMapper;

    private final AiQuestionPracticeService aiQuestionPracticeService;

    private final ObjectMapper objectMapper;

    private final StringRedisTemplate stringRedisTemplate;

    private final TransactionTemplate transactionTemplate;

    private final UserAbilitySnapshotService userAbilitySnapshotService;

    private final RagRetrievalService ragRetrievalService;

    private final RagProperties ragProperties;

    private final ReportQualityPostProcessor reportQualityPostProcessor;
    private final UserMemoryService userMemoryService;
    private final SingleFlightService singleFlightService;

    public QuestionSessionServiceImpl(
            KnowledgeTopicMapper knowledgeTopicMapper,
            QuestionTrainingSessionMapper questionTrainingSessionMapper,
            QuestionTrainingMessageMapper questionTrainingMessageMapper,
            QuestionTrainingReportMapper questionTrainingReportMapper,
            AiQuestionPracticeService aiQuestionPracticeService,
            ObjectMapper objectMapper,
            StringRedisTemplate stringRedisTemplate,
            TransactionTemplate transactionTemplate,
            UserAbilitySnapshotService userAbilitySnapshotService,
            RagRetrievalService ragRetrievalService,
            RagProperties ragProperties,
            ReportQualityPostProcessor reportQualityPostProcessor,
            UserMemoryService userMemoryService,
            SingleFlightService singleFlightService
    ) {
        this.knowledgeTopicMapper = knowledgeTopicMapper;
        this.questionTrainingSessionMapper = questionTrainingSessionMapper;
        this.questionTrainingMessageMapper = questionTrainingMessageMapper;
        this.questionTrainingReportMapper = questionTrainingReportMapper;
        this.aiQuestionPracticeService = aiQuestionPracticeService;
        this.objectMapper = objectMapper;
        this.stringRedisTemplate = stringRedisTemplate;
        this.transactionTemplate = transactionTemplate;
        this.userAbilitySnapshotService = userAbilitySnapshotService;
        this.ragRetrievalService = ragRetrievalService;
        this.ragProperties = ragProperties;
        this.reportQualityPostProcessor = reportQualityPostProcessor;
        this.userMemoryService = userMemoryService;
        this.singleFlightService = singleFlightService;
    }

    @Override
    @Transactional
    public QuestionSessionCreateResponse createSession(QuestionSessionCreateRequest request) {
        Long currentUserId = UserContext.getCurrentUserId();
        KnowledgeTopic topic = knowledgeTopicMapper.selectById(request.getTopicId());
        if (topic == null
                || Integer.valueOf(DELETED).equals(topic.getIsDeleted())
                || !STATUS_ENABLED.equals(topic.getStatus())) {
            throw new BusinessException(TOPIC_NOT_FOUND_CODE, "知识点不存在");
        }

        String difficulty = normalizeDifficulty(request.getDifficulty());
        LocalDateTime now = LocalDateTime.now();
        QuestionTrainingSession session = new QuestionTrainingSession();
        session.setUserId(currentUserId);
        session.setTopicId(request.getTopicId());
        session.setTargetRole(request.getTargetRole());
        session.setDifficulty(difficulty);
        session.setStatus(STATUS_IN_PROGRESS);
        session.setCurrentRound(FIRST_ROUND_NO);
        session.setMaxRound(MAX_ROUND);
        session.setStartedAt(now);
        session.setIsDeleted(NOT_DELETED);
        questionTrainingSessionMapper.insert(session);

        String firstQuestion = aiQuestionPracticeService.generateFirstQuestion(buildQuestionPracticeContext(
                currentUserId,
                session.getId(),
                topic,
                request.getTargetRole(),
                difficulty
        ));

        QuestionTrainingMessage message = new QuestionTrainingMessage();
        message.setSessionId(session.getId());
        message.setUserId(currentUserId);
        message.setRole(ROLE_ASSISTANT);
        message.setMessageType(MESSAGE_TYPE_AI_QUESTION);
        message.setContent(firstQuestion);
        message.setRoundNo(FIRST_ROUND_NO);
        message.setCreatedAt(now);
        questionTrainingMessageMapper.insert(message);

        return new QuestionSessionCreateResponse(
                session.getId(),
                topic.getId(),
                topic.getCategory(),
                topic.getName(),
                session.getTargetRole(),
                session.getDifficulty(),
                session.getCurrentRound(),
                session.getMaxRound(),
                toQuestionMessageVO(message)
        );
    }

    @Override
    public PageResult<QuestionSessionHistoryVO> pageSessions(QuestionSessionPageRequest request) {
        Long currentUserId = UserContext.getCurrentUserId();
        long pageNum = normalizePageNum(request.getPageNum());
        long pageSize = normalizePageSize(request.getPageSize());

        List<Long> categoryTopicIds = getTopicIdsByCategory(request.getCategory());
        if (StringUtils.hasText(request.getCategory()) && categoryTopicIds.isEmpty()) {
            return new PageResult<>(Collections.emptyList(), 0L, pageNum, pageSize, 0L);
        }

        LambdaQueryWrapper<QuestionTrainingSession> queryWrapper = new LambdaQueryWrapper<QuestionTrainingSession>()
                .eq(QuestionTrainingSession::getUserId, currentUserId)
                .eq(QuestionTrainingSession::getIsDeleted, NOT_DELETED)
                .eq(request.getTopicId() != null, QuestionTrainingSession::getTopicId, request.getTopicId())
                .eq(StringUtils.hasText(request.getStatus()), QuestionTrainingSession::getStatus, request.getStatus())
                .in(StringUtils.hasText(request.getCategory()), QuestionTrainingSession::getTopicId, categoryTopicIds)
                .orderByDesc(QuestionTrainingSession::getCreatedAt);

        Page<QuestionTrainingSession> page = questionTrainingSessionMapper.selectPage(
                new Page<>(pageNum, pageSize),
                queryWrapper
        );
        List<QuestionTrainingSession> sessions = page.getRecords();
        Map<Long, KnowledgeTopic> topicMap = getTopicMap(sessions);
        Map<Long, Long> reportIdMap = getReportIdMap(sessions);
        List<QuestionSessionHistoryVO> records = sessions.stream()
                .map(session -> toQuestionSessionHistoryVO(session, topicMap, reportIdMap))
                .toList();

        return new PageResult<>(records, page.getTotal(), page.getCurrent(), page.getSize(), page.getPages());
    }

    @Override
    public QuestionSessionDetailVO getSessionDetail(Long sessionId) {
        Long currentUserId = UserContext.getCurrentUserId();
        QuestionTrainingSession session = questionTrainingSessionMapper.selectById(sessionId);
        if (session == null || Integer.valueOf(DELETED).equals(session.getIsDeleted())) {
            throw new BusinessException(SESSION_NOT_FOUND_CODE, "八股训练会话不存在");
        }
        if (!currentUserId.equals(session.getUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }

        KnowledgeTopic topic = knowledgeTopicMapper.selectById(session.getTopicId());
        LambdaQueryWrapper<QuestionTrainingMessage> queryWrapper = new LambdaQueryWrapper<QuestionTrainingMessage>()
                .eq(QuestionTrainingMessage::getSessionId, sessionId)
                .orderByAsc(QuestionTrainingMessage::getCreatedAt)
                .orderByAsc(QuestionTrainingMessage::getId);
        List<QuestionMessageVO> messages = questionTrainingMessageMapper.selectList(queryWrapper).stream()
                .map(this::toQuestionMessageVO)
                .toList();

        return new QuestionSessionDetailVO(
                session.getId(),
                session.getTopicId(),
                topic == null ? null : topic.getCategory(),
                topic == null ? null : topic.getName(),
                topic == null ? null : topic.getDescription(),
                session.getTargetRole(),
                session.getDifficulty(),
                session.getStatus(),
                session.getCurrentRound(),
                session.getMaxRound(),
                session.getTotalScore(),
                session.getCreatedAt(),
                session.getEndedAt(),
                messages
        );
    }

    @Override
    public QuestionAnswerResponse submitAnswer(Long sessionId, QuestionAnswerRequest request) {
        return submitAnswerInternal(sessionId, request, null, false);
    }

    @Override
    public QuestionAnswerResponse submitAnswerStream(
            Long sessionId,
            QuestionAnswerRequest request,
            AiTokenStreamHandler streamHandler
    ) {
        return submitAnswerInternal(sessionId, request, streamHandler, true);
    }

    private QuestionAnswerResponse submitAnswerInternal(
            Long sessionId,
            QuestionAnswerRequest request,
            AiTokenStreamHandler streamHandler,
            boolean deferReportGeneration
    ) {
        String lockKey = ANSWER_LOCK_KEY_PREFIX + sessionId;
        String lockValue = UUID.randomUUID().toString();
        acquireAnswerLock(lockKey, lockValue);
        try {
            QuestionAnswerResponse response = transactionTemplate.execute(
                    status -> submitAnswerInTransaction(sessionId, request, streamHandler, deferReportGeneration)
            );
            if (response == null) {
                throw new BusinessException(AI_CALL_FAILED_CODE, "AI 调用失败，请稍后重试");
            }
            if (deferReportGeneration && Boolean.TRUE.equals(response.getFinished())) {
                startAsyncReportGeneration(sessionId);
            }
            return response;
        } finally {
            releaseAnswerLock(lockKey, lockValue);
        }
    }

    private QuestionAnswerResponse submitAnswerInTransaction(
            Long sessionId,
            QuestionAnswerRequest request,
            AiTokenStreamHandler streamHandler,
            boolean deferReportGeneration
    ) {
        Long currentUserId = UserContext.getCurrentUserId();
        QuestionTrainingSession session = getSessionForUpdate(sessionId);
        if (session == null || Integer.valueOf(DELETED).equals(session.getIsDeleted())) {
            throw new BusinessException(SESSION_NOT_FOUND_CODE, "八股训练会话不存在");
        }
        if (!currentUserId.equals(session.getUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        if (STATUS_FINISHED.equals(session.getStatus())) {
            throw new BusinessException(SESSION_ENDED_CODE, "八股训练已结束");
        }

        KnowledgeTopic topic = knowledgeTopicMapper.selectById(session.getTopicId());
        if (topic == null) {
            throw new BusinessException(TOPIC_NOT_FOUND_CODE, "知识点不存在");
        }

        Integer currentRound = session.getCurrentRound();
        Integer maxRound = session.getMaxRound();
        checkCurrentRoundAnswerNotSubmitted(sessionId, currentUserId, currentRound);

        List<QuestionTrainingMessage> historyMessages = listSessionMessages(sessionId);
        LocalDateTime now = LocalDateTime.now();

        QuestionTrainingMessage userAnswer = new QuestionTrainingMessage();
        userAnswer.setSessionId(sessionId);
        userAnswer.setUserId(currentUserId);
        userAnswer.setRole(ROLE_USER);
        userAnswer.setMessageType(MESSAGE_TYPE_USER_ANSWER);
        userAnswer.setContent(request.getAnswer());
        userAnswer.setRoundNo(currentRound);
        userAnswer.setCreatedAt(now);
        questionTrainingMessageMapper.insert(userAnswer);

        boolean finished = currentRound >= maxRound;
        QuestionPracticeContext answerContext = buildQuestionPracticeContext(session, topic, historyMessages, request.getAnswer());
        enrichAnswerContextWithRag(answerContext);
        QuestionFeedbackResult aiResult = generateFeedbackAndNextQuestion(answerContext, !finished, streamHandler);

        QuestionTrainingMessage aiFeedback = new QuestionTrainingMessage();
        aiFeedback.setSessionId(sessionId);
        aiFeedback.setUserId(currentUserId);
        aiFeedback.setRole(ROLE_ASSISTANT);
        aiFeedback.setMessageType(MESSAGE_TYPE_AI_FEEDBACK);
        aiFeedback.setContent(aiResult.getFeedback());
        aiFeedback.setRoundNo(currentRound);
        aiFeedback.setScore(aiResult.getScore());
        aiFeedback.setCreatedAt(now);
        questionTrainingMessageMapper.insert(aiFeedback);

        QuestionTrainingMessage referenceAnswer = new QuestionTrainingMessage();
        referenceAnswer.setSessionId(sessionId);
        referenceAnswer.setUserId(currentUserId);
        referenceAnswer.setRole(ROLE_ASSISTANT);
        referenceAnswer.setMessageType(MESSAGE_TYPE_AI_REFERENCE_ANSWER);
        referenceAnswer.setContent(aiResult.getReferenceAnswer());
        referenceAnswer.setRoundNo(currentRound);
        referenceAnswer.setCreatedAt(now);
        questionTrainingMessageMapper.insert(referenceAnswer);

        QuestionTrainingMessage nextQuestion = null;
        if (!finished) {
            nextQuestion = new QuestionTrainingMessage();
            nextQuestion.setSessionId(sessionId);
            nextQuestion.setUserId(currentUserId);
            nextQuestion.setRole(ROLE_ASSISTANT);
            nextQuestion.setMessageType(MESSAGE_TYPE_AI_FOLLOW_UP);
            nextQuestion.setContent(aiResult.getNextQuestion());
            nextQuestion.setRoundNo(currentRound + 1);
            nextQuestion.setCreatedAt(now);
            questionTrainingMessageMapper.insert(nextQuestion);
        }

        QuestionTrainingReport report = null;
        if (finished) {
            if (deferReportGeneration) {
                session.setStatus(STATUS_FINISHED);
                session.setEndedAt(LocalDateTime.now());
                session.setCurrentRound(session.getMaxRound());
                questionTrainingSessionMapper.updateById(session);
            } else {
                List<QuestionTrainingMessage> reportMessages = new ArrayList<>(historyMessages);
                reportMessages.add(userAnswer);
                reportMessages.add(aiFeedback);
                reportMessages.add(referenceAnswer);
                report = finishSessionWithReport(session, topic, reportMessages, LocalDateTime.now());
            }
        } else {
            session.setCurrentRound(currentRound + 1);
            questionTrainingSessionMapper.updateById(session);
        }

        return new QuestionAnswerResponse(
                toQuestionMessageVO(userAnswer),
                toQuestionMessageVO(aiFeedback),
                toQuestionMessageVO(referenceAnswer),
                nextQuestion == null ? null : toQuestionMessageVO(nextQuestion),
                finished,
                report == null ? null : report.getId(),
                report == null ? null : report.getTotalScore()
        );
    }

    @Override
    @Transactional
    public QuestionFinishResponse finishSession(Long sessionId) {
        Long currentUserId = UserContext.getCurrentUserId();
        QuestionTrainingSession session = questionTrainingSessionMapper.selectById(sessionId);
        if (session == null || Integer.valueOf(DELETED).equals(session.getIsDeleted())) {
            throw new BusinessException(SESSION_NOT_FOUND_CODE, "八股训练会话不存在");
        }
        if (!currentUserId.equals(session.getUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }

        QuestionTrainingReport existingReport = getReportBySessionId(sessionId);
        if (existingReport != null) {
            session.setStatus(STATUS_FINISHED);
            session.setEndedAt(session.getEndedAt() == null ? LocalDateTime.now() : session.getEndedAt());
            session.setTotalScore(existingReport.getTotalScore());
            session.setCurrentRound(session.getMaxRound());
            questionTrainingSessionMapper.updateById(session);
            KnowledgeTopic topic = knowledgeTopicMapper.selectById(session.getTopicId());
            userAbilitySnapshotService.createQuestionReportSnapshot(existingReport, session, topic);
            return new QuestionFinishResponse(existingReport.getId(), sessionId, existingReport.getTotalScore());
        }

        if (!STATUS_FINISHED.equals(session.getStatus())) {
            session.setStatus(STATUS_FINISHED);
            session.setEndedAt(LocalDateTime.now());
            session.setCurrentRound(session.getMaxRound());
            questionTrainingSessionMapper.updateById(session);
        }
        startAsyncReportGeneration(sessionId);
        return new QuestionFinishResponse(null, sessionId, null);
    }

    private void startAsyncReportGeneration(Long sessionId) {
        CompletableFuture.runAsync(() -> {
            try {
                singleFlightService.execute(
                        reportRequestKey(sessionId),
                        REPORT_LOCK_TTL,
                        REPORT_CACHE_TTL,
                        QuestionTrainingReport.class,
                        () -> transactionTemplate.execute(status -> {
                            QuestionTrainingReport existingReport = getReportBySessionId(sessionId);
                            if (existingReport != null) {
                                return existingReport;
                            }
                            QuestionTrainingSession session = questionTrainingSessionMapper.selectById(sessionId);
                            if (session == null || Integer.valueOf(DELETED).equals(session.getIsDeleted())) {
                                return null;
                            }
                            KnowledgeTopic topic = knowledgeTopicMapper.selectById(session.getTopicId());
                            List<QuestionTrainingMessage> messages = listSessionMessages(sessionId);
                            return finishSessionWithReport(session, topic, messages, LocalDateTime.now());
                        }),
                        () -> getReportBySessionId(sessionId),
                        ANSWER_PROCESSING_CODE,
                        "八股训练报告正在生成中，请稍后刷新。"
                );
            } catch (Exception exception) {
                log.warn("Async question report generation failed, sessionId={}, error={}",
                        sessionId,
                        exception.getMessage(),
                        exception);
            }
        });
    }

    private QuestionPracticeContext buildQuestionPracticeContext(
            Long userId,
            Long sessionId,
            KnowledgeTopic topic,
            String targetRole,
            String difficulty
    ) {
        QuestionPracticeContext context = new QuestionPracticeContext();
        context.setUserId(userId);
        context.setTopicId(topic.getId());
        context.setSessionId(sessionId);
        context.setCategory(topic.getCategory());
        context.setTopicName(topic.getName());
        context.setTopicDescription(topic.getDescription());
        context.setInterviewFocus(topic.getInterviewFocus());
        context.setTags(topic.getTags());
        context.setTargetRole(targetRole);
        context.setDifficulty(difficulty);
        context.setCurrentRound(FIRST_ROUND_NO);
        context.setMaxRound(MAX_ROUND);
        return context;
    }

    private QuestionPracticeContext buildQuestionPracticeContext(
            QuestionTrainingSession session,
            KnowledgeTopic topic,
            List<QuestionTrainingMessage> historyMessages,
            String answer
    ) {
        QuestionPracticeContext context = new QuestionPracticeContext();
        context.setUserId(session.getUserId());
        context.setTopicId(session.getTopicId());
        context.setSessionId(session.getId());
        context.setCategory(topic == null ? null : topic.getCategory());
        context.setTopicName(topic == null ? null : topic.getName());
        context.setTopicDescription(topic == null ? null : topic.getDescription());
        context.setInterviewFocus(topic == null ? null : topic.getInterviewFocus());
        context.setTags(topic == null ? null : topic.getTags());
        context.setTargetRole(session.getTargetRole());
        context.setDifficulty(session.getDifficulty());
        context.setCurrentRound(session.getCurrentRound());
        context.setMaxRound(session.getMaxRound());
        context.setHistoryMessages(buildHistoryMessages(historyMessages));
        context.setCurrentQuestion(getCurrentQuestion(historyMessages, session.getCurrentRound()));
        context.setUserAnswer(answer);
        return context;
    }

    private void enrichAnswerContextWithRag(QuestionPracticeContext context) {
        String query = """
                分类：%s
                知识点：%s
                当前问题：%s
                用户回答：%s
                训练难度：%s
                目标岗位：%s
                """.formatted(
                textValue(context.getCategory()),
                textValue(context.getTopicName()),
                truncate(context.getCurrentQuestion(), RAG_QUESTION_TRUNCATE_LENGTH),
                truncate(context.getUserAnswer(), RAG_ANSWER_TRUNCATE_LENGTH),
                textValue(context.getDifficulty()),
                textValue(context.getTargetRole())
        );
        context.setRagContext(retrieveRagContext(context, query));
    }

    private void enrichReportContextWithRag(QuestionPracticeContext context) {
        String query = """
                分类：%s
                知识点：%s
                训练难度：%s
                目标岗位：%s
                训练记录摘要：%s
                """.formatted(
                textValue(context.getCategory()),
                textValue(context.getTopicName()),
                textValue(context.getDifficulty()),
                textValue(context.getTargetRole()),
                truncate(context.getHistoryMessages(), RAG_HISTORY_TRUNCATE_LENGTH)
        );
        context.setRagContext(retrieveRagContext(context, query));
    }

    private String retrieveRagContext(QuestionPracticeContext context, String query) {
        if (ragProperties == null || !Boolean.TRUE.equals(ragProperties.getEnabled())) {
            return "";
        }
        try {
            List<RagRetrievedChunk> chunks = searchRagWithFallback(context, query);
            if (chunks.isEmpty()) {
                log.info(
                        "RAG retrieval returned no chunks for question practice, sessionId={}, topicId={}",
                        context.getSessionId(),
                        context.getTopicId()
                );
                return "";
            }
            log.info(
                    "RAG retrieval hit chunks for question practice, sessionId={}, topicId={}, chunkCount={}",
                    context.getSessionId(),
                    context.getTopicId(),
                    chunks.size()
            );
            int maxContextChars = ragProperties.getMaxContextChars() == null ? 4000 : ragProperties.getMaxContextChars();
            return ragRetrievalService.buildContextBlock(chunks, maxContextChars);
        } catch (Exception exception) {
            log.warn(
                    "RAG retrieval failed, fallback to normal question practice. sessionId={}, topicId={}, error={}",
                    context.getSessionId(),
                    context.getTopicId(),
                    abbreviate(exception.getMessage())
            );
            return "";
        }
    }

    private List<RagRetrievedChunk> searchRagWithFallback(QuestionPracticeContext context, String query) {
        if (context.getTopicId() != null) {
            List<RagRetrievedChunk> chunks = searchRag(query, Map.of("topicId", context.getTopicId()));
            if (!chunks.isEmpty()) {
                return chunks;
            }
        }
        if (StringUtils.hasText(context.getCategory())) {
            List<RagRetrievedChunk> chunks = searchRag(query, Map.of("category", context.getCategory()));
            if (!chunks.isEmpty()) {
                return chunks;
            }
        }
        return searchRag(query, Collections.emptyMap());
    }

    private List<RagRetrievedChunk> searchRag(String query, Map<String, Object> filter) {
        RagSearchRequest request = new RagSearchRequest();
        request.setQuery(query);
        request.setSourceTypes(List.of(RagConstants.SOURCE_TYPE_KNOWLEDGE_ARTICLE));
        request.setTopK(ragProperties.getTopK());
        request.setFilter(filter);
        RagSearchResponse response = ragRetrievalService.search(request);
        if (response == null || response.getChunks() == null) {
            return Collections.emptyList();
        }
        return response.getChunks();
    }

    private void acquireAnswerLock(String lockKey, String lockValue) {
        if (tryAcquireLock(lockKey, lockValue, ANSWER_LOCK_TTL)) {
            return;
        }
        throw new BusinessException(ResultCode.ANSWER_PROCESSING);
    }

    private boolean tryAcquireLock(String lockKey, String lockValue, Duration ttl) {
        try {
            return Boolean.TRUE.equals(stringRedisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, ttl));
        } catch (RuntimeException exception) {
            log.warn("Failed to acquire question lock, key={}", lockKey, exception);
            return false;
        }
    }

    private void releaseAnswerLock(String lockKey, String lockValue) {
        try {
            stringRedisTemplate.execute(RELEASE_LOCK_SCRIPT, Collections.singletonList(lockKey), lockValue);
        } catch (RuntimeException exception) {
            log.warn("Failed to release question answer lock", exception);
        }
    }

    private String reportRequestKey(Long sessionId) {
        return "question:report:" + sessionId;
    }

    private QuestionTrainingSession getSessionForUpdate(Long sessionId) {
        LambdaQueryWrapper<QuestionTrainingSession> queryWrapper = new LambdaQueryWrapper<QuestionTrainingSession>()
                .eq(QuestionTrainingSession::getId, sessionId)
                .last("FOR UPDATE");
        return questionTrainingSessionMapper.selectOne(queryWrapper);
    }

    private void checkCurrentRoundAnswerNotSubmitted(Long sessionId, Long userId, Integer currentRound) {
        Long count = questionTrainingMessageMapper.selectCount(new LambdaQueryWrapper<QuestionTrainingMessage>()
                .eq(QuestionTrainingMessage::getSessionId, sessionId)
                .eq(QuestionTrainingMessage::getUserId, userId)
                .eq(QuestionTrainingMessage::getMessageType, MESSAGE_TYPE_USER_ANSWER)
                .eq(QuestionTrainingMessage::getRoundNo, currentRound));
        if (count != null && count > 0) {
            throw new BusinessException(ANSWER_PROCESSING_CODE, "当前轮次已提交，请刷新页面查看最新结果");
        }
    }

    private QuestionFeedbackResult generateFeedbackAndNextQuestion(
            QuestionPracticeContext context,
            boolean needNextQuestion,
            AiTokenStreamHandler streamHandler
    ) {
        try {
            QuestionFeedbackResult result = streamHandler == null
                    ? aiQuestionPracticeService.generateFeedbackAndNextQuestion(context, needNextQuestion)
                    : aiQuestionPracticeService.generateFeedbackAndNextQuestionStream(
                            context,
                            needNextQuestion,
                            streamHandler
                    );
            if (result == null
                    || !StringUtils.hasText(result.getFeedback())
                    || !StringUtils.hasText(result.getReferenceAnswer())
                    || result.getScore() == null) {
                throw new BusinessException(AI_CALL_FAILED_CODE, "AI 调用失败，请稍后重试");
            }
            if (needNextQuestion && !StringUtils.hasText(result.getNextQuestion())) {
                throw new BusinessException(AI_CALL_FAILED_CODE, "AI 调用失败，请稍后重试");
            }
            return result;
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException(AI_CALL_FAILED_CODE, "AI 调用失败，请稍后重试");
        }
    }

    private QuestionReportGenerateResult generateReport(QuestionPracticeContext context) {
        try {
            QuestionReportGenerateResult result = aiQuestionPracticeService.generateReport(context);
            if (result == null || result.getTotalScore() == null || !StringUtils.hasText(result.getSummary())) {
                throw new BusinessException(AI_CALL_FAILED_CODE, "AI 调用失败，请稍后重试");
            }
            return result;
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException(AI_CALL_FAILED_CODE, "AI 调用失败，请稍后重试");
        }
    }

    private QuestionTrainingReport finishSessionWithReport(
            QuestionTrainingSession session,
            KnowledgeTopic topic,
            List<QuestionTrainingMessage> messages,
            LocalDateTime now
    ) {
        QuestionTrainingReport existingReport = getReportBySessionId(session.getId());
        if (existingReport != null) {
            session.setStatus(STATUS_FINISHED);
            session.setEndedAt(now);
            session.setTotalScore(existingReport.getTotalScore());
            session.setCurrentRound(session.getMaxRound());
            questionTrainingSessionMapper.updateById(session);
            userAbilitySnapshotService.createQuestionReportSnapshot(existingReport, session, topic);
            return existingReport;
        }

        QuestionPracticeContext reportContext = buildQuestionPracticeContext(session, topic, messages, null);
        enrichReportContextWithRag(reportContext);
        QuestionReportGenerateResult reportResult = generateReport(reportContext);
        ReportQualityAssessment qualityAssessment = reportQualityPostProcessor.processQuestionReport(
                reportResult,
                extractUserAnswers(messages),
                session.getMaxRound() == null ? MAX_ROUND : session.getMaxRound()
        );

        QuestionTrainingReport report = new QuestionTrainingReport();
        report.setSessionId(session.getId());
        report.setUserId(session.getUserId());
        report.setTopicId(session.getTopicId());
        report.setTotalScore(reportResult.getTotalScore());
        report.setSummary(reportResult.getSummary());
        report.setStrengths(toJson(reportResult.getStrengths()));
        report.setWeaknesses(toJson(reportResult.getWeaknesses()));
        report.setSuggestions(toJson(reportResult.getSuggestions()));
        report.setQaReview(toJson(reportResult.getQaReview()));
        report.setKnowledgeGaps(toJson(reportResult.getKnowledgeGaps()));
        report.setCreatedAt(now);
        questionTrainingReportMapper.insert(report);

        session.setStatus(STATUS_FINISHED);
        session.setEndedAt(now);
        session.setTotalScore(reportResult.getTotalScore());
        session.setCurrentRound(session.getMaxRound());
        questionTrainingSessionMapper.updateById(session);

        if (!qualityAssessment.isLowConfidence()) {
            userAbilitySnapshotService.createQuestionReportSnapshot(report, session, topic);
        }
        userMemoryService.sinkQuestionReport(report, session, topic);

        return report;
    }

    private List<String> extractUserAnswers(List<QuestionTrainingMessage> messages) {
        if (messages == null) {
            return List.of();
        }
        return messages.stream()
                .filter(message -> MESSAGE_TYPE_USER_ANSWER.equals(message.getMessageType()))
                .map(QuestionTrainingMessage::getContent)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .toList();
    }

    private List<QuestionTrainingMessage> listSessionMessages(Long sessionId) {
        LambdaQueryWrapper<QuestionTrainingMessage> queryWrapper = new LambdaQueryWrapper<QuestionTrainingMessage>()
                .eq(QuestionTrainingMessage::getSessionId, sessionId)
                .orderByAsc(QuestionTrainingMessage::getCreatedAt)
                .orderByAsc(QuestionTrainingMessage::getId);
        return questionTrainingMessageMapper.selectList(queryWrapper);
    }

    private QuestionTrainingReport getReportBySessionId(Long sessionId) {
        LambdaQueryWrapper<QuestionTrainingReport> queryWrapper = new LambdaQueryWrapper<QuestionTrainingReport>()
                .eq(QuestionTrainingReport::getSessionId, sessionId)
                .last("LIMIT 1");
        return questionTrainingReportMapper.selectOne(queryWrapper);
    }

    private List<Long> getTopicIdsByCategory(String category) {
        if (!StringUtils.hasText(category)) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<KnowledgeTopic> queryWrapper = new LambdaQueryWrapper<KnowledgeTopic>()
                .eq(KnowledgeTopic::getCategory, category);
        return knowledgeTopicMapper.selectList(queryWrapper).stream()
                .map(KnowledgeTopic::getId)
                .toList();
    }

    private Map<Long, KnowledgeTopic> getTopicMap(List<QuestionTrainingSession> sessions) {
        Set<Long> topicIds = sessions.stream()
                .map(QuestionTrainingSession::getTopicId)
                .collect(Collectors.toSet());
        if (topicIds.isEmpty()) {
            return Collections.emptyMap();
        }
        LambdaQueryWrapper<KnowledgeTopic> queryWrapper = new LambdaQueryWrapper<KnowledgeTopic>()
                .in(KnowledgeTopic::getId, topicIds);
        return knowledgeTopicMapper.selectList(queryWrapper).stream()
                .collect(Collectors.toMap(KnowledgeTopic::getId, Function.identity()));
    }

    private Map<Long, Long> getReportIdMap(List<QuestionTrainingSession> sessions) {
        Set<Long> sessionIds = sessions.stream()
                .map(QuestionTrainingSession::getId)
                .collect(Collectors.toSet());
        if (sessionIds.isEmpty()) {
            return Collections.emptyMap();
        }
        LambdaQueryWrapper<QuestionTrainingReport> queryWrapper = new LambdaQueryWrapper<QuestionTrainingReport>()
                .in(QuestionTrainingReport::getSessionId, sessionIds);
        return questionTrainingReportMapper.selectList(queryWrapper).stream()
                .collect(Collectors.toMap(QuestionTrainingReport::getSessionId, QuestionTrainingReport::getId));
    }

    private QuestionSessionHistoryVO toQuestionSessionHistoryVO(
            QuestionTrainingSession session,
            Map<Long, KnowledgeTopic> topicMap,
            Map<Long, Long> reportIdMap
    ) {
        KnowledgeTopic topic = topicMap.get(session.getTopicId());
        return new QuestionSessionHistoryVO(
                session.getId(),
                session.getTopicId(),
                topic == null ? null : topic.getCategory(),
                topic == null ? null : topic.getName(),
                session.getTargetRole(),
                session.getDifficulty(),
                session.getStatus(),
                session.getCurrentRound(),
                session.getMaxRound(),
                session.getTotalScore(),
                reportIdMap.get(session.getId()),
                session.getCreatedAt(),
                session.getEndedAt()
        );
    }

    private String buildHistoryMessages(List<QuestionTrainingMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return "暂无历史消息";
        }
        List<String> items = new ArrayList<>();
        for (QuestionTrainingMessage message : messages) {
            items.add("""
                    轮次：%s
                    角色：%s
                    类型：%s
                    内容：%s
                    得分：%s
                    """.formatted(
                    textValue(message.getRoundNo()),
                    textValue(message.getRole()),
                    textValue(message.getMessageType()),
                    textValue(message.getContent()),
                    textValue(message.getScore())
            ));
        }
        return String.join("\n", items);
    }

    private String getCurrentQuestion(List<QuestionTrainingMessage> messages, Integer currentRound) {
        if (messages == null) {
            return null;
        }
        for (int i = messages.size() - 1; i >= 0; i--) {
            QuestionTrainingMessage message = messages.get(i);
            boolean questionMessage = MESSAGE_TYPE_AI_QUESTION.equals(message.getMessageType())
                    || MESSAGE_TYPE_AI_FOLLOW_UP.equals(message.getMessageType());
            if (questionMessage && currentRound.equals(message.getRoundNo())) {
                return message.getContent();
            }
        }
        return null;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(AI_CALL_FAILED_CODE, "AI 调用失败，请稍后重试");
        }
    }

    private String textValue(Object value) {
        if (value == null) {
            return "";
        }
        return String.valueOf(value);
    }

    private String truncate(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength);
    }

    private String abbreviate(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= 300) {
            return normalized;
        }
        return normalized.substring(0, 300);
    }

    private long normalizePageNum(Long pageNum) {
        if (pageNum == null || pageNum < DEFAULT_PAGE_NUM) {
            return DEFAULT_PAGE_NUM;
        }
        return pageNum;
    }

    private long normalizePageSize(Long pageSize) {
        if (pageSize == null || pageSize < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    private String normalizeDifficulty(String difficulty) {
        if (!StringUtils.hasText(difficulty)) {
            return DIFFICULTY_NORMAL;
        }
        String normalized = difficulty.trim().toUpperCase(Locale.ROOT);
        if (DIFFICULTY_EASY.equals(normalized)
                || DIFFICULTY_NORMAL.equals(normalized)
                || DIFFICULTY_HARD.equals(normalized)) {
            return normalized;
        }
        return DIFFICULTY_NORMAL;
    }

    private QuestionMessageVO toQuestionMessageVO(QuestionTrainingMessage message) {
        return new QuestionMessageVO(
                message.getId(),
                message.getRole(),
                message.getMessageType(),
                message.getContent(),
                message.getRoundNo(),
                message.getScore(),
                message.getCreatedAt()
        );
    }
}
