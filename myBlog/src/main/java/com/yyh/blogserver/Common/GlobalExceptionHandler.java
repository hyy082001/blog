package com.yyh.blogserver.Common;

import com.yyh.blogserver.Common.enums.ErrorCode;
import com.yyh.blogserver.Common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Controller 及 Service 层异常的统一 JSON 响应处理器。
 *
 * <p>业务异常由 {@link BusinessException} 携带 {@link ErrorCode}，
 * 在这里统一转换为 {@link ApiResponse}。其他异常也使用相同结构，
 * 因此前端任何时候只会收到一种响应格式。</p>
 *
 * <p>JWT 过滤器和 Spring Security 拦截到的 401/403 发生在 Controller 之前，
 * 由 {@link ApiErrorResponseWriter} 直接输出相同结构。</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(
            GlobalExceptionHandler.class
    );

    /**
     * 处理 Service 层主动抛出的业务异常。
     *
     * <p>HTTP 状态码、业务错误码和用户提示都来自 {@link ErrorCode}，
     * Service 不需要重复指定这些信息。</p>
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(
            BusinessException exception
    ) {
        return buildResponse(
                exception.getErrorCode(),
                exception.getMessage()
        );
    }

    /**
     * 兜底处理非业务代码抛出的 ResponseStatusException。
     *
     * <p>项目业务代码统一使用 {@link BusinessException}；这里保留对
     * ResponseStatusException 的处理，避免框架或第三方代码的异常被误报为 500。</p>
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Void>> handleResponseStatusException(
            ResponseStatusException exception
    ) {
        HttpStatus status = HttpStatus.valueOf(exception.getStatusCode().value());
        String message = exception.getReason() != null
                ? exception.getReason()
                : status.getReasonPhrase();

        return ResponseEntity.status(status)
                .body(ApiResponse.error(status.value(), message));
    }

    /**
     * 将 @Valid 的字段校验失败转换成具体提示，例如“新密码长度必须在 8 到 72 个字符之间”。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(
            MethodArgumentNotValidException exception
    ) {
        FieldError fieldError = exception.getBindingResult().getFieldError();
        String message = fieldError != null && fieldError.getDefaultMessage() != null
                ? fieldError.getDefaultMessage()
                : ErrorCode.INVALID_PARAMETER.getMessage();

        return buildResponse(ErrorCode.INVALID_PARAMETER, message);
    }

    /**
     * 请求体不是合法 JSON、字段类型不匹配等属于客户端错误，
     * 不应落入兜底处理器后被误报为 500。
     */
    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleMalformedRequest() {
        return buildResponse(
                ErrorCode.INVALID_PARAMETER,
                ErrorCode.INVALID_PARAMETER.getMessage()
        );
    }

    /** 路径不存在时返回 404，而不是笼统的 500。 */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResourceFound() {
        return buildResponse(
                ErrorCode.NOT_FOUND,
                ErrorCode.NOT_FOUND.getMessage()
        );
    }

    /**
     * 未预期异常不向前端暴露数据库、堆栈等内部细节，但完整记录到后端日志，
     * 便于排查问题。
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(
            Exception exception,
            HttpServletRequest request
    ) {
        log.error(
                "未处理的服务器异常，path={}，异常类型={}",
                request.getRequestURI(),
                exception.getClass().getName(),
                exception
        );

        return buildResponse(
                ErrorCode.INTERNAL_ERROR,
                ErrorCode.INTERNAL_ERROR.getMessage()
        );
    }

    private ResponseEntity<ApiResponse<Void>> buildResponse(
            ErrorCode errorCode,
            String message
    ) {
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(ApiResponse.error(errorCode.getCode(), message));
    }
}
