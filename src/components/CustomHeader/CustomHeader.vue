<template>
  <view>
    <!-- 状态栏占位 -->
    <view class="status-bar" :style="{ height: statusBarHeight + 'px' }"></view>
    <view
      class="header"
      :class="{ 'header-glass': showGlass, 'header-mb': !showGlass }"
    >
      <view class="header-left" @click="goBack">
        <text class="back-icon">←</text>
      </view>
      <text class="center-title">{{ title }}</text>
      <view class="header-right">
        <slot name="header-right"></slot>
      </view>
    </view>
  </view>
</template>

<script setup>
import { defineProps, ref, onMounted } from "vue";

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

const goBack = () => {
  uni.navigateBack();
};

onMounted(() => {
  // 获取系统信息，设置状态栏高度
  uni.getSystemInfo({
    success: (res) => {
      statusBarHeight.value = res.statusBarHeight || 20;
    },
    fail: () => {
      statusBarHeight.value = 20; // 默认值
    },
  });
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
  padding: 32rpx 24rpx;
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
  width: 80rpx;
  display: flex;
  align-items: center;
}

.back-icon {
  font-size: 40rpx;
  color: #5a7c9a;
}

.center-title {
  font-size: 36rpx;
  font-weight: 700;
  color: #5a7c9a;
  text-align: center;
  flex: 1;
}
</style>
