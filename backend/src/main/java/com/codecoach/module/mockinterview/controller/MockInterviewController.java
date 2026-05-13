package com.codecoach.module.mockinterview.controller;

import com.codecoach.common.dto.StreamAnswerRequest;
import com.codecoach.common.exception.BusinessException;
import com.codecoach.common.result.PageResult;
import com.codecoach.common.result.Result;
import com.codecoach.common.result.ResultCode;
import com.codecoach.common.web.NdjsonStreamWriter;
import com.codecoach.module.mockinterview.dto.MockInterviewAnswerRequest;
import com.codecoach.module.mockinterview.dto.MockInterviewCreateRequest;
import com.codecoach.module.mockinterview.dto.MockInterviewPageRequest;
import com.codecoach.module.mockinterview.service.MockInterviewSessionService;
import com.codecoach.module.mockinterview.vo.MockInterviewAnswerResponse;
import com.codecoach.module.mockinterview.vo.MockInterviewCreateResponse;
import com.codecoach.module.mockinterview.vo.MockInterviewFinishResponse;
import com.codecoach.module.mockinterview.vo.MockInterviewHistoryVO;
import com.codecoach.module.mockinterview.vo.MockInterviewReportVO;
import com.codecoach.module.mockinterview.vo.MockInterviewSessionDetailVO;
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
@RequestMapping("/api/mock-interviews")
public class MockInterviewController {

    private final MockInterviewSessionService mockInterviewSessionService;
    private final ObjectMapper objectMapper;

    public MockInterviewController(MockInterviewSessionService mockInterviewSessionService, ObjectMapper objectMapper) {
        this.mockInterviewSessionService = mockInterviewSessionService;
        this.objectMapper = objectMapper;
    }

    @PostMapping
    public Result<MockInterviewCreateResponse> create(@Valid @RequestBody MockInterviewCreateRequest request) {
        return Result.success(mockInterviewSessionService.createSession(request));
    }

    @GetMapping
    public Result<PageResult<MockInterviewHistoryVO>> page(MockInterviewPageRequest request) {
        return Result.success(mockInterviewSessionService.pageSessions(request));
    }

    @GetMapping("/{sessionId}")
    public Result<MockInterviewSessionDetailVO> detail(@PathVariable Long sessionId) {
        return Result.success(mockInterviewSessionService.getSessionDetail(sessionId));
    }

    @PostMapping("/{sessionId}/answer")
    public Result<MockInterviewAnswerResponse> answer(
            @PathVariable Long sessionId,
            @Valid @RequestBody MockInterviewAnswerRequest request
    ) {
        return Result.success(mockInterviewSessionService.submitAnswer(sessionId, request));
    }

    @PostMapping(value = "/{sessionId}/answers/stream", produces = "application/x-ndjson;charset=UTF-8")
    public ResponseEntity<StreamingResponseBody> answerStream(
            @PathVariable Long sessionId,
            @RequestBody StreamAnswerRequest request
    ) {
        if (request == null || !StringUtils.hasText(request.getContent())) {
            throw new BusinessException(ResultCode.BAD_REQUEST);
        }
        StreamingResponseBody body = outputStream -> {
            NdjsonStreamWriter writer = new NdjsonStreamWriter(outputStream, objectMapper);
            try {
                writer.start("AI 面试官正在阅读你的回答");
                writer.stage("正在结合阶段目标和个人材料组织追问");
                MockInterviewAnswerRequest answerRequest = new MockInterviewAnswerRequest();
                answerRequest.setAnswer(request.getContent());
                MockInterviewAnswerResponse response = mockInterviewSessionService.submitAnswerStream(
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

    @PostMapping("/{sessionId}/finish")
    public Result<MockInterviewFinishResponse> finish(@PathVariable Long sessionId) {
        return Result.success(mockInterviewSessionService.finishSession(sessionId));
    }

    @GetMapping("/{sessionId}/report")
    public Result<MockInterviewReportVO> report(@PathVariable Long sessionId) {
        return Result.success(mockInterviewSessionService.getReport(sessionId));
    }

    private void writeDelta(NdjsonStreamWriter writer, String delta) {
        try {
            writer.delta(delta);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to write stream delta", exception);
        }
    }

    private void writeStreamError(NdjsonStreamWriter writer, Exception exception) throws IOException {
        String message = "生成失败，请稍后重试";
        if (exception instanceof BusinessException businessException && StringUtils.hasText(businessException.getMessage())) {
            message = businessException.getMessage();
        }
        writer.error(message);
    }
}
