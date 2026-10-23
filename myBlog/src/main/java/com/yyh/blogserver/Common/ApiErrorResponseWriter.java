package com.yyh.blogserver.Common;

import com.yyh.blogserver.Common.enums.ErrorCode;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 在 Spring MVC Controller 之外输出统一 JSON 错误响应的工具。
 *
 * <p>JWT 过滤器和 Spring Security 的认证/授权失败发生在 Controller 之前，
 * 无法交给 {@link GlobalExceptionHandler} 处理。因此它们共用本类，
 * 输出与 Controller 相同的 {@link ApiResponse} 结构。</p>
 */
@Component
public class ApiErrorResponseWriter {

    private final ObjectMapper objectMapper;

    public ApiErrorResponseWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 将错误状态和提示写入 HTTP 响应体。
     *
     * @param response 当前 HTTP 响应
     * @param errorCode 统一的业务错误码，同时提供 HTTP 状态码和用户提示
     */
    public void write(
            HttpServletResponse response,
            ErrorCode errorCode
    ) throws IOException {
        // 已有响应内容时不能重复写入，避免覆盖已经提交给客户端的数据。
        if (response.isCommitted()) {
            return;
        }

        response.setStatus(errorCode.getHttpStatus().value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        // 复用 Spring Boot 配置的 ObjectMapper，保证与 Controller 响应格式一致。
        String body = objectMapper.writeValueAsString(
                ApiResponse.error(errorCode.getCode(), errorCode.getMessage())
        );
        response.getWriter().write(body);
    }
}
