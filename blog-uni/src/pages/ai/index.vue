<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import {
  createAiConversation,
  deleteAiConversation,
  getAiConversations,
} from '../../api/ai'
import { ApiError } from '../../api/client'
import { getToken, removeToken } from '../../auth/token'
import type { AiConversation } from '../../types/ai'
import { formatDate } from '../../utils/date'
import BottomNav from '../../components/navigation/BottomNav.vue'

const conversations = ref<AiConversation[]>([])
const loading = ref(true)
const creating = ref(false)
const deletingId = ref<number | null>(null)
const errorMessage = ref('')

onShow(() => {
  void loadConversations()
})

/** 清除失效的登录状态并返回登录页。 */
function goLogin() {
  removeToken()
  uni.reLaunch({ url: '/pages/auth/login/index' })
}

/** 统一处理 AI 页面中的接口错误。 */
function handleError(error: unknown, fallbackMessage: string) {
  if (error instanceof ApiError && error.statusCode === 401) {
    goLogin()
    return
  }

  errorMessage.value = error instanceof Error ? error.message : fallbackMessage
}

/** 加载当前用户自己的全部 AI 会话。 */
async function loadConversations() {
  if (!getToken()) {
    goLogin()
    return
  }

  loading.value = true
  errorMessage.value = ''

  try {
    conversations.value = await getAiConversations()
  } catch (error) {
    handleError(error, 'AI 会话加载失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

/** 先在后端创建会话，成功后再进入聊天页。 */
async function createConversation() {
  if (creating.value) {
    return
  }

  creating.value = true
  errorMessage.value = ''

  try {
    const conversation = await createAiConversation()
    uni.navigateTo({ url: `/pages/ai/chat/index?id=${conversation.id}` })
  } catch (error) {
    handleError(error, '创建会话失败，请稍后重试')
  } finally {
    creating.value = false
  }
}

/** 打开一个已有的 AI 会话。 */
function openConversation(conversationId: number) {
  uni.navigateTo({ url: `/pages/ai/chat/index?id=${conversationId}` })
}

/** 删除前先让用户确认，避免误触。 */
function confirmDelete(conversation: AiConversation) {
  uni.showModal({
    title: '删除对话',
    content: `确定删除“${conversation.title}”吗？删除后无法恢复。`,
    confirmText: '删除',
    confirmColor: '#ef4444',
    success(result) {
      if (result.confirm) {
        void removeConversation(conversation.id)
      }
    },
  })
}

/** 调用后端删除会话，并同步移除页面中的会话卡片。 */
async function removeConversation(conversationId: number) {
  if (deletingId.value !== null) {
    return
  }

  deletingId.value = conversationId

  try {
    await deleteAiConversation(conversationId)
    conversations.value = conversations.value.filter(
      conversation => conversation.id !== conversationId,
    )
    uni.showToast({ title: '会话已删除', icon: 'success' })
  } catch (error) {
    handleError(error, '删除会话失败，请稍后重试')
    uni.showToast({
      title: errorMessage.value,
      icon: 'none',
      duration: 2500,
    })
  } finally {
    deletingId.value = null
  }
}
</script>

<template>
  <view class="page">
    <view class="content-shell">
      <view class="page-heading">
        <view class="heading-copy">
          <text class="brand">AI ASSISTANT</text>
          <text class="page-title">AI 助手</text>
          <text class="page-description">
            和 AI 一起解决技术问题、整理思路和辅助写作。
          </text>
        </view>

        <view
          :class="['new-button', { disabled: creating }]"
          @tap="createConversation"
        >
          <uni-icons type="plusempty" color="#ffffff" :size="22" />
          <text>{{ creating ? '创建中' : '新对话' }}</text>
        </view>
      </view>

      <view v-if="loading" class="state-panel">
        <uni-load-more status="loading" />
        <text>正在加载会话</text>
      </view>

      <view v-else-if="errorMessage" class="state-panel">
        <text class="state-title">加载失败</text>
        <text class="state-description">{{ errorMessage }}</text>
        <text class="retry" @tap="loadConversations">重新加载</text>
      </view>

      <view v-else-if="conversations.length === 0" class="empty-card">
        <view class="empty-icon">
          <uni-icons type="chatbubble-filled" color="#2563eb" :size="38" />
        </view>
        <text class="empty-title">开始第一次对话</text>
        <text class="empty-description">
          你可以让 AI 解答技术问题、生成文章大纲或者优化文章内容。
        </text>
        <view class="empty-button" @tap="createConversation">
          <text>创建新对话</text>
        </view>
      </view>

      <view v-else class="conversation-list">
        <view
          v-for="conversation in conversations"
          :key="conversation.id"
          class="conversation-card"
          @tap="openConversation(conversation.id)"
        >
          <view class="conversation-icon">
            <uni-icons type="chatbubble-filled" color="#2563eb" :size="26" />
          </view>

          <view class="conversation-content">
            <text class="conversation-title">{{ conversation.title }}</text>
            <text class="conversation-time">
              更新于 {{ formatDate(conversation.updatedAt) }}
            </text>
          </view>

          <view class="delete-button" @tap.stop="confirmDelete(conversation)">
            <uni-icons
              type="trash"
              :color="deletingId === conversation.id ? '#cbd5e1' : '#94a3b8'"
              :size="22"
            />
          </view>
        </view>
      </view>
    </view>
  </view>

  <BottomNav active="ai" />
</template>

<style scoped lang="scss">
.page {
  min-height: 100vh;
  box-sizing: border-box;
  padding: 50rpx 32rpx 210rpx;
  overflow-x: hidden;
  background: #f4f7fb;
}

.content-shell {
  width: 100%;
  max-width: 920px;
  box-sizing: border-box;
  margin: 0 auto;
}

.page-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24rpx;
  margin-bottom: 42rpx;
}

.heading-copy {
  min-width: 0;
}

.brand {
  display: block;
  margin-bottom: 12rpx;
  color: #2563eb;
  font-size: 22rpx;
  font-weight: 800;
  letter-spacing: 3rpx;
}

.page-title {
  display: block;
  color: #0f172a;
  font-size: 46rpx;
  font-weight: 800;
}

.page-description {
  display: block;
  max-width: 500rpx;
  margin-top: 14rpx;
  color: #64748b;
  font-size: 26rpx;
  line-height: 1.6;
}

.new-button {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  gap: 6rpx;
  padding: 18rpx 24rpx;
  color: #ffffff;
  font-size: 25rpx;
  font-weight: 700;
  background: #2563eb;
  border-radius: 20rpx;
  box-shadow: 0 10rpx 22rpx rgb(37 99 235 / 22%);
}

.new-button.disabled {
  opacity: 0.6;
}

.conversation-list {
  display: grid;
  width: 100%;
  min-width: 0;
  gap: 20rpx;
}

.conversation-card {
  display: flex;
  width: 100%;
  min-width: 0;
  box-sizing: border-box;
  align-items: center;
  gap: 22rpx;
  padding: 28rpx;
  overflow: hidden;
  background: #ffffff;
  border: 2rpx solid transparent;
  border-radius: 26rpx;
  box-shadow: 0 8rpx 24rpx rgb(15 23 42 / 5%);
}

.conversation-card:active {
  background: #f8fbff;
  border-color: #bfdbfe;
}

.conversation-icon {
  display: grid;
  width: 82rpx;
  height: 82rpx;
  flex-shrink: 0;
  place-items: center;
  background: #eaf2ff;
  border-radius: 24rpx;
}

.conversation-content {
  display: flex;
  min-width: 0;
  flex: 1;
  overflow: hidden;
  flex-direction: column;
  gap: 10rpx;
}

.conversation-title {
  display: block;
  width: 100%;
  overflow: hidden;
  color: #172033;
  font-size: 29rpx;
  font-weight: 750;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.conversation-time {
  color: #94a3b8;
  font-size: 23rpx;
}

.delete-button {
  display: grid;
  width: 64rpx;
  height: 64rpx;
  flex-shrink: 0;
  place-items: center;
  border-radius: 50%;
}

.delete-button:active {
  background: #fef2f2;
}

.empty-card,
.state-panel {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 90rpx 44rpx;
  color: #64748b;
  font-size: 26rpx;
  text-align: center;
  background: #ffffff;
  border-radius: 28rpx;
}

.empty-icon {
  display: grid;
  width: 112rpx;
  height: 112rpx;
  place-items: center;
  background: #eaf2ff;
  border-radius: 50%;
}

.empty-title,
.state-title {
  margin-top: 28rpx;
  color: #172033;
  font-size: 32rpx;
  font-weight: 800;
}

.empty-description,
.state-description {
  margin-top: 16rpx;
  color: #64748b;
  font-size: 25rpx;
  line-height: 1.65;
}

.empty-button {
  margin-top: 34rpx;
  padding: 20rpx 38rpx;
  color: #ffffff;
  font-size: 25rpx;
  font-weight: 700;
  background: #2563eb;
  border-radius: 20rpx;
}

.retry {
  margin-top: 24rpx;
  color: #2563eb;
  font-weight: 700;
}
</style>
