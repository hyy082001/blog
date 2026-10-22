package com.yyh.blogserver.Auth.DTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * {@code POST /api/auth/register} 接收的请求体。
 *
 * <p>当 Controller 参数添加 {@code @Valid} 时，Spring 会在请求进入认证服务前校验这些约束。
 * 本请求故意不提供 {@code role} 字段，客户端不能自行把账号注册为管理员。</p>
 *
 * @param username 由字母、数字、下划线组成的唯一登录名
 * @param email 用于联系和未来账号找回的唯一邮箱，不参与登录
 * @param password 仅在本次请求中接收的明文密码，入库前必须转换为 BCrypt 哈希
 */
public record RegisterRequest(
        /** 拒绝 null、空字符串和仅含空格的用户名。 */
        @NotBlank(message = "用户名不能为空")
        /** 控制用户名长度，便于展示和建立数据库索引。 */
        @Size(min = 3, max = 30, message = "用户名长度应为 3 到 30 位")
        /** 避免公开用户名中出现空格和特殊字符。 */
        @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户名只能包含字母、数字和下划线")
        String username,

        /** 先保证邮箱有值，再校验邮箱格式。 */
        @NotBlank(message = "邮箱不能为空")
        /** 在接口边界拒绝格式错误的邮箱。 */
        @Email(message = "邮箱格式不正确")
        String email,

        /** BCrypt 哈希前先校验密码。 */
        @NotBlank(message = "密码不能为空")
        /** BCrypt 最多处理 72 字节，此处提供第一层输入长度限制。 */
        @Size(min = 8, max = 72, message = "密码长度应为 8 到 72 位")
        String password
) {
}
