<script setup lang="ts">
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getCurrentUser } from '../../../api/auth'
import { updateMyProfile } from '../../../api/user'
import { getToken, removeToken } from '../../../auth/token'
import type { User } from '../../../types/auth'

const user = ref<User | null>(null)
const nickname = ref('')
const email = ref('')
const loading = ref(true)
const submitting = ref(false)

onLoad(() => {
  void loadProfile()
})

function goBack() {
  uni.navigateBack()
}

function goLogin() {
  uni.reLaunch({ url: '/pages/auth/login/index' })
}

/** 读取当前资料，并将后端的最新值填入可编辑表单。 */
async function loadProfile() {
  if (!getToken()) {
    goLogin()
    return
  }

  loading.value = true

  try {
    const currentUser = await getCurrentUser()
    user.value = currentUser
    nickname.value = currentUser.nickname
    email.value = currentUser.email
  } catch {
    removeToken()
    uni.showToast({ title: '登录状态已失效，请重新登录', icon: 'none' })
    setTimeout(goLogin, 800)
  } finally {
    loading.value = false
  }
}

/** 前端做基础校验；后端仍会执行完整校验。 */
function validateForm(): boolean {
  if (!nickname.value.trim()) {
    uni.showToast({ title: '请输入昵称', icon: 'none' })
    return false
  }

  if (!email.value.trim()) {
    uni.showToast({ title: '请输入邮箱', icon: 'none' })
    return false
  }

  return true
}

/** 保存成功后直接采用 PATCH 响应中的已持久化资料更新当前页面。 */
async function handleSave() {
  if (submitting.value || !validateForm()) {
    return
  }

  submitting.value = true

  try {
    const updatedUser = await updateMyProfile({
      nickname: nickname.value.trim(),
      email: email.value.trim(),
    })

    // 后端可能会规范化数据，例如把邮箱转换成小写，因此以响应为准。
    user.value = updatedUser
    nickname.value = updatedUser.nickname
    email.value = updatedUser.email

    uni.showToast({ title: '资料保存成功', icon: 'success' })
    setTimeout(goBack, 700)
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
      <view class="back-button" @tap="goBack">
        <uni-icons type="left" size="24" color="#14213d" />
      </view>
      <text class="page-title">账号设置</text>
      <view class="top-placeholder" />
    </view>

    <view v-if="loading" class="state-panel">
      <uni-load-more status="loading" content-text="正在读取资料" />
    </view>

    <template v-else-if="user">
      <view class="avatar-section">
        <view class="avatar">
          <text>{{ nickname.slice(0, 1).toUpperCase() }}</text>
        </view>
        <view>
          <text class="avatar-title">头像</text>
          <text class="avatar-tip">第一版使用昵称首字母头像</text>
        </view>
      </view>

      <view class="form-card">
        <view class="field">
          <text class="field-label">昵称</text>
          <input
            v-model="nickname"
            class="field-input"
            maxlength="30"
            placeholder="请输入昵称"
            placeholder-class="placeholder"
          />
        </view>

        <view class="field">
          <text class="field-label">邮箱</text>
          <input
            v-model="email"
            class="field-input"
            type="text"
            placeholder="请输入邮箱"
            placeholder-class="placeholder"
          />
          <text class="field-tip">邮箱用于联系和未来账号找回，不用于登录。</text>
        </view>

        <view class="field">
          <text class="field-label">登录用户名</text>
          <view class="readonly-value">
            <text>{{ user.username }}</text>
          </view>
          <text class="field-tip">登录用户名当前不支持修改。</text>
        </view>
      </view>

      <button
        class="save-button"
        :loading="submitting"
        :disabled="submitting"
        @tap="handleSave"
      >
        保存修改
      </button>
    </template>
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
  margin-bottom: 28rpx;
}

.back-button,
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

.state-panel {
  padding: 120rpx 0;
}

.avatar-section {
  display: flex;
  align-items: center;
  gap: 24rpx;
  padding: 34rpx;
  background: #ffffff;
  border-radius: 24rpx;
}

.avatar {
  display: grid;
  width: 108rpx;
  height: 108rpx;
  place-items: center;
  color: #ffffff;
  font-size: 44rpx;
  font-weight: 800;
  background: #2563eb;
  border-radius: 50%;
}

.avatar-title {
  display: block;
  color: #1e293b;
  font-size: 30rpx;
  font-weight: 700;
}

.avatar-tip,
.field-tip {
  display: block;
  margin-top: 10rpx;
  color: #94a3b8;
  font-size: 23rpx;
  line-height: 1.5;
}

.form-card {
  padding: 10rpx 30rpx;
  margin-top: 24rpx;
  background: #ffffff;
  border-radius: 24rpx;
}

.field {
  padding: 30rpx 0;
  border-bottom: 1rpx solid #e2e8f0;
}

.field:last-child {
  border-bottom: 0;
}

.field-label {
  display: block;
  color: #1e293b;
  font-size: 29rpx;
  font-weight: 700;
}

.field-input,
.readonly-value {
  box-sizing: border-box;
  width: 100%;
  height: 88rpx;
  padding: 0 24rpx;
  margin-top: 18rpx;
  border: 2rpx solid #d8e1ee;
  border-radius: 16rpx;
  color: #1e293b;
  font-size: 28rpx;
  line-height: 88rpx;
}

.readonly-value {
  color: #64748b;
  background: #f8fafc;
}

.placeholder {
  color: #94a3b8;
}

.save-button {
  height: 90rpx;
  margin-top: 44rpx;
  border-radius: 18rpx;
  color: #ffffff;
  font-size: 30rpx;
  font-weight: 700;
  line-height: 90rpx;
  background: #2563eb;
}
</style>
