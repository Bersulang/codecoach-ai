package com.codecoach.module.agent.tool.controller;

import com.codecoach.common.result.Result;
import com.codecoach.module.agent.tool.dto.ToolActionVO;
import com.codecoach.module.agent.tool.dto.ToolExecuteRequest;
import com.codecoach.module.agent.tool.dto.ToolExecuteResult;
import com.codecoach.module.agent.tool.service.AgentToolExecutor;
import com.codecoach.module.agent.tool.service.AgentToolRegistry;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agent/tools")
public class AgentToolController {

    private final AgentToolRegistry agentToolRegistry;
    private final AgentToolExecutor agentToolExecutor;

    public AgentToolController(AgentToolRegistry agentToolRegistry, AgentToolExecutor agentToolExecutor) {
        this.agentToolRegistry = agentToolRegistry;
        this.agentToolExecutor = agentToolExecutor;
    }

    @GetMapping
    public Result<List<ToolActionVO>> listTools() {
        return Result.success(agentToolRegistry.listActions());
    }

    @PostMapping("/execute")
    public Result<ToolExecuteResult> execute(@RequestBody ToolExecuteRequest request) {
        return Result.success(agentToolExecutor.execute(request));
    }
}
