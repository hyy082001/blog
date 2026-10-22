package com.yyh.blogserver.Ai.controller;

import com.yyh.blogserver.Ai.DTO.AiMessageResponse;
import com.yyh.blogserver.Ai.DTO.AiStreamTicketResponse;
import com.yyh.blogserver.Ai.DTO.ConversationResponse;
import com.yyh.blogserver.Ai.DTO.CreateConversationRequest;
import com.yyh.blogserver.Ai.DTO.SendMessageRequest;
import com.yyh.blogserver.Ai.DTO.SendMessageResponse;
import com.yyh.blogserver.Ai.Service.AiConversationService;
import com.yyh.blogserver.Ai.Service.AiStreamTicketService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * AI 会话接口。
 *
 * <p>所有接口默认经过 Spring Security 和 JWT 认证。前端不传 userId，
 * 当前用户 ID 始终从 {@link Authentication} 中获取。</p>
 */
@RestController
@RequestMapping("/api/ai/conversations")
public class AiConversationController {

    private final AiConversationService aiConversationService;
    private final AiStreamTicketService aiStreamTicketService;

    public AiConversationController(
            AiConversationService aiConversationService,
            AiStreamTicketService aiStreamTicketService
    ) {
        this.aiConversationService = aiConversationService;
        this.aiStreamTicketService = aiStreamTicketService;
    }

    /** 创建一个新的 AI 会话；请求体可以省略。 */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ConversationResponse createConversation(
            Authentication authentication,
            @Valid @RequestBody(required = false) CreateConversationRequest request
    ) {
        Long currentUserId = Long.parseLong(authentication.getName());
        return aiConversationService.createConversation(currentUserId, request);
    }

    /** 查询当前用户自己的会话列表。 */
    @GetMapping
    public List<ConversationResponse> myConversations(
            Authentication authentication
    ) {
        Long currentUserId = Long.parseLong(authentication.getName());
        return aiConversationService.myConversations(currentUserId);
    }

    /** 查询一个会话中的全部历史消息。 */
    @GetMapping("/{conversationId}/messages")
    public List<AiMessageResponse> conversationMessages(
            Authentication authentication,
            @PathVariable Long conversationId
    ) {
        Long currentUserId = Long.parseLong(authentication.getName());
        return aiConversationService.conversationMessages(
                currentUserId,
                conversationId
        );
    }

    /** 保存用户问题、调用大模型并返回 AI 回复。 */
    @PostMapping("/{conversationId}/messages")
    public SendMessageResponse sendMessage(
            Authentication authentication,
            @PathVariable Long conversationId,
            @Valid @RequestBody SendMessageRequest request
    ) {
        Long currentUserId = Long.parseLong(authentication.getName());
        return aiConversationService.sendMessage(
                currentUserId,
                conversationId,
                request
        );
    }

    /** 申请一个 60 秒有效、仅可使用一次的 AI WebSocket 连接凭证。 */
    @PostMapping("/{conversationId}/stream-ticket")
    public AiStreamTicketResponse createStreamTicket(
            Authentication authentication,
            @PathVariable Long conversationId
    ) {
        Long currentUserId = Long.parseLong(authentication.getName());
        return aiStreamTicketService.issueTicket(currentUserId, conversationId);
    }

    /** 删除当前用户自己的一个会话。 */
    @DeleteMapping("/{conversationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteConversation(
            Authentication authentication,
            @PathVariable Long conversationId
    ) {
        Long currentUserId = Long.parseLong(authentication.getName());
        aiConversationService.deleteConversation(currentUserId, conversationId);
    }
}
