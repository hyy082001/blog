package com.yyh.blogserver.Ai.WebSocket;

import com.yyh.blogserver.Ai.DTO.AiStreamEvent;
import com.yyh.blogserver.Ai.DTO.AiStreamRequest;
import com.yyh.blogserver.Ai.Service.AiConversationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

/** AI 流式对话的 WebSocket 入口。 */
@Component
public class AiWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(
            AiWebSocketHandler.class
    );

    private final ObjectMapper objectMapper;
    private final AiConversationService aiConversationService;
    private final Executor aiStreamTaskExecutor;

    /** 保存包装后的 session，避免异步模型回调并发写同一个连接。 */
    private final Map<String, WebSocketSession> safeSessions =
            new ConcurrentHashMap<>();

    public AiWebSocketHandler(
            ObjectMapper objectMapper,
            AiConversationService aiConversationService,
            @Qualifier("aiStreamTaskExecutor") Executor aiStreamTaskExecutor
    ) {
        this.objectMapper = objectMapper;
        this.aiConversationService = aiConversationService;
        this.aiStreamTaskExecutor = aiStreamTaskExecutor;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        WebSocketSession safeSession = new ConcurrentWebSocketSessionDecorator(
                session,
                10_000,
                512 * 1024
        );

        safeSessions.put(session.getId(), safeSession);
        session.getAttributes().put(
                AiWebSocketAttributes.IS_STREAMING,
                new AtomicBoolean(false)
        );

        sendEvent(safeSession, AiStreamEvent.connected(getConversationId(session)));
    }

    @Override
    protected void handleTextMessage(
            WebSocketSession session,
            TextMessage message
    ) {
        WebSocketSession safeSession = safeSessions.get(session.getId());

        if (safeSession == null) {
            return;
        }

        try {
            AiStreamRequest request = objectMapper.readValue(
                    message.getPayload(),
                    AiStreamRequest.class
            );

            if (!"chat".equals(request.type())) {
                sendEvent(safeSession, AiStreamEvent.error("不支持的消息类型"));
                return;
            }

            if (request.content() == null || request.content().isBlank()) {
                sendEvent(safeSession, AiStreamEvent.error("消息内容不能为空"));
                return;
            }

            AtomicBoolean streaming = getStreamingFlag(session);

            if (!streaming.compareAndSet(false, true)) {
                sendEvent(safeSession, AiStreamEvent.error("上一条消息仍在生成中"));
                return;
            }

            Long userId = getUserId(session);
            Long conversationId = getConversationId(session);
            String content = request.content().trim();

            aiStreamTaskExecutor.execute(() ->
                    aiConversationService.streamMessage(
                            userId,
                            conversationId,
                            content,
                            delta -> sendEvent(safeSession, AiStreamEvent.delta(delta)),
                            messageId -> {
                                sendEvent(safeSession, AiStreamEvent.done(messageId));
                                streaming.set(false);
                            },
                            error -> {
                                sendEvent(safeSession, AiStreamEvent.error(error));
                                streaming.set(false);
                            }
                    )
            );
        } catch (Exception exception) {
            log.error("处理 AI WebSocket 消息失败", exception);
            getStreamingFlag(session).set(false);
            sendEvent(safeSession, AiStreamEvent.error("消息格式错误"));
        }
    }

    @Override
    public void handleTransportError(
            WebSocketSession session,
            Throwable exception
    ) {
        log.warn("AI WebSocket 连接异常，sessionId={}", session.getId(), exception);
        safeSessions.remove(session.getId());
    }

    @Override
    public void afterConnectionClosed(
            WebSocketSession session,
            CloseStatus status
    ) {
        safeSessions.remove(session.getId());
    }

    private void sendEvent(WebSocketSession session, AiStreamEvent event) {
        if (session == null || !session.isOpen()) {
            return;
        }

        try {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(event)));
        } catch (IOException exception) {
            log.warn("发送 AI WebSocket 消息失败，sessionId={}", session.getId(), exception);
        }
    }

    private Long getUserId(WebSocketSession session) {
        return (Long) session.getAttributes().get(AiWebSocketAttributes.USER_ID);
    }

    private Long getConversationId(WebSocketSession session) {
        return (Long) session.getAttributes().get(
                AiWebSocketAttributes.CONVERSATION_ID
        );
    }

    private AtomicBoolean getStreamingFlag(WebSocketSession session) {
        return (AtomicBoolean) session.getAttributes().get(
                AiWebSocketAttributes.IS_STREAMING
        );
    }
}
