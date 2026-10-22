package com.yyh.blogserver.Ai.WebSocket;

import com.yyh.blogserver.Ai.DTO.AiStreamTicket;
import com.yyh.blogserver.Ai.Service.AiStreamTicketService;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

/**
 * WebSocket 握手阶段消费 Redis ticket，并把已验证的身份写入 session。
 */
@Component
public class AiHandshakeInterceptor implements HandshakeInterceptor {

    private final AiStreamTicketService aiStreamTicketService;

    public AiHandshakeInterceptor(AiStreamTicketService aiStreamTicketService) {
        this.aiStreamTicketService = aiStreamTicketService;
    }

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes
    ) {
        String ticket = UriComponentsBuilder.fromUri(request.getURI())
                .build()
                .getQueryParams()
                .getFirst("ticket");

        AiStreamTicket streamTicket = aiStreamTicketService.consumeTicket(ticket);

        if (streamTicket == null) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        attributes.put(AiWebSocketAttributes.USER_ID, streamTicket.userId());
        attributes.put(
                AiWebSocketAttributes.CONVERSATION_ID,
                streamTicket.conversationId()
        );

        return true;
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception
    ) {
        // ticket 已在 beforeHandshake 中消费，不需要额外清理。
    }
}
