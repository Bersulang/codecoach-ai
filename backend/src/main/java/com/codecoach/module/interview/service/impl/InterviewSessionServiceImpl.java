package com.codecoach.module.interview.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.codecoach.common.exception.BusinessException;
import com.codecoach.common.result.PageResult;
import com.codecoach.common.result.ResultCode;
import com.codecoach.module.ai.dto.InterviewContext;
import com.codecoach.module.ai.service.AiInterviewService;
import com.codecoach.module.ai.vo.FeedbackAndQuestionResult;
import com.codecoach.module.ai.vo.ReportGenerateResult;
import com.codecoach.module.interview.dto.InterviewAnswerRequest;
import com.codecoach.module.interview.dto.InterviewSessionCreateRequest;
import com.codecoach.module.interview.dto.InterviewSessionPageRequest;
import com.codecoach.module.interview.entity.InterviewMessage;
import com.codecoach.module.interview.entity.InterviewSession;
import com.codecoach.module.interview.mapper.InterviewMessageMapper;
import com.codecoach.module.interview.mapper.InterviewSessionMapper;
import com.codecoach.module.interview.service.InterviewSessionService;
import com.codecoach.module.interview.vo.InterviewAnswerResponse;
import com.codecoach.module.interview.vo.InterviewFinishResponse;
import com.codecoach.module.interview.vo.InterviewMessageVO;
import com.codecoach.module.interview.vo.InterviewSessionCreateResponse;
import com.codecoach.module.interview.vo.InterviewSessionDetailVO;
import com.codecoach.module.interview.vo.InterviewSessionHistoryVO;
import com.codecoach.module.project.entity.Project;
import com.codecoach.module.project.mapper.ProjectMapper;
import com.codecoach.module.report.entity.InterviewReport;
import com.codecoach.module.report.mapper.InterviewReportMapper;
import com.codecoach.security.UserContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class InterviewSessionServiceImpl implements InterviewSessionService {

    private static final int PROJECT_NOT_FOUND_CODE = 2001;

    private static final int PROJECT_ACCESS_DENIED_CODE = 2002;

    private static final int AI_CALL_FAILED_CODE = 3003;

    private static final int SESSION_NOT_FOUND_CODE = 3001;

    private static final int SESSION_ENDED_CODE = 3002;

    private static final String STATUS_IN_PROGRESS = "IN_PROGRESS";

    private static final String STATUS_FINISHED = "FINISHED";

    private static final String ROLE_USER = "USER";

    private static final String ROLE_ASSISTANT = "ASSISTANT";

    private static final String MESSAGE_TYPE_AI_QUESTION = "AI_QUESTION";

    private static final String MESSAGE_TYPE_USER_ANSWER = "USER_ANSWER";

    private static final String MESSAGE_TYPE_AI_FEEDBACK = "AI_FEEDBACK";

    private static final String MESSAGE_TYPE_AI_FOLLOW_UP = "AI_FOLLOW_UP";

    private static final int FIRST_ROUND_NO = 1;

    private static final int MAX_ROUND = 5;

    private static final int NOT_DELETED = 0;

    private static final int DELETED = 1;

    private static final long DEFAULT_PAGE_NUM = 1L;

    private static final long DEFAULT_PAGE_SIZE = 10L;

    private static final long MAX_PAGE_SIZE = 100L;

    private final InterviewSessionMapper interviewSessionMapper;

    private final InterviewMessageMapper interviewMessageMapper;

    private final ProjectMapper projectMapper;

    private final InterviewReportMapper interviewReportMapper;

    private final AiInterviewService aiInterviewService;

    private final ObjectMapper objectMapper;

    public InterviewSessionServiceImpl(
            InterviewSessionMapper interviewSessionMapper,
            InterviewMessageMapper interviewMessageMapper,
            ProjectMapper projectMapper,
            InterviewReportMapper interviewReportMapper,
            AiInterviewService aiInterviewService,
            ObjectMapper objectMapper
    ) {
        this.interviewSessionMapper = interviewSessionMapper;
        this.interviewMessageMapper = interviewMessageMapper;
        this.projectMapper = projectMapper;
        this.interviewReportMapper = interviewReportMapper;
        this.aiInterviewService = aiInterviewService;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public InterviewSessionCreateResponse createSession(InterviewSessionCreateRequest request) {
        Long currentUserId = UserContext.getCurrentUserId();
        Project project = projectMapper.selectById(request.getProjectId());
        if (project == null || Integer.valueOf(DELETED).equals(project.getIsDeleted())) {
            throw new BusinessException(PROJECT_NOT_FOUND_CODE, "项目不存在");
        }
        if (!currentUserId.equals(project.getUserId())) {
            throw new BusinessException(PROJECT_ACCESS_DENIED_CODE, "无权访问该项目");
        }

        LocalDateTime now = LocalDateTime.now();
        InterviewSession session = new InterviewSession();
        session.setUserId(currentUserId);
        session.setProjectId(request.getProjectId());
        session.setTargetRole(request.getTargetRole());
        session.setDifficulty(request.getDifficulty());
        session.setStatus(STATUS_IN_PROGRESS);
        session.setCurrentRound(FIRST_ROUND_NO);
        session.setMaxRound(MAX_ROUND);
        session.setStartedAt(now);
        session.setIsDeleted(NOT_DELETED);
        interviewSessionMapper.insert(session);

        String firstQuestion = generateFirstQuestion(project, request.getTargetRole(), request.getDifficulty());
        InterviewMessage message = new InterviewMessage();
        message.setSessionId(session.getId());
        message.setUserId(currentUserId);
        message.setRole(ROLE_ASSISTANT);
        message.setMessageType(MESSAGE_TYPE_AI_QUESTION);
        message.setContent(firstQuestion);
        message.setRoundNo(FIRST_ROUND_NO);
        message.setCreatedAt(now);
        interviewMessageMapper.insert(message);

        return new InterviewSessionCreateResponse(session.getId(), toInterviewMessageVO(message));
    }

    @Override
    public PageResult<InterviewSessionHistoryVO> pageSessions(InterviewSessionPageRequest request) {
        Long currentUserId = UserContext.getCurrentUserId();
        long pageNum = normalizePageNum(request.getPageNum());
        long pageSize = normalizePageSize(request.getPageSize());

        LambdaQueryWrapper<InterviewSession> queryWrapper = new LambdaQueryWrapper<InterviewSession>()
                .eq(InterviewSession::getUserId, currentUserId)
                .eq(InterviewSession::getIsDeleted, NOT_DELETED)
                .eq(request.getProjectId() != null, InterviewSession::getProjectId, request.getProjectId())
                .eq(StringUtils.hasText(request.getStatus()), InterviewSession::getStatus, request.getStatus())
                .orderByDesc(InterviewSession::getCreatedAt);

        Page<InterviewSession> page = interviewSessionMapper.selectPage(new Page<>(pageNum, pageSize), queryWrapper);
        List<InterviewSession> sessions = page.getRecords();
        Map<Long, String> projectNameMap = getProjectNameMap(sessions);
        Map<Long, Long> reportIdMap = getReportIdMap(sessions);
        List<InterviewSessionHistoryVO> records = sessions.stream()
                .map(session -> toInterviewSessionHistoryVO(session, projectNameMap, reportIdMap))
                .toList();

        return new PageResult<>(records, page.getTotal(), page.getCurrent(), page.getSize(), page.getPages());
    }

    @Override
    public InterviewSessionDetailVO getSessionDetail(Long sessionId) {
        Long currentUserId = UserContext.getCurrentUserId();
        InterviewSession session = interviewSessionMapper.selectById(sessionId);
        if (session == null || Integer.valueOf(DELETED).equals(session.getIsDeleted())) {
            throw new BusinessException(SESSION_NOT_FOUND_CODE, "训练会话不存在");
        }
        if (!currentUserId.equals(session.getUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }

        Project project = projectMapper.selectById(session.getProjectId());
        String projectName = null;
        if (project != null && !Integer.valueOf(DELETED).equals(project.getIsDeleted())) {
            projectName = project.getName();
        }

        LambdaQueryWrapper<InterviewMessage> queryWrapper = new LambdaQueryWrapper<InterviewMessage>()
                .eq(InterviewMessage::getSessionId, sessionId)
                .orderByAsc(InterviewMessage::getCreatedAt);
        List<InterviewMessageVO> messages = interviewMessageMapper.selectList(queryWrapper).stream()
                .map(this::toInterviewMessageVO)
                .toList();

        return new InterviewSessionDetailVO(
                session.getId(),
                session.getProjectId(),
                projectName,
                session.getTargetRole(),
                session.getDifficulty(),
                session.getStatus(),
                session.getCurrentRound(),
                session.getMaxRound(),
                messages
        );
    }

    @Override
    @Transactional
    public InterviewAnswerResponse submitAnswer(Long sessionId, InterviewAnswerRequest request) {
        Long currentUserId = UserContext.getCurrentUserId();
        InterviewSession session = interviewSessionMapper.selectById(sessionId);
        if (session == null || Integer.valueOf(DELETED).equals(session.getIsDeleted())) {
            throw new BusinessException(SESSION_NOT_FOUND_CODE, "训练会话不存在");
        }
        if (!currentUserId.equals(session.getUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        if (!STATUS_IN_PROGRESS.equals(session.getStatus())) {
            throw new BusinessException(SESSION_ENDED_CODE, "训练会话已结束");
        }

        Integer currentRound = session.getCurrentRound();
        Integer maxRound = session.getMaxRound();
        List<InterviewMessage> historyMessages = listSessionMessages(sessionId);
        Project project = projectMapper.selectById(session.getProjectId());
        LocalDateTime now = LocalDateTime.now();

        InterviewMessage userAnswer = new InterviewMessage();
        userAnswer.setSessionId(sessionId);
        userAnswer.setUserId(currentUserId);
        userAnswer.setRole(ROLE_USER);
        userAnswer.setMessageType(MESSAGE_TYPE_USER_ANSWER);
        userAnswer.setContent(request.getAnswer());
        userAnswer.setRoundNo(currentRound);
        userAnswer.setCreatedAt(now);
        interviewMessageMapper.insert(userAnswer);

        boolean finished = currentRound >= maxRound;
        FeedbackAndQuestionResult aiResult = generateFeedbackAndNextQuestion(
                buildInterviewContext(session, project, historyMessages, request.getAnswer()),
                !finished
        );

        InterviewMessage aiFeedback = new InterviewMessage();
        aiFeedback.setSessionId(sessionId);
        aiFeedback.setUserId(currentUserId);
        aiFeedback.setRole(ROLE_ASSISTANT);
        aiFeedback.setMessageType(MESSAGE_TYPE_AI_FEEDBACK);
        aiFeedback.setContent(aiResult.getFeedback());
        aiFeedback.setRoundNo(currentRound);
        aiFeedback.setCreatedAt(now);
        interviewMessageMapper.insert(aiFeedback);

        InterviewMessage nextQuestion = null;
        if (!finished) {
            nextQuestion = new InterviewMessage();
            nextQuestion.setSessionId(sessionId);
            nextQuestion.setUserId(currentUserId);
            nextQuestion.setRole(ROLE_ASSISTANT);
            nextQuestion.setMessageType(MESSAGE_TYPE_AI_FOLLOW_UP);
            nextQuestion.setContent(aiResult.getNextQuestion());
            nextQuestion.setRoundNo(currentRound + 1);
            nextQuestion.setCreatedAt(now);
            interviewMessageMapper.insert(nextQuestion);
        }

        session.setCurrentRound(finished ? maxRound : currentRound + 1);
        interviewSessionMapper.updateById(session);

        return new InterviewAnswerResponse(
                toInterviewMessageVO(userAnswer),
                toInterviewMessageVO(aiFeedback),
                nextQuestion == null ? null : toInterviewMessageVO(nextQuestion),
                finished
        );
    }

    @Override
    @Transactional
    public InterviewFinishResponse finishSession(Long sessionId) {
        Long currentUserId = UserContext.getCurrentUserId();
        InterviewSession session = interviewSessionMapper.selectById(sessionId);
        if (session == null || Integer.valueOf(DELETED).equals(session.getIsDeleted())) {
            throw new BusinessException(SESSION_NOT_FOUND_CODE, "训练会话不存在");
        }
        if (!currentUserId.equals(session.getUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }

        InterviewReport existingReport = getReportBySessionId(sessionId);
        if (existingReport != null) {
            return new InterviewFinishResponse(existingReport.getId(), sessionId, existingReport.getTotalScore());
        }
        if (STATUS_FINISHED.equals(session.getStatus())) {
            throw new BusinessException(SESSION_ENDED_CODE, "训练会话已结束");
        }

        Project project = projectMapper.selectById(session.getProjectId());
        List<InterviewMessage> messages = listSessionMessages(sessionId);
        ReportGenerateResult reportResult = generateReport(buildInterviewContext(session, project, messages, null));

        LocalDateTime now = LocalDateTime.now();
        InterviewReport report = new InterviewReport();
        report.setSessionId(sessionId);
        report.setUserId(currentUserId);
        report.setProjectId(session.getProjectId());
        report.setTotalScore(reportResult.getTotalScore());
        report.setSummary(reportResult.getSummary());
        report.setStrengths(toJson(reportResult.getStrengths()));
        report.setWeaknesses(toJson(reportResult.getWeaknesses()));
        report.setSuggestions(toJson(reportResult.getSuggestions()));
        report.setQaReview(toJson(reportResult.getQaReview()));
        report.setCreatedAt(now);
        interviewReportMapper.insert(report);

        session.setStatus(STATUS_FINISHED);
        session.setEndedAt(now);
        session.setTotalScore(reportResult.getTotalScore());
        interviewSessionMapper.updateById(session);

        return new InterviewFinishResponse(report.getId(), sessionId, report.getTotalScore());
    }

    private String generateFirstQuestion(Project project, String targetRole, String difficulty) {
        try {
            String firstQuestion = aiInterviewService.generateFirstQuestion(project, targetRole, difficulty);
            if (!StringUtils.hasText(firstQuestion)) {
                throw new BusinessException(AI_CALL_FAILED_CODE, "AI 调用失败，请稍后重试");
            }
            return firstQuestion;
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException(AI_CALL_FAILED_CODE, "AI 调用失败，请稍后重试");
        }
    }

    private ReportGenerateResult generateReport(InterviewContext context) {
        try {
            ReportGenerateResult result = aiInterviewService.generateReport(context);
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

    private FeedbackAndQuestionResult generateFeedbackAndNextQuestion(InterviewContext context, boolean needNextQuestion) {
        try {
            FeedbackAndQuestionResult result = aiInterviewService.generateFeedbackAndNextQuestion(context);
            if (result == null || !StringUtils.hasText(result.getFeedback())) {
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

    private InterviewContext buildInterviewContext(
            InterviewSession session,
            Project project,
            List<InterviewMessage> historyMessages,
            String answer
    ) {
        InterviewContext context = new InterviewContext();
        context.setProject(project);
        context.setTargetRole(session.getTargetRole());
        context.setDifficulty(session.getDifficulty());
        context.setRoundNo(session.getCurrentRound());
        context.setCurrentQuestion(getCurrentQuestion(historyMessages, session.getCurrentRound()));
        context.setUserAnswer(answer);
        context.setQaRecords(buildQaRecords(historyMessages));
        return context;
    }

    private String getCurrentQuestion(List<InterviewMessage> messages, Integer currentRound) {
        for (int i = messages.size() - 1; i >= 0; i--) {
            InterviewMessage message = messages.get(i);
            boolean questionMessage = MESSAGE_TYPE_AI_QUESTION.equals(message.getMessageType())
                    || MESSAGE_TYPE_AI_FOLLOW_UP.equals(message.getMessageType());
            if (questionMessage && currentRound.equals(message.getRoundNo())) {
                return message.getContent();
            }
        }
        return null;
    }

    private List<InterviewContext.QaRecord> buildQaRecords(List<InterviewMessage> messages) {
        List<InterviewContext.QaRecord> qaRecords = new ArrayList<>();
        String question = null;
        String answer = null;
        for (InterviewMessage message : messages) {
            if (MESSAGE_TYPE_AI_QUESTION.equals(message.getMessageType())
                    || MESSAGE_TYPE_AI_FOLLOW_UP.equals(message.getMessageType())) {
                question = message.getContent();
                answer = null;
            } else if (MESSAGE_TYPE_USER_ANSWER.equals(message.getMessageType())) {
                answer = message.getContent();
            } else if (MESSAGE_TYPE_AI_FEEDBACK.equals(message.getMessageType())) {
                qaRecords.add(new InterviewContext.QaRecord(question, answer, message.getContent()));
            }
        }
        return qaRecords;
    }

    private List<InterviewMessage> listSessionMessages(Long sessionId) {
        LambdaQueryWrapper<InterviewMessage> queryWrapper = new LambdaQueryWrapper<InterviewMessage>()
                .eq(InterviewMessage::getSessionId, sessionId)
                .orderByAsc(InterviewMessage::getCreatedAt)
                .orderByAsc(InterviewMessage::getId);
        return interviewMessageMapper.selectList(queryWrapper);
    }

    private InterviewReport getReportBySessionId(Long sessionId) {
        LambdaQueryWrapper<InterviewReport> queryWrapper = new LambdaQueryWrapper<InterviewReport>()
                .eq(InterviewReport::getSessionId, sessionId)
                .last("LIMIT 1");
        return interviewReportMapper.selectOne(queryWrapper);
    }

    private Map<Long, String> getProjectNameMap(List<InterviewSession> sessions) {
        Set<Long> projectIds = sessions.stream()
                .map(InterviewSession::getProjectId)
                .collect(Collectors.toSet());
        if (projectIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return projectMapper.selectBatchIds(projectIds).stream()
                .collect(Collectors.toMap(Project::getId, Project::getName));
    }

    private Map<Long, Long> getReportIdMap(List<InterviewSession> sessions) {
        Set<Long> sessionIds = sessions.stream()
                .map(InterviewSession::getId)
                .collect(Collectors.toSet());
        if (sessionIds.isEmpty()) {
            return Collections.emptyMap();
        }

        LambdaQueryWrapper<InterviewReport> queryWrapper = new LambdaQueryWrapper<InterviewReport>()
                .in(InterviewReport::getSessionId, sessionIds);
        return interviewReportMapper.selectList(queryWrapper).stream()
                .collect(Collectors.toMap(InterviewReport::getSessionId, InterviewReport::getId));
    }

    private InterviewSessionHistoryVO toInterviewSessionHistoryVO(
            InterviewSession session,
            Map<Long, String> projectNameMap,
            Map<Long, Long> reportIdMap
    ) {
        return new InterviewSessionHistoryVO(
                session.getId(),
                session.getProjectId(),
                projectNameMap.get(session.getProjectId()),
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

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(AI_CALL_FAILED_CODE, "AI 调用失败，请稍后重试");
        }
    }

    private InterviewMessageVO toInterviewMessageVO(InterviewMessage message) {
        return new InterviewMessageVO(
                message.getId(),
                message.getRole(),
                message.getMessageType(),
                message.getContent(),
                message.getRoundNo(),
                message.getCreatedAt()
        );
    }
}
