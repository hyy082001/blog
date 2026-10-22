package com.yyh.blogserver.Ai.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 用户向 AI 会话发送消息时提交的数据。 */
public record SendMessageRequest(

        /** 第一版限制单条消息最多 4000 个字符。 */
        @NotBlank(message = "消息内容不能为空")
        @Size(max = 4000, message = "消息内容不能超过 4000 个字符")
        String content
) {
}
