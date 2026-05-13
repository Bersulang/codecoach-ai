package com.codecoach.module.agent.tool.support;

import com.codecoach.common.exception.BusinessException;
import com.codecoach.common.result.ResultCode;
import java.util.Map;
import org.springframework.util.StringUtils;

public final class AgentToolSupport {

    public static final String DEFAULT_TARGET_ROLE = "Java 后端实习";
    public static final String DEFAULT_DIFFICULTY = "NORMAL";

    private AgentToolSupport() {
    }

    public static Long longParam(Map<String, Object> params, String key) {
        if (params == null || !params.containsKey(key)) {
            return null;
        }
        Object value = params.get(key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && StringUtils.hasText(text)) {
            try {
                return Long.parseLong(text.trim());
            } catch (NumberFormatException exception) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), key + " 参数格式错误");
            }
        }
        return null;
    }

    public static Integer intParam(Map<String, Object> params, String key, Integer defaultValue) {
        if (params == null || !params.containsKey(key)) {
            return defaultValue;
        }
        Object value = params.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text && StringUtils.hasText(text)) {
            try {
                return Integer.parseInt(text.trim());
            } catch (NumberFormatException exception) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), key + " 参数格式错误");
            }
        }
        return defaultValue;
    }

    public static String stringParam(Map<String, Object> params, String key, String defaultValue) {
        if (params == null || !params.containsKey(key)) {
            return defaultValue;
        }
        Object value = params.get(key);
        if (value instanceof String text && StringUtils.hasText(text)) {
            return text.trim();
        }
        return defaultValue;
    }
}
