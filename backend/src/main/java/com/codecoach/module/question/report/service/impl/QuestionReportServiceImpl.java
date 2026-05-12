package com.codecoach.module.question.report.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.codecoach.common.exception.BusinessException;
import com.codecoach.common.result.ResultCode;
import com.codecoach.module.knowledge.entity.KnowledgeTopic;
import com.codecoach.module.knowledge.mapper.KnowledgeTopicMapper;
import com.codecoach.module.question.entity.QuestionTrainingMessage;
import com.codecoach.module.question.entity.QuestionTrainingReport;
import com.codecoach.module.question.entity.QuestionTrainingSession;
import com.codecoach.module.question.mapper.QuestionTrainingMessageMapper;
import com.codecoach.module.question.mapper.QuestionTrainingReportMapper;
import com.codecoach.module.question.mapper.QuestionTrainingSessionMapper;
import com.codecoach.module.question.report.service.QuestionReportService;
import com.codecoach.module.question.report.vo.QuestionQaReviewVO;
import com.codecoach.module.question.report.vo.QuestionReportVO;
import com.codecoach.module.report.quality.ReportQualityAssessment;
import com.codecoach.module.report.quality.ReportQualityEvaluator;
import com.codecoach.module.report.quality.ReportTrainingType;
import com.codecoach.security.UserContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class QuestionReportServiceImpl implements QuestionReportService {

    private static final Logger log = LoggerFactory.getLogger(QuestionReportServiceImpl.class);

    private static final int REPORT_NOT_FOUND_CODE = 5004;

    private final QuestionTrainingReportMapper questionTrainingReportMapper;

    private final QuestionTrainingSessionMapper questionTrainingSessionMapper;

    private final QuestionTrainingMessageMapper questionTrainingMessageMapper;

    private final KnowledgeTopicMapper knowledgeTopicMapper;

    private final ObjectMapper objectMapper;

    private final ReportQualityEvaluator reportQualityEvaluator;

    public QuestionReportServiceImpl(
            QuestionTrainingReportMapper questionTrainingReportMapper,
            QuestionTrainingSessionMapper questionTrainingSessionMapper,
            QuestionTrainingMessageMapper questionTrainingMessageMapper,
            KnowledgeTopicMapper knowledgeTopicMapper,
            ObjectMapper objectMapper,
            ReportQualityEvaluator reportQualityEvaluator
    ) {
        this.questionTrainingReportMapper = questionTrainingReportMapper;
        this.questionTrainingSessionMapper = questionTrainingSessionMapper;
        this.questionTrainingMessageMapper = questionTrainingMessageMapper;
        this.knowledgeTopicMapper = knowledgeTopicMapper;
        this.objectMapper = objectMapper;
        this.reportQualityEvaluator = reportQualityEvaluator;
    }

    @Override
    public QuestionReportVO getReport(Long reportId) {
        Long currentUserId = UserContext.getCurrentUserId();
        QuestionTrainingReport report = questionTrainingReportMapper.selectById(reportId);
        if (report == null) {
            throw new BusinessException(REPORT_NOT_FOUND_CODE, "八股训练报告不存在");
        }
        if (!currentUserId.equals(report.getUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }

        QuestionTrainingSession session = questionTrainingSessionMapper.selectById(report.getSessionId());
        KnowledgeTopic topic = knowledgeTopicMapper.selectById(report.getTopicId());
        ReportQualityAssessment qualityAssessment = reportQualityEvaluator.assess(
                listUserAnswers(report.getSessionId()),
                session == null || session.getMaxRound() == null ? 5 : session.getMaxRound(),
                ReportTrainingType.QUESTION
        );

        return new QuestionReportVO(
                report.getId(),
                report.getSessionId(),
                report.getTopicId(),
                topic == null ? null : topic.getCategory(),
                topic == null ? null : topic.getName(),
                session == null ? null : session.getTargetRole(),
                session == null ? null : session.getDifficulty(),
                report.getTotalScore(),
                qualityAssessment.getSampleSufficiency().name(),
                qualityAssessment.getAnswerQuality().name(),
                qualityAssessment.getAnswerCount(),
                qualityAssessment.getValidAnswerCount(),
                qualityAssessment.getDeductionReasons(),
                qualityAssessment.getNextActions(),
                report.getSummary(),
                parseStringList(report.getStrengths()),
                parseStringList(report.getWeaknesses()),
                parseStringList(report.getSuggestions()),
                parseStringList(report.getKnowledgeGaps()),
                parseQaReview(report.getQaReview()),
                report.getCreatedAt()
        );
    }

    private List<String> listUserAnswers(Long sessionId) {
        if (sessionId == null) {
            return Collections.emptyList();
        }
        return questionTrainingMessageMapper.selectList(new LambdaQueryWrapper<QuestionTrainingMessage>()
                        .eq(QuestionTrainingMessage::getSessionId, sessionId)
                        .eq(QuestionTrainingMessage::getMessageType, "USER_ANSWER")
                        .orderByAsc(QuestionTrainingMessage::getRoundNo)
                        .orderByAsc(QuestionTrainingMessage::getId))
                .stream()
                .map(QuestionTrainingMessage::getContent)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .toList();
    }

    private List<String> parseStringList(String json) {
        if (!StringUtils.hasText(json)) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (JsonProcessingException exception) {
            log.warn("Failed to parse question report JSON array: {}", json, exception);
            return Collections.emptyList();
        }
    }

    private List<QuestionQaReviewVO> parseQaReview(String json) {
        if (!StringUtils.hasText(json)) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<QuestionQaReviewVO>>() {
            });
        } catch (JsonProcessingException exception) {
            log.warn("Failed to parse question report qaReview JSON: {}", json, exception);
            return Collections.emptyList();
        }
    }
}
