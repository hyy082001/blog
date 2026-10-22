package com.yyh.blogserver.Ai.DTO;

import com.yyh.blogserver.Model.AiMessage;
import com.yyh.blogserver.Model.AiMessageRole;

import java.time.OffsetDateTime;

/** 返回给前端的一条 AI 会话消息。 */
public record AiMessageResponse(
        Long id,
        AiMessageRole role,
        String content,
        OffsetDateTime createdAt
) {

    /** 将数据库消息实体转换为接口响应对象。 */
    public static AiMessageResponse from(AiMessage message) {
        return new AiMessageResponse(
                message.getId(),
                message.getRole(),
                message.getContent(),
                message.getCreatedAt()
        );
    }
}
