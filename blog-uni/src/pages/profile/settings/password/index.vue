<script setup lang="ts">
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { changeMyPassword } from '../../../../api/user'
import { getToken, removeToken } from '../../../../auth/token'

const oldPassword = ref('')
const newPassword = ref('')
const confirmPassword = ref('')
const submitting = ref(false)

onLoad(() => {
  // 用户直接访问该页面、但本地已没有 Token 时，回到登录页。
  if (!getToken()) {
    uni.reLaunch({ url: '/pages/auth/login/index' })
  }
})

function goBack() {
  uni.navigateBack()
}

/**
 * 前端校验只负责即时反馈。
 * 后端仍会校验旧密码、新密码长度和新旧密码是否相同。
 */
function validateForm(): boolean {
  if (!oldPassword.value) {
    uni.showToast({ title: '请输入当前密码', icon: 'none' })
    return false
  }

  if (newPassword.value.length < 8) {
    uni.showToast({ title: '新密码至少需要 8 位', icon: 'none' })
    return false
  }

  if (newPassword.value !== confirmPassword.value) {
    uni.showToast({ title: '两次输入的新密码不一致', icon: 'none' })
    return false
  }

  return true
}

/** 密码修改成功后删除本地 Token，要求用户使用新密码重新登录。 */
async function handleSubmit() {
  if (submitting.value || !validateForm()) {
    return
  }

  submitting.value = true

  try {
    await changeMyPassword({
      oldPassword: oldPassword.value,
      newPassword: newPassword.value,
    })

    // 本设备不再保留旧 Access Token。
    removeToken()

    uni.showToast({ title: '密码已修改', icon: 'success' })
    setTimeout(() => {
      uni.reLaunch({ url: '/pages/auth/login/index' })
    }, 900)
  } catch (error) {
    uni.showToast({
      title: error instanceof Error ? error.message : '密码修改失败，请稍后重试',
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
      <text class="page-title">修改密码</text>
      <view class="top-placeholder" />
    </view>

    <view class="notice-card">
      <uni-icons type="info-filled" size="20" color="#2563eb" />
      <text>修改密码成功后，需要使用新密码重新登录。</text>
    </view>

    <view class="form-card">
      <view class="field">
        <text class="field-label">当前密码</text>
        <input
          v-model="oldPassword"
          class="field-input"
          :password="true"
          maxlength="72"
          placeholder="请输入当前密码"
          placeholder-class="placeholder"
        />
      </view>

      <view class="field">
        <text class="field-label">新密码</text>
        <input
          v-model="newPassword"
          class="field-input"
          :password="true"
          maxlength="72"
          placeholder="至少 8 位"
          placeholder-class="placeholder"
        />
      </view>

      <view class="field">
        <text class="field-label">确认新密码</text>
        <input
          v-model="confirmPassword"
          class="field-input"
          :password="true"
          maxlength="72"
          placeholder="请再次输入新密码"
          placeholder-class="placeholder"
        />
      </view>
    </view>

    <button
      class="submit-button"
      :loading="submitting"
      :disabled="submitting"
      @tap="handleSubmit"
    >
      确认修改
    </button>
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

.notice-card {
  display: flex;
  align-items: flex-start;
  gap: 12rpx;
  padding: 24rpx;
  margin-bottom: 24rpx;
  color: #466083;
  font-size: 25rpx;
  line-height: 1.5;
  background: #eaf2ff;
  border-radius: 18rpx;
}

.form-card {
  padding: 10rpx 30rpx;
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

.field-input {
  box-sizing: border-box;
  width: 100%;
  height: 88rpx;
  padding: 0 24rpx;
  margin-top: 18rpx;
  border: 2rpx solid #d8e1ee;
  border-radius: 16rpx;
  color: #1e293b;
  font-size: 28rpx;
}

.placeholder {
  color: #94a3b8;
}

.submit-button {
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

