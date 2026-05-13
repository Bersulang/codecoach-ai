package com.codecoach.module.agent.function.controller;

import com.codecoach.common.result.Result;
import com.codecoach.module.agent.function.model.FunctionToolCall;
import com.codecoach.module.agent.function.service.FunctionCallExecutorAdapter;
import com.codecoach.module.agent.function.service.FunctionCallParser;
import com.codecoach.module.agent.function.service.ToolSchemaExporter;
import com.codecoach.module.agent.tool.dto.ToolExecuteResult;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dev/function-calling")
public class DevFunctionCallingController {

    private final ToolSchemaExporter toolSchemaExporter;
    private final FunctionCallParser functionCallParser;
    private final FunctionCallExecutorAdapter executorAdapter;

    public DevFunctionCallingController(
            ToolSchemaExporter toolSchemaExporter,
            FunctionCallParser functionCallParser,
            FunctionCallExecutorAdapter executorAdapter
    ) {
        this.toolSchemaExporter = toolSchemaExporter;
        this.functionCallParser = functionCallParser;
        this.executorAdapter = executorAdapter;
    }

    @GetMapping("/tools")
    public Result<List<Map<String, Object>>> tools() {
        return Result.success(toolSchemaExporter.exportOpenAiTools());
    }

    @PostMapping("/parse")
    public Result<List<FunctionToolCall>> parse(@RequestBody Map<String, Object> request) {
        Object raw = request == null ? null : request.get("rawResponse");
        return Result.success(functionCallParser.parse(raw == null ? null : String.valueOf(raw)));
    }

    @PostMapping("/execute")
    public Result<ToolExecuteResult> execute(@RequestBody FunctionToolCall call) {
        return Result.success(executorAdapter.execute(call));
    }

    @PostMapping("/poc/ability-summary")
    public Result<ToolExecuteResult> abilitySummaryPoc() {
        return Result.success(executorAdapter.execute(new FunctionToolCall("poc", "GET_ABILITY_SUMMARY", Map.of())));
    }
}
