package com.yyh.blogserver.Common.enums;

import org.springframework.http.HttpStatus;

/**
 * 统一业务错误码。
 *
 * <p>错误码采用 5 位数字：前 3 位与 HTTP 状态码对应，后 2 位区分具体业务场景，
 * 例如 {@code 40401} 表示“文章不存在”。错误码用于前端判断和日志排查，
 * {@link #getMessage()} 才是可以直接展示给用户的提示。</p>
 */
public enum ErrorCode {

    /** 通用参数错误。 */
    INVALID_PARAMETER(40000, HttpStatus.BAD_REQUEST, "请求参数不正确"),

    /** 修改密码时旧密码错误。 */
    CURRENT_PASSWORD_INCORRECT(40001, HttpStatus.BAD_REQUEST, "当前密码不正确"),

    /** 新密码与当前密码相同。 */
    PASSWORD_UNCHANGED(40002, HttpStatus.BAD_REQUEST, "新密码不能与当前密码相同"),

    /** 未登录或登录状态已失效。 */
    UNAUTHORIZED(40100, HttpStatus.UNAUTHORIZED, "未登录或登录状态已失效"),

    /** JWT 无效、被篡改或已过期。 */
    TOKEN_INVALID(40101, HttpStatus.UNAUTHORIZED, "无效或已过期的令牌"),

    /** 用户名或密码错误。 */
    LOGIN_FAILED(40102, HttpStatus.UNAUTHORIZED, "用户名或密码错误"),

    /** JWT 对应的用户已不存在。 */
    USER_NOT_FOUND(40103, HttpStatus.UNAUTHORIZED, "用户不存在"),

    /** 没有访问该功能的权限。 */
    FORBIDDEN(40300, HttpStatus.FORBIDDEN, "无权访问此功能"),

    /** 试图操作其他用户的文章。 */
    ARTICLE_FORBIDDEN(40301, HttpStatus.FORBIDDEN, "无权操作其他用户的文章"),

    /** 试图操作其他用户的 AI 会话。 */
    CONVERSATION_FORBIDDEN(40302, HttpStatus.FORBIDDEN, "无权操作其他用户的 AI 会话"),

    /** 请求的资源不存在。 */
    NOT_FOUND(40400, HttpStatus.NOT_FOUND, "请求的资源不存在"),

    /** 文章不存在。 */
    ARTICLE_NOT_FOUND(40401, HttpStatus.NOT_FOUND, "文章不存在"),

    /** 文章不存在或尚未发布。 */
    ARTICLE_NOT_PUBLISHED(40402, HttpStatus.NOT_FOUND, "文章不存在或尚未发布"),

    /** AI 会话不存在。 */
    CONVERSATION_NOT_FOUND(40403, HttpStatus.NOT_FOUND, "AI 会话不存在"),

    /** 通用的数据冲突。 */
    CONFLICT(40900, HttpStatus.CONFLICT, "数据冲突"),

    /** 注册或改名时用户名已被占用。 */
    USERNAME_TAKEN(40901, HttpStatus.CONFLICT, "用户名已被使用"),

    /** 注册或修改资料时邮箱已被占用。 */
    EMAIL_TAKEN(40902, HttpStatus.CONFLICT, "邮箱已被注册"),

    /** 未预期的服务器内部错误。 */
    INTERNAL_ERROR(50000, HttpStatus.INTERNAL_SERVER_ERROR, "服务器内部错误，请稍后重试"),

    /** 通用外部服务错误。 */
    BAD_GATEWAY(50200, HttpStatus.BAD_GATEWAY, "外部服务暂不可用"),

    /** AI 服务暂时不可用。 */
    AI_SERVICE_UNAVAILABLE(50201, HttpStatus.BAD_GATEWAY, "AI 服务暂时不可用，请稍后重试");

    private final int code;
    private final HttpStatus httpStatus;
    private final String message;

    ErrorCode(int code, HttpStatus httpStatus, String message) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.message = message;
    }

    /** 业务错误码，例如 40401。 */
    public int getCode() {
        return code;
    }

    /** 该错误对应的 HTTP 状态码。 */
    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    /** 可直接展示给用户的提示信息。 */
    public String getMessage() {
        return message;
    }
}
