package com.yyh.blogserver.Model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 映射 PostgreSQL {@code ai_conversations} 表的 MyBatis-Plus 实体。
 *
 * <p>一条记录代表当前用户创建的一次 AI 对话。会话标题不要求唯一，
 * 同一用户可以拥有多个同名会话。</p>
 */
@Data
@TableName("ai_conversations")
public class AiConversation {

    /** PostgreSQL {@code BIGSERIAL} 自动生成的会话主键。 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 会话所属用户 ID，对应 {@code users.id}。 */
    @TableField("user_id")
    private Long userId;

    /** 会话列表中展示的标题。 */
    private String title;

    /** 会话创建时间。 */
    @TableField("created_at")
    private OffsetDateTime createdAt;

    /** 最近一次发送用户消息或收到 AI 回复的时间。 */
    @TableField("updated_at")
    private OffsetDateTime updatedAt;
}
