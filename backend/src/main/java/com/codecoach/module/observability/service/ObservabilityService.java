package com.codecoach.module.observability.service;

import com.codecoach.module.observability.vo.ObservabilityAgentRunVO;
import com.codecoach.module.observability.vo.ObservabilityAgentStepVO;
import com.codecoach.module.observability.vo.ObservabilityAiCallVO;
import com.codecoach.module.observability.vo.ObservabilityRagTraceVO;
import com.codecoach.module.observability.vo.ObservabilitySingleFlightTraceVO;
import com.codecoach.module.observability.vo.ObservabilitySummaryVO;
import com.codecoach.module.observability.vo.ObservabilityToolTraceVO;
import java.util.List;

public interface ObservabilityService {

    ObservabilitySummaryVO summary();

    List<ObservabilityAgentRunVO> listAgentRuns(String agentType, String status, Integer limit);

    ObservabilityAgentRunVO getAgentRun(String runId);

    List<ObservabilityAgentStepVO> listAgentSteps(String runId);

    List<ObservabilityToolTraceVO> listToolTraces(String runId, String agentType, String toolName, Boolean success, Integer limit);

    List<ObservabilityAiCallVO> listAiCalls(String requestType, Boolean success, Integer limit);

    List<ObservabilityRagTraceVO> listRagTraces(Boolean success, Integer limit);

    List<ObservabilitySingleFlightTraceVO> listSingleFlightTraces(Boolean success, Integer limit);
}
