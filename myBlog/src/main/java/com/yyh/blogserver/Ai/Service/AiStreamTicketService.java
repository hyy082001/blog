package com.yyh.blogserver.Ai.Service;

import com.yyh.blogserver.Ai.DTO.AiStreamTicket;
import com.yyh.blogserver.Ai.DTO.AiStreamTicketResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.List;

/**
 * 管理 AI 流式对话 WebSocket 的一次性连接凭证。
 *
 * <p>ticket 在 Redis 8 中保留 60 秒。连接建立时采用 Lua 原子执行 GET 和 DEL，
 * 所以即使多个请求并发到达，一个 ticket 也只能被消费一次。</p>
 */
@Service
public class AiStreamTicketService {

    private static final String TICKET_KEY_PREFIX = "blog:ai:stream-ticket:";

    private static final Duration TICKET_TTL = Duration.ofSeconds(60);

    private static final DefaultRedisScript<String> CONSUME_TICKET_SCRIPT =
            new DefaultRedisScript<>(
                    """
                    local value = redis.call('GET', KEYS[1])
                    if value then
                        redis.call('DEL', KEYS[1])
                    end
                    return value
                    """,
                    String.class
            );

    private final StringRedisTemplate redisTemplate;
    private final AiConversationService aiConversationService;
    private final SecureRandom secureRandom = new SecureRandom();

    public AiStreamTicketService(
            StringRedisTemplate redisTemplate,
            AiConversationService aiConversationService
    ) {
        this.redisTemplate = redisTemplate;
        this.aiConversationService = aiConversationService;
    }

    /** 为当前用户自己的会话生成短时、一次性的连接凭证。 */
    public AiStreamTicketResponse issueTicket(
            Long userId,
            Long conversationId
    ) {
        aiConversationService.findAndVerifyOwner(userId, conversationId);

        String ticket = createTicket();
        String ticketValue = userId + ":" + conversationId;

        redisTemplate.opsForValue().set(
                buildRedisKey(ticket),
                ticketValue,
                TICKET_TTL
        );

        return new AiStreamTicketResponse(ticket, TICKET_TTL.toSeconds());
    }

    /**
     * 读取并立即删除 ticket。
     *
     * @return ticket 有效时返回其中的用户和会话信息，否则返回 null
     */
    public AiStreamTicket consumeTicket(String ticket) {
        if (ticket == null || ticket.isBlank()) {
            return null;
        }

        String ticketValue = redisTemplate.execute(
                CONSUME_TICKET_SCRIPT,
                List.of(buildRedisKey(ticket))
        );

        if (ticketValue == null || ticketValue.isBlank()) {
            return null;
        }

        try {
            String[] values = ticketValue.split(":", 2);

            if (values.length != 2) {
                return null;
            }

            return new AiStreamTicket(
                    Long.parseLong(values[0]),
                    Long.parseLong(values[1])
            );
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String createTicket() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);
    }

    private String buildRedisKey(String ticket) {
        return TICKET_KEY_PREFIX + ticket;
    }
}
