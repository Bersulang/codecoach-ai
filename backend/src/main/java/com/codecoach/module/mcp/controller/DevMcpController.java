package com.codecoach.module.mcp.controller;

import com.codecoach.common.exception.BusinessException;
import com.codecoach.common.result.Result;
import com.codecoach.common.result.ResultCode;
import com.codecoach.module.agent.tool.dto.ToolExecuteResult;
import com.codecoach.module.mcp.config.McpProperties;
import com.codecoach.module.mcp.service.McpToolAdapter;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dev/mcp")
public class DevMcpController {

    private final McpToolAdapter mcpToolAdapter;
    private final McpProperties mcpProperties;

    public DevMcpController(McpToolAdapter mcpToolAdapter, McpProperties mcpProperties) {
        this.mcpToolAdapter = mcpToolAdapter;
        this.mcpProperties = mcpProperties;
    }

    @GetMapping("/tools")
    public Result<McpToolsResponse> listTools(HttpServletRequest request) {
        guard(request);
        return Result.success(new McpToolsResponse(mcpToolAdapter.listTools()));
    }

    @PostMapping("/tools/{name}/call")
    public Result<ToolExecuteResult> callTool(
            @PathVariable String name,
            @RequestBody(required = false) Map<String, Object> arguments,
            HttpServletRequest request
    ) {
        guard(request);
        return Result.success(mcpToolAdapter.callTool(name, arguments));
    }

    private void guard(HttpServletRequest request) {
        if (!Boolean.TRUE.equals(mcpProperties.getEnabled())) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "MCP POC 未启用");
        }
        if (Boolean.TRUE.equals(mcpProperties.getLocalOnly()) && !isLocal(request)) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "MCP POC 仅允许本地开发访问");
        }
    }

    private boolean isLocal(HttpServletRequest request) {
        String remote = request == null ? null : request.getRemoteAddr();
        return !StringUtils.hasText(remote)
                || "127.0.0.1".equals(remote)
                || "0:0:0:0:0:0:0:1".equals(remote)
                || "::1".equals(remote)
                || "localhost".equalsIgnoreCase(remote);
    }

    public record McpToolsResponse(List<Map<String, Object>> tools) {
    }
}
