package com.yyh.blogserver.Auth.controller;

import com.yyh.blogserver.Auth.DTO.UpdateProfileRequest;
import com.yyh.blogserver.Auth.DTO.UserResponse;
import com.yyh.blogserver.Auth.DTO.ChangePasswordRequest;
import com.yyh.blogserver.Auth.Service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;

/**
 * 当前登录用户的资料接口。
 *
 * <p>注册和登录继续使用 {@code /api/auth}；这里操作的是用户资料，
 * 因此使用 {@code /api/users} 路径。</p>
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final AuthService authService;

    public UserController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 修改当前用户的昵称和邮箱。
     *
     * <p>SecurityConfig 会要求该请求已经认证；JwtAuthenticationFilter 验证 Token 后，
     * 会把当前用户 ID 放入 Authentication。</p>
     *
     * @param authentication 当前请求的认证信息
     * @param request 前端提交的新昵称和邮箱
     * @return 更新后的安全用户资料
     */
    @PutMapping("/me")
    public UserResponse updateMyProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        Long currentUserId = Long.parseLong(authentication.getName());

        return authService.updateProfile(currentUserId, request);
    }

    /**
     * 修改当前登录用户的密码。
     *
     * <p>该接口需要有效 JWT。用户 ID 由 JwtAuthenticationFilter 写入 Authentication，
     * 前端不能指定要修改哪个用户的密码。</p>
     *
     * @param authentication 当前请求的认证信息
     * @param request 旧密码和新密码
     */
    @PutMapping("/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changeMyPassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        Long currentUserId = Long.parseLong(authentication.getName());

        authService.changePassword(currentUserId, request);
    }
}
