package com.yyh.blogserver.Common;

/**
 * 统一 API 响应体。
 *
 * <p>所有返回 JSON 的接口都使用同一结构：{@code code} 表示结果码，
 * {@code message} 表示可直接展示给用户的提示，{@code data} 携带业务数据。
 * 前端只需要判断一次结构，无需为每个接口编写不同的解析逻辑。</p>
 *
 * <p>HTTP 状态码仍保持语义正确（200、201、400、401、403、404、409、500 等）；
 * 业务结果码 {@code code} 成功时固定为 200，失败时使用
 * {@link com.yyh.blogserver.Common.enums.ErrorCode} 中定义的五位业务错误码。
 * 框架兜底异常如果无法归类到业务错误码，则直接使用 HTTP 状态码。
 * 删除成功使用 204 No Content，按 HTTP 规范不返回响应体。</p>
 *
 * @param code 结果码；成功为 200，失败通常为 {@code ErrorCode} 中定义的值
 * @param message 面向用户的提示信息，不包含堆栈、SQL 等内部细节
 * @param data 业务数据；失败时为 {@code null}
 * @param <T> 业务数据类型
 */
public record ApiResponse<T>(
        int code,
        String message,
        T data
) {

    /** 成功结果码，与 HTTP 200 对应。 */
    public static final int SUCCESS_CODE = 200;

    private static final String SUCCESS_MESSAGE = "操作成功";

    /** 构造带业务数据的成功响应。 */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(SUCCESS_CODE, SUCCESS_MESSAGE, data);
    }

    /** 构造不带业务数据的成功响应。 */
    public static ApiResponse<Void> success() {
        return new ApiResponse<>(SUCCESS_CODE, SUCCESS_MESSAGE, null);
    }

    /**
     * 构造失败响应。
     *
     * @param code 结果码，通常来自 {@code ErrorCode#getCode()}
     * @param message 面向用户的错误提示
     */
    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }
}
