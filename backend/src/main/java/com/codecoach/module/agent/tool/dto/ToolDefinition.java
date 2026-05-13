package com.codecoach.module.agent.tool.dto;

import com.codecoach.module.agent.tool.enums.ToolDisplayType;
import com.codecoach.module.agent.tool.enums.ToolExecutionMode;
import com.codecoach.module.agent.tool.enums.ToolRiskLevel;
import com.codecoach.module.agent.tool.enums.ToolType;
import java.util.Map;

public class ToolDefinition {

    private String toolName;
    private ToolType toolType;
    private ToolRiskLevel riskLevel;
    private ToolExecutionMode executionMode;
    private ToolDisplayType displayType;
    private String title;
    private String description;
    private String targetPath;
    private boolean requiresLogin = true;
    private boolean requiresConfirmation;
    private Map<String, Object> defaultParams;

    public ToolDefinition() {
    }

    public ToolDefinition(
            String toolName,
            ToolType toolType,
            ToolRiskLevel riskLevel,
            ToolExecutionMode executionMode,
            ToolDisplayType displayType,
            String title,
            String description,
            String targetPath,
            boolean requiresLogin,
            boolean requiresConfirmation,
            Map<String, Object> defaultParams
    ) {
        this.toolName = toolName;
        this.toolType = toolType;
        this.riskLevel = riskLevel;
        this.executionMode = executionMode;
        this.displayType = displayType;
        this.title = title;
        this.description = description;
        this.targetPath = targetPath;
        this.requiresLogin = requiresLogin;
        this.requiresConfirmation = requiresConfirmation;
        this.defaultParams = defaultParams;
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

    public boolean isRequiresLogin() {
        return requiresLogin;
    }

    public void setRequiresLogin(boolean requiresLogin) {
        this.requiresLogin = requiresLogin;
    }

    public boolean isRequiresConfirmation() {
        return requiresConfirmation;
    }

    public void setRequiresConfirmation(boolean requiresConfirmation) {
        this.requiresConfirmation = requiresConfirmation;
    }

    public Map<String, Object> getDefaultParams() {
        return defaultParams;
    }

    public void setDefaultParams(Map<String, Object> defaultParams) {
        this.defaultParams = defaultParams;
    }
}
