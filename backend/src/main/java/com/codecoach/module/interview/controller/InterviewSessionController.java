package com.codecoach.module.interview.controller;

import com.codecoach.common.result.Result;
import com.codecoach.module.interview.dto.InterviewAnswerRequest;
import com.codecoach.module.interview.dto.InterviewSessionCreateRequest;
import com.codecoach.module.interview.service.InterviewSessionService;
import com.codecoach.module.interview.vo.InterviewAnswerResponse;
import com.codecoach.module.interview.vo.InterviewFinishResponse;
import com.codecoach.module.interview.vo.InterviewSessionCreateResponse;
import com.codecoach.module.interview.vo.InterviewSessionDetailVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/interview-sessions")
public class InterviewSessionController {

    private final InterviewSessionService interviewSessionService;

    public InterviewSessionController(InterviewSessionService interviewSessionService) {
        this.interviewSessionService = interviewSessionService;
    }

    @PostMapping
    public Result<InterviewSessionCreateResponse> createSession(
            @Valid @RequestBody InterviewSessionCreateRequest request
    ) {
        return Result.success(interviewSessionService.createSession(request));
    }

    @GetMapping("/{sessionId}")
    public Result<InterviewSessionDetailVO> getSessionDetail(@PathVariable Long sessionId) {
        return Result.success(interviewSessionService.getSessionDetail(sessionId));
    }

    @PostMapping("/{sessionId}/answer")
    public Result<InterviewAnswerResponse> submitAnswer(
            @PathVariable Long sessionId,
            @Valid @RequestBody InterviewAnswerRequest request
    ) {
        return Result.success(interviewSessionService.submitAnswer(sessionId, request));
    }

    @PostMapping("/{sessionId}/finish")
    public Result<InterviewFinishResponse> finishSession(@PathVariable Long sessionId) {
        return Result.success(interviewSessionService.finishSession(sessionId));
    }
}
