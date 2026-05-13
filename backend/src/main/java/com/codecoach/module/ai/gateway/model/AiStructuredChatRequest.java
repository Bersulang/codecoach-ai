package com.codecoach.module.ai.gateway.model;

public class AiStructuredChatRequest extends AiChatRequest {

    private Class<?> responseType;

    public Class<?> getResponseType() {
        return responseType;
    }

    public void setResponseType(Class<?> responseType) {
        this.responseType = responseType;
    }
}
