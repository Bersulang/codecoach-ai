package com.codecoach.module.agent.tool.dto;

import java.util.LinkedHashMap;
import java.util.Map;

public class ToolExecuteRequest {

    private String toolName;
    private String agentType;
    private Boolean confirmed;
    private Map<String, Object> params = new LinkedHashMap<>();

    public String getToolName() {
        return toolName;
    }

    public void setToolName(String toolName) {
        this.toolName = toolName;
    }

    public String getAgentType() {
        return agentType;
    }

    public void setAgentType(String agentType) {
        this.agentType = agentType;
    }

    public Boolean getConfirmed() {
        return confirmed;
    }

    public void setConfirmed(Boolean confirmed) {
        this.confirmed = confirmed;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public void setParams(Map<String, Object> params) {
        this.params = params == null ? new LinkedHashMap<>() : params;
    }
}
