package com.codecoach.module.agent.tool.service;

import com.codecoach.common.exception.BusinessException;
import com.codecoach.common.result.ResultCode;
import com.codecoach.module.agent.tool.dto.ToolActionVO;
import com.codecoach.module.agent.tool.dto.ToolDefinition;
import com.codecoach.module.agent.tool.enums.ToolRiskLevel;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AgentToolRegistry {

    private final Map<String, AgentTool> tools;

    public AgentToolRegistry(List<AgentTool> toolList) {
        Map<String, AgentTool> registered = new LinkedHashMap<>();
        for (AgentTool tool : toolList) {
            ToolDefinition definition = tool.definition();
            if (definition == null || !StringUtils.hasText(definition.getToolName())) {
                continue;
            }
            if (definition.getRiskLevel() == ToolRiskLevel.HIGH) {
                continue;
            }
            registered.put(definition.getToolName(), tool);
        }
        this.tools = Map.copyOf(registered);
    }

    public AgentTool requireTool(String toolName) {
        AgentTool tool = getTool(toolName);
        if (tool == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "未知 Tool，已拒绝执行");
        }
        return tool;
    }

    public AgentTool getTool(String toolName) {
        if (!StringUtils.hasText(toolName)) {
            return null;
        }
        return tools.get(toolName.trim());
    }

    public List<ToolDefinition> listDefinitions() {
        return tools.values().stream()
                .map(AgentTool::definition)
                .sorted(Comparator.comparing(ToolDefinition::getToolName))
                .toList();
    }

    public List<ToolActionVO> listActions() {
        return listDefinitions().stream()
                .map(ToolActionVO::fromDefinition)
                .toList();
    }

    public ToolActionVO toAction(String toolName) {
        AgentTool tool = getTool(toolName);
        return tool == null ? null : ToolActionVO.fromDefinition(tool.definition());
    }

    public List<String> toolNames() {
        return listDefinitions().stream().map(ToolDefinition::getToolName).toList();
    }
}
