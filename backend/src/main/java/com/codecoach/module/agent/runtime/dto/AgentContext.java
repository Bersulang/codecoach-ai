package com.codecoach.module.agent.runtime.dto;

import com.codecoach.module.agent.tool.dto.ToolActionVO;
import java.util.ArrayList;
import java.util.List;

public class AgentContext {

    private String runId;
    private String traceId;
    private Long userId;
    private String agentType;
    private String currentPath;
    private String userMessage;
    private boolean loggedIn;
    private String userSummary;
    private List<ToolActionVO> availableTools = new ArrayList<>();
    private String pageDescription;
    private String recentTrainingSummary;
    private String abilitySummary;
    private String resumeRiskSummary;
    private String latestReviewSummary;

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

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getAgentType() {
        return agentType;
    }

    public void setAgentType(String agentType) {
        this.agentType = agentType;
    }

    public String getCurrentPath() {
        return currentPath;
    }

    public void setCurrentPath(String currentPath) {
        this.currentPath = currentPath;
    }

    public String getUserMessage() {
        return userMessage;
    }

    public void setUserMessage(String userMessage) {
        this.userMessage = userMessage;
    }

    public boolean isLoggedIn() {
        return loggedIn;
    }

    public void setLoggedIn(boolean loggedIn) {
        this.loggedIn = loggedIn;
    }

    public String getUserSummary() {
        return userSummary;
    }

    public void setUserSummary(String userSummary) {
        this.userSummary = userSummary;
    }

    public List<ToolActionVO> getAvailableTools() {
        return availableTools;
    }

    public void setAvailableTools(List<ToolActionVO> availableTools) {
        this.availableTools = availableTools == null ? new ArrayList<>() : availableTools;
    }

    public String getPageDescription() {
        return pageDescription;
    }

    public void setPageDescription(String pageDescription) {
        this.pageDescription = pageDescription;
    }

    public String getRecentTrainingSummary() {
        return recentTrainingSummary;
    }

    public void setRecentTrainingSummary(String recentTrainingSummary) {
        this.recentTrainingSummary = recentTrainingSummary;
    }

    public String getAbilitySummary() {
        return abilitySummary;
    }

    public void setAbilitySummary(String abilitySummary) {
        this.abilitySummary = abilitySummary;
    }

    public String getResumeRiskSummary() {
        return resumeRiskSummary;
    }

    public void setResumeRiskSummary(String resumeRiskSummary) {
        this.resumeRiskSummary = resumeRiskSummary;
    }

    public String getLatestReviewSummary() {
        return latestReviewSummary;
    }

    public void setLatestReviewSummary(String latestReviewSummary) {
        this.latestReviewSummary = latestReviewSummary;
    }
}
