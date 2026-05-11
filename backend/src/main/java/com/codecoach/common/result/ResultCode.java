package com.codecoach.common.result;

public enum ResultCode {

    SUCCESS(200, "success"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录"),
    FORBIDDEN(403, "无权限"),
    NOT_FOUND(404, "资源不存在"),
    ANSWER_PROCESSING(3004, "当前回答正在处理中，请稍后"),
    KNOWLEDGE_ARTICLE_NOT_FOUND(5101, "知识文章不存在"),
    KNOWLEDGE_ARTICLE_CONTENT_NOT_FOUND(5102, "知识文章内容不存在"),
    INTERNAL_ERROR(500, "系统异常");

    private final Integer code;

    private final String message;

    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public Integer getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
