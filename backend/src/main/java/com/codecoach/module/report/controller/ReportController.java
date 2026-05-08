package com.codecoach.module.report.controller;

import com.codecoach.common.result.Result;
import com.codecoach.module.report.service.ReportService;
import com.codecoach.module.report.vo.InterviewReportVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/{reportId}")
    public Result<InterviewReportVO> getReportDetail(@PathVariable Long reportId) {
        return Result.success(reportService.getReportDetail(reportId));
    }
}
