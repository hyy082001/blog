package com.yyh.blogserver.Auth.DTO;

import jakarta.validation.constraints.NotBlank;

/**
 * {@code POST /api/auth/login} 接收的请求体。
 *
 * @param username 用户提交的用户名
 * @param password 仅用于 BCrypt 比对的明文密码
 */
public record LoginRequest(
        /** 登录请求必须提供用户名。 */
        @NotBlank(message = "用户名不能为空")
        String username,

        /** 在查询数据库前拒绝空密码。 */
        @NotBlank(message = "密码不能为空")
        String password
) {
}
