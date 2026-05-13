package com.codecoach.module.agent.runtime.dto;

import com.codecoach.module.agent.runtime.enums.AgentStepStatus;
import com.codecoach.module.agent.runtime.enums.AgentStepType;

public class AgentStepRecord {

    private AgentStepType stepType;
    private String stepName;
    private String toolName;
    private String inputSummary;
    private String outputSummary;
    private AgentStepStatus status = AgentStepStatus.SUCCEEDED;
    private String errorCode;
    private String errorMessage;
    private long latencyMs;

    public AgentStepType getStepType() {
        return stepType;
    }

    public void setStepType(AgentStepType stepType) {
        this.stepType = stepType;
    }

    public String getStepName() {
        return stepName;
    }

    public void setStepName(String stepName) {
        this.stepName = stepName;
    }

    public String getToolName() {
        return toolName;
    }

    public void setToolName(String toolName) {
        this.toolName = toolName;
    }

    public String getInputSummary() {
        return inputSummary;
    }

    public void setInputSummary(String inputSummary) {
        this.inputSummary = inputSummary;
    }

    public String getOutputSummary() {
        return outputSummary;
    }

    public void setOutputSummary(String outputSummary) {
        this.outputSummary = outputSummary;
    }

    public AgentStepStatus getStatus() {
        return status;
    }

    public void setStatus(AgentStepStatus status) {
        this.status = status;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public long getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(long latencyMs) {
        this.latencyMs = latencyMs;
    }
}
