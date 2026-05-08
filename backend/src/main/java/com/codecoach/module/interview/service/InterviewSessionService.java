package com.codecoach.module.interview.service;

import com.codecoach.module.interview.dto.InterviewSessionCreateRequest;
import com.codecoach.module.interview.vo.InterviewSessionCreateResponse;

public interface InterviewSessionService {

    InterviewSessionCreateResponse createSession(InterviewSessionCreateRequest request);
}
