package com.codecoach.module.ai.support;

import com.codecoach.common.exception.BusinessException;
import com.codecoach.module.ai.model.QuestionFeedbackResult;
import com.codecoach.module.ai.model.QuestionQaReviewItem;
import com.codecoach.module.ai.model.QuestionReportGenerateResult;
import com.codecoach.module.ai.vo.FeedbackAndQuestionResult;
import com.codecoach.module.ai.vo.ReportGenerateResult;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AiResponseValidator {

    private static final int AI_CALL_FAILED_CODE = 3003;

    private static final String AI_CALL_FAILED_MESSAGE = "AI 调用失败，请稍后重试";

    private static final int MIN_SCORE = 0;

    private static final int MAX_SCORE = 100;

    private static final List<String> PLACEHOLDER_TEXTS = List.of(
            "本轮回答反馈",
            "结构化参考答案",
            "下一轮追问",
            "对用户本轮回答的具体评价",
            "根据用户回答生成的具体反馈",
            "根据当前知识点生成的结构化参考答案",
            "基于当前回答继续追问的具体问题"
    );

    public void validateFeedbackAndNextQuestion(FeedbackAndQuestionResult result) {
        validateFeedbackOnly(result);
        if (!StringUtils.hasText(result.getNextQuestion())) {
            throw aiCallFailed();
        }
    }

    public void validateFeedbackOnly(FeedbackAndQuestionResult result) {
        if (result == null
                || !StringUtils.hasText(result.getFeedback())
                || isPlaceholder(result.getFeedback())
                || isPlaceholder(result.getNextQuestion())) {
            throw aiCallFailed();
        }
    }

    public void validateReport(ReportGenerateResult result) {
        if (result == null
                || result.getTotalScore() == null
                || result.getTotalScore() < MIN_SCORE
                || result.getTotalScore() > MAX_SCORE
                || !StringUtils.hasText(result.getSummary())
                || isEmpty(result.getStrengths())
                || isEmpty(result.getWeaknesses())
                || isEmpty(result.getSuggestions())) {
            throw aiCallFailed();
        }

        if (result.getQaReview() == null) {
            result.setQaReview(new ArrayList<>());
            return;
        }

        List<ReportGenerateResult.QaReviewItem> validItems = result.getQaReview().stream()
                .filter(this::isValidQaReviewItem)
                .toList();
        result.setQaReview(validItems);
    }

    public void validateQuestionFeedbackAndNextQuestion(QuestionFeedbackResult result) {
        validateQuestionFeedbackOnly(result);
        if (!StringUtils.hasText(result.getNextQuestion())) {
            throw aiCallFailed();
        }
    }

    public void validateQuestionFeedbackOnly(QuestionFeedbackResult result) {
        if (result == null
                || !StringUtils.hasText(result.getFeedback())
                || !StringUtils.hasText(result.getReferenceAnswer())
                || result.getScore() == null
                || result.getScore() < MIN_SCORE
                || result.getScore() > MAX_SCORE
                || isPlaceholder(result.getFeedback())
                || isPlaceholder(result.getReferenceAnswer())
                || isPlaceholder(result.getNextQuestion())) {
            throw aiCallFailed();
        }
    }

    public void validateQuestionReport(QuestionReportGenerateResult result) {
        if (result == null
                || result.getTotalScore() == null
                || result.getTotalScore() < MIN_SCORE
                || result.getTotalScore() > MAX_SCORE
                || !StringUtils.hasText(result.getSummary())
                || isEmpty(result.getStrengths())
                || isEmpty(result.getWeaknesses())
                || isEmpty(result.getSuggestions())) {
            throw aiCallFailed();
        }

        if (result.getKnowledgeGaps() == null) {
            result.setKnowledgeGaps(new ArrayList<>());
        }
        if (result.getQaReview() == null) {
            result.setQaReview(new ArrayList<>());
            return;
        }

        List<QuestionQaReviewItem> validItems = result.getQaReview().stream()
                .filter(this::isValidQuestionQaReviewItem)
                .toList();
        result.setQaReview(validItems);
    }

    private boolean isEmpty(List<String> values) {
        if (values == null || values.isEmpty()) {
            return true;
        }
        return values.stream().noneMatch(StringUtils::hasText);
    }

    private boolean isPlaceholder(String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        String normalized = value.trim();
        return PLACEHOLDER_TEXTS.stream().anyMatch(placeholder -> placeholder.equals(normalized));
    }

    private boolean isValidQaReviewItem(ReportGenerateResult.QaReviewItem item) {
        return item != null
                && StringUtils.hasText(item.getQuestion())
                && StringUtils.hasText(item.getAnswer())
                && StringUtils.hasText(item.getFeedback());
    }

    private boolean isValidQuestionQaReviewItem(QuestionQaReviewItem item) {
        return item != null
                && StringUtils.hasText(item.getQuestion())
                && StringUtils.hasText(item.getAnswer())
                && StringUtils.hasText(item.getReferenceAnswer())
                && StringUtils.hasText(item.getFeedback());
    }

    private BusinessException aiCallFailed() {
        return new BusinessException(AI_CALL_FAILED_CODE, AI_CALL_FAILED_MESSAGE);
    }
}
