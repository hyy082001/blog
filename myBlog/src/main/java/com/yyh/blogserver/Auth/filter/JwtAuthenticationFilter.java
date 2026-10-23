package com.yyh.blogserver.Auth.filter;

import com.yyh.blogserver.Auth.Service.JwtService;
import com.yyh.blogserver.Common.ApiErrorResponseWriter;
import com.yyh.blogserver.Common.enums.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * 将 HTTP 请求中的 JWT 转换为 Spring Security 认证信息的过滤器。
 *
 * <p>每个请求最多执行一次。它会读取 {@code Authorization: Bearer <token>}，
 * 验证 Token 后将用户身份放入 {@link SecurityContextHolder}。后续的权限规则
 * 才能知道当前请求属于哪个用户及其角色。</p>
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final ApiErrorResponseWriter apiErrorResponseWriter;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            ApiErrorResponseWriter apiErrorResponseWriter
    ) {
        this.jwtService = jwtService;
        this.apiErrorResponseWriter = apiErrorResponseWriter;
    }

    /**
     * 执行 JWT 认证。
     *
     * <p>没有 Authorization 请求头并不代表请求错误：注册、登录等公开接口
     * 本来就不携带 Token，因此直接继续后续过滤器即可。</p>
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");

        // 仅处理标准 Bearer Token；公开接口或未登录请求会继续进入权限判断。
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String token = authorization.substring(7);
            Claims claims = jwtService.parse(token);

            // JwtService 约定 subject 保存用户 ID，role claim 保存用户角色。
            String userId = claims.getSubject();
            String role = claims.get("role", String.class);

            // Spring Security 的角色权限约定以 ROLE_ 作为前缀。
            var authentication = new UsernamePasswordAuthenticationToken(
                    userId,
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + role))
            );

            // 将认证结果写入当前请求线程的安全上下文。
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } catch (JwtException | IllegalArgumentException exception) {
            // 过滤器不再调用 sendError，以统一 JSON 格式返回给前端。
            SecurityContextHolder.clearContext();
            apiErrorResponseWriter.write(
                    response,
                    ErrorCode.TOKEN_INVALID
            );
        }
    }
}
