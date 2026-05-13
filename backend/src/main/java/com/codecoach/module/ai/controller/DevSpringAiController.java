package com.codecoach.module.ai.controller;

import com.codecoach.common.result.Result;
import com.codecoach.module.ai.gateway.AiModelGatewayRouter;
import com.codecoach.module.ai.gateway.model.AiChatRequest;
import com.codecoach.module.ai.gateway.model.AiChatResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dev/spring-ai")
public class DevSpringAiController {

    private final AiModelGatewayRouter gatewayRouter;

    public DevSpringAiController(AiModelGatewayRouter gatewayRouter) {
        this.gatewayRouter = gatewayRouter;
    }

    @PostMapping("/chat")
    public Result<SpringAiChatResponse> chat(@Valid @RequestBody SpringAiChatRequest request) {
        AiChatRequest chatRequest = new AiChatRequest();
        chatRequest.setUserMessage(request.message());
        chatRequest.setRequestType("SPRING_AI_POC");
        AiChatResponse response = gatewayRouter.require("spring-ai").chat(chatRequest);
        return Result.success(new SpringAiChatResponse(
                response.getContent(),
                response.getProvider(),
                response.getLatencyMs(),
                response.getTraceId()
        ));
    }

    public record SpringAiChatRequest(@NotBlank String message) {
    }

    public record SpringAiChatResponse(String answer, String provider, Long latencyMs, String traceId) {
    }
}
