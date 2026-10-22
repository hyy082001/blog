/** AI 消息角色，与后端 AiMessageRole 枚举保持一致。 */
export type AiMessageRole = 'USER' | 'ASSISTANT'

/** 后端返回的一个 AI 会话。 */
export interface AiConversation {
  id: number
  title: string
  createdAt: string
  updatedAt: string
}

/** AI 会话中的一条消息。 */
export interface AiMessage {
  id: number
  role: AiMessageRole
  content: string
  createdAt: string
}

/** 发送问题成功后，后端同时返回正式的用户消息和 AI 回复。 */
export interface SendAiMessageResponse {
  userMessage: AiMessage
  assistantMessage: AiMessage
}

/** 后端签发的 AI WebSocket 一次性连接凭证。 */
export interface AiStreamTicketResponse {
  ticket: string
  expiresInSeconds: number
}

/** AI WebSocket 推送给前端的流式事件。 */
export interface AiStreamEvent {
  type: 'connected' | 'delta' | 'done' | 'error'
  content?: string | null
  messageId?: number | null
  error?: string | null
  conversationId?: number | null
}
