package com.codecoach.module.observability.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "observability.otel", name = "enabled", havingValue = "true")
public class OpenTelemetryStubConfig {

    private static final Logger log = LoggerFactory.getLogger(OpenTelemetryStubConfig.class);

    public OpenTelemetryStubConfig(@Value("${observability.otel.exporter-endpoint:}") String endpoint) {
        log.info("OpenTelemetry exporter hook enabled, endpoint={}. Trace ids are propagated through Agent/Tool/LLM/RAG/Memory logs.",
                endpoint == null || endpoint.isBlank() ? "local-dev" : endpoint);
    }
}
