package com.codecoach.module.ai.gateway.model;

import java.util.List;
import java.util.Map;

public class AiChatRequest {

    private List<AiChatMessage> messages;
    private String systemPrompt;
    private String userMessage;
    private Double temperature;
    private String requestType;
    private String promptVersion;
    private List<Map<String, Object>> toolSchemas;

    public List<AiChatMessage> getMessages() {
        return messages;
    }

    public void setMessages(List<AiChatMessage> messages) {
        this.messages = messages;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    public String getUserMessage() {
        return userMessage;
    }

    public void setUserMessage(String userMessage) {
        this.userMessage = userMessage;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public String getRequestType() {
        return requestType;
    }

    public void setRequestType(String requestType) {
        this.requestType = requestType;
    }

    public String getPromptVersion() {
        return promptVersion;
    }

    public void setPromptVersion(String promptVersion) {
        this.promptVersion = promptVersion;
    }

    public List<Map<String, Object>> getToolSchemas() {
        return toolSchemas;
    }

    public void setToolSchemas(List<Map<String, Object>> toolSchemas) {
        this.toolSchemas = toolSchemas;
    }
}
