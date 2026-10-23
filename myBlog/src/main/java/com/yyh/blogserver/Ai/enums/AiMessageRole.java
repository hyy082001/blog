package com.yyh.blogserver.Ai.enums;

/**
 * AI 会话中的消息发送者类型。
 *
 * <p>枚举名称会直接保存到 {@code ai_messages.role} 字段中，
 * 因此必须与数据库约束中的 {@code USER}、{@code ASSISTANT} 保持一致。</p>
 */
public enum AiMessageRole {

    /** 用户在 AI 页面输入的问题或指令。 */
    USER,

    /** 大模型根据用户问题生成的回复。 */
    ASSISTANT
}
