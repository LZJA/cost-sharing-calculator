<template>
  <view class="simple-combo-chart">
    <canvas
      canvas-id="comboChart"
      id="comboChart"
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
    const ctx = uni.createCanvasContext("comboChart", instance);
    const w = canvasWidth.value;
    const h = canvasHeight.value;
    const padding = 40;
    const chartW = w - padding * 2;
    const chartH = h - padding * 2;

    ctx.clearRect(0, 0, w, h);

    ctx.setFillStyle("rgba(255, 255, 255, 0.9)");
    ctx.fillRect(0, 0, w, h);

    const categories = props.data.categories;
    const series = props.data.series;

    if (!categories || categories.length === 0) return;

    let maxVal = 0;
    series.forEach((s) => {
      s.data.forEach((v) => {
        if (v > maxVal) maxVal = v;
      });
    });
    if (maxVal === 0) maxVal = 100;
    maxVal = maxVal * 1.1;

    // 网格线
    ctx.setStrokeStyle("#e8f0f5");
    ctx.setLineWidth(1);
    for (let i = 0; i <= 5; i++) {
      const y = padding + (chartH / 5) * i;
      ctx.beginPath();
      ctx.moveTo(padding, y);
      ctx.lineTo(w - padding, y);
      ctx.stroke();

      const val = Math.round(maxVal - (maxVal / 5) * i);
      ctx.setFontSize(10);
      ctx.setFillStyle("#7c95aa");
      ctx.setTextAlign("right");
      ctx.fillText(val.toString(), padding - 5, y + 3);
    }

    // 数据
    const groupWidth = chartW / categories.length;

    // 折线
    series.forEach((s) => {
      if (s.type === "line" && s.data) {
        ctx.setStrokeStyle(s.color || "#a8edea");
        ctx.setLineWidth(3);

        ctx.beginPath();
        s.data.forEach((val, i) => {
          const x = padding + groupWidth * i + groupWidth / 2;
          const y = padding + chartH - (val / maxVal) * chartH;

          if (i === 0) {
            ctx.moveTo(x, y);
          } else {
            ctx.lineTo(x, y);
          }
        });
        ctx.stroke();
      }
    });

    // 柱状图
    series.forEach((s) => {
      if (s.type === "column" && s.data) {
        const barWidth = groupWidth / 2;

        s.data.forEach((val, i) => {
          const barH = (val / maxVal) * chartH;
          const x = padding + groupWidth * i + (groupWidth - barWidth) / 2;
          const y = padding + chartH - barH;

          ctx.setFillStyle(s.color || "#ff6b9d");
          ctx.fillRect(x, y, barWidth - 4, barH);
        });
      }
    });

    // X轴标签
    categories.forEach((cat, i) => {
      const x = padding + groupWidth * i + groupWidth / 2;
      ctx.setFontSize(10);
      ctx.setFillStyle("#7c95aa");
      ctx.setTextAlign("center");
      ctx.fillText(cat, x, h - padding + 15);
    });

    // 图例
    const legendY = 15;
    const legendItemW = 70;
    const legendGap = 8;
    let legendX = padding;

    series.forEach((s, i) => {
      if (s.type === "line") {
        ctx.setStrokeStyle(s.color || "#a8edea");
        ctx.setLineWidth(3);
        ctx.beginPath();
        ctx.moveTo(legendX, legendY + 6);
        ctx.lineTo(legendX + 18, legendY + 6);
        ctx.stroke();

        ctx.setFontSize(10);
        ctx.setFillStyle("#5a7c9a");
        ctx.setTextAlign("left");
        ctx.fillText(s.name, legendX + 24, legendY + 10);

        legendX += legendItemW + legendGap;
      } else {
        ctx.setFillStyle(s.color || "#ff6b9d");
        ctx.fillRect(legendX, legendY, 18, 12);

        ctx.setFontSize(10);
        ctx.setFillStyle("#5a7c9a");
        ctx.setTextAlign("left");
        ctx.fillText(s.name, legendX + 24, legendY + 10);

        legendX += legendItemW + legendGap;
      }
    });

    ctx.draw(false);
  });
};

onMounted(() => {
  const query = uni.createSelectorQuery().in(instance);
  query
    .select(".simple-combo-chart")
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
.simple-combo-chart {
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
