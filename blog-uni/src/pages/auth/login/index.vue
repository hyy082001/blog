<template>
  <view class="auth-page">
    <view class="auth-card">
      <text class="brand">MY BLOG</text>
      <text class="title">登录</text>
      <text class="subtitle">登录后开始阅读和创作技术文章。</text>

      <view class="form-item">
        <text class="label">用户名</text>
        <input v-model="username" placeholder="请输入用户名" />
      </view>

      <view class="form-item">
        <text class="label">密码</text>
        <input v-model="password" password placeholder="请输入密码" />
      </view>

      <button class="primary-button" :disabled="submitting" @click="handleLogin">
        {{ submitting ? '登录中…' : '登录' }}
      </button>

      <view class="switch-line">
        <text>还没有账号？</text>
        <text class="link" @click="goRegister">去注册</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { login } from '../../../api/auth'
import { saveToken } from '../../../auth/token'

const username = ref('')
const password = ref('')
const submitting = ref(false)

async function handleLogin() {
  if (!username.value.trim() || !password.value) {
    uni.showToast({ title: '请输入用户名和密码', icon: 'none' })
    return
  }

  submitting.value = true

  try {
    const response = await login({
      username: username.value.trim(),
      password: password.value,
    })

    saveToken(response.accessToken)
    uni.showToast({ title: `欢迎回来，${response.user.username}`, icon: 'success' })

    setTimeout(() => {
      uni.reLaunch({ url: '/pages/home/index' })
    }, 600)
  } catch (error) {
    uni.showToast({
      title: error instanceof Error ? error.message : '登录失败，请稍后重试',
      icon: 'none',
    })
  } finally {
    submitting.value = false
  }
}

function goRegister() {
  uni.navigateTo({ url: '/pages/auth/register/index' })
}
</script>

<style scoped>
.auth-page { display: flex; min-height: 100vh; align-items: center; padding: 36rpx; box-sizing: border-box; }
.auth-card { box-sizing: border-box; width: 100%; max-width: 440px; padding: 54rpx 42rpx; margin: 0 auto; background: #fff; border-radius: 28rpx; box-shadow: 0 16rpx 40rpx rgb(15 23 42 / 8%); }
.brand { display: block; margin-bottom: 20rpx; color: #2563eb; font-size: 23rpx; font-weight: 800; letter-spacing: 4rpx; }
.title { display: block; color: #0f172a; font-size: 54rpx; font-weight: 800; }.subtitle { display: block; margin: 16rpx 0 46rpx; color: #64748b; font-size: 28rpx; }
.form-item { margin-bottom: 28rpx; }.label { display: block; margin-bottom: 12rpx; color: #334155; font-size: 26rpx; font-weight: 700; }
input { height: 86rpx; padding: 0 22rpx; color: #172033; font-size: 27rpx; border: 2rpx solid #cbd5e1; border-radius: 14rpx; }
.primary-button { margin-top: 12rpx; color: #fff; background: #2563eb; border-radius: 14rpx; font-size: 29rpx; font-weight: 700; }.primary-button[disabled] { opacity: 0.65; }
.switch-line { display: flex; justify-content: center; gap: 8rpx; margin-top: 42rpx; color: #64748b; font-size: 26rpx; }.link { color: #2563eb; font-weight: 700; }
</style>


