package com.codecoach.module.mockinterview.service;

import com.codecoach.common.result.PageResult;
import com.codecoach.module.ai.service.AiTokenStreamHandler;
import com.codecoach.module.mockinterview.dto.MockInterviewAnswerRequest;
import com.codecoach.module.mockinterview.dto.MockInterviewCreateRequest;
import com.codecoach.module.mockinterview.dto.MockInterviewPageRequest;
import com.codecoach.module.mockinterview.vo.MockInterviewAnswerResponse;
import com.codecoach.module.mockinterview.vo.MockInterviewCreateResponse;
import com.codecoach.module.mockinterview.vo.MockInterviewFinishResponse;
import com.codecoach.module.mockinterview.vo.MockInterviewHistoryVO;
import com.codecoach.module.mockinterview.vo.MockInterviewReportVO;
import com.codecoach.module.mockinterview.vo.MockInterviewSessionDetailVO;

public interface MockInterviewSessionService {

    MockInterviewCreateResponse createSession(MockInterviewCreateRequest request);

    PageResult<MockInterviewHistoryVO> pageSessions(MockInterviewPageRequest request);

    MockInterviewSessionDetailVO getSessionDetail(Long sessionId);

    MockInterviewAnswerResponse submitAnswer(Long sessionId, MockInterviewAnswerRequest request);

    MockInterviewAnswerResponse submitAnswerStream(
            Long sessionId,
            MockInterviewAnswerRequest request,
            AiTokenStreamHandler streamHandler
    );

    MockInterviewFinishResponse finishSession(Long sessionId);

    MockInterviewReportVO getReport(Long sessionId);
}
