package com.yyh.blogserver.Ai.DTO;

/**
 * 用户发送消息后的完整响应。
 *
 * <p>同时返回用户消息和 AI 消息，让前端获得两条记录的数据库 ID。</p>
 */
public record SendMessageResponse(
        AiMessageResponse userMessage,
        AiMessageResponse assistantMessage
) {
}
