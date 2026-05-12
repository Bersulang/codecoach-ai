package com.codecoach.module.agent.service;

import com.codecoach.module.agent.model.AgentReviewContext;
import com.codecoach.module.agent.model.AgentReviewResult;

public interface AiAgentReviewService {

    AgentReviewResult generateReview(AgentReviewContext context);
}
