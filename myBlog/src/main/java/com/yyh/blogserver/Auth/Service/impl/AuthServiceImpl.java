package com.yyh.blogserver.Auth.Service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yyh.blogserver.Auth.DTO.AuthResponse;
import com.yyh.blogserver.Auth.DTO.ChangePasswordRequest;
import com.yyh.blogserver.Auth.DTO.LoginRequest;
import com.yyh.blogserver.Auth.DTO.RegisterRequest;
import com.yyh.blogserver.Auth.DTO.UpdateProfileRequest;
import com.yyh.blogserver.Auth.DTO.UserResponse;
import com.yyh.blogserver.Auth.Service.AuthService;
import com.yyh.blogserver.Auth.Service.JwtService;
import com.yyh.blogserver.Auth.enums.Role;
import com.yyh.blogserver.Common.enums.ErrorCode;
import com.yyh.blogserver.Common.exception.BusinessException;
import com.yyh.blogserver.Mapper.UserMapper;
import com.yyh.blogserver.Model.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/**
 * {@link AuthService} 的默认实现。
 *
 * <p>负责参数校验、重复检查、密码哈希、JWT 签发和响应对象转换；
 * Controller 只接收 HTTP 请求，Mapper 只负责读写数据库。</p>
 */
@Service
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthServiceImpl(
            UserMapper userMapper,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Override
    @Transactional
    public UserResponse register(RegisterRequest request) {
        String username = request.username().trim();
        // 邮箱大小写通常不影响实际投递，因此存储和查询时统一转小写。
        String email = request.email().trim().toLowerCase(Locale.ROOT);

        if (userMapper.selectCount(
                Wrappers.<User>lambdaQuery().eq(User::getUsername, username)
        ) > 0) {
            throw new BusinessException(ErrorCode.USERNAME_TAKEN);
        }

        if (userMapper.selectCount(
                Wrappers.<User>lambdaQuery().eq(User::getEmail, email)
        ) > 0) {
            throw new BusinessException(ErrorCode.EMAIL_TAKEN);
        }

        // 新注册用户默认用登录用户名作为昵称，之后可在账号设置中单独修改。
        // BCrypt 是单向哈希：登录时只能调用 matches 比较，无法还原原始密码。
        User user = new User(
                username,
                username,
                email,
                passwordEncoder.encode(request.password()),
                Role.USER
        );

        // BaseMapper.insert 会执行 INSERT，且把 PostgreSQL 自增生成的 ID 回填到 user.id。
        userMapper.insert(user);
        return toUserResponse(user);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        String username = request.username().trim();

        // 登录统一使用用户名查询，避免请求字段名与实际行为不一致。
        User user = userMapper.selectOne(
                Wrappers.<User>lambdaQuery()
                        .eq(User::getUsername, username)
        );

        // 无论用户名不存在还是密码错误，都返回相同提示，避免泄露用户名是否已注册。
        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }

        return new AuthResponse(
                jwtService.createAccessToken(user),
                toUserResponse(user)
        );
    }

    @Override
    public UserResponse currentUser(Long userId) {
        User user = userMapper.selectById(userId);

        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        return toUserResponse(user);
    }

    @Override
    @Transactional
    public UserResponse updateProfile(
            Long userId,
            UpdateProfileRequest request
    ) {
        User user = userMapper.selectById(userId);

        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        String nickname = request.nickname().trim();
        // 邮箱统一转小写后再查询和保存，保证唯一性判断规则一致。
        String email = request.email().trim().toLowerCase(Locale.ROOT);

        /*
         * 排除当前用户自己后，检查新邮箱是否已被其他账号占用。
         * 因此用户仅修改昵称、不修改邮箱时不会被误判为邮箱重复。
         */
        boolean emailUsedByOtherUser = userMapper.selectCount(
                Wrappers.<User>lambdaQuery()
                        .eq(User::getEmail, email)
                        .ne(User::getId, userId)
        ) > 0;

        if (emailUsedByOtherUser) {
            throw new BusinessException(ErrorCode.EMAIL_TAKEN);
        }

        user.setNickname(nickname);
        user.setEmail(email);
        userMapper.updateById(user);

        return toUserResponse(user);
    }

    @Override
    @Transactional
    public void changePassword(
            Long userId,
            ChangePasswordRequest request
    ) {
        User user = userMapper.selectById(userId);

        if (user == null) {
            // JWT 中对应的用户已经不存在时，不能继续修改密码。
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        /*
         * BCrypt 是单向哈希，不能将 passwordHash 解密为原密码。
         * matches(明文密码, 数据库哈希) 才是正确的验证方式。
         */
        if (!passwordEncoder.matches(request.oldPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.CURRENT_PASSWORD_INCORRECT);
        }

        /*
         * BCrypt 每次 encode 都会使用不同盐值，因此不能比较两个哈希字符串。
         * 再次使用 matches 才能判断新密码是否与当前密码相同。
         */
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.PASSWORD_UNCHANGED);
        }

        // encode 后保存 BCrypt 哈希，绝不能直接保存明文新密码。
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userMapper.updateById(user);
    }

    /**
     * 将数据库实体转换为 API 响应对象。
     * 密码哈希不会通过此方法返回给前端。
     */
    private UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getEmail(),
                user.getRole()
        );
    }
}
