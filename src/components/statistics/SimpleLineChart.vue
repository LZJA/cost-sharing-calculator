<template>
  <view class="simple-line-chart">
    <canvas
      canvas-id="lineChart"
      id="lineChart"
      class="chart-canvas"
      :style="{ width: canvasWidth + 'px', height: canvasHeight + 'px' }"
    ></canvas>
  </view>
</template>

<script setup>
import { ref, onMounted, watch, getCurrentInstance, nextTick } from "vue";

const props = defineProps({
  data: {
    type: Object,
    default: () => ({}),
  },
});

const canvasWidth = ref(351); // 固定宽度 375px (750rpx / 2)
const canvasHeight = ref(200); // 固定高度 200px (400rpx / 2)
const instance = getCurrentInstance();
const isReady = ref(false);

const drawChart = () => {
  if (!isReady.value) return;

  if (!props.data || !props.data.categories || !props.data.series) {
    return;
  }

  nextTick(() => {
    const ctx = uni.createCanvasContext("lineChart", instance);
    const w = canvasWidth.value;
    const h = canvasHeight.value;
    const padding = 40;
    const chartW = w - padding * 2;
    const chartH = h - padding * 2;

    // 清空画布
    ctx.clearRect(0, 0, w, h);

    // 绘制背景
    ctx.setFillStyle("rgba(255, 255, 255, 0.9)");
    ctx.fillRect(0, 0, w, h);

    const categories = props.data.categories;
    const series = props.data.series;

    if (!categories || categories.length === 0) return;

    // 找出最大值
    let maxVal = 0;
    series.forEach((s) => {
      s.data.forEach((v) => {
        if (v > maxVal) maxVal = v;
      });
    });
    if (maxVal === 0) maxVal = 100; // 防止除以0
    maxVal = maxVal * 1.1;

    // 绘制网格线
    ctx.setStrokeStyle("#e8f0f5");
    ctx.setLineWidth(1);
    for (let i = 0; i <= 5; i++) {
      const y = padding + (chartH / 5) * i;
      ctx.beginPath();
      ctx.moveTo(padding, y);
      ctx.lineTo(w - padding, y);
      ctx.stroke();

      // Y轴标签
      const val = Math.round(maxVal - (maxVal / 5) * i);
      ctx.setFontSize(10);
      ctx.setFillStyle("#7c95aa");
      ctx.setTextAlign("right");
      ctx.fillText(val.toString(), padding - 5, y + 3);
    }

    // X轴标签
    const stepX = chartW / Math.max(categories.length - 1, 1);
    categories.forEach((cat, i) => {
      const x = padding + stepX * i;
      ctx.setFontSize(10);
      ctx.setFillStyle("#7c95aa");
      ctx.setTextAlign("center");
      ctx.fillText(cat, x, h - padding + 15);
    });

    // 绘制折线
    const colors = ["#ff6b9d", "#a8edea", "#ffd93d", "#6bcf7f"];
    series.forEach((s, seriesIndex) => {
      if (!s.data || s.data.length === 0) return;

      ctx.setStrokeStyle(colors[seriesIndex] || "#ff6b9d");
      ctx.setLineWidth(2);

      ctx.beginPath();
      s.data.forEach((val, i) => {
        const x = padding + stepX * i;
        const y = padding + chartH - (val / maxVal) * chartH;

        if (i === 0) {
          ctx.moveTo(x, y);
        } else {
          ctx.lineTo(x, y);
        }
      });
      ctx.stroke();

      // 数据点
      ctx.setFillStyle(colors[seriesIndex] || "#ff6b9d");
      s.data.forEach((val, i) => {
        const x = padding + stepX * i;
        const y = padding + chartH - (val / maxVal) * chartH;

        ctx.beginPath();
        ctx.arc(x, y, 3, 0, Math.PI * 2);
        ctx.fill();
      });
    });

    // 图例
    const legendY = 15;
    const legendItemW = 70;
    const legendGap = 8;
    let legendX = padding;

    series.forEach((s, i) => {
      ctx.setFillStyle(colors[i] || "#ff6b9d");
      ctx.fillRect(legendX, legendY, 12, 12);

      ctx.setFontSize(10);
      ctx.setFillStyle("#5a7c9a");
      ctx.setTextAlign("left");
      ctx.fillText(s.name, legendX + 16, legendY + 10);

      legendX += legendItemW + legendGap;
    });

    ctx.draw(false);
  });
};

onMounted(() => {
  const query = uni.createSelectorQuery().in(instance);
  query
    .select(".simple-line-chart")
    .boundingClientRect(() => {
      // 使用固定尺寸，不动态获取
      isReady.value = true;
      setTimeout(() => {
        drawChart();
      }, 100);
    })
    .exec();
});

watch(
  () => props.data,
  () => {
    drawChart();
  },
  { deep: true },
);
</script>

<style lang="scss" scoped>
.simple-line-chart {
  width: 100%;
  height: 400rpx;
  display: flex;
  justify-content: center;
  align-items: center;
}

.chart-canvas {
  width: 375px;
  height: 200px;
  display: block;
}
</style>
