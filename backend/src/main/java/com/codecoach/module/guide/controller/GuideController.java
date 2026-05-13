package com.codecoach.module.guide.controller;

import com.codecoach.common.result.Result;
import com.codecoach.module.agent.runtime.dto.AgentRequest;
import com.codecoach.module.agent.runtime.dto.AgentResponse;
import com.codecoach.module.agent.runtime.service.AgentRuntime;
import com.codecoach.module.guide.dto.GuideChatRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/guide")
public class GuideController {

    private final AgentRuntime agentRuntime;

    public GuideController(AgentRuntime agentRuntime) {
        this.agentRuntime = agentRuntime;
    }

    @PostMapping("/chat")
    public Result<AgentResponse> chat(@RequestBody GuideChatRequest request) {
        AgentRequest agentRequest = new AgentRequest();
        agentRequest.setAgentType("GUIDE");
        agentRequest.setMessage(request == null ? null : request.getMessage());
        agentRequest.setCurrentPath(request == null ? null : request.getCurrentPath());
        agentRequest.setPageTitle(request == null ? null : request.getPageTitle());
        return Result.success(agentRuntime.run(agentRequest));
    }
}
