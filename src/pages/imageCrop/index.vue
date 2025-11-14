<template>
  <view class="crop-container">
    <!-- 状态栏占位 -->
    <view class="status-bar" :style="{ height: statusBarHeight + 'px' }"></view>

    <!-- 裁剪组件 -->
    <view class="crop-area">
      <QfImageCropper
        ref="cropper"
        :src="imagePath"
        :width="600"
        :height="400"
        :show-border="true"
        :show-grid="true"
        :show-angle="true"
        :area-scale="0.3"
        :min-scale="1"
        :max-scale="3"
        :check-range="true"
        :choosable="false"
        :rotatable="true"
        :reverse-rotatable="true"
        @load="onImageLoad"
        @crop="onCropResult"
      ></QfImageCropper>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from "vue";
import QfImageCropper from "@/components/qf-image-cropper/qf-image-cropper.vue";
// 状态栏高度
const statusBarHeight = ref(0);

// 图片信息
const imagePath = ref("");
const cardType = ref("");

// 裁剪器引用
const cropper = ref(null);

// 获取页面参数
onMounted(() => {
  // 获取系统信息
  uni.getSystemInfo({
    success: (res) => {
      statusBarHeight.value = res.statusBarHeight || 20;

      // 根据屏幕宽度调整裁剪器尺寸
      const screenWidth = res.windowWidth;
      const padding = 40;
      const maxSize = Math.min(screenWidth - padding, 400);

      // 设置裁剪器尺寸
      cropperWidth.value = maxSize;
      cropperHeight.value = maxSize;
    },
  });

  // 获取页面参数
  const pages = getCurrentPages();
  const currentPage = pages[pages.length - 1];
  const options = currentPage.options;

  imagePath.value = decodeURIComponent(options.imagePath || "");
  cardType.value = options.cardType || "";
});

// 图片加载完成
const onImageLoad = (e) => {
  console.log("图片加载完成", e);
};

// 裁剪结果回调
const onCropResult = (e) => {
  console.log("裁剪结果", e);
  if (e && e.tempFilePath) {
    // 保存裁剪后的图片并返回
    setCardBackgroundAndReturn(e.tempFilePath);
  }
};

// 重置裁剪
const resetCrop = () => {
  if (cropper.value && cropper.value.reset) {
    cropper.value.reset();
  }
};

// 设置背景并返回
const setCardBackgroundAndReturn = (finalImagePath) => {
  // 返回上一页并设置背景
  const pages = getCurrentPages();
  const prevPage = pages[pages.length - 2];

  // 调用上一页的方法设置背景
  if (prevPage && prevPage.$vm && prevPage.$vm.setCardBackground) {
    prevPage.$vm.setCardBackground(finalImagePath, cardType.value);
  }

  uni.navigateBack();
};

// 返回上一页
const goBack = () => {
  uni.navigateBack();
};
</script>

<style lang="scss" scoped>
.status-bar {
  width: 100%;
  background: transparent;
}

.crop-container {
  min-height: 100vh;
  background: #000;
  color: #fff;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20rpx 32rpx;
  background: rgba(0, 0, 0, 0.8);
}

.header-btn {
  width: 80rpx;
  height: 60rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.icon {
  font-size: 36rpx;
  color: #fff;
}

.confirm-text {
  font-size: 32rpx;
  color: #07c160;
  font-weight: 600;
}

.header-title {
  font-size: 36rpx;
  font-weight: 600;
  color: #fff;
}

.crop-area {
  position: relative;
  margin: 20rpx;
  height: 800rpx;
  overflow: hidden;
}

.actions {
  position: fixed;
  bottom: 60rpx;
  left: 40rpx;
  right: 40rpx;
  display: flex;
  gap: 24rpx;
}

.action-btn {
  flex: 1;
  height: 88rpx;
  border-radius: 44rpx;
  font-size: 32rpx;
  font-weight: 600;
  border: none;

  &::after {
    border: none;
  }
}

.secondary {
  background: rgba(255, 255, 255, 0.1);
  color: #fff;
}

.primary {
  background: #07c160;
  color: #fff;
}
</style>
