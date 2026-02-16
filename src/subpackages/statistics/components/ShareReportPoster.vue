<template>
  <view class="share-poster-container" v-if="visible" @touchmove.stop.prevent>
    <view class="mask" @click="close"></view>
    <view class="content">
      <canvas
        canvas-id="reportCanvas"
        id="reportCanvas"
        class="canvas"
        :style="{ width: canvasWidth + 'px', height: canvasHeight + 'px' }"
      ></canvas>
      <view class="btn-group">
        <button class="btn save-btn" @click="saveImage">保存到相册</button>
        <button class="btn share-btn" @click="shareToWeChat">分享好友</button>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, getCurrentInstance } from "vue";

const props = defineProps({
  data: {
    type: Object,
    required: true,
  },
});

const emit = defineEmits(["close"]);

const visible = ref(false);
const canvasWidth = ref(320);
const canvasHeight = ref(560);
const tempFilePath = ref("");

const instance = getCurrentInstance();

const show = () => {
  visible.value = true;
  setTimeout(() => {
    drawPoster();
  }, 100);
};

const close = () => {
  visible.value = false;
  emit("close");
};

const drawPoster = () => {
  const ctx = uni.createCanvasContext("reportCanvas", instance);
  const w = canvasWidth.value;
  const h = canvasHeight.value;
  const {
    billType,
    dateRange,
    metrics,
    comparison,
    splitStatistics,
    costDistribution,
  } = props.data;

  // Background gradient
  const gradient = ctx.createLinearGradient(0, 0, w, h);
  gradient.addColorStop(0, "#a8edea");
  gradient.addColorStop(0.5, "#fed6e3");
  gradient.addColorStop(1, "#ffecd2");
  ctx.setFillStyle(gradient);
  ctx.fillRect(0, 0, w, h);

  // White Card
  ctx.setFillStyle("rgba(255, 255, 255, 0.95)");
  ctx.setShadow(0, 10, 20, "rgba(0, 0, 0, 0.1)");
  const padding = 20;
  const cardY = 70;
  const cardH = h - cardY - 60;
  roundRect(ctx, padding, cardY, w - padding * 2, cardH, 15);
  ctx.fill();
  ctx.setShadow(0, 0, 0, "transparent");

  // Title
  ctx.setFontSize(22);
  ctx.setFillStyle("#5a7c9a");
  ctx.setTextAlign("center");
  const typeText = billType === "lizi" ? "🌰 李子账单" : "🕊️ 鸽子账单";
  ctx.fillText(`${typeText} 统计报告`, w / 2, 40);

  // Date range
  ctx.setFontSize(12);
  ctx.setFillStyle("#7c95aa");
  ctx.fillText(dateRange || "-", w / 2, 58);

  // Content
  let y = cardY + 35;
  const leftX = padding + 15;
  const rightX = w - padding - 15;
  const lineHeight = 26;

  // Section titles
  const drawSectionTitle = (title) => {
    ctx.setFontSize(13);
    ctx.setFillStyle("#5a7c9a");
    ctx.setTextAlign("left");
    ctx.fillText(title, leftX, y);
    y += lineHeight - 5;
  };

  // Draw row helper
  const drawRow = (label, value, color = "#6b7c93", isBold = false) => {
    ctx.setFontSize(11);
    ctx.setFillStyle("#6b7c93");
    ctx.setTextAlign("left");
    ctx.fillText(label, leftX + 5, y);

    ctx.setFontSize(isBold ? 13 : 11);
    ctx.setFillStyle(color);
    ctx.setTextAlign("right");
    ctx.fillText(value, rightX, y);
    y += lineHeight - 3;
  };

  // Core metrics section
  drawSectionTitle("📊 核心指标");
  drawRow("总支出", `¥${formatNumber(metrics.totalExpense)}`, "#ff6b9d", true);
  drawRow("月均支出", `¥${formatNumber(metrics.avgMonthly)}`, "#5a7c9a", true);
  drawRow("最高月份", `${metrics.maxMonth.month} ¥${formatNumber(metrics.maxMonth.amount)}`);
  drawRow("最低月份", `${metrics.minMonth.month} ¥${formatNumber(metrics.minMonth.amount)}`);

  // Year over year comparison
  if (comparison && comparison.yearOverYear !== undefined) {
    y += 5;
    const yoyColor = comparison.yearOverYear > 0 ? "#ff6b9d" : comparison.yearOverYear < 0 ? "#6bcf7f" : "#7c95aa";
    const yoyArrow = comparison.yearOverYear > 0 ? "↑" : comparison.yearOverYear < 0 ? "↓" : "-";
    drawRow("同比去年", `${yoyArrow} ${Math.abs(comparison.yearOverYear)}%`, yoyColor, true);
  }

  y += 8;

  // Split statistics
  drawSectionTitle("💰 分摊费用统计");
  splitStatistics.forEach((item) => {
    drawRow(item.name, `¥${formatNumber(item.amount)}  |  ${item.percentage}%`);
  });

  y += 8;

  // Cost distribution
  drawSectionTitle("📊 费用类型占比");
  costDistribution.forEach((item) => {
    drawRow(item.name, `¥${formatNumber(item.value)}  |  ${item.percentage}%`);
  });

  // Footer
  ctx.setFontSize(10);
  ctx.setFillStyle("#999");
  ctx.setTextAlign("center");
  ctx.fillText("水电燃气费分账计算器", w / 2, h - 15);

  ctx.draw(false, () => {
    uni.canvasToTempFilePath(
      {
        canvasId: "reportCanvas",
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

const formatNumber = (num) => {
  if (typeof num !== "number") return "0";
  return num.toFixed(2);
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
  wx.showShareImageMenu({
    path: tempFilePath.value,
    needShowEntrance: false,
    success: () => {
      close();
    },
    fail: (err) => {
      console.error("分享失败", err);
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
  z-index: 999;
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
  z-index: 999;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.canvas {
  box-shadow: 0 10rpx 40rpx rgba(0, 0, 0, 0.2);
  border-radius: 24rpx;
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
