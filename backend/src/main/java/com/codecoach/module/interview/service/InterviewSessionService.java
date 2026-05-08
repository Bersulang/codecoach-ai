package com.codecoach.module.interview.service;

import com.codecoach.module.interview.dto.InterviewSessionCreateRequest;
import com.codecoach.module.interview.vo.InterviewSessionCreateResponse;
import com.codecoach.module.interview.vo.InterviewSessionDetailVO;

public interface InterviewSessionService {

    InterviewSessionCreateResponse createSession(InterviewSessionCreateRequest request);

    InterviewSessionDetailVO getSessionDetail(Long sessionId);
}
