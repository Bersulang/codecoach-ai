package com.codecoach.module.agent.service;

import com.codecoach.module.agent.vo.AgentReviewListItemVO;
import com.codecoach.module.agent.vo.AgentReviewVO;
import java.util.List;

public interface AgentReviewService {

    AgentReviewVO generateReview(String scopeType);

    List<AgentReviewListItemVO> listReviews(Integer limit);

    AgentReviewVO getReview(Long reviewId);
}
