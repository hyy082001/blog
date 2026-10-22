<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { getCurrentUser } from '../../../api/auth'
import { getMyArticles } from '../../../api/article'
import { getToken, removeToken } from '../../../auth/token'
import type { Article, ArticleStatus } from '../../../types/article'
import ArticleCard from '../../../components/article/ArticleCard.vue'

const articles = ref<Article[]>([])
const activeStatus = ref<ArticleStatus>('PUBLISHED')
const loading = ref(true)
const errorMessage = ref('')

onShow(() => {
  void loadPage()
})

function goLogin() {
  uni.reLaunch({ url: '/pages/auth/login/index' })
}

function goBack() {
  uni.navigateBack()
}

/** 验证登录状态后，按当前 Tab 请求自己的文章。 */
async function loadPage() {
  if (!getToken()) {
    goLogin()
    return
  }

  loading.value = true
  errorMessage.value = ''

  try {
    // 当前用户 ID 只由后端从 JWT 读取，前端不传 authorId。
    await getCurrentUser()
    articles.value = await getMyArticles(activeStatus.value)
  } catch (error) {
    const message = error instanceof Error ? error.message : '文章加载失败，请稍后重试'

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

/** 切换“已发布”和“草稿箱”时重新请求对应状态的文章。 */
function switchStatus(status: ArticleStatus) {
  if (activeStatus.value === status) {
    return
  }

  activeStatus.value = status
  void loadPage()
}

/** 作者视角详情页允许打开自己的草稿。 */
function openMyArticle(articleId: number) {
  uni.navigateTo({ url: `/pages/article/detail/index?id=${articleId}&mine=1` })
}
</script>

<template>
  <view class="page">
    <view class="top-bar">
      <view class="back" @tap="goBack">
        <uni-icons type="left" size="24" color="#14213d" />
      </view>
      <text class="page-title">我的文章</text>
      <view class="top-placeholder" />
    </view>

    <view class="tabs">
      <text
        :class="['tab', { active: activeStatus === 'PUBLISHED' }]"
        @tap="switchStatus('PUBLISHED')"
      >
        已发布
      </text>
      <text
        :class="['tab', { active: activeStatus === 'DRAFT' }]"
        @tap="switchStatus('DRAFT')"
      >
        草稿箱
      </text>
    </view>

    <view v-if="loading" class="state-panel">
      <uni-load-more status="loading" content-text="加载文章中" />
    </view>

    <view v-else-if="errorMessage" class="state-panel">
      <text>{{ errorMessage }}</text>
      <text class="retry" @tap="loadPage">重新加载</text>
    </view>

    <view v-else-if="articles.length === 0" class="state-panel">
      <text>{{ activeStatus === 'DRAFT' ? '草稿箱还是空的。' : '你还没有发布文章。' }}</text>
    </view>

    <view v-else class="article-list">
      <ArticleCard
        v-for="(article, index) in articles"
        :key="article.id"
        :article="article"
        :index="index"
        show-status
        @open="() => openMyArticle(article.id)"
      />
    </view>
  </view>
</template>

<style scoped lang="scss">
.page {
  min-height: 100vh;
  box-sizing: border-box;
  padding: 20rpx 32rpx 64rpx;
  background: #f4f7fb;
}

.top-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 88rpx;
  margin-bottom: 26rpx;
}

.back,
.top-placeholder {
  display: flex;
  width: 100rpx;
  align-items: center;
}

.page-title {
  color: #14213d;
  font-size: 38rpx;
  font-weight: 800;
}

.tabs {
  display: flex;
  gap: 48rpx;
  border-bottom: 2rpx solid #e2e8f0;
}

.tab {
  padding: 16rpx 6rpx 18rpx;
  color: #64748b;
  font-size: 27rpx;
}

.tab.active {
  margin-bottom: -2rpx;
  color: #2563eb;
  font-weight: 700;
  border-bottom: 4rpx solid #2563eb;
}

.article-list {
  display: grid;
  gap: 20rpx;
  margin-top: 24rpx;
}

.state-panel {
  padding: 100rpx 24rpx;
  color: #64748b;
  font-size: 27rpx;
  text-align: center;
}

.retry {
  display: block;
  margin-top: 22rpx;
  color: #2563eb;
}
</style>
