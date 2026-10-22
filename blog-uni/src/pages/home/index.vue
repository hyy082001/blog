<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { getCurrentUser } from '../../api/auth'
import { getPublishedArticles } from '../../api/article'
import { getToken, removeToken } from '../../auth/token'
import type { User } from '../../types/auth'
import type { Article } from '../../types/article'
import ArticleCard from '../../components/article/ArticleCard.vue'
import BottomNav from '../../components/navigation/BottomNav.vue'

const user = ref<User | null>(null)
const articles = ref<Article[]>([])
const loadingArticles = ref(false)
const articleError = ref('')

onShow(() => {
  void loadPage()
})

async function loadPage() {
  if (!getToken()) {
    uni.reLaunch({ url: '/pages/auth/login/index' })
    return
  }

  try {
    // 先访问 /api/auth/me 验证当前 Token 是否仍然有效。
    user.value = await getCurrentUser()
    await loadArticles()
  } catch {
    removeToken()
    uni.showToast({ title: '登录状态已失效，请重新登录', icon: 'none' })
    setTimeout(() => {
      uni.reLaunch({ url: '/pages/auth/login/index' })
    }, 800)
  }
}

async function loadArticles() {
  loadingArticles.value = true
  articleError.value = ''

  try {
    // 首页只显示最新的三篇，完整列表交给“文章”页展示。
    articles.value = (await getPublishedArticles()).slice(0, 3)
  } catch (error) {
    articleError.value = error instanceof Error ? error.message : '文章加载失败，请稍后重试'
  } finally {
    loadingArticles.value = false
  }
}

/** 自定义导航下的主页面使用 reLaunch 切换。 */
function goArticles() {
  uni.reLaunch({ url: '/pages/article/index' })
}

/** 文章详情是二级页面，保留当前首页在返回栈中。 */
function goDetail(articleId: number) {
  uni.navigateTo({ url: `/pages/article/detail/index?id=${articleId}` })
}
</script>

<template>
  <view v-if="user" class="page">
    <view class="hero-section">
      <view>
        <text class="brand">MY SPACE</text>
        <text class="hero-title">你好，{{ user.nickname }}</text>
        <text class="hero-subtitle">开始记录你的技术思考。</text>
      </view>
      <view class="hero-code">&lt;/&gt;</view>
    </view>

    <view class="section-heading">
      <view>
        <text class="brand">EXPLORE</text>
        <text class="section-title">最新文章</text>
      </view>
      <text class="more" @tap="goArticles">查看更多 →</text>
    </view>

    <view v-if="loadingArticles" class="state-panel">
      <uni-load-more status="loading" content-text="加载文章中" />
    </view>

    <view v-else-if="articleError" class="state-panel">
      <text>{{ articleError }}</text>
      <text class="retry" @tap="loadArticles">重新加载</text>
    </view>

    <view v-else-if="articles.length === 0" class="state-panel">
      <text>还没有已发布的文章，开始写第一篇吧。</text>
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

  <view v-else class="loading">正在验证登录状态…</view>

  <BottomNav v-if="user" active="home" />
</template>

<style scoped lang="scss">
.page {
  min-height: 100vh;
  box-sizing: border-box;
  padding: 32rpx 32rpx 210rpx;
}

.hero-section {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 56rpx 44rpx;
  margin-bottom: 48rpx;
  background: #eff6ff;
  border-radius: 28rpx;
}

.brand {
  display: block;
  margin-bottom: 12rpx;
  color: #2563eb;
  font-size: 22rpx;
  font-weight: 800;
  letter-spacing: 3rpx;
}

.hero-title {
  display: block;
  color: #0f172a;
  font-size: 52rpx;
  font-weight: 800;
}

.hero-subtitle {
  display: block;
  margin-top: 14rpx;
  color: #64748b;
  font-size: 29rpx;
}

.hero-code {
  display: grid;
  width: 128rpx;
  height: 128rpx;
  place-items: center;
  color: #2563eb;
  font-size: 38rpx;
  font-weight: 800;
  background: #dbeafe;
  border-radius: 50%;
}

.section-heading {
  display: flex;
  align-items: end;
  justify-content: space-between;
  margin-bottom: 24rpx;
}

.section-title {
  display: block;
  color: #0f172a;
  font-size: 40rpx;
  font-weight: 800;
}

.more {
  color: #2563eb;
  font-size: 25rpx;
}

.article-list {
  display: grid;
  gap: 20rpx;
}

.state-panel {
  padding: 80rpx 24rpx;
  color: #64748b;
  font-size: 27rpx;
  text-align: center;
}

.retry {
  display: block;
  margin-top: 22rpx;
  color: #2563eb;
}

.loading {
  display: grid;
  min-height: 100vh;
  place-items: center;
  color: #64748b;
  font-size: 28rpx;
}
</style>

