<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getCurrentUser } from '../../../api/auth'
import {
  deleteMyArticle,
  getArticleDetail,
  getMyArticleDetail,
  updateMyArticle,
} from '../../../api/article'
import { getToken, removeToken } from '../../../auth/token'
import type { Article } from '../../../types/article'
import { formatDate } from '../../../utils/date'

const article = ref<Article | null>(null)
const articleId = ref<number | null>(null)
const loading = ref(true)
const operating = ref(false)
const errorMessage = ref('')
const mine = ref(false)

/** 只有作者查看自己的草稿时，才显示“发布”按钮。 */
const canPublish = computed(() => mine.value && article.value?.status === 'DRAFT')
/** 作者查看自己的文章时，草稿和已发布文章都允许删除。 */
const canDelete = computed(() => mine.value && article.value !== null)

function goLogin() {
  uni.reLaunch({ url: '/pages/auth/login/index' })
}

function goBack() {
  uni.navigateBack()
}

async function loadArticle(id: number) {
  if (!getToken()) {
    goLogin()
    return
  }

  loading.value = true
  errorMessage.value = ''

  try {
    // 先验证 Token，失效时统一回到登录页。
    await getCurrentUser()
    // “我的文章”页传入 mine=1，作者才能读取自己的草稿。
    article.value = mine.value
      ? await getMyArticleDetail(id)
      : await getArticleDetail(id)
  } catch (error) {
    const message = error instanceof Error ? error.message : '加载文章失败，请稍后重试'
    if (message.includes('401') || message.includes('未登录') || message.includes('令牌')) {
      removeToken()
      goLogin()
      return
    }
    errorMessage.value = message
  } finally {
    loading.value = false
  }
}

/** 将当前草稿的完整内容提交给 PUT 接口，仅把状态改为 PUBLISHED。 */
async function publishDraft() {
  if (!article.value || articleId.value === null || operating.value) {
    return
  }

  operating.value = true
  try {
    article.value = await updateMyArticle(articleId.value, {
      title: article.value.title,
      summary: article.value.summary,
      content: article.value.content,
      coverUrl: article.value.coverUrl,
      status: 'PUBLISHED',
    })
    uni.showToast({ title: '文章已发布', icon: 'success' })

    // 返回“我的文章”页后，onShow 会重新请求列表，草稿会从草稿箱移到已发布列表。
    setTimeout(goBack, 800)
  } catch (error) {
    uni.showToast({
      title: error instanceof Error ? error.message : '发布失败，请稍后重试',
      icon: 'none',
      duration: 2500,
    })
  } finally {
    operating.value = false
  }
}

/** 删除属于当前用户的文章；操作不可恢复，因此先要求用户确认。 */
function confirmDelete() {
  if (!article.value || articleId.value === null || operating.value) {
    return
  }

  uni.showModal({
    title: '删除文章',
    content: `确定删除《${article.value.title}》吗？删除后无法恢复。`,
    confirmText: '删除',
    confirmColor: '#dc2626',
    success: async (result) => {
      if (!result.confirm || articleId.value === null) {
        return
      }

      operating.value = true
      try {
        await deleteMyArticle(articleId.value)
        uni.showToast({ title: '文章已删除', icon: 'success' })
        setTimeout(goBack, 800)
      } catch (error) {
        uni.showToast({
          title: error instanceof Error ? error.message : '删除失败，请稍后重试',
          icon: 'none',
          duration: 2500,
        })
      } finally {
        operating.value = false
      }
    },
  })
}

onLoad((options) => {
  const id = Number(options?.id)
  mine.value = options?.mine === '1'
  if (!Number.isInteger(id) || id <= 0) {
    errorMessage.value = '文章参数不正确'
    loading.value = false
    return
  }

  articleId.value = id
  loadArticle(id)
})
</script>

<template>
  <view class="page" :class="{ 'has-action-bar': canDelete }">
    <view class="top-bar" @tap="goBack">
      <uni-icons type="left" size="22" color="#14213d" />
      <text>返回</text>
    </view>

    <view v-if="loading" class="state-panel">
      <uni-load-more status="loading" content-text="加载中" />
    </view>

    <view v-else-if="errorMessage" class="state-panel">
      <text class="state-title">{{ errorMessage }}</text>
      <button class="retry-button" @tap="goBack">返回上一页</button>
    </view>

    <view v-else-if="article" class="article-detail">
      <view v-if="mine" class="status-row">
        <text :class="['status-tag', article.status.toLowerCase()]">
          {{ article.status === 'DRAFT' ? '草稿' : '已发布' }}
        </text>
      </view>
      <text class="article-title">{{ article.title }}</text>
      <view class="article-meta">
        <text>{{ formatDate(article.publishedAt || article.createdAt) }}</text>
        <text>{{ article.viewCount }} 阅读</text>
      </view>

      <!-- 摘要在创建时可选；未填写时不渲染空白区域。 -->
      <text v-if="article.summary" class="article-summary">{{ article.summary }}</text>

      <image v-if="article.coverUrl" class="cover" :src="article.coverUrl" mode="widthFix" />

      <!-- 第一版直接展示纯文本；后续可接入 Markdown 渲染组件。 -->
      <text class="article-body">{{ article.content }}</text>
    </view>

    <!-- 仅从“我的文章”进入时展示。公开文章详情不显示作者操作。 -->
    <view v-if="canDelete" class="action-bar">
      <button
        v-if="canPublish"
        class="publish-button"
        :loading="operating"
        :disabled="operating"
        @tap="publishDraft"
      >
        发布文章
      </button>
      <button
        class="delete-button"
        :disabled="operating"
        @tap="confirmDelete"
      >
        删除
      </button>
    </view>
  </view>
</template>

<style scoped lang="scss">
.page {
  min-height: 100vh;
  box-sizing: border-box;
  padding: 28rpx 32rpx 64rpx;
  background: #f4f7fb;
}

.has-action-bar {
  padding-bottom: 190rpx;
}

.top-bar {
  display: inline-flex;
  align-items: center;
  gap: 4rpx;
  margin-bottom: 44rpx;
  color: #14213d;
  font-size: 28rpx;
}

.state-panel {
  padding: 120rpx 30rpx;
  color: #64748b;
  text-align: center;
}

.state-title { display: block; }

.retry-button {
  width: 300rpx;
  margin: 36rpx auto 0;
  border-radius: 18rpx;
  background: #2f64e9;
  color: #ffffff;
}

.article-detail {
  padding: 44rpx 36rpx 56rpx;
  border-radius: 30rpx;
  background: #ffffff;
}

.status-row {
  margin-bottom: 20rpx;
}

.status-tag {
  display: inline-block;
  padding: 7rpx 16rpx;
  border-radius: 999rpx;
  font-size: 22rpx;
  font-weight: 700;
}

.status-tag.draft {
  color: #b45309;
  background: #fef3c7;
}

.status-tag.published {
  color: #047857;
  background: #d1fae5;
}

.article-title {
  color: #14213d;
  font-size: 48rpx;
  font-weight: 800;
  line-height: 1.35;
}

.article-meta {
  display: flex;
  gap: 24rpx;
  margin-top: 24rpx;
  color: #72809a;
  font-size: 24rpx;
}

.article-summary {
  display: block;
  padding: 22rpx 24rpx;
  margin-top: 30rpx;
  border-left: 6rpx solid #2563eb;
  border-radius: 0 14rpx 14rpx 0;
  color: #52647f;
  font-size: 28rpx;
  line-height: 1.65;
  background: #f1f6ff;
}

.cover {
  width: 100%;
  margin-top: 36rpx;
  border-radius: 20rpx;
}

.article-body {
  display: block;
  margin-top: 40rpx;
  color: #334155;
  font-size: 30rpx;
  line-height: 1.85;
  white-space: pre-wrap;
}

.action-bar {
  position: fixed;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: 10;
  display: flex;
  gap: 20rpx;
  box-sizing: border-box;
  padding: 24rpx 30rpx calc(24rpx + env(safe-area-inset-bottom));
  background: #ffffff;
  box-shadow: 0 -8rpx 24rpx rgb(30 64 175 / 8%);
}

.publish-button,
.delete-button {
  flex: 1;
  height: 88rpx;
  margin: 0;
  border-radius: 18rpx;
  font-size: 30rpx;
  font-weight: 700;
  line-height: 88rpx;
}

.publish-button {
  color: #ffffff;
  background: #2563eb;
}

.delete-button {
  color: #dc2626;
  background: #fef2f2;
  border: 2rpx solid #fecaca;
}
</style>


