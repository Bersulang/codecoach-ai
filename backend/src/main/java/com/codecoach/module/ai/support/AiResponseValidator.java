package com.codecoach.module.ai.support;

import com.codecoach.common.exception.BusinessException;
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

    public void validateFeedbackAndNextQuestion(FeedbackAndQuestionResult result) {
        validateFeedbackOnly(result);
        if (!StringUtils.hasText(result.getNextQuestion())) {
            throw aiCallFailed();
        }
    }

    public void validateFeedbackOnly(FeedbackAndQuestionResult result) {
        if (result == null || !StringUtils.hasText(result.getFeedback())) {
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

    private boolean isEmpty(List<String> values) {
        if (values == null || values.isEmpty()) {
            return true;
        }
        return values.stream().noneMatch(StringUtils::hasText);
    }

    private boolean isValidQaReviewItem(ReportGenerateResult.QaReviewItem item) {
        return item != null
                && StringUtils.hasText(item.getQuestion())
                && StringUtils.hasText(item.getAnswer())
                && StringUtils.hasText(item.getFeedback());
    }

    private BusinessException aiCallFailed() {
        return new BusinessException(AI_CALL_FAILED_CODE, AI_CALL_FAILED_MESSAGE);
    }
}
