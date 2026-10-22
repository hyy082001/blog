package com.yyh.blogserver.Auth.DTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 修改当前用户基础资料时的请求体。
 *
 * <p>该对象故意不包含 id、username、password 和 role，
 * 防止客户端越权修改账号身份、密码或权限。</p>
 *
 * @param nickname 博客中公开展示的昵称
 * @param email 联系邮箱，不参与登录
 */
public record UpdateProfileRequest(

        /** 昵称不能为空，最多 30 个字符。 */
        @NotBlank(message = "昵称不能为空")
        @Size(max = 30, message = "昵称不能超过 30 个字符")
        String nickname,

        /** 邮箱不能为空，且必须符合邮箱格式。 */
        @NotBlank(message = "邮箱不能为空")
        @Email(message = "邮箱格式不正确")
        String email
) {
}
