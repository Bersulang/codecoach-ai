package com.codecoach.module.agent.tool.dto;

import com.codecoach.module.agent.tool.enums.ToolDisplayType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ToolExecuteResult {

    private boolean success;
    private String message;
    private Map<String, Object> data;
    private String targetPath;
    private String errorCode;
    private ToolDisplayType displayType;
    private List<ToolActionVO> nextActions = new ArrayList<>();
    private String traceId;

    public static ToolExecuteResult success(String message, Map<String, Object> data, String targetPath, ToolDisplayType displayType) {
        ToolExecuteResult result = new ToolExecuteResult();
        result.setSuccess(true);
        result.setMessage(message);
        result.setData(data);
        result.setTargetPath(targetPath);
        result.setDisplayType(displayType);
        return result;
    }

    public static ToolExecuteResult failure(String message, String errorCode, ToolDisplayType displayType) {
        ToolExecuteResult result = new ToolExecuteResult();
        result.setSuccess(false);
        result.setMessage(message);
        result.setErrorCode(errorCode);
        result.setDisplayType(displayType == null ? ToolDisplayType.ERROR : displayType);
        return result;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    public String getTargetPath() {
        return targetPath;
    }

    public void setTargetPath(String targetPath) {
        this.targetPath = targetPath;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public ToolDisplayType getDisplayType() {
        return displayType;
    }

    public void setDisplayType(ToolDisplayType displayType) {
        this.displayType = displayType;
    }

    public List<ToolActionVO> getNextActions() {
        return nextActions;
    }

    public void setNextActions(List<ToolActionVO> nextActions) {
        this.nextActions = nextActions == null ? new ArrayList<>() : nextActions;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }
}
