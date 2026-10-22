package com.yyh.blogserver.Auth.Service;

import com.yyh.blogserver.Model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/**
 * JWT 的签发和解析服务。
 *
 * <p>JWT 适合无状态认证：服务端不需要为每个登录用户保存 Session，
 * 令牌本身携带用户 ID、角色和过期时间，并由密钥签名防篡改。</p>
 *
 * <p>本类只负责令牌格式与签名；哪个接口能访问、哪些角色有权限，
 * 仍由 Spring Security 的过滤器链负责。</p>
 */
@Service
public class JwtService {

    /** 用于 HMAC 签名与验签的密钥。 */
    private final SecretKey secretKey;

    /** Access Token 的有效期，例如 application.yml 中的 2h。 */
    private final Duration accessTokenTtl;

    /**
     * 从配置中读取 Base64 格式密钥并转换为 Java 的签名密钥对象。
     *
     * @param secret Base64 编码的随机密钥，不应硬编码或提交到 Git
     * @param accessTokenTtl Access Token 的有效期
     */
    public JwtService(
            @Value("${security.jwt.secret}") String secret,
            @Value("${security.jwt.access-token-ttl}") Duration accessTokenTtl
    ) {
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.accessTokenTtl = accessTokenTtl;
    }

    /**
     * 为登录成功的用户创建 Access Token。
     *
     * <p>Token 只保存身份识别所需的最小信息：用户 ID 放在 subject 中，
     * 角色放在 claim 中。密码、邮箱等敏感或易变化信息不应写入 JWT。</p>
     *
     * @param user 已通过密码校验的用户
     * @return 已签名的 JWT 字符串
     */
    public String createAccessToken(User user) {
        Instant now = Instant.now();

        return Jwts.builder()
                // subject 是 JWT 标准字段，此处约定保存用户主键。
                .subject(user.getId().toString())
                // Spring Security 后续将读取此字段构造 ROLE_USER 或 ROLE_ADMIN 权限。
                .claim("role", user.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTokenTtl)))
                .signWith(secretKey)
                .compact();
    }

    /**
     * 解析并验证 JWT。
     *
     * <p>解析过程会同时检查签名是否由当前密钥生成，以及令牌是否已经过期。
     * 签名错误、令牌格式错误或过期时，JJWT 会抛出 {@code JwtException}，
     * 调用方应把该请求作为未认证请求处理。</p>
     *
     * @param token 去掉 {@code Bearer } 前缀后的 JWT
     * @return 经验证的 JWT 载荷
     */
    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
