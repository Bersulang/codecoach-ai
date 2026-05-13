package com.codecoach.module.observability.controller;

import com.codecoach.common.result.Result;
import com.codecoach.module.observability.service.ObservabilityService;
import com.codecoach.module.observability.vo.ObservabilityAgentRunVO;
import com.codecoach.module.observability.vo.ObservabilityAgentStepVO;
import com.codecoach.module.observability.vo.ObservabilityAiCallVO;
import com.codecoach.module.observability.vo.ObservabilitySummaryVO;
import com.codecoach.module.observability.vo.ObservabilityToolTraceVO;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dev/observability")
public class DevObservabilityController {

    private final ObservabilityService observabilityService;

    public DevObservabilityController(ObservabilityService observabilityService) {
        this.observabilityService = observabilityService;
    }

    @GetMapping("/summary")
    public Result<ObservabilitySummaryVO> summary() {
        return Result.success(observabilityService.summary());
    }

    @GetMapping("/agent-runs")
    public Result<List<ObservabilityAgentRunVO>> listAgentRuns(
            @RequestParam(required = false) String agentType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer limit
    ) {
        return Result.success(observabilityService.listAgentRuns(agentType, status, limit));
    }

    @GetMapping("/agent-runs/{runId}")
    public Result<ObservabilityAgentRunVO> getAgentRun(@PathVariable String runId) {
        return Result.success(observabilityService.getAgentRun(runId));
    }

    @GetMapping("/agent-runs/{runId}/steps")
    public Result<List<ObservabilityAgentStepVO>> listAgentSteps(@PathVariable String runId) {
        return Result.success(observabilityService.listAgentSteps(runId));
    }

    @GetMapping("/tool-traces")
    public Result<List<ObservabilityToolTraceVO>> listToolTraces(
            @RequestParam(required = false) String runId,
            @RequestParam(required = false) String agentType,
            @RequestParam(required = false) String toolName,
            @RequestParam(required = false) Boolean success,
            @RequestParam(required = false) Integer limit
    ) {
        return Result.success(observabilityService.listToolTraces(runId, agentType, toolName, success, limit));
    }

    @GetMapping("/ai-calls")
    public Result<List<ObservabilityAiCallVO>> listAiCalls(
            @RequestParam(required = false) String requestType,
            @RequestParam(required = false) Boolean success,
            @RequestParam(required = false) Integer limit
    ) {
        return Result.success(observabilityService.listAiCalls(requestType, success, limit));
    }
}
