package com.yyh.blogserver.Model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * 映射 PostgreSQL {@code ai_conversations} 表的 MyBatis-Plus 实体。
 *
 * <p>一条记录代表当前用户创建的一次 AI 对话。会话标题不要求唯一，
 * 同一用户可以拥有多个同名会话。</p>
 */
@Data
@NoArgsConstructor
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

    /**
     * 创建一条尚未保存到数据库的会话记录。
     *
     * <p>{@code id} 由 PostgreSQL 自增生成，因此不通过构造方法传入。
     * 从数据库查询记录时，MyBatis-Plus 使用方法参数为空的无参构造方法。</p>
     */
    public AiConversation(
            Long userId,
            String title,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
        this.userId = userId;
        this.title = title;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
