package com.codecoach.module.interview.service;

import com.codecoach.common.result.PageResult;
import com.codecoach.module.ai.service.AiTokenStreamHandler;
import com.codecoach.module.interview.dto.InterviewAnswerRequest;
import com.codecoach.module.interview.dto.InterviewSessionCreateRequest;
import com.codecoach.module.interview.dto.InterviewSessionPageRequest;
import com.codecoach.module.interview.vo.InterviewAnswerResponse;
import com.codecoach.module.interview.vo.InterviewFinishResponse;
import com.codecoach.module.interview.vo.InterviewSessionHistoryVO;
import com.codecoach.module.interview.vo.InterviewSessionCreateResponse;
import com.codecoach.module.interview.vo.InterviewSessionDetailVO;

public interface InterviewSessionService {

    InterviewSessionCreateResponse createSession(InterviewSessionCreateRequest request);

    PageResult<InterviewSessionHistoryVO> pageSessions(InterviewSessionPageRequest request);

    InterviewSessionDetailVO getSessionDetail(Long sessionId);

    InterviewAnswerResponse submitAnswer(Long sessionId, InterviewAnswerRequest request);

    InterviewAnswerResponse submitAnswerStream(
            Long sessionId,
            InterviewAnswerRequest request,
            AiTokenStreamHandler streamHandler
    );

    InterviewFinishResponse finishSession(Long sessionId);
}
