package com.yyh.blogserver.Model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yyh.blogserver.Ai.enums.AiMessageRole;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * 映射 PostgreSQL {@code ai_messages} 表的 MyBatis-Plus 实体。
 *
 * <p>一条记录代表会话中的一条用户消息或 AI 回复。</p>
 */
@Data
@NoArgsConstructor
@TableName("ai_messages")
public class AiMessage {

    /** PostgreSQL {@code BIGSERIAL} 自动生成的消息主键。 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 消息所属会话 ID，对应 {@code ai_conversations.id}。 */
    @TableField("conversation_id")
    private Long conversationId;

    /** 消息角色：用户 {@code USER} 或 AI {@code ASSISTANT}。 */
    private AiMessageRole role;

    /** 消息正文。数据库使用 TEXT 保存。 */
    private String content;

    /** 消息创建时间。 */
    @TableField("created_at")
    private OffsetDateTime createdAt;

    /**
     * 创建一条尚未保存到数据库的会话消息。
     *
     * <p>{@code id} 由 PostgreSQL 自增生成，因此不通过构造方法传入。
     * 从数据库查询记录时，MyBatis-Plus 使用方法参数为空的无参构造方法。</p>
     */
    public AiMessage(
            Long conversationId,
            AiMessageRole role,
            String content,
            OffsetDateTime createdAt
    ) {
        this.conversationId = conversationId;
        this.role = role;
        this.content = content;
        this.createdAt = createdAt;
    }
}
