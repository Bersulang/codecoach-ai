package com.codecoach.module.agent.controller;

import com.codecoach.common.result.Result;
import com.codecoach.module.agent.service.AgentReviewService;
import com.codecoach.module.agent.vo.AgentReviewListItemVO;
import com.codecoach.module.agent.vo.AgentReviewVO;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agent-reviews")
public class AgentReviewController {

    private final AgentReviewService agentReviewService;

    public AgentReviewController(AgentReviewService agentReviewService) {
        this.agentReviewService = agentReviewService;
    }

    @PostMapping
    public Result<AgentReviewVO> generateReview(@RequestParam(required = false) String scopeType) {
        return Result.success(agentReviewService.generateReview(scopeType));
    }

    @GetMapping
    public Result<List<AgentReviewListItemVO>> listReviews(@RequestParam(required = false) Integer limit) {
        return Result.success(agentReviewService.listReviews(limit));
    }

    @GetMapping("/{reviewId}")
    public Result<AgentReviewVO> getReview(@PathVariable Long reviewId) {
        return Result.success(agentReviewService.getReview(reviewId));
    }
}
