package com.yyh.blogserver.Auth.Service;

import com.yyh.blogserver.Auth.DTO.AuthResponse;
import com.yyh.blogserver.Auth.DTO.ChangePasswordRequest;
import com.yyh.blogserver.Auth.DTO.LoginRequest;
import com.yyh.blogserver.Auth.DTO.RegisterRequest;
import com.yyh.blogserver.Auth.DTO.UpdateProfileRequest;
import com.yyh.blogserver.Auth.DTO.UserResponse;

/**
 * 注册、登录和用户资料相关的业务接口。
 *
 * <p>Controller 只依赖本接口，具体实现由
 * {@link com.yyh.blogserver.Auth.Service.impl.AuthServiceImpl} 提供。</p>
 */
public interface AuthService {

    /**
     * 注册普通用户。
     *
     * <p>公开注册的账号固定是普通用户角色，角色不能由前端传入，
     * 否则任何用户都能伪造管理员权限。</p>
     *
     * @param request 已通过 Controller 参数校验的注册信息
     * @return 不含密码哈希的安全用户信息
     */
    UserResponse register(RegisterRequest request);

    /**
     * 校验用户名和密码并签发 Access Token。
     *
     * @param request 包含用户名与密码的登录请求
     * @return JWT 和可安全展示的用户资料
     */
    AuthResponse login(LoginRequest request);

    /**
     * 根据 JWT 中的用户 ID 查询当前用户。
     *
     * @param userId JWT subject 中保存的用户主键
     * @return 当前用户的安全资料
     */
    UserResponse currentUser(Long userId);

    /**
     * 修改当前登录用户的基础资料。
     *
     * <p>当前用户身份由 JWT 决定，前端不传 userId。该方法只允许修改昵称和邮箱，
     * 不允许修改登录用户名、密码哈希或角色。</p>
     *
     * @param userId 从 JWT 解析出的当前用户 ID
     * @param request 前端提交的新昵称与邮箱
     * @return 更新后的安全用户资料
     */
    UserResponse updateProfile(Long userId, UpdateProfileRequest request);

    /**
     * 修改当前登录用户的密码。
     *
     * @param userId 从 JWT 解析得到的当前用户 ID
     * @param request 前端提交的旧密码和新密码
     */
    void changePassword(Long userId, ChangePasswordRequest request);
}
