package com.codecoach.module.observability.trace;

public final class SpanNames {

    public static final String AGENT_RUN = "agent.run";
    public static final String AGENT_STEP = "agent.step";
    public static final String TOOL_CALL = "tool.call";
    public static final String FUNCTION_CALL = "function.call";
    public static final String LLM_CALL = "llm.call";
    public static final String RAG_RETRIEVE = "rag.retrieve";
    public static final String MEMORY_SEARCH = "memory.search";
    public static final String SINGLE_FLIGHT_LOCK = "singleflight.lock";
    public static final String MCP_TOOL_CALL = "mcp.tool.call";

    private SpanNames() {
    }
}
