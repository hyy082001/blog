package com.yyh.blogserver.Auth.DTO;

/**
 * 登录成功后返回的响应。
 *
 * <p>前端在访问受保护接口时，把 {@code accessToken} 放入 HTTP 的
 * {@code Authorization} 请求头，格式为 {@code Bearer <accessToken>}。
 * 内嵌的 {@link UserResponse} 让页面无需额外请求 {@code /api/auth/me}，
 * 即可立即展示当前登录用户。</p>
 *
 * @param accessToken 已签名且有有效期的 JWT 访问令牌
 * @param user 当前用户的安全身份与角色信息
 */
public record AuthResponse(
        String accessToken,
        UserResponse user
) {
}
