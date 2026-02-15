<template>
  <view
    class="share-report-poster-container"
    v-if="visible"
    @touchmove.stop.prevent
  >
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
import { ref, watch, getCurrentInstance } from "vue";

const props = defineProps({
  reportData: {
    type: Object,
    default: () => ({}),
  },
});

const emit = defineEmits(["close"]);

const visible = ref(false);
const canvasWidth = ref(300);
const canvasHeight = ref(600);
const tempFilePath = ref("");

const instance = getCurrentInstance();

const show = () => {
  visible.value = true;
  setTimeout(() => {
    drawReport();
  }, 100);
};

const close = () => {
  visible.value = false;
  emit("close");
};

const drawReport = () => {
  const ctx = uni.createCanvasContext("reportCanvas", instance);
  const w = canvasWidth.value;
  const h = canvasHeight.value;
  const { reportData } = props;

  // 绘制渐变背景
  const gradient = ctx.createLinearGradient(0, 0, w, h);
  gradient.addColorStop(0, "#a8edea");
  gradient.addColorStop(0.5, "#fed6e3");
  gradient.addColorStop(1, "#ffecd2");
  ctx.setFillStyle(gradient);
  ctx.fillRect(0, 0, w, h);

  // 绘制白色卡片
  ctx.setFillStyle("rgba(255, 255, 255, 0.95)");
  ctx.setShadow(0, 10, 20, "rgba(0, 0, 0, 0.1)");
  const padding = 20;
  const cardY = 60;
  const cardH = h - cardY - 60;
  roundRect(ctx, padding, cardY, w - padding * 2, cardH, 15);
  ctx.fill();
  ctx.setShadow(0, 0, 0, "transparent");

  // 标题
  ctx.setFontSize(20);
  ctx.setFillStyle("#5a7c9a");
  ctx.setTextAlign("center");
  ctx.fillText("统计分析报告", w / 2, 40);

  // 日期区间
  ctx.setFontSize(14);
  ctx.setFillStyle("#6b8aa6");
  ctx.fillText(reportData.dateRange || "", w / 2, cardY + 30);

  // 内容
  let y = cardY + 60;
  const leftX = padding + 20;
  const rightX = w - padding - 20;

  // 核心指标
  ctx.setFontSize(14);
  ctx.setFillStyle("#3e627f");
  ctx.setTextAlign("left");
  ctx.fillText("💰 总支出", leftX, y);
  ctx.setTextAlign("right");
  ctx.setFillStyle("#ff6b9d");
  ctx.fillText(
    `¥${formatNum(reportData.metrics?.totalExpense || 0)}`,
    rightX,
    y,
  );
  y += 30;

  ctx.setTextAlign("left");
  ctx.setFillStyle("#3e627f");
  ctx.fillText("📊 月均支出", leftX, y);
  ctx.setTextAlign("right");
  ctx.setFillStyle("#a8edea");
  ctx.fillText(`¥${formatNum(reportData.metrics?.avgMonthly || 0)}`, rightX, y);
  y += 30;

  // 分隔线
  y += 10;
  ctx.setStrokeStyle("#e0e0e0");
  ctx.setLineDash([5, 5]);
  ctx.beginPath();
  ctx.moveTo(leftX, y);
  ctx.lineTo(rightX, y);
  ctx.stroke();
  ctx.setLineDash([]);
  y += 30;

  // 分摊统计标题
  ctx.setFontSize(14);
  ctx.setFillStyle("#3e627f");
  ctx.setTextAlign("left");
  ctx.fillText("💰 分摊费用统计", leftX, y);
  y += 30;

  // 分摊统计列表
  const splitList = reportData.splitStatistics || [];
  splitList.forEach((item) => {
    ctx.setFontSize(12);
    ctx.setFillStyle("#6b7c93");
    ctx.setTextAlign("left");
    ctx.fillText(item.name, leftX, y);

    ctx.setTextAlign("right");
    ctx.setFillStyle("#5a7c9a");
    ctx.fillText(`¥${formatNum(item.amount)} (${item.percentage}%)`, rightX, y);
    y += 25;
  });

  // 费用占比
  y += 15;
  ctx.setFontSize(14);
  ctx.setFillStyle("#3e627f");
  ctx.setTextAlign("left");
  ctx.fillText("📊 费用类型占比", leftX, y);
  y += 30;

  const distList = reportData.costDistribution || [];
  distList.forEach((item) => {
    ctx.setFontSize(12);
    ctx.setFillStyle("#6b7c93");
    ctx.setTextAlign("left");
    ctx.fillText(item.name, leftX, y);

    ctx.setTextAlign("right");
    ctx.setFillStyle("#5a7c9a");
    ctx.fillText(`¥${formatNum(item.value)} (${item.percentage}%)`, rightX, y);
    y += 25;
  });

  // 底部
  ctx.setFontSize(12);
  ctx.setFillStyle("#999");
  ctx.setTextAlign("center");
  ctx.fillText("分账计算器 - 让合租生活更简单", w / 2, h - 20);

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
      instance,
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

const formatNum = (num) => {
  return (num || 0).toFixed(2);
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

watch(
  () => props.reportData,
  () => {
    if (visible.value) {
      drawReport();
    }
  },
  { deep: true },
);

defineExpose({
  show,
});
</script>

<style lang="scss" scoped>
.share-report-poster-container {
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
  z-index: 88;
  display: flex;
  flex-direction: column;
  align-items: center;
  box-sizing: border-box;
}

.canvas {
  width: 300px;
  height: 600px;
  border-radius: 16rpx;
  overflow: hidden;
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
