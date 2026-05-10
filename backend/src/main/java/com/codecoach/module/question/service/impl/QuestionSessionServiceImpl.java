package com.codecoach.module.question.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.codecoach.common.exception.BusinessException;
import com.codecoach.common.result.ResultCode;
import com.codecoach.module.ai.model.QuestionPracticeContext;
import com.codecoach.module.ai.service.AiQuestionPracticeService;
import com.codecoach.module.knowledge.entity.KnowledgeTopic;
import com.codecoach.module.knowledge.mapper.KnowledgeTopicMapper;
import com.codecoach.module.question.dto.QuestionSessionCreateRequest;
import com.codecoach.module.question.entity.QuestionTrainingMessage;
import com.codecoach.module.question.entity.QuestionTrainingSession;
import com.codecoach.module.question.mapper.QuestionTrainingMessageMapper;
import com.codecoach.module.question.mapper.QuestionTrainingSessionMapper;
import com.codecoach.module.question.service.QuestionSessionService;
import com.codecoach.module.question.vo.QuestionMessageVO;
import com.codecoach.module.question.vo.QuestionSessionCreateResponse;
import com.codecoach.module.question.vo.QuestionSessionDetailVO;
import com.codecoach.security.UserContext;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class QuestionSessionServiceImpl implements QuestionSessionService {

    private static final int TOPIC_NOT_FOUND_CODE = 5001;

    private static final int SESSION_NOT_FOUND_CODE = 5002;

    private static final String STATUS_ENABLED = "ENABLED";

    private static final String STATUS_IN_PROGRESS = "IN_PROGRESS";

    private static final String ROLE_ASSISTANT = "ASSISTANT";

    private static final String MESSAGE_TYPE_AI_QUESTION = "AI_QUESTION";

    private static final String DIFFICULTY_EASY = "EASY";

    private static final String DIFFICULTY_NORMAL = "NORMAL";

    private static final String DIFFICULTY_HARD = "HARD";

    private static final int FIRST_ROUND_NO = 1;

    private static final int MAX_ROUND = 5;

    private static final int NOT_DELETED = 0;

    private static final int DELETED = 1;

    private final KnowledgeTopicMapper knowledgeTopicMapper;

    private final QuestionTrainingSessionMapper questionTrainingSessionMapper;

    private final QuestionTrainingMessageMapper questionTrainingMessageMapper;

    private final AiQuestionPracticeService aiQuestionPracticeService;

    public QuestionSessionServiceImpl(
            KnowledgeTopicMapper knowledgeTopicMapper,
            QuestionTrainingSessionMapper questionTrainingSessionMapper,
            QuestionTrainingMessageMapper questionTrainingMessageMapper,
            AiQuestionPracticeService aiQuestionPracticeService
    ) {
        this.knowledgeTopicMapper = knowledgeTopicMapper;
        this.questionTrainingSessionMapper = questionTrainingSessionMapper;
        this.questionTrainingMessageMapper = questionTrainingMessageMapper;
        this.aiQuestionPracticeService = aiQuestionPracticeService;
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
