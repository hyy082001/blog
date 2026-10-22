<script setup lang="ts">
import type { Article } from '../../types/article'
import { formatDate } from '../../utils/date'

defineProps<{
  /** 当前要展示的文章。 */
  article: Article
  /** 用于让没有封面的文章，也能显示不同的默认颜色。 */
  index: number
  /** 在“我的文章”页展示文章状态；首页和文章广场不需要展示。 */
  showStatus?: boolean
}>()

/**
 * 子组件不负责跳转，只在用户点击卡片时通知父页面。
 * open: [] 表示事件名为 open，且不携带任何额外数据。
 */
const emit = defineEmits<{
  open: []
}>()

const fallbackColors = ['blue', 'purple', 'green']

function getFallbackColor(index: number) {
  return fallbackColors[index % fallbackColors.length]
}
</script>

<template>
  <view class="article-card" @tap="emit('open')">
    <image
      v-if="article.coverUrl"
      class="article-cover"
      :src="article.coverUrl"
      mode="aspectFill"
    />

    <view v-else class="article-cover article-cover--fallback" :class="`article-cover--${getFallbackColor(index)}`">
      <text class="fallback-text">ARTICLE</text>
    </view>

    <view class="article-content">
      <text class="article-title">{{ article.title }}</text>
      <text class="article-summary">{{ article.summary || '这篇文章暂未填写摘要。' }}</text>
      <view class="article-meta">
        <text v-if="showStatus" :class="['status', article.status.toLowerCase()]">
          {{ article.status === 'DRAFT' ? '草稿' : '已发布' }}
        </text>
        <text>{{ formatDate(article.publishedAt || article.createdAt) }}</text>
        <text>{{ article.viewCount }} 阅读</text>
      </view>
    </view>
  </view>
</template>

<style scoped lang="scss">
.article-card {
  display: flex;
  gap: 24rpx;
  box-sizing: border-box;
  width: 100%;
  padding: 24rpx;
  border-radius: 28rpx;
  background: #ffffff;
  box-shadow: 0 12rpx 32rpx rgba(30, 61, 114, 0.06);
}

.article-cover {
  flex-shrink: 0;
  width: 190rpx;
  height: 190rpx;
  border-radius: 20rpx;
  background: #dbeafe;
}

.article-cover--fallback {
  display: flex;
  align-items: center;
  justify-content: center;
}

.article-cover--blue { background: #2f64e9; }
.article-cover--purple { background: #7c3aed; }
.article-cover--green { background: #15847d; }

.fallback-text {
  color: #ffffff;
  font-size: 26rpx;
  font-weight: 700;
}

.article-content {
  display: flex;
  flex: 1;
  min-width: 0;
  flex-direction: column;
  justify-content: center;
}

.article-title {
  overflow: hidden;
  color: #14213d;
  font-size: 32rpx;
  font-weight: 700;
  line-height: 1.35;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.article-summary {
  display: -webkit-box;
  overflow: hidden;
  margin-top: 12rpx;
  color: #64748b;
  font-size: 25rpx;
  line-height: 1.55;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.article-meta {
  display: flex;
  gap: 20rpx;
  margin-top: 16rpx;
  color: #2f64e9;
  font-size: 22rpx;
}

.status {
  padding: 2rpx 12rpx;
  border-radius: 10rpx;
  font-size: 20rpx;
}

.status.draft {
  background: #fff7e5;
  color: #d97706;
}

.status.published {
  background: #eaf2ff;
  color: #2563eb;
}
</style>
