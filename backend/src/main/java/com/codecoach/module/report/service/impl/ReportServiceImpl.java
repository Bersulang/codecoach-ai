package com.codecoach.module.report.service.impl;

import com.codecoach.common.exception.BusinessException;
import com.codecoach.common.result.ResultCode;
import com.codecoach.module.interview.entity.InterviewSession;
import com.codecoach.module.interview.mapper.InterviewSessionMapper;
import com.codecoach.module.project.entity.Project;
import com.codecoach.module.project.mapper.ProjectMapper;
import com.codecoach.module.report.entity.InterviewReport;
import com.codecoach.module.report.mapper.InterviewReportMapper;
import com.codecoach.module.report.service.ReportService;
import com.codecoach.module.report.vo.InterviewReportVO;
import com.codecoach.module.report.vo.QaReviewVO;
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
public class ReportServiceImpl implements ReportService {

    private static final Logger log = LoggerFactory.getLogger(ReportServiceImpl.class);

    private static final int REPORT_NOT_FOUND_CODE = 4001;

    private final InterviewReportMapper interviewReportMapper;

    private final InterviewSessionMapper interviewSessionMapper;

    private final ProjectMapper projectMapper;

    private final ObjectMapper objectMapper;

    public ReportServiceImpl(
            InterviewReportMapper interviewReportMapper,
            InterviewSessionMapper interviewSessionMapper,
            ProjectMapper projectMapper,
            ObjectMapper objectMapper
    ) {
        this.interviewReportMapper = interviewReportMapper;
        this.interviewSessionMapper = interviewSessionMapper;
        this.projectMapper = projectMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public InterviewReportVO getReportDetail(Long reportId) {
        Long currentUserId = UserContext.getCurrentUserId();
        InterviewReport report = interviewReportMapper.selectById(reportId);
        if (report == null) {
            throw new BusinessException(REPORT_NOT_FOUND_CODE, "报告不存在");
        }
        if (!currentUserId.equals(report.getUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }

        InterviewSession session = interviewSessionMapper.selectById(report.getSessionId());
        Project project = projectMapper.selectById(report.getProjectId());

        return new InterviewReportVO(
                report.getId(),
                report.getSessionId(),
                report.getProjectId(),
                project == null ? null : project.getName(),
                session == null ? null : session.getTargetRole(),
                session == null ? null : session.getDifficulty(),
                report.getTotalScore(),
                report.getSummary(),
                parseStringList(report.getStrengths()),
                parseStringList(report.getWeaknesses()),
                parseStringList(report.getSuggestions()),
                parseQaReview(report.getQaReview()),
                report.getCreatedAt()
        );
    }

    private List<String> parseStringList(String json) {
        if (!StringUtils.hasText(json)) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (JsonProcessingException exception) {
            log.warn("Failed to parse report JSON array: {}", json, exception);
            return Collections.emptyList();
        }
    }

    private List<QaReviewVO> parseQaReview(String json) {
        if (!StringUtils.hasText(json)) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<QaReviewVO>>() {
            });
        } catch (JsonProcessingException exception) {
            log.warn("Failed to parse report qaReview JSON: {}", json, exception);
            return Collections.emptyList();
        }
    }
}
