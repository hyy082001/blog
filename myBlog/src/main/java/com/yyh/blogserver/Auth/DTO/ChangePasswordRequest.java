package com.yyh.blogserver.Auth.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 修改当前用户密码时的请求体。
 *
 * <p>确认密码只用于前端页面校验，后端只需要验证旧密码、
 * 校验新密码并保存新的 BCrypt 哈希。</p>
 *
 * @param oldPassword 当前正在使用的密码
 * @param newPassword 用户希望设置的新密码
 */
public record ChangePasswordRequest(

        /** 旧密码不能为空，用于与数据库中的 BCrypt 哈希进行比对。 */
        @NotBlank(message = "当前密码不能为空")
        @Size(max = 72, message = "当前密码长度不能超过 72 位")
        String oldPassword,

        /** 新密码不能为空，规则与注册密码保持一致。 */
        @NotBlank(message = "新密码不能为空")
        @Size(min = 8, max = 72, message = "新密码长度应为 8 到 72 位")
        String newPassword
) {
}
