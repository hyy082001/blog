<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { getCurrentUser } from '../../api/auth'
import { getPublishedArticles } from '../../api/article'
import { getToken, removeToken } from '../../auth/token'
import type { Article } from '../../types/article'
import ArticleCard from '../../components/article/ArticleCard.vue'
import BottomNav from '../../components/navigation/BottomNav.vue'

const articles = ref<Article[]>([])
const loading = ref(true)
const errorMessage = ref('')

onShow(() => {
  void loadPage()
})

async function loadPage() {
  if (!getToken()) {
    goLogin()
    return
  }

  loading.value = true
  errorMessage.value = ''

  try {
    // 先验证 JWT；如果 Token 已过期，避免把接口报错直接显示给用户。
    await getCurrentUser()
    articles.value = await getPublishedArticles()
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

function goLogin() {
  uni.reLaunch({ url: '/pages/auth/login/index' })
}

function goDetail(articleId: number) {
  uni.navigateTo({ url: `/pages/article/detail/index?id=${articleId}` })
}
</script>

<template>
  <view class="page">
    <view class="page-heading">
      <text class="brand">ARTICLES</text>
      <text class="page-title">全部文章</text>
      <text class="page-description">记录学习过程中的思考与实践。</text>
    </view>

    <view v-if="loading" class="state-panel">
      <uni-load-more status="loading" content-text="加载文章中" />
    </view>

    <view v-else-if="errorMessage" class="state-panel">
      <text>{{ errorMessage }}</text>
      <text class="retry" @tap="loadPage">重新加载</text>
    </view>

    <view v-else-if="articles.length === 0" class="state-panel">
      <text>暂时没有已发布的文章。</text>
    </view>

    <view v-else class="article-list">
      <ArticleCard
        v-for="(article, index) in articles"
        :key="article.id"
        :article="article"
        :index="index"
        @open="() => goDetail(article.id)"
      />
    </view>
  </view>

  <BottomNav active="article" />
</template>

<style scoped lang="scss">
.page {
  min-height: 100vh;
  box-sizing: border-box;
  padding: 50rpx 32rpx 210rpx;
}

.page-heading {
  margin-bottom: 40rpx;
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
  margin-top: 14rpx;
  color: #64748b;
  font-size: 27rpx;
}

.article-list {
  display: grid;
  gap: 20rpx;
}

.state-panel {
  padding: 90rpx 24rpx;
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

