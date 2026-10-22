package com.yyh.blogserver.Common;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

/**
 * Controller 及 Service 层异常的统一 JSON 响应处理器。
 *
 * <p>它只处理已经进入 Spring MVC 的请求。JWT 过滤器和 Spring Security 拦截到的
 * 401/403 会由 {@link ApiErrorResponseWriter} 直接输出。</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 将 Service 中主动抛出的业务异常转换成前端可读的 JSON。
     * 例如“新密码不能与当前密码相同”会返回 400，而不是空响应体。
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> handleResponseStatusException(
            ResponseStatusException exception,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.valueOf(exception.getStatusCode().value());
        String message = exception.getReason() != null
                ? exception.getReason()
                : "请求处理失败";

        return buildResponse(status, message, request);
    }

    /**
     * 将 @Valid 的字段校验失败转换成具体提示，例如“新密码长度必须在 8 到 72 个字符之间”。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationException(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        FieldError fieldError = exception.getBindingResult().getFieldError();
        String message = fieldError != null && fieldError.getDefaultMessage() != null
                ? fieldError.getDefaultMessage()
                : "提交的数据不符合要求";

        return buildResponse(HttpStatus.BAD_REQUEST, message, request);
    }

    /**
     * 未预期异常不向前端暴露数据库、堆栈等内部细节，但仍保证返回 JSON。
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpectedException(HttpServletRequest request) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "服务器内部错误，请稍后重试", request);
    }

    private ResponseEntity<ApiError> buildResponse(
            HttpStatus status,
            String message,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(status)
                .body(new ApiError(status.value(), message, request.getRequestURI()));
    }
}
