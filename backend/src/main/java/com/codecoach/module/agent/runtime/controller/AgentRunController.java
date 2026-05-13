package com.codecoach.module.agent.runtime.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.codecoach.common.exception.BusinessException;
import com.codecoach.common.result.Result;
import com.codecoach.common.result.ResultCode;
import com.codecoach.module.agent.runtime.entity.AgentRun;
import com.codecoach.module.agent.runtime.entity.AgentStep;
import com.codecoach.module.agent.runtime.mapper.AgentRunMapper;
import com.codecoach.module.agent.runtime.mapper.AgentStepMapper;
import com.codecoach.security.UserContext;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agent/runs")
public class AgentRunController {

    private final AgentRunMapper agentRunMapper;
    private final AgentStepMapper agentStepMapper;

    public AgentRunController(AgentRunMapper agentRunMapper, AgentStepMapper agentStepMapper) {
        this.agentRunMapper = agentRunMapper;
        this.agentStepMapper = agentStepMapper;
    }

    @GetMapping("/{runId}")
    public Result<AgentRun> getRun(@PathVariable String runId) {
        Long userId = UserContext.getCurrentUserId();
        AgentRun run = agentRunMapper.selectOne(new LambdaQueryWrapper<AgentRun>()
                .eq(AgentRun::getRunId, runId)
                .eq(AgentRun::getUserId, userId)
                .last("LIMIT 1"));
        if (run == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        return Result.success(run);
    }

    @GetMapping("/{runId}/steps")
    public Result<List<AgentStep>> listSteps(@PathVariable String runId) {
        Long userId = UserContext.getCurrentUserId();
        Long count = agentRunMapper.selectCount(new LambdaQueryWrapper<AgentRun>()
                .eq(AgentRun::getRunId, runId)
                .eq(AgentRun::getUserId, userId));
        if (count == null || count == 0) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        List<AgentStep> steps = agentStepMapper.selectList(new LambdaQueryWrapper<AgentStep>()
                .eq(AgentStep::getRunId, runId)
                .eq(AgentStep::getUserId, userId)
                .orderByAsc(AgentStep::getCreatedAt)
                .orderByAsc(AgentStep::getId));
        return Result.success(steps);
    }
}
