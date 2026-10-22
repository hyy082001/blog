package com.yyh.blogserver.Ai.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yyh.blogserver.Ai.DTO.AiMessageResponse;
import com.yyh.blogserver.Ai.DTO.ConversationResponse;
import com.yyh.blogserver.Ai.DTO.CreateConversationRequest;
import com.yyh.blogserver.Ai.DTO.SendMessageRequest;
import com.yyh.blogserver.Ai.DTO.SendMessageResponse;
import com.yyh.blogserver.Mapper.AiConversationMapper;
import com.yyh.blogserver.Mapper.AiMessageMapper;
import com.yyh.blogserver.Model.AiConversation;
import com.yyh.blogserver.Model.AiMessage;
import com.yyh.blogserver.Model.AiMessageRole;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * AI 会话业务层。
 *
 * <p>负责管理会话、读取数据库中的历史消息、调用 LangChain4j，
 * 并把用户消息和 AI 回复持久化到 PostgreSQL。</p>
 */
@Service
public class AiConversationService {

    private static final Logger log = LoggerFactory.getLogger(
            AiConversationService.class
    );

    private static final String DEFAULT_CONVERSATION_TITLE = "新对话";

    /** 每次请求最多向模型携带最近 12 条历史消息。 */
    private static final int MAX_HISTORY_MESSAGES = 12;

    /**
     * 后端固定的系统提示词，不保存到聊天记录，也不允许由前端修改。
     */
    private static final String SYSTEM_PROMPT = """
            你是 MY BLOG 的 AI 技术写作助手。

            请遵守以下规则：
            1. 默认使用中文回答。
            2. 回答应准确、清晰、有条理。
            3. 遇到不确定的信息时，要明确说明不确定，不得编造事实。
            4. 不得要求或输出密码、JWT、API Key 等敏感信息。
            5. 如果用户询问编程问题，应优先给出容易理解的解释和必要示例。
            6. 当前阶段只提供建议，不得声称已经替用户修改了代码或文章。
            """;

    private final AiConversationMapper aiConversationMapper;
    private final AiMessageMapper aiMessageMapper;
    private final ChatModel chatModel;
    private final StreamingChatModel streamingChatModel;

    /**
     * LangChain4j Starter 会根据 application.yml 自动创建 ChatModel Bean。
     */
    public AiConversationService(
            AiConversationMapper aiConversationMapper,
            AiMessageMapper aiMessageMapper,
            ChatModel chatModel,
            StreamingChatModel streamingChatModel
    ) {
        this.aiConversationMapper = aiConversationMapper;
        this.aiMessageMapper = aiMessageMapper;
        this.chatModel = chatModel;
        this.streamingChatModel = streamingChatModel;
    }

    /** 为当前用户创建一个新的 AI 会话。 */
    public ConversationResponse createConversation(
            Long currentUserId,
            CreateConversationRequest request
    ) {
        OffsetDateTime now = nowUtc();

        AiConversation conversation = new AiConversation();
        conversation.setUserId(currentUserId);
        conversation.setTitle(resolveTitle(request));
        conversation.setCreatedAt(now);
        conversation.setUpdatedAt(now);

        aiConversationMapper.insert(conversation);

        return ConversationResponse.from(conversation);
    }

    /** 查询当前用户自己的会话，最近活动的会话排在最前面。 */
    public List<ConversationResponse> myConversations(Long currentUserId) {
        return aiConversationMapper.selectList(
                        new LambdaQueryWrapper<AiConversation>()
                                .eq(AiConversation::getUserId, currentUserId)
                                .orderByDesc(AiConversation::getUpdatedAt)
                )
                .stream()
                .map(ConversationResponse::from)
                .toList();
    }

    /** 查询当前用户指定会话中的全部历史消息。 */
    public List<AiMessageResponse> conversationMessages(
            Long currentUserId,
            Long conversationId
    ) {
        findAndVerifyOwner(currentUserId, conversationId);

        return findStoredMessages(conversationId)
                .stream()
                .map(AiMessageResponse::from)
                .toList();
    }

    /**
     * 保存用户问题、携带最近历史调用模型，再保存 AI 回复。
     *
     * <p>该方法没有使用长事务，因为外部模型调用可能等待几十秒。
     * 若模型暂时不可用，用户刚发送的问题仍会保留在数据库中。</p>
     */
    public SendMessageResponse sendMessage(
            Long currentUserId,
            Long conversationId,
            SendMessageRequest request
    ) {
        AiConversation conversation = findAndVerifyOwner(
                currentUserId,
                conversationId
        );

        String userContent = request.content().trim();
        OffsetDateTime userMessageTime = nowUtc();

        AiMessage userMessage = new AiMessage();
        userMessage.setConversationId(conversationId);
        userMessage.setRole(AiMessageRole.USER);
        userMessage.setContent(userContent);
        userMessage.setCreatedAt(userMessageTime);
        aiMessageMapper.insert(userMessage);

        // 第一条问题会成为会话标题，之后不再自动覆盖用户看到的标题。
        if (DEFAULT_CONVERSATION_TITLE.equals(conversation.getTitle())) {
            conversation.setTitle(createTitleFromMessage(userContent));
        }
        conversation.setUpdatedAt(userMessageTime);
        aiConversationMapper.updateById(conversation);

        // 此时数据库历史已经包含刚保存的本次用户问题。
        List<AiMessage> recentMessages = takeRecentMessages(
                findStoredMessages(conversationId)
        );

        List<ChatMessage> langChainMessages = new ArrayList<>();
        langChainMessages.add(SystemMessage.from(SYSTEM_PROMPT));
        recentMessages.stream()
                .map(this::toLangChainMessage)
                .forEach(langChainMessages::add);

        String assistantContent;

        try {
            ChatResponse response = chatModel.chat(langChainMessages);
            assistantContent = response.aiMessage().text();

            if (assistantContent == null || assistantContent.isBlank()) {
                throw new IllegalStateException("模型返回了空回复");
            }
        } catch (Exception exception) {
            /*
             * 前端只接收通用提示，避免暴露第三方接口和内部实现细节；
             * 完整异常记录在后端日志中，用于区分密钥、余额、模型名、限流和网络问题。
             */
            log.error(
                    "调用 AI 服务失败，userId={}，conversationId={}，异常类型={}，原因={}",
                    currentUserId,
                    conversationId,
                    exception.getClass().getName(),
                    exception.getMessage(),
                    exception
            );

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "AI 服务暂时不可用，请稍后重试",
                    exception
            );
        }

        OffsetDateTime assistantMessageTime = nowUtc();

        AiMessage assistantMessage = new AiMessage();
        assistantMessage.setConversationId(conversationId);
        assistantMessage.setRole(AiMessageRole.ASSISTANT);
        assistantMessage.setContent(assistantContent);
        assistantMessage.setCreatedAt(assistantMessageTime);
        aiMessageMapper.insert(assistantMessage);

        conversation.setUpdatedAt(assistantMessageTime);
        aiConversationMapper.updateById(conversation);

        return new SendMessageResponse(
                AiMessageResponse.from(userMessage),
                AiMessageResponse.from(assistantMessage)
        );
    }

    /**
     * 保存用户消息后，以流式方式调用模型。
     *
     * <p>模型每产生一个文本片段就调用 {@code onDelta}；完整回复保存到数据库后调用
     * {@code onComplete}。该方法不使用事务，避免外部模型请求长时间占用数据库事务。</p>
     */
    public void streamMessage(
            Long currentUserId,
            Long conversationId,
            String content,
            Consumer<String> onDelta,
            Consumer<Long> onComplete,
            Consumer<String> onError
    ) {
        try {
            AiConversation conversation = findAndVerifyOwner(
                    currentUserId,
                    conversationId
            );

            String userContent = content.trim();
            OffsetDateTime userMessageTime = nowUtc();

            AiMessage userMessage = new AiMessage();
            userMessage.setConversationId(conversationId);
            userMessage.setRole(AiMessageRole.USER);
            userMessage.setContent(userContent);
            userMessage.setCreatedAt(userMessageTime);
            aiMessageMapper.insert(userMessage);

            if (DEFAULT_CONVERSATION_TITLE.equals(conversation.getTitle())) {
                conversation.setTitle(createTitleFromMessage(userContent));
            }
            conversation.setUpdatedAt(userMessageTime);
            aiConversationMapper.updateById(conversation);

            List<AiMessage> recentMessages = takeRecentMessages(
                    findStoredMessages(conversationId)
            );

            List<ChatMessage> langChainMessages = new ArrayList<>();
            langChainMessages.add(SystemMessage.from(SYSTEM_PROMPT));
            recentMessages.stream()
                    .map(this::toLangChainMessage)
                    .forEach(langChainMessages::add);

            StringBuffer assistantContent = new StringBuffer();

            streamingChatModel.chat(
                    langChainMessages,
                    new StreamingChatResponseHandler() {
                        @Override
                        public void onPartialResponse(String partialResponse) {
                            if (partialResponse == null || partialResponse.isEmpty()) {
                                return;
                            }

                            assistantContent.append(partialResponse);
                            onDelta.accept(partialResponse);
                        }

                        @Override
                        public void onCompleteResponse(ChatResponse response) {
                            try {
                                String finalContent = assistantContent.toString().trim();

                                if (finalContent.isBlank()) {
                                    onError.accept("AI 没有返回有效内容，请重试");
                                    return;
                                }

                                OffsetDateTime assistantMessageTime = nowUtc();

                                AiMessage assistantMessage = new AiMessage();
                                assistantMessage.setConversationId(conversationId);
                                assistantMessage.setRole(AiMessageRole.ASSISTANT);
                                assistantMessage.setContent(finalContent);
                                assistantMessage.setCreatedAt(assistantMessageTime);
                                aiMessageMapper.insert(assistantMessage);

                                conversation.setUpdatedAt(assistantMessageTime);
                                aiConversationMapper.updateById(conversation);

                                onComplete.accept(assistantMessage.getId());
                            } catch (Exception exception) {
                                log.error(
                                        "保存 AI 流式回复失败，userId={}，conversationId={}",
                                        currentUserId,
                                        conversationId,
                                        exception
                                );
                                onError.accept("AI 回复保存失败，请稍后重试");
                            }
                        }

                        @Override
                        public void onError(Throwable error) {
                            log.error(
                                    "调用 AI 流式服务失败，userId={}，conversationId={}",
                                    currentUserId,
                                    conversationId,
                                    error
                            );
                            onError.accept("AI 服务暂时不可用，请稍后重试");
                        }
                    }
            );
        } catch (Exception exception) {
            log.error(
                    "准备 AI 流式请求失败，userId={}，conversationId={}",
                    currentUserId,
                    conversationId,
                    exception
            );
            onError.accept("AI 请求创建失败，请稍后重试");
        }
    }

    /** 删除当前用户自己的会话；数据库会级联删除其中的消息。 */
    @Transactional
    public void deleteConversation(
            Long currentUserId,
            Long conversationId
    ) {
        AiConversation conversation = findAndVerifyOwner(
                currentUserId,
                conversationId
        );
        aiConversationMapper.deleteById(conversation.getId());
    }

    /** 查询会话并确保当前用户只能操作自己的记录。 */
    public AiConversation findAndVerifyOwner(
            Long currentUserId,
            Long conversationId
    ) {
        AiConversation conversation = aiConversationMapper.selectById(
                conversationId
        );

        if (conversation == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "AI 会话不存在"
            );
        }

        if (!conversation.getUserId().equals(currentUserId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "无权操作其他用户的 AI 会话"
            );
        }

        return conversation;
    }

    /** 按创建时间和主键正序读取会话消息。 */
    private List<AiMessage> findStoredMessages(Long conversationId) {
        return aiMessageMapper.selectList(
                new LambdaQueryWrapper<AiMessage>()
                        .eq(AiMessage::getConversationId, conversationId)
                        .orderByAsc(AiMessage::getCreatedAt)
                        .orderByAsc(AiMessage::getId)
        );
    }

    /** 只截取最近的消息交给模型，完整记录仍保留在数据库中。 */
    private List<AiMessage> takeRecentMessages(List<AiMessage> messages) {
        int startIndex = Math.max(0, messages.size() - MAX_HISTORY_MESSAGES);
        return messages.subList(startIndex, messages.size());
    }

    /** 将数据库消息转换为 LangChain4j 可以识别的消息类型。 */
    private ChatMessage toLangChainMessage(AiMessage message) {
        if (message.getRole() == AiMessageRole.USER) {
            return UserMessage.from(message.getContent());
        }

        if (message.getRole() == AiMessageRole.ASSISTANT) {
            return dev.langchain4j.data.message.AiMessage.from(
                    message.getContent()
            );
        }

        throw new IllegalStateException(
                "不支持的 AI 消息角色：" + message.getRole()
        );
    }

    /** 使用用户第一条消息的前 30 个字符作为会话标题。 */
    private String createTitleFromMessage(String message) {
        if (message.length() <= 30) {
            return message;
        }
        return message.substring(0, 30) + "…";
    }

    /** 创建会话时没有提供标题，则使用默认标题。 */
    private String resolveTitle(CreateConversationRequest request) {
        if (request == null
                || request.title() == null
                || request.title().isBlank()) {
            return DEFAULT_CONVERSATION_TITLE;
        }
        return request.title().trim();
    }

    /** 统一使用 UTC 保存带时区时间。 */
    private OffsetDateTime nowUtc() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }
}
