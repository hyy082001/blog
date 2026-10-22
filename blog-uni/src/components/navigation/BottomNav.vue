<template>
  <view class="nav-wrapper">
    <view class="nav-dock">
      <view class="nav-grid">
        <view
          v-for="item in leftTabs"
          :key="item.key"
          :class="['nav-item', { active: props.active === item.key }]"
          @tap="goPage(item.key, item.path)"
        >
          <view :class="['icon-background', { selected: props.active === item.key }]">
            <uni-icons
              :type="props.active === item.key ? item.activeIcon : item.icon"
              :color="props.active === item.key ? '#2563EB' : '#64748B'"
              :size="25"
            />
          </view>
          <text>{{ item.label }}</text>
        </view>

        <!-- 为中间悬浮的写作按钮预留一个网格位置。 -->
        <view class="compose-placeholder" />

        <view
          v-for="item in rightTabs"
          :key="item.key"
          :class="['nav-item', { active: props.active === item.key }]"
          @tap="goPage(item.key, item.path)"
        >
          <view :class="['icon-background', { selected: props.active === item.key }]">
            <uni-icons
              :type="props.active === item.key ? item.activeIcon : item.icon"
              :color="props.active === item.key ? '#2563EB' : '#64748B'"
              :size="25"
            />
          </view>
          <text>{{ item.label }}</text>
        </view>
      </view>

      <!-- 居中的写作入口不属于主导航项，因此独立于四个导航按钮。 -->
      <view class="compose-button" @tap="handleCompose">
        <uni-icons type="compose" color="#FFFFFF" :size="30" />
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
type MainPage = 'home' | 'article' | 'ai' | 'profile'

const props = defineProps<{
  /** 当前主页面，用于高亮对应导航项。 */
  active: MainPage
}>()

const leftTabs = [
  {
    key: 'home' as const,
    label: '首页',
    path: '/pages/home/index',
    icon: 'home',
    activeIcon: 'home-filled',
  },
  {
    key: 'article' as const,
    label: '文章',
    path: '/pages/article/index',
    icon: 'list',
    activeIcon: 'list',
  },
]

const rightTabs = [
  {
    key: 'ai' as const,
    label: 'AI',
    path: '/pages/ai/index',
    icon: 'chatbubble',
    activeIcon: 'chatbubble-filled',
  },
  {
    key: 'profile' as const,
    label: '我的',
    path: '/pages/profile/index',
    icon: 'person',
    activeIcon: 'person-filled',
  },
]

/**
 * 现在使用的是自定义导航，而非 pages.json 的原生 tabBar。
 * 因此使用 reLaunch 切换主页面，并清理旧页面栈。
 */
function goPage(target: MainPage, path: string) {
  if (target === props.active) {
    return
  }

  uni.reLaunch({ url: path })
}

/** 打开二级写作页，保留当前主页面供用户发布后返回。 */
function handleCompose() {
  uni.navigateTo({ url: '/pages/article/create/index' })
}
</script>

<style scoped>
.nav-wrapper {
  position: fixed;
  right: 24rpx;
  bottom: calc(20rpx + env(safe-area-inset-bottom));
  left: 24rpx;
  z-index: 1000;
}

.nav-dock {
  position: relative;
  height: 124rpx;
  background: #ffffff;
  border-radius: 32rpx;
  box-shadow: 0 12rpx 32rpx rgb(37 99 235 / 16%);
}

.nav-grid {
  display: grid;
  height: 100%;
  grid-template-columns: repeat(5, 1fr);
  align-items: center;
}

.nav-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4rpx;
  color: #64748b;
  font-size: 21rpx;
}

.nav-item.active {
  color: #2563eb;
  font-weight: 700;
}

.icon-background {
  display: grid;
  width: 54rpx;
  height: 50rpx;
  place-items: center;
  border-radius: 25rpx;
}

.icon-background.selected {
  background: #e0edff;
}

.compose-placeholder {
  width: 100%;
  height: 100%;
}

.compose-button {
  position: absolute;
  top: -40rpx;
  left: 50%;
  display: grid;
  width: 108rpx;
  height: 108rpx;
  place-items: center;
  background: #2563eb;
  border: 6rpx solid #ffffff;
  border-radius: 50%;
  box-shadow: 0 12rpx 24rpx rgb(37 99 235 / 32%);
  transform: translateX(-50%);
}
</style>
