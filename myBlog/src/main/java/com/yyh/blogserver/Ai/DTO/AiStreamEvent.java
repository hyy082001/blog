package com.yyh.blogserver.Ai.DTO;

/**
 * 后端通过 AI WebSocket 推送给前端的事件。
 */
public record AiStreamEvent(
        String type,
        String content,
        Long messageId,
        String error,
        Long conversationId
) {

    public static AiStreamEvent connected(Long conversationId) {
        return new AiStreamEvent(
                "connected", null, null, null, conversationId
        );
    }

    public static AiStreamEvent delta(String content) {
        return new AiStreamEvent(
                "delta", content, null, null, null
        );
    }

    public static AiStreamEvent done(Long messageId) {
        return new AiStreamEvent(
                "done", null, messageId, null, null
        );
    }

    public static AiStreamEvent error(String error) {
        return new AiStreamEvent(
                "error", null, null, error, null
        );
    }
}
