<script setup lang="ts">
import { nextTick, ref } from 'vue'
import { onLoad, onUnload } from '@dcloudio/uni-app'
import { createAiStreamTicket, getAiMessages } from '../../../api/ai'
import { ApiError } from '../../../api/client'
import { getToken, removeToken } from '../../../auth/token'
import { connectAiSocket, type AiSocketConnection } from '../../../socket/aiSocket'
import type { AiMessage, AiStreamEvent } from '../../../types/ai'

/** 页面消息可以通过 pending 标记表示它还在等待后端确认。 */
interface UiMessage extends AiMessage {
  pending?: boolean
}

const conversationId = ref<number | null>(null)
const messages = ref<UiMessage[]>([])
const inputText = ref('')
const loading = ref(true)
const sending = ref(false)
const errorMessage = ref('')
const scrollTarget = ref('')
const streamingAssistantId = ref<number | null>(null)

// 临时消息使用负数 ID，避免与数据库生成的正数 ID 冲突。
let temporaryMessageId = -1
let aiSocket: AiSocketConnection | null = null

onLoad(options => {
  const id = Number(options?.id)

  if (!Number.isInteger(id) || id <= 0) {
    uni.showToast({ title: '会话参数错误', icon: 'none' })
    setTimeout(() => uni.navigateBack(), 800)
    return
  }

  conversationId.value = id
  void loadMessages()
})

/** 离开页面时主动关闭连接，避免后台继续保留无用 WebSocket。 */
onUnload(() => {
  aiSocket?.close()
  aiSocket = null
})

function goBack() {
  uni.navigateBack()
}

function goLogin() {
  removeToken()
  uni.reLaunch({ url: '/pages/auth/login/index' })
}

/** 等待 Vue 更新消息节点后，把聊天记录滚动到最底部。 */
async function scrollToBottom() {
  await nextTick()
  scrollTarget.value = ''
  await nextTick()
  scrollTarget.value = 'message-bottom'
}

/** 加载该会话已经保存的全部历史消息。 */
async function loadMessages() {
  if (!getToken()) {
    goLogin()
    return
  }

  if (conversationId.value === null) {
    return
  }

  loading.value = true
  errorMessage.value = ''

  try {
    await synchronizeMessages()
  } catch (error) {
    if (error instanceof ApiError && error.statusCode === 401) {
      goLogin()
      return
    }

    errorMessage.value = error instanceof Error
      ? error.message
      : '聊天记录加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

/** 从数据库重新同步已确认的聊天记录。 */
async function synchronizeMessages() {
  if (conversationId.value === null) {
    return
  }

  messages.value = await getAiMessages(conversationId.value)
  await scrollToBottom()
}

/** 需要时申请 Redis ticket，并建立当前会话的 WebSocket 连接。 */
async function ensureAiSocket(): Promise<AiSocketConnection> {
  if (aiSocket) {
    return aiSocket
  }

  if (conversationId.value === null) {
    throw new Error('会话参数错误')
  }

  const ticket = await createAiStreamTicket(conversationId.value)

  aiSocket = await connectAiSocket(ticket.ticket, {
    onEvent(event) {
      void handleStreamEvent(event)
    },
    onDisconnected(message) {
      void handleSocketDisconnected(message)
    },
  })

  return aiSocket
}

/** 处理服务端推送的增量文本、完成和错误事件。 */
async function handleStreamEvent(event: AiStreamEvent) {
  if (event.type === 'connected') {
    return
  }

  if (event.type === 'delta') {
    let assistantMessage = messages.value.find(
      message => message.id === streamingAssistantId.value,
    )

    // 第一段文本到达时才创建 AI 气泡，之前页面展示“正在思考”动画。
    if (!assistantMessage) {
      const id = temporaryMessageId--
      assistantMessage = {
        id,
        role: 'ASSISTANT',
        content: '',
        createdAt: new Date().toISOString(),
        pending: true,
      }
      streamingAssistantId.value = id
      messages.value.push(assistantMessage)
    }

    assistantMessage.content += event.content ?? ''
    assistantMessage.pending = false
    await scrollToBottom()
    return
  }

  if (event.type === 'done') {
    sending.value = false
    streamingAssistantId.value = null
    await synchronizeMessages()
    return
  }

  if (event.type === 'error') {
    sending.value = false
    streamingAssistantId.value = null
    uni.showToast({
      title: event.error || 'AI 回复失败，请稍后重试',
      icon: 'none',
      duration: 3000,
    })
    await synchronizeMessages()
  }
}

/** 非预期断开时让页面恢复可输入状态，并回读已经入库的用户消息。 */
async function handleSocketDisconnected(message: string) {
  aiSocket = null

  if (!sending.value) {
    return
  }

  sending.value = false
  streamingAssistantId.value = null
  uni.showToast({ title: message, icon: 'none', duration: 2500 })

  try {
    await synchronizeMessages()
  } catch {
    // 断线后如果同步也失败，保留当前页面内容，用户可以手动返回再进入。
  }
}

/** 显示用户消息后通过 WebSocket 接收 AI 的实时片段。 */
async function submitMessage() {
  const content = inputText.value.trim()

  if (!content || sending.value || conversationId.value === null) {
    return
  }

  const currentTemporaryId = temporaryMessageId--
  const temporaryMessage: UiMessage = {
    id: currentTemporaryId,
    role: 'USER',
    content,
    createdAt: new Date().toISOString(),
    pending: true,
  }

  messages.value.push(temporaryMessage)
  inputText.value = ''
  sending.value = true
  await scrollToBottom()

  try {
    const socket = await ensureAiSocket()
    socket.sendChat(content)
  } catch (error) {
    if (error instanceof ApiError && error.statusCode === 401) {
      goLogin()
      return
    }

    const message = error instanceof Error
      ? error.message
      : '消息发送失败，请稍后重试'

    uni.showToast({ title: message, icon: 'none', duration: 3000 })

    sending.value = false
    streamingAssistantId.value = null

    // ticket 申请失败时用户消息尚未保存；模型失败时则可能已保存。
    // 统一回读数据库，确保页面和后端最终状态一致。
    await synchronizeMessages()
  }
}
</script>

<template>
  <view class="chat-page">
    <view class="top-bar">
      <view class="back-button" @tap="goBack">
        <uni-icons type="left" color="#172033" :size="25" />
      </view>

      <view class="assistant-info">
        <text class="assistant-name">技术学习助手</text>
        <view class="online-state">
          <view class="online-dot" />
          <text>AI 在线</text>
        </view>
      </view>

      <view class="top-placeholder" />
    </view>

    <scroll-view
      class="message-scroll"
      scroll-y
      :scroll-into-view="scrollTarget"
      :show-scrollbar="false"
    >
      <view class="message-container">
        <view v-if="loading" class="state-panel">
          <uni-load-more status="loading" />
          <text>正在加载聊天记录</text>
        </view>

        <view v-else-if="errorMessage" class="state-panel">
          <text>{{ errorMessage }}</text>
          <text class="retry" @tap="loadMessages">重新加载</text>
        </view>

        <view v-else-if="messages.length === 0" class="welcome-card">
          <view class="welcome-icon">
            <uni-icons type="chatbubble-filled" color="#2563eb" :size="34" />
          </view>
          <text class="welcome-title">你好，我是 AI 助手</text>
          <text class="welcome-description">
            你可以问我 Java、Spring Boot、Vue、数据库等技术问题，
            也可以让我协助整理文章大纲。
          </text>

          <view class="suggestion-list">
            <view
              class="suggestion"
              @tap="inputText = '帮我梳理一下 Spring Security 的认证流程'"
            >
              Spring Security 的认证流程
            </view>
            <view
              class="suggestion"
              @tap="inputText = '帮我生成一篇技术博客的文章大纲'"
            >
              生成技术博客文章大纲
            </view>
          </view>
        </view>

        <view
          v-for="message in messages"
          :key="message.id"
          :class="[
            'message-row',
            message.role === 'USER' ? 'user-row' : 'assistant-row',
          ]"
        >
          <view v-if="message.role === 'ASSISTANT'" class="message-avatar">
            <text>AI</text>
          </view>

          <view
            :class="[
              'message-bubble',
              message.role === 'USER' ? 'user-bubble' : 'assistant-bubble',
              { pending: message.pending },
            ]"
          >
            <text class="message-content" selectable>{{ message.content }}</text>
          </view>
        </view>

        <view
          v-if="sending && streamingAssistantId === null"
          class="message-row assistant-row"
        >
          <view class="message-avatar"><text>AI</text></view>
          <view class="message-bubble assistant-bubble thinking">
            <view class="thinking-dot" />
            <view class="thinking-dot" />
            <view class="thinking-dot" />
          </view>
        </view>

        <view id="message-bottom" class="message-bottom" />
      </view>
    </scroll-view>

    <view class="input-area">
      <view class="input-box">
        <textarea
          v-model="inputText"
          class="message-input"
          :maxlength="4000"
          :disabled="sending"
          placeholder="输入你的问题..."
          confirm-type="send"
          @confirm="submitMessage"
        />

        <view
          :class="['send-button', { disabled: !inputText.trim() || sending }]"
          @tap="submitMessage"
        >
          <text>{{ sending ? '思考中' : '发送' }}</text>
        </view>
      </view>

      <text class="input-tip">AI 生成的内容可能存在错误，请结合实际情况判断。</text>
    </view>
  </view>
</template>

<style scoped lang="scss">
.chat-page {
  display: flex;
  width: 100%;
  height: 100vh;
  overflow: hidden;
  flex-direction: column;
  background: #f4f7fb;
}

.top-bar {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: space-between;
  padding: calc(20rpx + env(safe-area-inset-top)) 28rpx 20rpx;
  background: #ffffff;
  border-bottom: 2rpx solid #e8edf5;
}

.back-button,
.top-placeholder {
  display: flex;
  width: 80rpx;
  height: 64rpx;
  align-items: center;
}

.assistant-info {
  display: flex;
  align-items: center;
  flex-direction: column;
}

.assistant-name {
  color: #172033;
  font-size: 29rpx;
  font-weight: 800;
}

.online-state {
  display: flex;
  align-items: center;
  gap: 8rpx;
  margin-top: 5rpx;
  color: #64748b;
  font-size: 20rpx;
}

.online-dot {
  width: 12rpx;
  height: 12rpx;
  background: #22c55e;
  border-radius: 50%;
}

.message-scroll {
  height: 0;
  flex: 1;
}

.message-container {
  width: 100%;
  max-width: 920px;
  min-height: 100%;
  box-sizing: border-box;
  padding: 34rpx 28rpx 50rpx;
  margin: 0 auto;
}

.message-row {
  display: flex;
  align-items: flex-start;
  gap: 16rpx;
  margin-bottom: 28rpx;
}

.user-row {
  justify-content: flex-end;
}

.message-avatar {
  display: grid;
  width: 64rpx;
  height: 64rpx;
  flex-shrink: 0;
  place-items: center;
  color: #ffffff;
  font-size: 22rpx;
  font-weight: 800;
  background: #2563eb;
  border-radius: 20rpx;
}

.message-bubble {
  max-width: 76%;
  padding: 22rpx 26rpx;
  font-size: 27rpx;
  line-height: 1.7;
  word-break: break-word;
}

.assistant-bubble {
  color: #24324a;
  background: #ffffff;
  border-radius: 8rpx 26rpx 26rpx;
  box-shadow: 0 6rpx 18rpx rgb(15 23 42 / 5%);
}

.user-bubble {
  color: #ffffff;
  background: #2563eb;
  border-radius: 26rpx 8rpx 26rpx 26rpx;
}

.message-bubble.pending {
  opacity: 0.72;
}

.message-content {
  white-space: pre-wrap;
}

.thinking {
  display: flex;
  align-items: center;
  gap: 9rpx;
  min-width: 80rpx;
}

.thinking-dot {
  width: 12rpx;
  height: 12rpx;
  background: #94a3b8;
  border-radius: 50%;
  animation: thinking 1.2s infinite ease-in-out;
}

.thinking-dot:nth-child(2) {
  animation-delay: 0.16s;
}

.thinking-dot:nth-child(3) {
  animation-delay: 0.32s;
}

@keyframes thinking {
  0%,
  60%,
  100% {
    opacity: 0.35;
    transform: translateY(0);
  }

  30% {
    opacity: 1;
    transform: translateY(-6rpx);
  }
}

.welcome-card,
.state-panel {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 64rpx 38rpx;
  color: #64748b;
  font-size: 26rpx;
  text-align: center;
  background: #ffffff;
  border-radius: 28rpx;
}

.welcome-icon {
  display: grid;
  width: 104rpx;
  height: 104rpx;
  place-items: center;
  background: #eaf2ff;
  border-radius: 50%;
}

.welcome-title {
  margin-top: 26rpx;
  color: #172033;
  font-size: 32rpx;
  font-weight: 800;
}

.welcome-description {
  margin-top: 16rpx;
  color: #64748b;
  font-size: 25rpx;
  line-height: 1.65;
}

.suggestion-list {
  display: flex;
  width: 100%;
  flex-direction: column;
  gap: 16rpx;
  margin-top: 32rpx;
}

.suggestion {
  padding: 22rpx;
  color: #2563eb;
  font-size: 24rpx;
  background: #eff6ff;
  border-radius: 18rpx;
}

.retry {
  margin-top: 22rpx;
  color: #2563eb;
  font-weight: 700;
}

.message-bottom {
  height: 2rpx;
}

.input-area {
  flex-shrink: 0;
  padding: 18rpx 24rpx calc(18rpx + env(safe-area-inset-bottom));
  background: #ffffff;
  border-top: 2rpx solid #e8edf5;
}

.input-box {
  display: flex;
  width: 100%;
  max-width: 920px;
  box-sizing: border-box;
  align-items: flex-end;
  gap: 16rpx;
  padding: 12rpx;
  margin: 0 auto;
  background: #f1f5f9;
  border: 2rpx solid #dbe3ef;
  border-radius: 24rpx;
}

.message-input {
  width: 0;
  height: 72rpx;
  min-height: 72rpx;
  max-height: 180rpx;
  flex: 1;
  box-sizing: border-box;
  padding: 15rpx 12rpx;
  color: #172033;
  font-size: 26rpx;
  line-height: 42rpx;
  background: transparent;
}

.send-button {
  display: flex;
  height: 72rpx;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  padding: 0 28rpx;
  color: #ffffff;
  font-size: 24rpx;
  font-weight: 700;
  background: #2563eb;
  border-radius: 18rpx;
}

.send-button.disabled {
  background: #a9c4f8;
}

.input-tip {
  display: block;
  max-width: 920px;
  margin: 10rpx auto 0;
  color: #94a3b8;
  font-size: 19rpx;
  text-align: center;
}
</style>
