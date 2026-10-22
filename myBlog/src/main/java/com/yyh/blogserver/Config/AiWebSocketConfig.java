package com.yyh.blogserver.Config;

import com.yyh.blogserver.Ai.WebSocket.AiHandshakeInterceptor;
import com.yyh.blogserver.Ai.WebSocket.AiWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/** 注册 AI 流式回复的 WebSocket 地址。 */
@Configuration
@EnableWebSocket
public class AiWebSocketConfig implements WebSocketConfigurer {

    private final AiWebSocketHandler aiWebSocketHandler;
    private final AiHandshakeInterceptor aiHandshakeInterceptor;

    public AiWebSocketConfig(
            AiWebSocketHandler aiWebSocketHandler,
            AiHandshakeInterceptor aiHandshakeInterceptor
    ) {
        this.aiWebSocketHandler = aiWebSocketHandler;
        this.aiHandshakeInterceptor = aiHandshakeInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(aiWebSocketHandler, "/ws/ai")
                .addInterceptors(aiHandshakeInterceptor)
                .setAllowedOriginPatterns(
                        "http://localhost:*",
                        "http://127.0.0.1:*"
                );
    }
}
