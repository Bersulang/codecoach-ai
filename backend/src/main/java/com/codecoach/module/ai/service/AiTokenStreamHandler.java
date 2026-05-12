package com.codecoach.module.ai.service;

@FunctionalInterface
public interface AiTokenStreamHandler {

    void onDelta(String content);
}
