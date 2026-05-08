package com.codecoach.module.interview.service.impl;

import com.codecoach.common.exception.BusinessException;
import com.codecoach.module.ai.service.AiInterviewService;
import com.codecoach.module.interview.dto.InterviewSessionCreateRequest;
import com.codecoach.module.interview.entity.InterviewMessage;
import com.codecoach.module.interview.entity.InterviewSession;
import com.codecoach.module.interview.mapper.InterviewMessageMapper;
import com.codecoach.module.interview.mapper.InterviewSessionMapper;
import com.codecoach.module.interview.service.InterviewSessionService;
import com.codecoach.module.interview.vo.InterviewMessageVO;
import com.codecoach.module.interview.vo.InterviewSessionCreateResponse;
import com.codecoach.module.project.entity.Project;
import com.codecoach.module.project.mapper.ProjectMapper;
import com.codecoach.security.UserContext;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class InterviewSessionServiceImpl implements InterviewSessionService {

    private static final int PROJECT_NOT_FOUND_CODE = 2001;

    private static final int PROJECT_ACCESS_DENIED_CODE = 2002;

    private static final int AI_CALL_FAILED_CODE = 3003;

    private static final String STATUS_IN_PROGRESS = "IN_PROGRESS";

    private static final String ROLE_ASSISTANT = "ASSISTANT";

    private static final String MESSAGE_TYPE_AI_QUESTION = "AI_QUESTION";

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
