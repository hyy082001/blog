package com.yyh.blogserver.Common.exception;

import com.yyh.blogserver.Common.enums.ErrorCode;

/**
 * 统一的业务异常。
 *
 * <p>Service 层发现业务规则不满足时抛出本异常，由
 * {@link com.yyh.blogserver.Common.GlobalExceptionHandler} 统一转换为
 * {@code ApiResponse}，避免业务代码各自返回不同的错误结构。</p>
 *
 * <p>异常携带 {@link ErrorCode}，因此 HTTP 状态码、业务错误码和用户提示
 * 都来自同一处定义，不会在多个类中重复维护。</p>
 */
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
