package com.codecoach.module.interview.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.codecoach.common.exception.BusinessException;
import com.codecoach.common.result.ResultCode;
import com.codecoach.module.ai.dto.InterviewContext;
import com.codecoach.module.ai.service.AiInterviewService;
import com.codecoach.module.ai.vo.FeedbackAndQuestionResult;
import com.codecoach.module.interview.dto.InterviewAnswerRequest;
import com.codecoach.module.interview.dto.InterviewSessionCreateRequest;
import com.codecoach.module.interview.entity.InterviewMessage;
import com.codecoach.module.interview.entity.InterviewSession;
import com.codecoach.module.interview.mapper.InterviewMessageMapper;
import com.codecoach.module.interview.mapper.InterviewSessionMapper;
import com.codecoach.module.interview.service.InterviewSessionService;
import com.codecoach.module.interview.vo.InterviewAnswerResponse;
import com.codecoach.module.interview.vo.InterviewMessageVO;
import com.codecoach.module.interview.vo.InterviewSessionCreateResponse;
import com.codecoach.module.interview.vo.InterviewSessionDetailVO;
import com.codecoach.module.project.entity.Project;
import com.codecoach.module.project.mapper.ProjectMapper;
import com.codecoach.security.UserContext;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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

    private final InterviewSessionMapper interviewSessionMapper;

    private final InterviewMessageMapper interviewMessageMapper;

    private final ProjectMapper projectMapper;

    private final AiInterviewService aiInterviewService;

    public InterviewSessionServiceImpl(
            InterviewSessionMapper interviewSessionMapper,
            InterviewMessageMapper interviewMessageMapper,
            ProjectMapper projectMapper,
            AiInterviewService aiInterviewService
    ) {
        this.interviewSessionMapper = interviewSessionMapper;
        this.interviewMessageMapper = interviewMessageMapper;
        this.projectMapper = projectMapper;
        this.aiInterviewService = aiInterviewService;
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
