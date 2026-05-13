package com.codecoach.module.observability.trace;

import com.codecoach.module.agent.runtime.support.AgentExecutionContext;
import com.codecoach.module.agent.runtime.support.AgentRuntimeContextHolder;
import java.util.UUID;
import org.springframework.util.StringUtils;

public final class TraceContextHolder {

    private static final ThreadLocal<String> TRACE_ID = new ThreadLocal<>();

    private TraceContextHolder() {
    }

    public static String getTraceId() {
        AgentExecutionContext agentContext = AgentRuntimeContextHolder.get();
        if (agentContext != null && StringUtils.hasText(agentContext.getTraceId())) {
            return agentContext.getTraceId();
        }
        return TRACE_ID.get();
    }

    public static String getOrCreateTraceId() {
        String existing = getTraceId();
        if (StringUtils.hasText(existing)) {
            return existing;
        }
        String traceId = UUID.randomUUID().toString();
        TRACE_ID.set(traceId);
        return traceId;
    }

    public static void setTraceId(String traceId) {
        if (StringUtils.hasText(traceId)) {
            TRACE_ID.set(traceId);
        }
    }

    public static void clear() {
        TRACE_ID.remove();
    }
}
