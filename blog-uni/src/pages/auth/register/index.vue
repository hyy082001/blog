<template>
  <view class="auth-page">
    <view class="auth-card">
      <text class="brand">MY BLOG</text>
      <text class="title">创建账号</text>
      <text class="subtitle">注册后即可发布属于自己的文章。</text>

      <view class="form-item">
        <text class="label">用户名</text>
        <input v-model="username" maxlength="30" placeholder="3 至 30 位字母、数字或下划线" />
      </view>

      <view class="form-item">
        <text class="label">邮箱</text>
        <input v-model="email" type="text" placeholder="name@example.com" />
      </view>

      <view class="form-item">
        <text class="label">密码</text>
        <input v-model="password" password maxlength="72" placeholder="8 至 72 位" />
      </view>

      <view class="form-item">
        <text class="label">确认密码</text>
        <input v-model="confirmPassword" password maxlength="72" placeholder="请再次输入密码" />
      </view>

      <button class="primary-button" :disabled="submitting" @click="handleRegister">
        {{ submitting ? '注册中…' : '注册' }}
      </button>

      <view class="switch-line">
        <text>已有账号？</text>
        <text class="link" @click="goLogin">去登录</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { register } from '../../../api/auth'

const username = ref('')
const email = ref('')
const password = ref('')
const confirmPassword = ref('')
const submitting = ref(false)

async function handleRegister() {
  if (!username.value.trim() || !email.value.trim() || !password.value) {
    uni.showToast({ title: '请完整填写注册信息', icon: 'none' })
    return
  }

  if (password.value !== confirmPassword.value) {
    uni.showToast({ title: '两次输入的密码不一致', icon: 'none' })
    return
  }

  submitting.value = true

  try {
    await register({
      username: username.value.trim(),
      email: email.value.trim(),
      password: password.value,
    })

    uni.showToast({ title: '注册成功，请登录', icon: 'success' })
    setTimeout(() => {
      uni.redirectTo({ url: '/pages/auth/login/index' })
    }, 800)
  } catch (error) {
    uni.showToast({
      title: error instanceof Error ? error.message : '注册失败，请稍后重试',
      icon: 'none',
    })
  } finally {
    submitting.value = false
  }
}

function goLogin() {
  uni.navigateTo({ url: '/pages/auth/login/index' })
}
</script>

<style scoped>
.auth-page { display: flex; min-height: 100vh; align-items: center; padding: 36rpx; box-sizing: border-box; }
.auth-card { box-sizing: border-box; width: 100%; max-width: 440px; padding: 54rpx 42rpx; margin: 0 auto; background: #fff; border-radius: 28rpx; box-shadow: 0 16rpx 40rpx rgb(15 23 42 / 8%); }
.brand { display: block; margin-bottom: 20rpx; color: #2563eb; font-size: 23rpx; font-weight: 800; letter-spacing: 4rpx; }
.title { display: block; color: #0f172a; font-size: 54rpx; font-weight: 800; }.subtitle { display: block; margin: 16rpx 0 42rpx; color: #64748b; font-size: 28rpx; }
.form-item { margin-bottom: 25rpx; }.label { display: block; margin-bottom: 12rpx; color: #334155; font-size: 26rpx; font-weight: 700; }
input { height: 86rpx; padding: 0 22rpx; color: #172033; font-size: 27rpx; border: 2rpx solid #cbd5e1; border-radius: 14rpx; }
.primary-button { margin-top: 12rpx; color: #fff; background: #2563eb; border-radius: 14rpx; font-size: 29rpx; font-weight: 700; }.primary-button[disabled] { opacity: 0.65; }
.switch-line { display: flex; justify-content: center; gap: 8rpx; margin-top: 42rpx; color: #64748b; font-size: 26rpx; }.link { color: #2563eb; font-weight: 700; }
</style>

