package com.codecoach.module.question.report.controller;

import com.codecoach.common.result.Result;
import com.codecoach.module.question.report.service.QuestionReportService;
import com.codecoach.module.question.report.vo.QuestionReportVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/question-reports")
public class QuestionReportController {

    private final QuestionReportService questionReportService;

    public QuestionReportController(QuestionReportService questionReportService) {
        this.questionReportService = questionReportService;
    }

    @GetMapping("/{reportId}")
    public Result<QuestionReportVO> getReport(@PathVariable Long reportId) {
        return Result.success(questionReportService.getReport(reportId));
    }
}
