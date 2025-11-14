<template>
  <view class="container">
    <!-- 状态栏占位 -->
    <view class="status-bar" :style="{ height: statusBarHeight + 'px' }"></view>

    <!-- 头部 -->
    <view class="header">
      <text class="header-title">分账计算器</text>
      <!-- <view class="header-actions">
        <view class="icon-btn" @click="handleSettings">
          <text class="icon">⚙️</text>
        </view>
      </view> -->
    </view>

    <!-- 卡片列表 -->
    <view class="cards-container">
      <!-- 李子的分账计算器 -->
      <view class="card" @click="navigateToLizi">
        <view class="card-header">
          <view class="card-avatars">
            <view class="avatar">🍐</view>
          </view>
        </view>
        <view class="card-content">
          <text class="card-title">李子的分账计算器</text>
          <text class="card-balance">分账让生活更简单</text>
        </view>
        <view class="card-footer">
          <view class="card-action-icon">📷</view>
          <view class="card-action-icon">📋</view>
        </view>
      </view>

      <!-- 鸽子的分账计算器 -->
      <view class="card" @click="navigateToGezi">
        <view class="card-header">
          <view class="card-avatars">
            <view class="avatar">🕊️</view>
          </view>
          
        </view>
        <view class="card-content">
          <text class="card-title">鸽子的分账计算器</text>
          <text class="card-balance">记录每一份美好小账单</text>
        </view>
        <view class="card-footer">
          <view class="card-action-icon">📷</view>
          <view class="card-action-icon">📋</view>
        </view>
      </view>
    </view>

    <!-- 底部提示 -->
    <view class="footer-tip">
      <text class="tip-text">💡 选择计算器开始分账</text>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted, getCurrentInstance } from "vue";

// 状态栏高度
const statusBarHeight = ref(0);

// 导航到李子的分账计算器
const navigateToLizi = () => {
  uni.navigateTo({
    url: "/pages/lizi/index",
    fail: (err) => {
      console.error("导航失败", err);
      uni.showToast({
        title: "页面跳转失败",
        icon: "none",
      });
    },
  });
};

// 导航到鸽子的分账计算器
const navigateToGezi = () => {
  uni.navigateTo({
    url: "/pages/gezi/index",
    fail: (err) => {
      console.error("导航失败", err);
      uni.showToast({
        title: "页面跳转失败",
        icon: "none",
      });
    },
  });
};

// 设置按钮
const handleSettings = () => {
  uni.showToast({
    title: "设置功能开发中",
    icon: "none",
  });
};

// 页面加载时初始化
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

// 页面分享配置
const onShareAppMessage = () => {
  return {
    title: "分账计算器",
    desc: "轻松计算水电燃气费用分摊，让合租生活更简单！",
    path: "/pages/home/home",
    imageUrl: "", // 可以设置自定义分享图片
    success: function (res) {
      console.log("分享成功", res);
    },
    fail: function (res) {
      console.log("分享失败", res);
    },
  };
};

// 分享到朋友圈
const onShareTimeline = () => {
  return {
    title: "分账计算器 - 让合租生活更简单",
    query: "",
    imageUrl: "", // 可以设置自定义分享图片
    success: function (res) {
      console.log("分享朋友圈成功", res);
    },
    fail: function (res) {
      console.log("分享朋友圈失败", res);
    },
  };
};

// 获取当前实例并注册分享方法
const instance = getCurrentInstance();
if (instance) {
  instance.ctx.onShareAppMessage = onShareAppMessage;
  instance.ctx.onShareTimeline = onShareTimeline;
}
</script>

<style lang="scss" scoped>
.status-bar {
  width: 100%;
  background: transparent;
}

.container {
  min-height: 100vh;
  padding: 0 24rpx 32rpx;
  background: linear-gradient(135deg, #a8edea 0%, #fed6e3 50%, #ffecd2 100%);
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 32rpx 0 48rpx;
}

.header-title {
  font-size: 40rpx;
  font-weight: 700;
  color: #5a7c9a;
  text-shadow: 2rpx 2rpx 4rpx rgba(0, 0, 0, 0.05);
}

.header-actions {
  display: flex;
  gap: 24rpx;
  align-items: center;
}

.icon-btn {
  width: 64rpx;
  height: 64rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.3);
  backdrop-filter: blur(10px);
}

.icon {
  font-size: 36rpx;
}

.cards-container {
  display: flex;
  flex-direction: column;
  gap: 32rpx;
  margin-bottom: 48rpx;
}

.card {
  background: linear-gradient(145deg, #ffffff 0%, #fafbfc 50%, #f5f7fa 100%);
  border-radius: 32rpx;
  padding: 40rpx;
  box-shadow: 0 12rpx 40rpx rgba(90, 124, 154, 0.08),
              0 4rpx 16rpx rgba(90, 124, 154, 0.06),
              inset 0 1rpx 0 rgba(255, 255, 255, 0.9);
  border: 1rpx solid rgba(255, 255, 255, 0.2);
  position: relative;
  overflow: hidden;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.card:hover {
  transform: translateY(-4rpx) scale(1.02);
  box-shadow: 0 20rpx 60rpx rgba(90, 124, 154, 0.15),
              0 8rpx 24rpx rgba(90, 124, 154, 0.1),
              inset 0 1rpx 0 rgba(255, 255, 255, 0.95);
}

.card:active {
  transform: translateY(-2rpx) scale(1.01);
  transition-duration: 0.1s;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 32rpx;
}

.card-avatars {
  display: flex;
  gap: 16rpx;
}

.avatar {
  width: 120rpx;
  height: 120rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, #ff9a9e 0%, #fad0c4 50%, #fecfef 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 64rpx;
  box-shadow: 0 8rpx 24rpx rgba(255, 154, 158, 0.25),
              0 4rpx 12rpx rgba(255, 154, 158, 0.15);
  border: 3rpx solid rgba(255, 255, 255, 0.8);
  position: relative;
  overflow: hidden;
}

.avatar::before {
  content: '';
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: linear-gradient(45deg, transparent 30%, rgba(255, 255, 255, 0.3) 50%, transparent 70%);
  transform: rotate(45deg);
  animation: shimmer 3s ease-in-out infinite;
}

@keyframes shimmer {
  0%, 100% {
    transform: translateX(-100%) translateY(-100%) rotate(45deg);
    opacity: 0;
  }
  50% {
    transform: translateX(100%) translateY(100%) rotate(45deg);
    opacity: 1;
  }
}


.card-content {
  margin-bottom: 32rpx;
}

.card-title {
  display: block;
  font-size: 36rpx;
  font-weight: 600;
  color: #2c5472;
  margin-bottom: 16rpx;
  text-shadow: 0 1rpx 2rpx rgba(44, 84, 114, 0.1);
}

.card-balance {
  display: block;
  font-size: 28rpx;
  font-weight: 500;
  color: #6b8aa6;
  background: linear-gradient(135deg, #6b8aa6 0%, #8aa8c3 100%);
  background-clip: text;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  letter-spacing: 0.5rpx;
}

.card-footer {
  display: flex;
  justify-content: flex-end;
  gap: 24rpx;
  padding-top: 24rpx;
  border-top: 1rpx solid rgba(232, 240, 245, 0.6);
  position: relative;
}

.card-footer::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 1rpx;
  background: linear-gradient(90deg, transparent 0%, rgba(255, 255, 255, 0.8) 50%, transparent 100%);
}

.card-action-icon {
  width: 48rpx;
  height: 48rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32rpx;
  opacity: 0.6;
  border-radius: 50%;
  transition: all 0.2s ease;
  position: relative;
}

.card-action-icon:hover {
  opacity: 1;
  background: rgba(255, 255, 255, 0.5);
  transform: scale(1.1);
  box-shadow: 0 4rpx 12rpx rgba(90, 124, 154, 0.1);
}

.card-action-icon:active {
  transform: scale(0.95);
}

.footer-tip {
  text-align: center;
  padding: 32rpx 0;
}

.tip-text {
  font-size: 28rpx;
  color: #7c95aa;
  font-weight: 500;
}
</style>

