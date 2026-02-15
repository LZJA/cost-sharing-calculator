<template>
  <view class="simple-pie-chart">
    <canvas
      canvas-id="pieChart"
      id="pieChart"
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

const canvasWidth = ref(300); // 固定宽度 300px
const canvasHeight = ref(300); // 固定高度 300px
const instance = getCurrentInstance();
const isReady = ref(false);

const drawChart = () => {
  if (!isReady.value) return;

  if (!props.data || !props.data.series) {
    return;
  }

  nextTick(() => {
    const ctx = uni.createCanvasContext("pieChart", instance);
    const w = canvasWidth.value;
    const h = canvasHeight.value;
    const centerX = w / 2;
    const centerY = h / 2 - 20;
    const radius = Math.min(w, h) / 2 - 50;

    ctx.clearRect(0, 0, w, h);

    ctx.setFillStyle("rgba(255, 255, 255, 0.9)");
    ctx.fillRect(0, 0, w, h);

    const series = props.data.series;
    const colors = ["#a8edea", "#ffd93d", "#ff6b9d"];
    let total = 0;
    series.forEach((s) => (total += s.data));

    if (total === 0) return;

    let startAngle = -Math.PI / 2;

    series.forEach((s, i) => {
      const angle = (s.data / total) * Math.PI * 2;
      const endAngle = startAngle + angle;

      ctx.beginPath();
      ctx.moveTo(centerX, centerY);
      ctx.arc(centerX, centerY, radius, startAngle, endAngle);
      ctx.closePath();

      ctx.setFillStyle(s.color || colors[i] || "#a8edea");
      ctx.fill();

      ctx.setStrokeStyle("#fff");
      ctx.setLineWidth(2);
      ctx.stroke();

      startAngle = endAngle;
    });

    // 图例
    const legendY = h - 40;
    const legendItemW = 80;
    const legendGap = 8;
    let legendX = (w - legendItemW * series.length - legendGap * (series.length - 1)) / 2;

    series.forEach((s, i) => {
      ctx.setFillStyle(s.color || colors[i] || "#a8edea");
      ctx.fillRect(legendX, legendY, 12, 12);

      ctx.setFontSize(10);
      ctx.setFillStyle("#5a7c9a");
      ctx.setTextAlign("left");
      ctx.fillText(`${s.name} ${s.data}%`, legendX + 16, legendY + 10);

      legendX += legendItemW + legendGap;
    });

    ctx.draw(false);
  });
};

onMounted(() => {
  const query = uni.createSelectorQuery().in(instance);
  query
    .select(".simple-pie-chart")
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
  { deep: true }
);
</script>

<style lang="scss" scoped>
.simple-pie-chart {
  width: 100%;
  height: 400rpx;
  display: flex;
  justify-content: center;
  align-items: center;
}

.chart-canvas {
  width: 300px;
  height: 300px;
  display: block;
}
</style>
