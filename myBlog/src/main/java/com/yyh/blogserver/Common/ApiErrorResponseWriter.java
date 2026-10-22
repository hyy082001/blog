package com.yyh.blogserver.Common;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 在 Spring MVC Controller 之外输出 JSON 错误响应的工具。
 *
 * <p>JWT 过滤器和 Spring Security 的认证/授权失败发生在 Controller 之前，
 * 无法交给 {@link GlobalExceptionHandler} 处理。因此它们共用本类，确保响应格式一致。</p>
 */
@Component
public class ApiErrorResponseWriter {

    /**
     * 将错误状态和提示写入 HTTP 响应体。
     *
     * @param response 当前 HTTP 响应
     * @param status HTTP 状态码
     * @param message 前端可展示的错误说明
     * @param path 发生错误的请求路径
     */
    public void write(
            HttpServletResponse response,
            int status,
            String message,
            String path
    ) throws IOException {
        // 已有响应内容时不能重复写入，避免覆盖已经提交给客户端的数据。
        if (response.isCommitted()) {
            return;
        }

        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        /*
         * 此处不依赖 ObjectMapper：当前项目没有直接引入 Jackson 的编译依赖。
         * 字段值来自受控错误文案和请求路径，仍统一进行 JSON 转义，保证输出是合法 JSON。
         */
        String body = "{\"status\":" + status
                + ",\"message\":\"" + escapeJson(message)
                + "\",\"path\":\"" + escapeJson(path)
                + "\"}";
        response.getWriter().write(body);
    }

    /** 将字符串转为可安全放入 JSON 双引号中的文本。 */
    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
