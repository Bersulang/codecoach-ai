package com.codecoach.module.question.controller;

import com.codecoach.common.result.Result;
import com.codecoach.module.question.dto.QuestionSessionCreateRequest;
import com.codecoach.module.question.service.QuestionSessionService;
import com.codecoach.module.question.vo.QuestionSessionCreateResponse;
import jakarta.validation.Valid;
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
}
