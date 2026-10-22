package com.yyh.blogserver.Ai.WebSocket;

/** WebSocket 握手后存放在 session attributes 中的数据键。 */
public final class AiWebSocketAttributes {

    public static final String USER_ID = "ai.user-id";
    public static final String CONVERSATION_ID = "ai.conversation-id";
    public static final String IS_STREAMING = "ai.is-streaming";

    private AiWebSocketAttributes() {
    }
}
