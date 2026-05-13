package com.codecoach.module.agent.runtime.support;

public final class AgentRuntimeContextHolder {

    private static final ThreadLocal<AgentExecutionContext> HOLDER = new ThreadLocal<>();

    private AgentRuntimeContextHolder() {
    }

    public static void set(AgentExecutionContext context) {
        HOLDER.set(context);
    }

    public static AgentExecutionContext get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
