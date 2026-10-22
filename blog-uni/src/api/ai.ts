import { request } from './client'
import type {
  AiConversation,
  AiMessage,
  AiStreamTicketResponse,
  SendAiMessageResponse,
} from '../types/ai'

/** 创建 AI 会话。标题为空时，后端会使用“新对话”。 */
export function createAiConversation(title?: string) {
  return request<AiConversation>('/api/ai/conversations', {
    method: 'POST',
    data: title ? { title } : {},
  })
}

/** 查询当前登录用户自己的 AI 会话。 */
export function getAiConversations() {
  return request<AiConversation[]>('/api/ai/conversations')
}

/** 查询指定会话的全部历史消息。 */
export function getAiMessages(conversationId: number) {
  return request<AiMessage[]>(
    `/api/ai/conversations/${conversationId}/messages`,
  )
}

/** 向指定会话发送问题，并等待 AI 返回答案。 */
export function sendAiMessage(conversationId: number, content: string) {
  return request<SendAiMessageResponse>(
    `/api/ai/conversations/${conversationId}/messages`,
    {
      method: 'POST',
      data: { content },
    },
  )
}

/** 申请 AI WebSocket 的一次性 ticket。 */
export function createAiStreamTicket(conversationId: number) {
  return request<AiStreamTicketResponse>(
    `/api/ai/conversations/${conversationId}/stream-ticket`,
    { method: 'POST' },
  )
}

/** 删除当前用户自己的一个 AI 会话。 */
export function deleteAiConversation(conversationId: number) {
  return request<void>(`/api/ai/conversations/${conversationId}`, {
    method: 'DELETE',
  })
}
