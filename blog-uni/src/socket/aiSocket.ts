import { WS_BASE_URL } from '../config/api'
import type { AiStreamEvent } from '../types/ai'

interface AiSocketHandlers {
  onEvent: (event: AiStreamEvent) => void
  onDisconnected: (message: string) => void
}

export interface AiSocketConnection {
  sendChat(content: string): void
  close(): void
}

/**
 * 建立 AI 流式对话连接。
 *
 * URL 中只携带后端刚签发的短时 ticket，不携带长期 JWT。
 */
export function connectAiSocket(
  ticket: string,
  handlers: AiSocketHandlers,
): Promise<AiSocketConnection> {
  return new Promise((resolve, reject) => {
    let opened = false
    let closedByClient = false

    const socketTask = uni.connectSocket({
      url: `${WS_BASE_URL}/ws/ai?ticket=${encodeURIComponent(ticket)}`,
      // 传入回调可让 uni-app 返回 SocketTask，而不是 Promise。
      success() {},
    })

    socketTask.onOpen(() => {
      opened = true

      resolve({
        sendChat(content: string) {
          socketTask.send({
            data: JSON.stringify({ type: 'chat', content }),
            fail(error) {
              handlers.onDisconnected(error.errMsg || '消息发送失败')
            },
          })
        },
        close() {
          closedByClient = true
          socketTask.close({})
        },
      })
    })

    socketTask.onMessage(message => {
      try {
        handlers.onEvent(JSON.parse(message.data as string) as AiStreamEvent)
      } catch {
        handlers.onDisconnected('AI 服务返回了无法识别的数据')
      }
    })

    socketTask.onError(error => {
      const message = error.errMsg || 'AI 流式连接失败'

      if (!opened) {
        reject(new Error(message))
        return
      }

      handlers.onDisconnected(message)
    })

    socketTask.onClose(() => {
      if (!closedByClient) {
        handlers.onDisconnected('AI 流式连接已断开')
      }
    })
  })
}
