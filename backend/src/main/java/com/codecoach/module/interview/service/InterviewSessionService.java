package com.codecoach.module.interview.service;

import com.codecoach.module.interview.dto.InterviewAnswerRequest;
import com.codecoach.module.interview.dto.InterviewSessionCreateRequest;
import com.codecoach.module.interview.vo.InterviewAnswerResponse;
import com.codecoach.module.interview.vo.InterviewSessionCreateResponse;
import com.codecoach.module.interview.vo.InterviewSessionDetailVO;

public interface InterviewSessionService {

    InterviewSessionCreateResponse createSession(InterviewSessionCreateRequest request);

    InterviewSessionDetailVO getSessionDetail(Long sessionId);

    InterviewAnswerResponse submitAnswer(Long sessionId, InterviewAnswerRequest request);
}
