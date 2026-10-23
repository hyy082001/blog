package com.yyh.blogserver.Ai.Service;

import com.yyh.blogserver.Ai.DTO.AiStreamTicket;
import com.yyh.blogserver.Ai.DTO.AiStreamTicketResponse;

/**
 * AI 流式对话 WebSocket 一次性连接凭证的业务接口。
 *
 * <p>ticket 在 Redis 中保留 60 秒，并且只能被消费一次。实现类为
 * {@link com.yyh.blogserver.Ai.Service.impl.AiStreamTicketServiceImpl}。</p>
 */
public interface AiStreamTicketService {

    /** 为当前用户自己的会话生成短时、一次性的连接凭证。 */
    AiStreamTicketResponse issueTicket(Long userId, Long conversationId);

    /**
     * 读取并立即删除 ticket。
     *
     * @return ticket 有效时返回其中的用户和会话信息，否则返回 {@code null}
     */
    AiStreamTicket consumeTicket(String ticket);
}
