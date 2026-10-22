package com.yyh.blogserver.Ai.DTO;

/**
 * 前端在 AI WebSocket 中发送的请求。
 *
 * <p>当前仅支持 type 为 {@code chat}。</p>
 */
public record AiStreamRequest(
        String type,
        String content
) {
}
