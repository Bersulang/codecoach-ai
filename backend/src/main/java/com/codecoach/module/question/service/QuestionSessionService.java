package com.codecoach.module.question.service;

import com.codecoach.common.result.PageResult;
import com.codecoach.module.question.dto.QuestionSessionCreateRequest;
import com.codecoach.module.question.dto.QuestionAnswerRequest;
import com.codecoach.module.question.dto.QuestionSessionPageRequest;
import com.codecoach.module.ai.service.AiTokenStreamHandler;
import com.codecoach.module.question.vo.QuestionAnswerResponse;
import com.codecoach.module.question.vo.QuestionFinishResponse;
import com.codecoach.module.question.vo.QuestionSessionCreateResponse;
import com.codecoach.module.question.vo.QuestionSessionDetailVO;
import com.codecoach.module.question.vo.QuestionSessionHistoryVO;

public interface QuestionSessionService {

    QuestionSessionCreateResponse createSession(QuestionSessionCreateRequest request);

    PageResult<QuestionSessionHistoryVO> pageSessions(QuestionSessionPageRequest request);

    QuestionSessionDetailVO getSessionDetail(Long sessionId);

    QuestionAnswerResponse submitAnswer(Long sessionId, QuestionAnswerRequest request);

    QuestionAnswerResponse submitAnswerStream(
            Long sessionId,
            QuestionAnswerRequest request,
            AiTokenStreamHandler streamHandler
    );

    QuestionFinishResponse finishSession(Long sessionId);
}
