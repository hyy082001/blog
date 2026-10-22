package com.yyh.blogserver.Ai.DTO;

/**
 * 前端建立 AI WebSocket 连接前申请到的一次性凭证。
 */
public record AiStreamTicketResponse(
        String ticket,
        long expiresInSeconds
) {
}
