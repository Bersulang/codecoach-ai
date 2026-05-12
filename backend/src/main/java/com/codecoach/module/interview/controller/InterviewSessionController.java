package com.codecoach.module.interview.controller;

import com.codecoach.common.result.PageResult;
import com.codecoach.common.result.Result;
import com.codecoach.common.dto.StreamAnswerRequest;
import com.codecoach.common.exception.BusinessException;
import com.codecoach.common.result.ResultCode;
import com.codecoach.common.web.NdjsonStreamWriter;
import com.codecoach.module.interview.dto.InterviewAnswerRequest;
import com.codecoach.module.interview.dto.InterviewSessionPageRequest;
import com.codecoach.module.interview.dto.InterviewSessionCreateRequest;
import com.codecoach.module.interview.service.InterviewSessionService;
import com.codecoach.module.interview.vo.InterviewAnswerResponse;
import com.codecoach.module.interview.vo.InterviewFinishResponse;
import com.codecoach.module.interview.vo.InterviewSessionHistoryVO;
import com.codecoach.module.interview.vo.InterviewSessionCreateResponse;
import com.codecoach.module.interview.vo.InterviewSessionDetailVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import java.io.IOException;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
@RequestMapping("/api/interview-sessions")
public class InterviewSessionController {

    private final InterviewSessionService interviewSessionService;

    private final ObjectMapper objectMapper;

    public InterviewSessionController(InterviewSessionService interviewSessionService, ObjectMapper objectMapper) {
        this.interviewSessionService = interviewSessionService;
        this.objectMapper = objectMapper;
    }

    @PostMapping
    public Result<InterviewSessionCreateResponse> createSession(
            @Valid @RequestBody InterviewSessionCreateRequest request
    ) {
        return Result.success(interviewSessionService.createSession(request));
    }

    @GetMapping
    public Result<PageResult<InterviewSessionHistoryVO>> pageSessions(InterviewSessionPageRequest request) {
        return Result.success(interviewSessionService.pageSessions(request));
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
                writer.start("AI 正在分析你的项目表达");
                writer.stage("正在检索项目上下文");
                writer.stage("正在生成反馈和下一轮追问");
                InterviewAnswerRequest answerRequest = new InterviewAnswerRequest();
                answerRequest.setAnswer(request.getContent());
                InterviewAnswerResponse response = interviewSessionService.submitAnswerStream(
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
    public Result<InterviewFinishResponse> finishSession(@PathVariable Long sessionId) {
        return Result.success(interviewSessionService.finishSession(sessionId));
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
