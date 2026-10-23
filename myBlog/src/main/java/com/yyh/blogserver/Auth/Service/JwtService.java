package com.yyh.blogserver.Auth.Service;

import com.yyh.blogserver.Model.User;
import io.jsonwebtoken.Claims;

/**
 * JWT 签发和解析的业务接口。
 *
 * <p>JWT 适合无状态认证：服务端不需要为每个登录用户保存 Session，
 * 令牌本身携带用户 ID、角色和过期时间，并由密钥签名防篡改。</p>
 *
 * <p>本接口只负责令牌格式与签名；哪个接口能访问、哪些角色有权限，
 * 仍由 Spring Security 的过滤器链负责。实现类为
 * {@link com.yyh.blogserver.Auth.Service.impl.JwtServiceImpl}。</p>
 */
public interface JwtService {

    /**
     * 为登录成功的用户创建 Access Token。
     *
     * @param user 已通过密码校验的用户
     * @return 已签名的 JWT 字符串
     */
    String createAccessToken(User user);

    /**
     * 解析并验证 JWT。
     *
     * <p>签名错误、令牌格式错误或过期时，JJWT 会抛出 {@code JwtException}，
     * 调用方应把该请求作为未认证请求处理。</p>
     *
     * @param token 去掉 {@code Bearer } 前缀后的 JWT
     * @return 经验证的 JWT 载荷
     */
    Claims parse(String token);
}
