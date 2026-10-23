package com.yyh.blogserver.Ai.Service;

import com.yyh.blogserver.Ai.DTO.AiMessageResponse;
import com.yyh.blogserver.Ai.DTO.ConversationResponse;
import com.yyh.blogserver.Ai.DTO.CreateConversationRequest;
import com.yyh.blogserver.Ai.DTO.SendMessageRequest;
import com.yyh.blogserver.Ai.DTO.SendMessageResponse;

import java.util.List;
import java.util.function.Consumer;

/**
 * AI 会话业务接口。
 *
 * <p>负责管理会话、读取历史消息、调用大模型并持久化消息。
 * Controller 和 WebSocket 处理器只依赖本接口，具体实现由
 * {@link com.yyh.blogserver.Ai.Service.impl.AiConversationServiceImpl} 提供。</p>
 */
public interface AiConversationService {

    /** 为当前用户创建一个新的 AI 会话。 */
    ConversationResponse createConversation(
            Long currentUserId,
            CreateConversationRequest request
    );

    /** 查询当前用户自己的会话，最近活动的会话排在最前面。 */
    List<ConversationResponse> myConversations(Long currentUserId);

    /** 查询当前用户指定会话中的全部历史消息。 */
    List<AiMessageResponse> conversationMessages(
            Long currentUserId,
            Long conversationId
    );

    /**
     * 保存用户问题、携带最近历史调用模型，再保存 AI 回复。
     *
     * @param currentUserId 当前登录用户 ID
     * @param conversationId 会话 ID
     * @param request 用户发送的问题
     * @return 本次用户消息和 AI 回复
     */
    SendMessageResponse sendMessage(
            Long currentUserId,
            Long conversationId,
            SendMessageRequest request
    );

    /**
     * 保存用户消息后，以流式方式调用模型。
     *
     * <p>模型每产生一个文本片段就调用 {@code onDelta}；完整回复保存到数据库后调用
     * {@code onComplete}；发生错误时调用 {@code onError}。</p>
     *
     * @param currentUserId 当前登录用户 ID
     * @param conversationId 会话 ID
     * @param content 用户发送的问题
     * @param onDelta 文本片段回调
     * @param onComplete 回复保存完成回调，参数为 AI 消息 ID
     * @param onError 错误提示回调
     */
    void streamMessage(
            Long currentUserId,
            Long conversationId,
            String content,
            Consumer<String> onDelta,
            Consumer<Long> onComplete,
            Consumer<String> onError
    );

    /** 删除当前用户自己的会话；数据库会级联删除其中的消息。 */
    void deleteConversation(Long currentUserId, Long conversationId);

    /**
     * 校验会话存在且属于当前用户，否则抛出 404 或 403。
     *
     * @param currentUserId 当前登录用户 ID
     * @param conversationId 会话 ID
     */
    void verifyOwner(Long currentUserId, Long conversationId);
}
