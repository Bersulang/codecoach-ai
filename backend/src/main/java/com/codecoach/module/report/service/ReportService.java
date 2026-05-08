package com.codecoach.module.report.service;

import com.codecoach.module.report.vo.InterviewReportVO;

public interface ReportService {

    InterviewReportVO getReportDetail(Long reportId);
}
