package com.codecoach.module.agent.tool.dto;

import com.codecoach.module.agent.tool.enums.ToolDisplayType;
import com.codecoach.module.agent.tool.enums.ToolExecutionMode;
import com.codecoach.module.agent.tool.enums.ToolRiskLevel;
import com.codecoach.module.agent.tool.enums.ToolType;
import java.util.LinkedHashMap;
import java.util.Map;

public class ToolActionVO {

    private String actionType;
    private String toolName;
    private ToolType toolType;
    private ToolRiskLevel riskLevel;
    private ToolExecutionMode executionMode;
    private ToolDisplayType displayType;
    private String title;
    private String description;
    private String targetPath;
    private boolean requiresConfirmation;
    private Map<String, Object> params = new LinkedHashMap<>();
    private String runId;
    private String traceId;

    public static ToolActionVO fromDefinition(ToolDefinition definition) {
        ToolActionVO vo = new ToolActionVO();
        vo.setActionType(definition.getToolName());
        vo.setToolName(definition.getToolName());
        vo.setToolType(definition.getToolType());
        vo.setRiskLevel(definition.getRiskLevel());
        vo.setExecutionMode(definition.getExecutionMode());
        vo.setDisplayType(definition.getDisplayType());
        vo.setTitle(definition.getTitle());
        vo.setDescription(definition.getDescription());
        vo.setTargetPath(definition.getTargetPath());
        vo.setRequiresConfirmation(definition.isRequiresConfirmation());
        if (definition.getDefaultParams() != null) {
            vo.setParams(new LinkedHashMap<>(definition.getDefaultParams()));
        }
        return vo;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public String getToolName() {
        return toolName;
    }

    public void setToolName(String toolName) {
        this.toolName = toolName;
    }

    public ToolType getToolType() {
        return toolType;
    }

    public void setToolType(ToolType toolType) {
        this.toolType = toolType;
    }

    public ToolRiskLevel getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(ToolRiskLevel riskLevel) {
        this.riskLevel = riskLevel;
    }

    public ToolExecutionMode getExecutionMode() {
        return executionMode;
    }

    public void setExecutionMode(ToolExecutionMode executionMode) {
        this.executionMode = executionMode;
    }

    public ToolDisplayType getDisplayType() {
        return displayType;
    }

    public void setDisplayType(ToolDisplayType displayType) {
        this.displayType = displayType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTargetPath() {
        return targetPath;
    }

    public void setTargetPath(String targetPath) {
        this.targetPath = targetPath;
    }

    public boolean isRequiresConfirmation() {
        return requiresConfirmation;
    }

    public void setRequiresConfirmation(boolean requiresConfirmation) {
        this.requiresConfirmation = requiresConfirmation;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public void setParams(Map<String, Object> params) {
        this.params = params == null ? new LinkedHashMap<>() : params;
    }

    public String getRunId() {
        return runId;
    }

    public void setRunId(String runId) {
        this.runId = runId;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }
}
