package com.codecoach.module.ai.gateway;

import com.codecoach.module.ai.gateway.model.AiChatRequest;
import com.codecoach.module.ai.gateway.model.AiChatResponse;
import com.codecoach.module.ai.gateway.model.AiStructuredChatRequest;
import com.codecoach.module.ai.gateway.model.ParsedToolIntent;
import com.codecoach.module.ai.service.AiTokenStreamHandler;
import java.util.List;

public interface AiModelGateway {

    String provider();

    AiChatResponse chat(AiChatRequest request);

    AiChatResponse streamChat(AiChatRequest request, AiTokenStreamHandler streamHandler);

    <T> T structuredChat(AiStructuredChatRequest request, Class<T> responseType);

    List<ParsedToolIntent> parseToolIntent(String modelResponse);
}
