package com.yyh.blogserver.Auth.controller;

import com.yyh.blogserver.Auth.DTO.AuthResponse;
import com.yyh.blogserver.Auth.DTO.LoginRequest;
import com.yyh.blogserver.Auth.DTO.RegisterRequest;
import com.yyh.blogserver.Auth.DTO.UserResponse;
import com.yyh.blogserver.Auth.Service.AuthService;
import com.yyh.blogserver.Common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 面向前端的认证 REST 接口。
 *
 * <p>接口统一以 {@code /api/auth} 为前缀。注册和登录是公开接口；
 * {@code /me} 需要 JWT 过滤器先完成认证。</p>
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 注册普通用户。
     *
     * @param request 前端提交的注册资料；{@code @Valid} 会执行 DTO 上的格式校验
     * @return HTTP 201 及新用户的安全资料
     */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UserResponse> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        return ApiResponse.success(authService.register(request));
    }

    /**
     * 登录并签发 JWT Access Token。
     *
     * @param request 前端提交的用户名及密码
     * @return Access Token 和当前用户资料
     */
    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    /**
     * 返回当前登录用户。
     *
     * <p>JWT 过滤器把用户 ID 写入 Authentication 的 principal，
     * 本接口再用该 ID 查询数据库取得最新用户资料。</p>
     *
     * @param authentication Spring Security 在 JWT 验证成功后注入的认证对象
     * @return 当前用户的安全资料
     */
    @GetMapping("/me")
    public ApiResponse<UserResponse> me(Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        return ApiResponse.success(authService.currentUser(userId));
    }
}
