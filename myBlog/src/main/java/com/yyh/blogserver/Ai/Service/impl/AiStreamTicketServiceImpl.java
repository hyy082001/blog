package com.yyh.blogserver.Ai.Service.impl;

import com.yyh.blogserver.Ai.DTO.AiStreamTicket;
import com.yyh.blogserver.Ai.DTO.AiStreamTicketResponse;
import com.yyh.blogserver.Ai.Service.AiConversationService;
import com.yyh.blogserver.Ai.Service.AiStreamTicketService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.List;

/**
 * {@link AiStreamTicketService} 的默认实现。
 *
 * <p>连接建立时采用 Lua 原子执行 GET 和 DEL，所以即使多个请求并发到达，
 * 一个 ticket 也只能被消费一次。</p>
 */
@Service
public class AiStreamTicketServiceImpl implements AiStreamTicketService {

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

    public AiStreamTicketServiceImpl(
            StringRedisTemplate redisTemplate,
            AiConversationService aiConversationService
    ) {
        this.redisTemplate = redisTemplate;
        this.aiConversationService = aiConversationService;
    }

    @Override
    public AiStreamTicketResponse issueTicket(
            Long userId,
            Long conversationId
    ) {
        aiConversationService.verifyOwner(userId, conversationId);

        String ticket = createTicket();
        String ticketValue = userId + ":" + conversationId;

        redisTemplate.opsForValue().set(
                buildRedisKey(ticket),
                ticketValue,
                TICKET_TTL
        );

        return new AiStreamTicketResponse(ticket, TICKET_TTL.toSeconds());
    }

    @Override
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
