<template>
  <view class="share-poster-container" v-if="visible">
    <view class="mask" @click="close"></view>
    <view class="content">
      <view class="canvas-content">
        <canvas
          canvas-id="shareCanvas"
          id="shareCanvas"
          class="canvas"
          :style="{ width: canvasWidth + 'px', height: canvasHeight + 'px' }"
        ></canvas>
      </view>
      <view class="btn-group">
        <button class="btn save-btn" @click="saveImage">保存到相册</button>
        <button class="btn share-btn" @click="shareToWeChat">分享好友</button>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted, getCurrentInstance } from "vue";

const props = defineProps({
  data: {
    type: Object,
    required: true,
  },
});

const emit = defineEmits(["close"]);

const visible = ref(false);
const canvasWidth = ref(300);
const canvasHeight = ref(500);
const tempFilePath = ref("");

const instance = getCurrentInstance();

const show = () => {
  visible.value = true;
  // Wait for DOM update
  setTimeout(() => {
    drawPoster();
  }, 100);
};

const close = () => {
  visible.value = false;
  emit("close");
};

const drawPoster = () => {
  const ctx = uni.createCanvasContext("shareCanvas", instance);
  const w = canvasWidth.value;
  const h = canvasHeight.value;
  const {
    month,
    totalDays,
    waterBill,
    electricBill,
    gasBill,
    ownerDays,
    splitRule, // Add splitRule
    result,
  } = props.data;

  // Background
  const gradient = ctx.createLinearGradient(0, 0, w, h);
  gradient.addColorStop(0, "#a8edea");
  gradient.addColorStop(0.5, "#fed6e3");
  gradient.addColorStop(1, "#ffecd2");
  ctx.setFillStyle(gradient);
  ctx.fillRect(0, 0, w, h);

  // White Card
  ctx.setFillStyle("rgba(255, 255, 255, 0.9)");
  ctx.setShadow(0, 10, 20, "rgba(0, 0, 0, 0.1)");
  const padding = 20;
  const cardY = 60;
  const cardH = h - cardY - 50;
  roundRect(ctx, padding, cardY, w - padding * 2, cardH, 15);
  ctx.fill();
  ctx.setShadow(0, 0, 0, "transparent"); // Reset shadow

  // Title
  ctx.setFontSize(24);
  ctx.setFillStyle("#5a7c9a");
  ctx.setTextAlign("center");
  ctx.fillText("费用分摊结果", w / 2, 40);

  // Content
  let y = cardY + 40;
  const leftX = padding + 20;
  const rightX = w - padding - 20;
  const lineHeight = 30;

  ctx.setFontSize(18);
  ctx.setFillStyle("#3e627f");
  ctx.setTextAlign("left");

  // Gezi page doesn't have month/totalDays, uses "本次账单"
  if (splitRule) {
    ctx.fillText("本次账单：", leftX, y);
  } else {
    ctx.fillText(`${month}月账单（共${totalDays}天）`, leftX, y);
  }

  y += lineHeight + 10;

  // Bills
  const drawRow = (label, value, color = "#6b7c93", isBold = false) => {
    ctx.setFontSize(14);
    ctx.setFillStyle("#6b7c93");
    ctx.setTextAlign("left");
    ctx.fillText(label, leftX, y);

    ctx.setFontSize(isBold ? 16 : 14);
    ctx.setFillStyle(color);
    ctx.setTextAlign("right");
    ctx.fillText(value, rightX, y);
    y += lineHeight;
  };

  drawRow("💧 水费", `${waterBill || 0} 元`);
  drawRow("⚡ 电费", `${electricBill || 0} 元`);
  drawRow("🔥 燃气费", `${gasBill || 0} 元`);

  // Divider
  y -= 10;
  ctx.setStrokeStyle("#e0e0e0");
  ctx.setLineDash([5, 5]);
  ctx.beginPath();
  ctx.moveTo(leftX, y);
  ctx.lineTo(rightX, y);
  ctx.stroke();
  ctx.setLineDash([]);
  y += 30;

  // Result
  drawRow("💵 总费用", `${result.totalAmount} 元`, "#ff6b9d", true);

  if (!splitRule) {
    // Lizi Page Logic
    drawRow(`👤 房主 (${ownerDays}天)`, `${result.ownerAmount} 元`);
    drawRow("📊 剩余费用", `${result.remainingAmount} 元`);

    y += 10;

    // Draw background box for "分摊结果" section
    const boxStartY = y - 30;
    const boxHeight = lineHeight * 3 + 20; // Title + 2 rows + padding
    ctx.setFillStyle("#fff5f5"); // Light pink background
    roundRect(ctx, leftX - 10, boxStartY, rightX - leftX + 20, boxHeight, 8);
    ctx.fill();

    ctx.setFontSize(14);
    ctx.setFillStyle("#3e627f");
    ctx.setTextAlign("left");
    ctx.fillText("💫 分摊结果：", leftX, y);
    y += lineHeight;

    drawRow("🌸 谢林珠", `${result.sisterAmount} 元`, "#5a7c9a", true);
    drawRow("🌻 张锦豪", `${result.datouAmount} 元`, "#5a7c9a", true);
  } else {
    // Gezi Page Logic
    if (splitRule === "普通分账") {
      drawRow("🌸 人均费用", `${result.chunfengAmount} 元`, "#ff6b9d", true);
    } else if (splitRule === "特殊分账") {
      y += 10;

      // Draw background box for "分摊结果" section
      const boxStartY = y - 30;
      const boxHeight = lineHeight * 5 + 20; // Title + 4 rows + padding
      ctx.setFillStyle("#e8f5f8"); // Light blue background for Gezi page
      roundRect(ctx, leftX - 10, boxStartY, rightX - leftX + 20, boxHeight, 8);
      ctx.fill();

      ctx.setFontSize(14);
      ctx.setFillStyle("#3e627f");
      ctx.setTextAlign("left");
      ctx.fillText("💫 费用分摊明细：", leftX, y);
      y += lineHeight;

      drawRow("🍐 李子应承担", `${result.liziAmount} 元`, "#5a7c9a", true);
      drawRow("🕊️ 鸽子应承担", `${result.geziAmount} 元`, "#5a7c9a", true);
      drawRow("🌸 春风应承担", `${result.chunfengAmount} 元`, "#5a7c9a", true);
      drawRow("🍊 橙子应承担", `${result.chengziAmount} 元`, "#5a7c9a", true);
    }
  }

  // Footer
  ctx.setFontSize(12);
  ctx.setFillStyle("#999");
  ctx.setTextAlign("center");

  ctx.draw(false, () => {
    uni.canvasToTempFilePath(
      {
        canvasId: "shareCanvas",
        success: (res) => {
          tempFilePath.value = res.tempFilePath;
        },
        fail: (err) => {
          console.error("生成图片失败", err);
        },
      },
      instance
    );
  });
};

const roundRect = (ctx, x, y, w, h, r) => {
  if (w < 2 * r) r = w / 2;
  if (h < 2 * r) r = h / 2;
  ctx.beginPath();
  ctx.moveTo(x + r, y);
  ctx.arcTo(x + w, y, x + w, y + h, r);
  ctx.arcTo(x + w, y + h, x, y + h, r);
  ctx.arcTo(x, y + h, x, y, r);
  ctx.arcTo(x, y, x + w, y, r);
  ctx.closePath();
};

const saveImage = () => {
  if (!tempFilePath.value) {
    uni.showToast({
      title: "图片生成中...",
      icon: "none",
    });
    return;
  }
  uni.saveImageToPhotosAlbum({
    filePath: tempFilePath.value,
    success: () => {
      uni.showToast({
        title: "已保存到相册",
        icon: "success",
      });
      close();
    },
    fail: (err) => {
      console.error("保存失败", err);
      uni.showToast({
        title: "保存失败，请检查权限",
        icon: "none",
      });
    },
  });
};

const shareToWeChat = () => {
  if (!tempFilePath.value) {
    uni.showToast({
      title: "图片生成中...",
      icon: "none",
    });
    return;
  }

  // 使用 wx.showShareImageMenu 直接分享图片
  // 这个 API 会唤起分享菜单，用户可以选择发送给朋友
  // 发送出去的是图片预览形式，而不是文件
  wx.showShareImageMenu({
    path: tempFilePath.value,
    success: () => {
      close();
    },
    fail: (err) => {
      console.error("分享失败", err);
      // 如果 API 不支持或失败，降级提示
      if (err.errMsg.indexOf("cancel") === -1) {
        uni.showModal({
          title: "提示",
          content: "分享失败，请尝试保存到相册后手动分享",
          showCancel: false,
        });
      }
    },
  });
};

defineExpose({
  show,
});
</script>

<style lang="scss" scoped>
.share-poster-container {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  z-index: 88;
  display: flex;
  justify-content: center;
  align-items: center;
}

.mask {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.6);
}

.content {
  position: relative;
  z-index: 1000;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.canvas-content {
  border-radius: 32rpx;
  overflow: hidden;
}

.canvas {
  background: #fff;
  box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.2);
}

.btn-group {
  margin-top: 30rpx;
  display: flex;
  gap: 20rpx;
}

.btn {
  font-size: 28rpx;
  padding: 10rpx 40rpx;
  border-radius: 40rpx;
  border: none;
  &::after {
    border: none;
  }
}

.save-btn {
  background: linear-gradient(135deg, #ff9a9e 0%, #fecfef 100%);
  color: #fff;
}

.share-btn {
  background: linear-gradient(135deg, #a8edea 0%, #fed6e3 100%);
  color: #fff;
}
</style>
