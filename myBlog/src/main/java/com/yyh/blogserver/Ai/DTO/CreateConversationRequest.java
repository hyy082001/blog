package com.yyh.blogserver.Ai.DTO;

import jakarta.validation.constraints.Size;

/** 创建 AI 会话时前端可选提交的数据。 */
public record CreateConversationRequest(

        /** 为空时后端使用“新对话”，最长 100 个字符。 */
        @Size(max = 100, message = "会话标题不能超过 100 个字符")
        String title
) {
}
