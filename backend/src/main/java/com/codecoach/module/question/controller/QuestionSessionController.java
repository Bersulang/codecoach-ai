package com.codecoach.module.question.controller;

import com.codecoach.common.result.Result;
import com.codecoach.common.result.PageResult;
import com.codecoach.module.question.dto.QuestionAnswerRequest;
import com.codecoach.module.question.dto.QuestionSessionCreateRequest;
import com.codecoach.module.question.dto.QuestionSessionPageRequest;
import com.codecoach.module.question.service.QuestionSessionService;
import com.codecoach.module.question.vo.QuestionAnswerResponse;
import com.codecoach.module.question.vo.QuestionFinishResponse;
import com.codecoach.module.question.vo.QuestionSessionHistoryVO;
import com.codecoach.module.question.vo.QuestionSessionCreateResponse;
import com.codecoach.module.question.vo.QuestionSessionDetailVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/question-sessions")
public class QuestionSessionController {

    private final QuestionSessionService questionSessionService;

    public QuestionSessionController(QuestionSessionService questionSessionService) {
        this.questionSessionService = questionSessionService;
    }

    @PostMapping
    public Result<QuestionSessionCreateResponse> createSession(
            @Valid @RequestBody QuestionSessionCreateRequest request
    ) {
        return Result.success(questionSessionService.createSession(request));
    }

    @GetMapping
    public Result<PageResult<QuestionSessionHistoryVO>> pageSessions(QuestionSessionPageRequest request) {
        return Result.success(questionSessionService.pageSessions(request));
    }

    @GetMapping("/{sessionId}")
    public Result<QuestionSessionDetailVO> getSessionDetail(@PathVariable Long sessionId) {
        return Result.success(questionSessionService.getSessionDetail(sessionId));
    }

    @PostMapping("/{sessionId}/answer")
    public Result<QuestionAnswerResponse> submitAnswer(
            @PathVariable Long sessionId,
            @Valid @RequestBody QuestionAnswerRequest request
    ) {
        return Result.success(questionSessionService.submitAnswer(sessionId, request));
    }

    @PostMapping("/{sessionId}/finish")
    public Result<QuestionFinishResponse> finishSession(@PathVariable Long sessionId) {
        return Result.success(questionSessionService.finishSession(sessionId));
    }
}
