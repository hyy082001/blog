package com.yyh.blogserver.Auth.Service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yyh.blogserver.Auth.DTO.AuthResponse;
import com.yyh.blogserver.Auth.DTO.ChangePasswordRequest;
import com.yyh.blogserver.Auth.DTO.LoginRequest;
import com.yyh.blogserver.Auth.DTO.RegisterRequest;
import com.yyh.blogserver.Auth.DTO.UpdateProfileRequest;
import com.yyh.blogserver.Auth.DTO.UserResponse;
import com.yyh.blogserver.Mapper.UserMapper;
import com.yyh.blogserver.Model.Role;
import com.yyh.blogserver.Model.User;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

/**
 * 注册、登录和当前用户查询的业务层。
 *
 * <p>Controller 只负责接收 HTTP 请求，Mapper 只负责读写数据库；本类负责把
 * 参数校验、重复检查、密码哈希、JWT 签发和响应对象转换组合成完整业务流程。</p>
 */
@Service
public class AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserMapper userMapper,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /**
     * 注册普通用户。
     *
     * <p>公开注册的账号固定是 {@link Role#USER}。角色不能由前端传入，
     * 否则任何用户都能伪造管理员权限。</p>
     *
     * @param request 已通过 Controller 参数校验的注册信息
     * @return 不含密码哈希的安全用户信息
     */
    @Transactional
    public UserResponse register(RegisterRequest request) {
        String username = request.username().trim();
        // 邮箱大小写通常不影响实际投递，因此存储和查询时统一转小写。
        String email = request.email().trim().toLowerCase(Locale.ROOT);

        if (userMapper.selectCount(
                Wrappers.<User>lambdaQuery().eq(User::getUsername, username)
        ) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "用户名已被使用");
        }

        if (userMapper.selectCount(
                Wrappers.<User>lambdaQuery().eq(User::getEmail, email)
        ) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "邮箱已被注册");
        }

        User user = new User();
        user.setUsername(username);
        // 新注册用户默认用登录用户名作为昵称，之后可在账号设置中单独修改。
        user.setNickname(username);
        user.setEmail(email);
        // BCrypt 是单向哈希：登录时只能调用 matches 比较，无法还原原始密码。
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(Role.USER);

        // BaseMapper.insert 会执行 INSERT，且把 PostgreSQL 自增生成的 ID 回填到 user.id。
        userMapper.insert(user);
        return toUserResponse(user);
    }

    /**
     * 校验用户名和密码并签发 Access Token。
     *
     * @param request 包含用户名与密码的登录请求
     * @return JWT 和可安全展示的用户资料
     */
    public AuthResponse login(LoginRequest request) {
        String username = request.username().trim();

        // 登录统一使用用户名查询，避免请求字段名与实际行为不一致。
        User user = userMapper.selectOne(
                Wrappers.<User>lambdaQuery()
                        .eq(User::getUsername, username)
        );

        // 无论用户名不存在还是密码错误，都返回相同提示，避免泄露用户名是否已注册。
        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户名或密码错误");
        }

        return new AuthResponse(
                jwtService.createAccessToken(user),
                toUserResponse(user)
        );
    }

    /**
     * 根据 JWT 中的用户 ID 查询当前用户。
     *
     * <p>每次请求数据库而不是完全相信 Token 中的用户资料，可以确保账号删除后，
     * 已持有旧 Token 的用户不会继续被视为有效用户。</p>
     *
     * @param userId JWT subject 中保存的用户主键
     * @return 当前用户的安全资料
     */
    public UserResponse currentUser(Long userId) {
        User user = userMapper.selectById(userId);

        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户不存在");
        }

        return toUserResponse(user);
    }

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
    @Transactional
    public UserResponse updateProfile(
            Long userId,
            UpdateProfileRequest request
    ) {
        User user = userMapper.selectById(userId);

        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户不存在");
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
            throw new ResponseStatusException(HttpStatus.CONFLICT, "邮箱已被注册");
        }

        user.setNickname(nickname);
        user.setEmail(email);
        userMapper.updateById(user);

        return toUserResponse(user);
    }

    /**
     * 修改当前登录用户的密码。
     *
     * <p>密码不会明文保存。先用 BCrypt 校验旧密码，
     * 再将新密码转换为新的 BCrypt 哈希后写入数据库。</p>
     *
     * @param userId 从 JWT 解析得到的当前用户 ID
     * @param request 前端提交的旧密码和新密码
     */
    @Transactional
    public void changePassword(
            Long userId,
            ChangePasswordRequest request
    ) {
        User user = userMapper.selectById(userId);

        if (user == null) {
            // JWT 中对应的用户已经不存在时，不能继续修改密码。
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户不存在");
        }

        /*
         * BCrypt 是单向哈希，不能将 passwordHash 解密为原密码。
         * matches(明文密码, 数据库哈希) 才是正确的验证方式。
         */
        if (!passwordEncoder.matches(request.oldPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "当前密码不正确"
            );
        }

        /*
         * BCrypt 每次 encode 都会使用不同盐值，因此不能比较两个哈希字符串。
         * 再次使用 matches 才能判断新密码是否与当前密码相同。
         */
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "新密码不能与当前密码相同"
            );
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
