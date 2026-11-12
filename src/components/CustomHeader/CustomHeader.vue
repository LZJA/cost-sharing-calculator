<template>
  <view>
    <!-- 状态栏占位 -->
    <view class="status-bar" :style="{ height: statusBarHeight + 'px' }"></view>
    <view
      class="header"
      :class="{ 'header-glass': showGlass, 'header-mb': !showGlass }"
      :style="{ minHeight: navHeight + 'px', paddingRight: rightInset + 'px' }"
    >
      <view class="header-left" @click="goBack">
        <text class="back-icon">←</text>
      </view>
      <text class="center-title">{{ title }}</text>
      <view v-if="$slots['header-right']" class="header-right">
        <slot name="header-right"></slot>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { navigationLayout } from "@/utils/navigation.js";

const props = defineProps({
  title: {
    type: String,
    required: true,
  },
  showGlass: {
    type: Boolean,
    default: false,
  },
});

// 状态栏高度
const statusBarHeight = ref(0);
const navHeight = ref(44);
const rightInset = ref(104);

const goBack = () => {
  uni.navigateBack({ fail: () => uni.reLaunch({ url: "/pages/home/home" }) });
};

onMounted(() => {
  const applyLayout = (info) => {
    let capsule;
    const hasSystemMenu = typeof wx !== "undefined";
    try {
      if (typeof uni.getMenuButtonBoundingClientRect === "function") capsule = uni.getMenuButtonBoundingClientRect();
      else if (hasSystemMenu && typeof wx.getMenuButtonBoundingClientRect === "function") capsule = wx.getMenuButtonBoundingClientRect();
    } catch (error) { /* 获取失败时保留系统按钮的预留宽度。 */ }
    const layout = navigationLayout(info, capsule, hasSystemMenu);
    statusBarHeight.value = layout.statusBarHeight;
    navHeight.value = layout.navHeight;
    rightInset.value = layout.rightInset;
  };
  if (typeof uni.getWindowInfo === "function") {
    try { applyLayout(uni.getWindowInfo()); return; } catch (error) { /* 回退到旧基础库 API。 */ }
  }
  uni.getSystemInfo({ success: applyLayout, fail: () => applyLayout({ statusBarHeight: 20, windowWidth: 375 }) });
});
</script>

<style lang="scss" scoped>
.status-bar {
  width: 100%;
  background: transparent;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  box-sizing: border-box;
}

.header-mb {
  margin-bottom: 60rpx;
}

.header-glass {
  padding: 12rpx 16rpx;
  background: rgba(255, 255, 255, 0.3);
  backdrop-filter: blur(10px);
}

/* Home Mode Styles */
.title {
  font-size: 36rpx;
  font-weight: 700;
  color: #5a7c9a;
  text-shadow: 2rpx 2rpx 4rpx rgba(0, 0, 0, 0.05);
  letter-spacing: 2rpx;
}

.header-action {
  padding: 16rpx 32rpx;
  background: linear-gradient(135deg, #ff9ab8 0%, #ffb3d9 100%);
  border-radius: 44rpx;
  box-shadow: 0 4rpx 16rpx rgba(255, 154, 184, 0.3);
}

.action-text {
  font-size: 28rpx;
  color: #fff;
  font-weight: 600;
}

/* Subpage Mode Styles */
.header-left {
  flex: 0 0 80rpx;
  width: 80rpx;
  display: flex;
  align-items: center;
}

.back-icon {
  font-size: 40rpx;
  color: #5a7c9a;
}

.center-title {
  min-width: 0;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  font-size: 36rpx;
  font-weight: 700;
  color: #5a7c9a;
  text-align: center;
  flex: 1;
}

.header-right {
  flex: 0 0 80rpx;
  min-width: 0;
  display: flex;
  justify-content: flex-end;
}
</style>
