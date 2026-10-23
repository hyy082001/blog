package com.yyh.blogserver.Auth.Service.impl;

import com.yyh.blogserver.Auth.Service.JwtService;
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
 * {@link JwtService} 的默认实现。
 *
 * <p>从配置读取 Base64 密钥，使用 HMAC-SHA 算法完成签发和验签。</p>
 */
@Service
public class JwtServiceImpl implements JwtService {

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
    public JwtServiceImpl(
            @Value("${security.jwt.secret}") String secret,
            @Value("${security.jwt.access-token-ttl}") Duration accessTokenTtl
    ) {
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.accessTokenTtl = accessTokenTtl;
    }

    @Override
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

    @Override
    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
