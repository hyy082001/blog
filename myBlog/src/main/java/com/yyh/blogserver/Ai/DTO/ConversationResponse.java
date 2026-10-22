package com.yyh.blogserver.Ai.DTO;

import com.yyh.blogserver.Model.AiConversation;

import java.time.OffsetDateTime;

/** 返回给前端的 AI 会话资料。 */
public record ConversationResponse(
        Long id,
        String title,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    /** 将数据库实体转换为接口响应对象。 */
    public static ConversationResponse from(AiConversation conversation) {
        return new ConversationResponse(
                conversation.getId(),
                conversation.getTitle(),
                conversation.getCreatedAt(),
                conversation.getUpdatedAt()
        );
    }
}
