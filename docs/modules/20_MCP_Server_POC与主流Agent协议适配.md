# MCP Server POC 与主流 Agent 协议适配

本次改造先不强制引入 MCP Java Server 运行时依赖，避免影响主应用启动。当前完成的是 MCP Adapter POC：

- 默认关闭，通过 `MCP_POC_ENABLED=true` 启用。
- 默认仅允许本地访问，通过 `MCP_LOCAL_ONLY=true` 控制。
- 只导出低风险只读工具：`GET_ABILITY_SUMMARY`、`GET_RECENT_TRAINING_SUMMARY`、`SEARCH_KNOWLEDGE`、`GET_RESUME_RISK_SUMMARY`、`GET_USER_MEMORY_SUMMARY`。
- 调用仍然回到内部 `AgentToolExecutor`，模型或外部客户端不能绕过 Tool Registry、风险等级和用户隔离。

开发接口：

- `GET /api/dev/mcp/tools`
- `POST /api/dev/mcp/tools/{name}/call`

后续如果接入标准 MCP SDK，可复用 `McpToolAdapter` 的工具导出和调用逻辑，只替换传输层。
