<script setup lang="ts">
import { ref } from 'vue'
import { createArticle } from '../../../api/article'
import type { ArticleStatus } from '../../../types/article'

const title = ref('')
const summary = ref('')
const coverUrl = ref('')
const content = ref('')
const submitting = ref(false)

/** 回到打开写作页前的页面。 */
function goBack() {
  uni.navigateBack()
}

/**
 * 保存前只校验后端要求的必填字段。
 * 摘要和封面 URL 可为空；空字符串转换为 null，使请求含义更清晰。
 */
function validateForm(): boolean {
  if (!title.value.trim()) {
    uni.showToast({ title: '请输入文章标题', icon: 'none' })
    return false
  }

  if (!content.value.trim()) {
    uni.showToast({ title: '请输入文章正文', icon: 'none' })
    return false
  }

  return true
}

async function submit(status: ArticleStatus) {
  if (submitting.value || !validateForm()) {
    return
  }

  submitting.value = true

  try {
    await createArticle({
      title: title.value.trim(),
      summary: summary.value.trim() || null,
      content: content.value.trim(),
      coverUrl: coverUrl.value.trim() || null,
      status,
    })

    const published = status === 'PUBLISHED'
    uni.showToast({
      title: published ? '文章发布成功' : '草稿保存成功',
      icon: 'success',
    })

    setTimeout(() => {
      // 已发布文章会出现在文章列表；草稿暂时没有单独的管理页，返回上一页即可。
      if (published) {
        uni.reLaunch({ url: '/pages/article/index' })
        return
      }
      goBack()
    }, 900)
  } catch (error) {
    uni.showToast({
      title: error instanceof Error ? error.message : '保存失败，请稍后重试',
      icon: 'none',
      duration: 2500,
    })
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <view class="page">
    <view class="top-bar">
      <view class="back" @tap="goBack">
        <uni-icons type="left" size="24" color="#14213d" />
      </view>
      <text class="page-title">新建文章</text>
      <text class="draft-text" @tap="submit('DRAFT')">存草稿</text>
    </view>

    <view class="editor-card">
      <view class="field">
        <text class="field-label">文章标题</text>
        <input
          v-model="title"
          class="title-input"
          maxlength="200"
          placeholder="输入文章标题"
          placeholder-class="placeholder"
        />
      </view>

      <view class="field">
        <view class="label-row">
          <text class="field-label">摘要 <text class="optional">（选填）</text></text>
          <text class="count">{{ summary.length }}/500</text>
        </view>
        <textarea
          v-model="summary"
          class="summary-input"
          maxlength="500"
          placeholder="用一句话介绍文章内容"
          placeholder-class="placeholder"
        />
      </view>

      <view class="field">
        <text class="field-label">封面图 <text class="optional">（选填）</text></text>
        <!-- 第一版不接对象存储，先让作者填入可访问的图片 URL。 -->
        <input
          v-model="coverUrl"
          class="cover-url-input"
          maxlength="500"
          placeholder="输入封面图片 URL"
          placeholder-class="placeholder"
        />
      </view>

      <view class="field content-field">
        <text class="field-label">文章内容</text>
        <textarea
          v-model="content"
          class="content-input"
          maxlength="-1"
          placeholder="开始写作… 支持 Markdown"
          placeholder-class="placeholder"
        />
      </view>
    </view>

    <view class="action-bar">
      <button class="draft-button" :disabled="submitting" @tap="submit('DRAFT')">存为草稿</button>
      <button class="publish-button" :loading="submitting" :disabled="submitting" @tap="submit('PUBLISHED')">
        发布文章
      </button>
    </view>
  </view>
</template>

<style scoped lang="scss">
.page {
  min-height: 100vh;
  box-sizing: border-box;
  padding: 20rpx 30rpx 188rpx;
  background: #f4f7fb;
}

.top-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 88rpx;
  margin-bottom: 22rpx;
}

.back,
.draft-text {
  display: flex;
  width: 120rpx;
  align-items: center;
}

.draft-text {
  justify-content: flex-end;
  color: #2563eb;
  font-size: 29rpx;
  font-weight: 700;
}

.page-title {
  color: #14213d;
  font-size: 38rpx;
  font-weight: 800;
}

.editor-card {
  padding: 36rpx 30rpx 44rpx;
  border-radius: 30rpx;
  background: #ffffff;
}

.field + .field {
  margin-top: 38rpx;
}

.field-label {
  color: #1e293b;
  font-size: 30rpx;
  font-weight: 750;
}

.optional {
  color: #71809a;
  font-weight: 400;
}

.label-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.count {
  color: #94a3b8;
  font-size: 22rpx;
}

.title-input,
.summary-input,
.cover-url-input,
.content-input {
  box-sizing: border-box;
  width: 100%;
  margin-top: 18rpx;
  border: 2rpx solid #d8e1ee;
  border-radius: 16rpx;
  color: #1e293b;
  font-size: 28rpx;
  line-height: 1.5;
}

.title-input,
.cover-url-input {
  height: 88rpx;
  padding: 0 24rpx;
}

.summary-input {
  height: 150rpx;
  padding: 20rpx 24rpx;
}

.content-field {
  margin-bottom: 4rpx;
}

.content-input {
  min-height: 470rpx;
  padding: 22rpx 24rpx;
}

.placeholder {
  color: #94a3b8;
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
  box-shadow: 0 -8rpx 24rpx rgb(30 64 175 / 7%);
}

.draft-button,
.publish-button {
  flex: 1;
  height: 86rpx;
  margin: 0;
  border-radius: 18rpx;
  font-size: 30rpx;
  font-weight: 700;
  line-height: 86rpx;
}

.draft-button {
  border: 2rpx solid #2563eb;
  background: #ffffff;
  color: #2563eb;
}

.publish-button {
  background: #2563eb;
  color: #ffffff;
}
</style>
