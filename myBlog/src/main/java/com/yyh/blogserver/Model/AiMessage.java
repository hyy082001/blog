package com.yyh.blogserver.Model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 映射 PostgreSQL {@code ai_messages} 表的 MyBatis-Plus 实体。
 *
 * <p>一条记录代表会话中的一条用户消息或 AI 回复。</p>
 */
@Data
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
}
