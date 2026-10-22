<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { getCurrentUser } from '../../api/auth'
import { getToken, removeToken } from '../../auth/token'
import type { User } from '../../types/auth'
import BottomNav from '../../components/navigation/BottomNav.vue'

/** 当前已经登录的用户资料。 */
const user = ref<User | null>(null)

/** 每次进入“我的”页时，重新确认登录状态并刷新用户资料。 */
onShow(() => {
  void loadCurrentUser()
})

async function loadCurrentUser() {
  if (!getToken()) {
    uni.reLaunch({ url: '/pages/auth/login/index' })
    return
  }

  try {
    user.value = await getCurrentUser()
  } catch {
    removeToken()
    uni.showToast({ title: '登录状态已失效，请重新登录', icon: 'none' })
    setTimeout(() => {
      uni.reLaunch({ url: '/pages/auth/login/index' })
    }, 800)
  }
}

/** “我的文章”是二级页面，使用 navigateTo 保留当前个人页。 */
function goMyArticles() {
  uni.navigateTo({ url: '/pages/profile/articles/index' })
}

/** 打开二级账号设置页面。 */
function goSettings() {
  uni.navigateTo({ url: '/pages/profile/settings/index' })
}

/** 修改密码是常用安全操作，直接在“我的”页提供入口。 */
function goChangePassword() {
  uni.navigateTo({ url: '/pages/profile/settings/password/index' })
}

/** JWT 为无状态认证，删除本地 Token 后即退出当前账号。 */
function handleLogout() {
  removeToken()
  uni.showToast({ title: '已退出登录', icon: 'success' })
  setTimeout(() => {
    uni.reLaunch({ url: '/pages/auth/login/index' })
  }, 600)
}
</script>

<template>
  <view v-if="user" class="page">
    <view class="profile-card">
      <view class="avatar">
        <text>{{ user.nickname.slice(0, 1).toUpperCase() }}</text>
      </view>

      <view class="user-info">
        <text class="username">{{ user.nickname }}</text>
        <text class="account">@{{ user.username }}</text>
        <text class="email">{{ user.email }}</text>
      </view>
    </view>

    <view class="menu-card">
      <view class="menu-item clickable" @tap="goSettings">
        <text class="menu-label">账号设置</text>
        <view class="menu-action">
          <text class="menu-value">昵称、邮箱</text>
          <uni-icons type="right" size="18" color="#94a3b8" />
        </view>
      </view>

      <view class="menu-item clickable" @tap="goChangePassword">
        <text class="menu-label">修改密码</text>
        <view class="menu-action">
          <text class="menu-value">保护账号安全</text>
          <uni-icons type="right" size="18" color="#94a3b8" />
        </view>
      </view>

      <view class="menu-item">
        <text class="menu-label">账号角色</text>
        <text class="menu-value">{{ user.role === 'ADMIN' ? '管理员' : '普通用户' }}</text>
      </view>

      <view class="menu-item clickable" @tap="goMyArticles">
        <text class="menu-label">我的文章</text>
        <view class="menu-action">
          <text class="menu-value">已发布与草稿</text>
          <uni-icons type="right" size="18" color="#94a3b8" />
        </view>
      </view>
    </view>

    <button class="logout-button" @tap="handleLogout">退出登录</button>
  </view>

  <view v-else class="loading">正在读取用户信息…</view>

  <BottomNav v-if="user" active="profile" />
</template>

<style scoped lang="scss">
.page {
  min-height: 100vh;
  box-sizing: border-box;
  padding: 50rpx 32rpx 210rpx;
}

.profile-card {
  display: flex;
  align-items: center;
  gap: 24rpx;
  padding: 36rpx;
  background: #ffffff;
  border-radius: 24rpx;
}

.avatar {
  display: grid;
  width: 104rpx;
  height: 104rpx;
  place-items: center;
  color: #ffffff;
  font-size: 42rpx;
  font-weight: 800;
  background: #2563eb;
  border-radius: 50%;
}

.user-info {
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}

.username {
  color: #0f172a;
  font-size: 34rpx;
  font-weight: 800;
}

.email {
  color: #64748b;
  font-size: 25rpx;
}

.account {
  color: #94a3b8;
  font-size: 22rpx;
}

.menu-card {
  margin-top: 24rpx;
  overflow: hidden;
  background: #ffffff;
  border-radius: 24rpx;
}

.menu-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 102rpx;
  padding: 0 30rpx;
  border-bottom: 1rpx solid #e2e8f0;
}

.menu-item:last-child {
  border-bottom: 0;
}

.clickable {
  cursor: pointer;
}

.menu-label {
  color: #334155;
  font-size: 28rpx;
}

.menu-action {
  display: flex;
  align-items: center;
  gap: 8rpx;
}

.menu-value {
  color: #94a3b8;
  font-size: 24rpx;
}

.logout-button {
  margin-top: 44rpx;
  color: #dc2626;
  font-size: 28rpx;
  background: #fee2e2;
  border-radius: 16rpx;
}

.loading {
  display: grid;
  min-height: 100vh;
  place-items: center;
  color: #64748b;
  font-size: 28rpx;
}
</style>
