package com.codecoach.module.question.controller;

import com.codecoach.common.dto.StreamAnswerRequest;
import com.codecoach.common.exception.BusinessException;
import com.codecoach.common.result.Result;
import com.codecoach.common.result.PageResult;
import com.codecoach.common.result.ResultCode;
import com.codecoach.common.web.NdjsonStreamWriter;
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
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.springframework.util.StringUtils;

@RestController
@RequestMapping("/api/question-sessions")
public class QuestionSessionController {

    private final QuestionSessionService questionSessionService;

    private final ObjectMapper objectMapper;

    public QuestionSessionController(QuestionSessionService questionSessionService, ObjectMapper objectMapper) {
        this.questionSessionService = questionSessionService;
        this.objectMapper = objectMapper;
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

    @PostMapping(
            value = "/{sessionId}/answers/stream",
            produces = "application/x-ndjson;charset=UTF-8"
    )
    public ResponseEntity<StreamingResponseBody> submitAnswerStream(
            @PathVariable Long sessionId,
            @RequestBody StreamAnswerRequest request
    ) {
        if (request == null || !StringUtils.hasText(request.getContent())) {
            throw new BusinessException(ResultCode.BAD_REQUEST);
        }
        StreamingResponseBody body = outputStream -> {
            NdjsonStreamWriter writer = new NdjsonStreamWriter(outputStream, objectMapper);
            try {
                writer.start("AI 正在分析你的回答");
                writer.stage("正在检索相关知识");
                writer.stage("正在生成反馈和参考答案");
                QuestionAnswerRequest answerRequest = new QuestionAnswerRequest();
                answerRequest.setAnswer(request.getContent());
                QuestionAnswerResponse response = questionSessionService.submitAnswerStream(
                        sessionId,
                        answerRequest,
                        delta -> writeDelta(writer, delta)
                );
                writer.done(response);
            } catch (Exception exception) {
                writeStreamError(writer, exception);
            }
        };
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "application/x-ndjson;charset=UTF-8")
                .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-transform")
                .header("X-Accel-Buffering", "no")
                .cacheControl(CacheControl.noCache())
                .body(body);
    }

    private void writeDelta(NdjsonStreamWriter writer, String delta) {
        try {
            writer.delta(delta);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to write stream delta", exception);
        }
    }

    @PostMapping("/{sessionId}/finish")
    public Result<QuestionFinishResponse> finishSession(@PathVariable Long sessionId) {
        return Result.success(questionSessionService.finishSession(sessionId));
    }

    private void writeStreamError(NdjsonStreamWriter writer, Exception exception) throws IOException {
        String message = "生成失败，请稍后重试";
        if (exception instanceof BusinessException businessException
                && StringUtils.hasText(businessException.getMessage())) {
            message = businessException.getMessage();
        }
        writer.error(message);
    }
}
