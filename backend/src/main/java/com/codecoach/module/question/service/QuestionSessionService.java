package com.codecoach.module.question.service;

import com.codecoach.module.question.dto.QuestionSessionCreateRequest;
import com.codecoach.module.question.vo.QuestionSessionCreateResponse;
import com.codecoach.module.question.vo.QuestionSessionDetailVO;

public interface QuestionSessionService {

    QuestionSessionCreateResponse createSession(QuestionSessionCreateRequest request);

    QuestionSessionDetailVO getSessionDetail(Long sessionId);
}
