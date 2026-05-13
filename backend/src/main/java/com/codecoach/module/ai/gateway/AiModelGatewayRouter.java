package com.codecoach.module.ai.gateway;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AiModelGatewayRouter {

    private final List<AiModelGateway> gateways;

    public AiModelGatewayRouter(List<AiModelGateway> gateways) {
        this.gateways = gateways;
    }

    public AiModelGateway primary(String provider) {
        if (StringUtils.hasText(provider)) {
            for (AiModelGateway gateway : gateways) {
                if (provider.trim().equalsIgnoreCase(gateway.provider())) {
                    return gateway;
                }
            }
        }
        return gateways.stream()
                .filter(gateway -> "openai-compatible".equalsIgnoreCase(gateway.provider()))
                .findFirst()
                .orElseGet(() -> gateways.isEmpty() ? null : gateways.get(0));
    }

    public AiModelGateway require(String provider) {
        AiModelGateway gateway = primary(provider);
        if (gateway == null) {
            throw new IllegalStateException("No AI model gateway available");
        }
        return gateway;
    }
}
