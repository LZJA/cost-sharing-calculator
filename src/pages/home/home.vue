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
      <view
        class="card"
        :class="{ 'has-bg': liziCard.background && liziCard.enableBackground }"
        @click="navigateToLizi"
      >
        <image
          v-if="liziCard.background && liziCard.enableBackground"
          mode="aspectFill"
          :src="liziCard.background"
          class="card-bg-image"
        />
        <view
          v-if="liziCard.background && liziCard.enableBackground"
          class="card-overlay"
        ></view>
        <view class="card-header">
          <view class="card-avatars">
            <view class="avatar">{{ liziCard.avatar }}</view>
          </view>
        </view>
        <view class="card-content">
          <text class="card-title">{{ liziCard.name }}</text>
          <text class="card-balance">{{ liziCard.description }}</text>
        </view>
        <view class="card-footer">
          <view class="card-action-icon" @tap.stop="editLiziCard">
            <text>✏️</text>
          </view>
          <view class="card-action-icon" @tap.stop="editLiziBackground">
            <text>🎨</text>
          </view>
        </view>
      </view>

      <!-- 鸽子的分账计算器 -->
      <view
        class="card"
        :class="{ 'has-bg': geziCard.background && geziCard.enableBackground }"
        @click="navigateToGezi"
      >
        <image
          v-if="geziCard.background && geziCard.enableBackground"
          mode="aspectFill"
          :src="geziCard.background"
          class="card-bg-image"
        />
        <view
          v-if="geziCard.background && geziCard.enableBackground"
          class="card-overlay"
        ></view>
        <view class="card-header">
          <view class="card-avatars">
            <view class="avatar">{{ geziCard.avatar }}</view>
          </view>
        </view>
        <view class="card-content">
          <text class="card-title">{{ geziCard.name }}</text>
          <text class="card-balance">{{ geziCard.description }}</text>
        </view>
        <view class="card-footer">
          <view class="card-action-icon" @tap.stop="editGeziCard">
            <text>✏️</text>
          </view>
          <view class="card-action-icon" @tap.stop="editGeziBackground">
            <text>🎨</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 底部提示 -->
    <view class="footer-tip">
      <text class="tip-text">💡 选择计算器开始分账</text>
    </view>

    <!-- 编辑卡片弹窗 -->
    <view v-if="showEditModal" class="modal-overlay" @tap="closeEditModal">
      <view class="modal-content" @tap.stop>
        <view class="modal-header">
          <text class="modal-title">编辑卡片信息</text>
          <view class="modal-close" @tap="closeEditModal">
            <text>✕</text>
          </view>
        </view>
        <view class="modal-body">
          <view class="form-group">
            <text class="form-label">卡片名称</text>
            <input
              v-model="editingCard.name"
              placeholder="请输入卡片名称"
              class="form-input"
            />
          </view>
          <view class="form-group">
            <text class="form-label">描述语</text>
            <input
              v-model="editingCard.description"
              placeholder="请输入描述语"
              class="form-input"
            />
          </view>
          <view class="form-group">
            <text class="form-label">头像 (emoji)</text>
            <input
              v-model="editingCard.avatar"
              placeholder="请输入emoji"
              class="form-input"
            />
          </view>
          <view class="form-group">
            <text class="form-label">启用背景图</text>
            <view class="switch-container">
              <view class="custom-switch" @tap="toggleBackground">
                <view
                  class="switch-track"
                  :class="{ active: editingCard.enableBackground }"
                >
                  <view
                    class="switch-thumb"
                    :class="{ active: editingCard.enableBackground }"
                  ></view>
                </view>
              </view>
              <text
                class="switch-label"
                :class="{ active: editingCard.enableBackground }"
              >
                {{ editingCard.enableBackground ? "已启用" : "已禁用" }}
              </text>
            </view>
            <view
              v-if="editingCard.enableBackground && !editingCard.background"
              class="background-tip"
            >
              <text class="tip-text">💡 启用后需要设置背景图片</text>
            </view>
            <view
              v-if="editingCard.enableBackground && editingCard.background"
              class="background-preview"
            >
              <image
                :src="editingCard.background"
                mode="aspectFill"
                class="preview-image"
              />
              <text class="preview-text">当前背景图片</text>
            </view>
          </view>
        </view>
        <view class="modal-footer">
          <button class="btn btn-secondary" @tap="closeEditModal">取消</button>
          <button class="btn btn-primary" @tap="saveCardEdit">保存</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted, getCurrentInstance } from "vue";

// 状态栏高度
const statusBarHeight = ref(0);

// 卡片数据
const liziCard = ref({
  name: "李子的分账计算器",
  description: "分账让生活更简单",
  avatar: "🍐",
  background: "",
  enableBackground: true,
});

const geziCard = ref({
  name: "鸽子的分账计算器",
  description: "记录每一份美好小账单",
  avatar: "🕊️",
  background: "",
  enableBackground: true,
});

// 弹窗相关状态
const showEditModal = ref(false);
const editingCard = ref({});
const editingCardType = ref("");

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

// 编辑李子卡片
const editLiziCard = () => {
  editingCard.value = { ...liziCard.value };
  editingCardType.value = "lizi";
  showEditModal.value = true;
};

// 编辑鸽子卡片
const editGeziCard = () => {
  editingCard.value = { ...geziCard.value };
  editingCardType.value = "gezi";
  showEditModal.value = true;
};

// 关闭编辑弹窗
const closeEditModal = () => {
  showEditModal.value = false;
  editingCard.value = {};
  editingCardType.value = "";
};

// 背景图开关切换
const toggleBackground = () => {
  editingCard.value.enableBackground = !editingCard.value.enableBackground;
};

// 保存卡片编辑
const saveCardEdit = () => {
  if (!editingCard.value.name || !editingCard.value.description) {
    uni.showToast({
      title: "请填写完整信息",
      icon: "none",
    });
    return;
  }

  if (editingCardType.value === "lizi") {
    liziCard.value = { ...editingCard.value };
    // 保存到本地存储
    uni.setStorageSync("lizi_card", liziCard.value);
  } else if (editingCardType.value === "gezi") {
    geziCard.value = { ...editingCard.value };
    // 保存到本地存储
    uni.setStorageSync("gezi_card", geziCard.value);
  }

  uni.showToast({
    title: "保存成功",
    icon: "success",
  });

  closeEditModal();
};

// 编辑李子卡片背景
const editLiziBackground = () => {
  uni.chooseImage({
    count: 1,
    sourceType: ["album"],
    success: (res) => {
      const tempFilePath = res.tempFilePaths[0];
      // 先保存图片到本地
      saveImageToLocal(tempFilePath, "lizi");
    },
    fail: (err) => {
      console.error("选择图片失败", err);
      uni.showToast({
        title: "选择图片失败",
        icon: "none",
      });
    },
  });
};

// 编辑鸽子卡片背景
const editGeziBackground = () => {
  uni.chooseImage({
    count: 1,
    sourceType: ["album"],
    success: (res) => {
      const tempFilePath = res.tempFilePaths[0];
      // 先保存图片到本地
      saveImageToLocal(tempFilePath, "gezi");
    },
    fail: (err) => {
      console.error("选择图片失败", err);
      uni.showToast({
        title: "选择图片失败",
        icon: "none",
      });
    },
  });
};

// 保存图片到本地
const saveImageToLocal = (tempFilePath, cardType) => {
  uni.showLoading({
    title: "保存图片中...",
  });

  // 保存临时文件到本地永久目录
  uni.saveFile({
    tempFilePath: tempFilePath,
    success: (saveRes) => {
      uni.hideLoading();
      const savedFilePath = saveRes.savedFilePath;

      console.log("savedFilePath", savedFilePath);

      // 询问是否需要裁剪
      uni.showModal({
        title: "图片处理",
        content: "是否要裁剪图片？",
        success: (modalRes) => {
          if (modalRes.confirm) {
            // 图片裁剪
            cropImage(savedFilePath, cardType);
          } else {
            // 直接使用
            setCardBackground(savedFilePath, cardType);
          }
        },
      });
    },
    fail: (err) => {
      uni.hideLoading();
      console.error("保存图片失败", err);
      uni.showToast({
        title: "保存图片失败，请重试",
        icon: "none",
      });
    },
  });
};

// 图片裁剪
const cropImage = (imagePath, cardType) => {
  // 微信小程序图片裁剪实现
  uni.navigateTo({
    url: `/pages/imageCrop/index?imagePath=${encodeURIComponent(
      imagePath
    )}&cardType=${cardType}`,
    fail: () => {
      // 如果没有裁剪页面，使用简单的预览方式
      uni.previewImage({
        urls: [imagePath],
        success: () => {
          // 预览后直接使用原图
          setCardBackground(imagePath, cardType);
        },
      });
    },
  });
};

// 设置卡片背景
const setCardBackground = (imagePath, cardType) => {
  // 更新卡片对象中的背景图片
  if (cardType === "lizi") {
    liziCard.value.background = imagePath;
    // 保存整个卡片对象到本地存储
    uni.setStorageSync("lizi_card", liziCard.value);
  } else if (cardType === "gezi") {
    geziCard.value.background = imagePath;
    // 保存整个卡片对象到本地存储
    uni.setStorageSync("gezi_card", geziCard.value);
  }

  uni.showToast({
    title: "背景设置成功",
    icon: "success",
  });
};

// 加载卡片数据
const loadCardData = () => {
  try {
    // 加载李子卡片数据
    const liziData = uni.getStorageSync("lizi_card");
    if (liziData) {
      liziCard.value = {
        ...liziCard.value,
        ...liziData,
        // 确保enableBackground字段有默认值
        enableBackground:
          liziData.enableBackground !== undefined
            ? liziData.enableBackground
            : false,
      };
    }

    // 加载鸽子卡片数据
    const geziData = uni.getStorageSync("gezi_card");
    if (geziData) {
      geziCard.value = {
        ...geziCard.value,
        ...geziData,
        // 确保enableBackground字段有默认值
        enableBackground:
          geziData.enableBackground !== undefined
            ? geziData.enableBackground
            : false,
      };
    }

    // 兼容旧版本数据
    const oldLiziBackground = uni.getStorageSync("lizi_background");
    if (oldLiziBackground && !liziCard.value.background) {
      liziCard.value.background = oldLiziBackground;
      // 如果有旧背景图，默认启用
      liziCard.value.enableBackground = true;
    }

    const oldGeziBackground = uni.getStorageSync("gezi_background");
    if (oldGeziBackground && !geziCard.value.background) {
      geziCard.value.background = oldGeziBackground;
      // 如果有旧背景图，默认启用
      geziCard.value.enableBackground = true;
    }
  } catch (e) {
    console.error("加载卡片数据失败", e);
  }
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

  // 加载卡片数据
  loadCardData();
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

// 暴露方法供其他页面调用
defineExpose({
  setCardBackground,
  onShareAppMessage,
  onShareTimeline,
});
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

.edit-bg-btn {
  width: 48rpx;
  height: 48rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.8);
  border-radius: 50%;
  box-shadow: 0 4rpx 12rpx rgba(90, 124, 154, 0.15);
  transition: all 0.2s ease;
  backdrop-filter: blur(10px);
  border: 1rpx solid rgba(255, 255, 255, 0.3);
}

.edit-bg-btn:hover {
  background: rgba(255, 255, 255, 0.9);
  transform: scale(1.1);
  box-shadow: 0 6rpx 18rpx rgba(90, 124, 154, 0.2);
}

.edit-bg-btn:active {
  transform: scale(0.95);
}

.edit-bg-btn .icon {
  font-size: 24rpx;
}

.card-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(
    135deg,
    rgba(0, 0, 0, 0.2) 0%,
    rgba(0, 0, 0, 0.1) 50%,
    rgba(0, 0, 0, 0.3) 100%
  );
  border-radius: 32rpx;
  z-index: 1;
}

.card-bg-image {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  border-radius: 32rpx;
  z-index: 0;
}

.card {
  background: linear-gradient(
    145deg,
    rgba(255, 255, 255, 0.3) 0%,
    rgba(250, 251, 252, 0.3) 50%,
    rgba(245, 247, 250, 0.3) 100%
  );
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

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 32rpx;
  position: relative;
  z-index: 2;
}

/* 弹窗样式 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40rpx 20rpx;
}

/* 小屏幕适配 */
@media screen and (max-height: 700px) {
  .modal-overlay {
    padding: 20rpx 20rpx;
    align-items: flex-start;
    padding-top: 60rpx;
  }

  .modal-content {
    max-height: 85vh;
  }

  .modal-body {
    max-height: calc(85vh - 180rpx);
  }
}

.modal-content {
  background: #fff;
  border-radius: 24rpx;
  width: 100%;
  max-width: 600rpx;
  max-height: 80vh;
  overflow: hidden;
  box-shadow: 0 20rpx 60rpx rgba(0, 0, 0, 0.2);
  display: flex;
  flex-direction: column;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 40rpx;
  border-bottom: 1rpx solid #e8f0f5;
  flex-shrink: 0;
}

.modal-title {
  font-size: 36rpx;
  font-weight: 600;
  color: #5a7c9a;
}

.modal-close {
  width: 60rpx;
  height: 60rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: #f5f5f5;
  color: #666;
  font-size: 32rpx;
}

.modal-body {
  padding: 40rpx;
  flex: 1;
  overflow-y: auto;
  max-height: calc(80vh - 200rpx);
  -webkit-overflow-scrolling: touch;
}

.modal-body::-webkit-scrollbar {
  width: 6rpx;
}

.modal-body::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 10rpx;
}

.modal-body::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 10rpx;
}

.modal-body::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}

.form-group {
  margin-bottom: 32rpx;
}

.form-group:last-child {
  margin-bottom: 0;
}

.form-label {
  display: block;
  margin-bottom: 16rpx;
  color: #5a7c9a;
  font-weight: 600;
  font-size: 30rpx;
}

.form-input {
  width: 100%;
  padding: 0 32rpx;
  height: 104rpx;
  border: 2rpx solid #e8f0f5;
  border-radius: 16rpx;
  font-size: 32rpx;
  background: #fafbfc;
  color: #5a7c9a;
  box-sizing: border-box;
}

.form-input:focus {
  border-color: #ffb3d9;
  background: #fff;
}

.switch-container {
  display: flex;
  align-items: center;
  gap: 24rpx;
}

.custom-switch {
  cursor: pointer;
  display: flex;
  align-items: center;
}

.switch-track {
  width: 100rpx;
  height: 60rpx;
  background: linear-gradient(135deg, #e2e8f0 0%, #f1f5f9 100%);
  border-radius: 30rpx;
  position: relative;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border: 2rpx solid #e2e8f0;
  box-shadow: inset 0 2rpx 4rpx rgba(0, 0, 0, 0.1);
}

.switch-track.active {
  background: linear-gradient(135deg, #ffb3d9 0%, #ff9ab8 100%);
  border-color: #ff9ab8;
  box-shadow: 0 4rpx 12rpx rgba(255, 154, 184, 0.3),
    inset 0 1rpx 0 rgba(255, 255, 255, 0.3);
}

.switch-thumb {
  width: 48rpx;
  height: 48rpx;
  background: linear-gradient(135deg, #ffffff 0%, #f8fafc 100%);
  border-radius: 50%;
  position: absolute;
  top: 4rpx;
  left: 4rpx;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 4rpx 8rpx rgba(0, 0, 0, 0.15), 0 2rpx 4rpx rgba(0, 0, 0, 0.1);
  border: 2rpx solid rgba(255, 255, 255, 0.8);
  transform: scale(1);
}

.switch-thumb.active {
  left: 44rpx;
  background: linear-gradient(135deg, #ffffff 0%, #fef7f7 100%);
  transform: scale(1.05);
  box-shadow: 0 6rpx 12rpx rgba(255, 154, 184, 0.2),
    0 2rpx 6rpx rgba(0, 0, 0, 0.1);
}

.switch-label {
  font-size: 32rpx;
  color: #64748b;
  font-weight: 500;
  transition: all 0.3s ease;
  user-select: none;
}

.switch-label.active {
  color: #ff6b9d;
  font-weight: 600;
}

.background-tip {
  margin-top: 20rpx;
  padding: 20rpx 24rpx;
  background: linear-gradient(
    135deg,
    rgba(255, 193, 7, 0.1) 0%,
    rgba(255, 235, 59, 0.05) 100%
  );
  border-radius: 16rpx;
  border-left: 6rpx solid #ffc107;
  border: 1rpx solid rgba(255, 193, 7, 0.2);
  box-shadow: 0 2rpx 8rpx rgba(255, 193, 7, 0.1);
}

.tip-text {
  font-size: 28rpx;
  color: #b45309;
  font-weight: 500;
  line-height: 1.4;
}

.background-preview {
  margin-top: 20rpx;
  display: flex;
  flex-direction: column;
  gap: 16rpx;
  padding: 20rpx;
  background: linear-gradient(
    135deg,
    rgba(255, 179, 217, 0.05) 0%,
    rgba(255, 154, 184, 0.03) 100%
  );
  border-radius: 16rpx;
  border: 1rpx solid rgba(255, 154, 184, 0.2);
}

.preview-image {
  width: 100%;
  height: 200rpx;
  border-radius: 12rpx;
  background: #f5f5f5;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.1);
  border: 2rpx solid rgba(255, 255, 255, 0.8);
}

.preview-text {
  font-size: 28rpx;
  color: #ff6b9d;
  text-align: center;
  font-weight: 500;
}

.modal-footer {
  padding: 40rpx;
  border-top: 1rpx solid #e8f0f5;
  display: flex;
  gap: 24rpx;
  flex-shrink: 0;
}

.btn {
  flex: 1;
  height: 88rpx;
  border-radius: 44rpx;
  font-size: 32rpx;
  font-weight: 600;
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;

  &::after {
    border: none;
  }
}

.btn-primary {
  background: linear-gradient(135deg, #ff9a9e 0%, #fecfef 50%, #fecfef 100%);
  color: #fff;
  box-shadow: 0 8rpx 30rpx rgba(255, 154, 158, 0.3);
}

.btn-secondary {
  background: linear-gradient(135deg, #a8edea 0%, #fed6e3 100%);
  color: #fff;
  box-shadow: 0 8rpx 30rpx rgba(168, 237, 234, 0.3);
}

.card-avatars {
  display: flex;
  gap: 16rpx;
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
  content: "";
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: linear-gradient(
    45deg,
    transparent 30%,
    rgba(255, 255, 255, 0.3) 50%,
    transparent 70%
  );
  transform: rotate(45deg);
  animation: shimmer 3s ease-in-out infinite;
}

@keyframes shimmer {
  0%,
  100% {
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
  position: relative;
  z-index: 2;
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
}

.card-footer {
  display: flex;
  justify-content: flex-end;
  gap: 24rpx;
  padding-top: 24rpx;
  border-top: 1rpx solid rgba(232, 240, 245, 0.6);
  position: relative;
  z-index: 2;
}

.card-footer::before {
  content: "";
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 1rpx;
  background: linear-gradient(
    90deg,
    transparent 0%,
    rgba(255, 255, 255, 0.8) 50%,
    transparent 100%
  );
}

.card-action-icon {
  width: 60rpx;
  height: 60rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32rpx;
  transition: all 0.2s ease;
  position: relative;
}

.card-action-icon:hover {
  transform: scale(1.1);
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
.card.has-bg {
  border: none;
}

/* 有背景图时的样式 */
.card.has-bg .card-title {
  color: #ffffff;
}

.card.has-bg .card-balance {
  color: #ffffff;
}

.card.has-bg .card-footer {
  border-top: 1rpx solid rgba(255, 255, 255, 0.3);
}

.card.has-bg .card-footer::before {
  background: linear-gradient(
    90deg,
    transparent 0%,
    rgba(255, 255, 255, 0.4) 50%,
    transparent 100%
  );
}
</style>
