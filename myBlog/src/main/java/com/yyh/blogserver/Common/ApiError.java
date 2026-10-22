package com.yyh.blogserver.Common;

/**
 * 后端统一的错误响应结构。
 *
 * <p>无论错误来自业务校验、Spring Security 还是 JWT 过滤器，前端都能从
 * {@code message} 字段读取可展示的提示信息。</p>
 *
 * @param status HTTP 状态码，例如 400、401、403、409
 * @param message 可直接展示给用户的错误说明
 * @param path 发生错误的请求路径
 */
public record ApiError(
        int status,
        String message,
        String path
) {
}
