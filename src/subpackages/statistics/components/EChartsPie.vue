<template>
  <view class="echarts-pie-chart">
    <ECharts
      :canvas-id="canvasId"
      :option="chartOption"
      :width="375"
      :height="300"
      @ready="onChartReady"
    />
  </view>
</template>

<script setup>
import { computed, ref } from "vue";
import ECharts from "./ECharts.vue";

const props = defineProps({
  data: {
    type: Object,
    default: () => ({}),
  },
});

const canvasId = ref("pieChart-" + Date.now());

const chartOption = computed(() => {
  if (!props.data || !props.data.series) {
    return {};
  }

  const series = props.data.series || [];

  return {
    // 动画配置 - 极致性能优化
    animation: false,
    animationDuration: 0,

    // 背景配置
    backgroundColor: "rgba(255, 255, 255, 0.9)",

    // 提示框 - 极简
    tooltip: {
      trigger: "item",
      confine: true,
      backgroundColor: "rgba(255, 255, 255, 1)",
      borderColor: "#e8f0f5",
      borderWidth: 1,
      textStyle: {
        color: "#5a7c9a",
        fontSize: 12,
      },
      formatter: "{b}: ¥{c} ({d}%)",
    },

    // 图例 - 简化
    legend: {
      orient: "horizontal",
      bottom: 10,
      left: "center",
      textStyle: {
        color: "#5a7c9a",
        fontSize: 12,
      },
      data: series.map((s) => s.name),
    },

    // 饼图系列 - 极致性能优化
    series: [
      {
        type: "pie",
        radius: ["40%", "65%"],
        center: ["50%", "45%"],

        itemStyle: {
          borderColor: "#fff",
          borderWidth: 2,
        },

        label: {
          show: true,
          position: "outside",
          formatter: "{b}\n{d}%",
          color: "#5a7c9a",
          fontSize: 11,
        },

        labelLine: {
          show: true,
          length: 15,
          lineStyle: {
            color: "#7c95aa",
          },
        },

        data: series.map((s, index) => ({
          value: s.data,
          name: s.name,
          itemStyle: {
            color: s.color || ["#a8edea", "#ffd93d", "#ff6b9d"][index % 3],
          },
        })),
      },
    ],

    color: series.map(
      (s) =>
        s.color || ["#a8edea", "#ffd93d", "#ff6b9d"][series.indexOf(s) % 3],
    ),
  };
});

const onChartReady = (chart) => {
  console.log("饼图已就绪", chart);
};
</script>

<style lang="scss" scoped>
.echarts-pie-chart {
  width: 100%;
  min-height: 400rpx;
  display: flex;
  justify-content: center;
  align-items: center;
}
</style>
