package com.yyh.blogserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot 应用程序入口。
 *
 * <p>{@link SpringBootApplication} 组合了配置、组件扫描和自动配置功能。
 * 本类位于根包 {@code com.yyh.blogserver}，因此 Spring 会自动扫描其子包，
 * 例如 {@code Auth}、{@code Mapper}、{@code Model} 和后续新增的 {@code Config}。</p>
 */
@SpringBootApplication
public class MyBlogApplication {

    /**
     * 启动内嵌 Web 服务器并创建 Spring 应用上下文。
     *
     * <p>启动过程中，Spring 会创建 MyBatis-Plus 的 Mapper 代理对象、
     * Spring Security 过滤器链、认证服务和 REST Controller 等 Bean。</p>
     *
     * @param args 由 IntelliJ 或命令行传入的启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(MyBlogApplication.class, args);
    }

}
