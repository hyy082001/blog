package com.yyh.blogserver.Config;

import com.yyh.blogserver.Auth.filter.JwtAuthenticationFilter;
import com.yyh.blogserver.Common.ApiErrorResponseWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Spring Security 的全局安全配置。
 *
 * <p>本项目采用 JWT 无状态认证，因此不创建服务器 Session。每次受保护请求都必须携带
 * Bearer Token，由 {@link JwtAuthenticationFilter} 建立本次请求的用户身份。</p>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * 提供 BCrypt 密码哈希器。
     *
     * <p>BCrypt 会为每个密码生成随机盐值，因此相同密码的哈希结果也不同。
     * 登录时必须使用 {@code matches(明文, 哈希)}，而不是再次 encode 后比较字符串。</p>
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 配置请求认证、授权规则及过滤器顺序。
     *
     * @param http Spring Security 的 HTTP 配置入口
     * @param jwtAuthenticationFilter 负责从请求头恢复 JWT 身份的过滤器
     * @param apiErrorResponseWriter 负责将 401、403 写成统一 JSON 的工具
     * @return 应用于所有 Web 请求的安全过滤器链
     */
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            ApiErrorResponseWriter apiErrorResponseWriter
    ) throws Exception {
        return http
                // JWT 不依赖 Cookie Session，第一版关闭 CSRF 防护；若后续改用 Cookie，需重新评估。
                .csrf(AbstractHttpConfigurer::disable)
                // 允许 Vite 开发服务器调用后端接口。
                .cors(Customizer.withDefaults())
                // STATELESS 表示 Spring Security 不会在服务端保存登录 Session。
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                // Security 在进入 Controller 前产生的错误，也返回前端能解析的 JSON。
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) ->
                                apiErrorResponseWriter.write(
                                        response,
                                        401,
                                        "未登录或登录状态已失效",
                                        request.getRequestURI()
                                )
                        )
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                apiErrorResponseWriter.write(
                                        response,
                                        403,
                                        "无权访问此功能",
                                        request.getRequestURI()
                                )
                        )
                )
                .authorizeHttpRequests(auth -> auth
                        // 浏览器跨域预检请求不包含 JWT，必须先放行。
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // 仅注册与登录接口可匿名调用。
                        .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login").permitAll()
                        // WebSocket 握手不携带 JWT，而是由握手拦截器校验 Redis 一次性 ticket。
                        .requestMatchers("/ws/ai").permitAll()
                        // 其余接口默认必须通过 JWT 认证。
                        .anyRequest().authenticated()
                )
                // 在用户名密码认证过滤器之前恢复 JWT 身份。
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /**
     * 配置开发阶段的跨域规则。
     *
     * <p>前端 Vite 服务运行在 5173 端口，后端运行在 8080 端口，两者属于不同源。
     * 生产部署时应将此地址替换为真实前端域名，而不是使用通配符。</p>
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Vite 在默认端口被占用时会自动切换端口，例如 5174、5175。
        // 这里只允许本机开发服务器，不向任意互联网来源开放跨域。
        configuration.setAllowedOriginPatterns(List.of(
                "http://localhost:*",
                "http://127.0.0.1:*"
        ));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
