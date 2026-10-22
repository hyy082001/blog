package com.yyh.blogserver.Ai.DTO;

/**
 * Redis 中一次性 WebSocket ticket 对应的身份信息。
 */
public record AiStreamTicket(
        Long userId,
        Long conversationId
) {
}
