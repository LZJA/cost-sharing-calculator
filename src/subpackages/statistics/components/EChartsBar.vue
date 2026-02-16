<template>
  <view class="echarts-bar-chart">
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

const canvasId = ref("barChart-" + Date.now());

const chartOption = computed(() => {
  if (!props.data || !props.data.categories || !props.data.series) {
    return {};
  }

  const categories = props.data.categories;
  const series = props.data.series || [];
  const colors = ["#a8edea", "#ffd93d", "#6bcf7f"];

  return {
    // 动画配置 - 极致性能优化
    animation: false,
    animationDuration: 0,

    // 背景配置
    backgroundColor: "rgba(255, 255, 255, 0.9)",

    // 网格布局
    grid: {
      top: 50,
      left: 20,
      right: 20,
      bottom: 20,
      containLabel: true,
    },

    // 数据缩放 - 简化
    dataZoom: [
      {
        type: "inside",
        start: 0,
        end: 100,
        zoomOnMouseWheel: false,
        moveOnMouseMove: true,
      },
    ],

    // 提示框 - 极简
    tooltip: {
      trigger: "axis",
      confine: true,
      backgroundColor: "rgba(255, 255, 255, 1)",
      borderColor: "#e8f0f5",
      borderWidth: 1,
      textStyle: {
        color: "#5a7c9a",
        fontSize: 12,
      },
      formatter: (params) => {
        if (!params || params.length === 0) return "";
        let result = params[0].name + "\n";
        params.forEach((item) => {
          result += `${item.marker}${item.seriesName}: ¥${item.value.toFixed(2)}\n`;
        });
        return result.trim();
      },
    },

    // 图例 - 简化
    legend: {
      top: 10,
      left: "center",
      textStyle: {
        color: "#5a7c9a",
        fontSize: 12,
      },
      data: series.map((s) => s.name),
    },

    // X轴
    xAxis: {
      type: "category",
      data: categories,
      axisLine: {
        lineStyle: {
          color: "#e8f0f5",
        },
      },
      axisTick: {
        show: false,
      },
      axisLabel: {
        color: "#7c95aa",
        fontSize: 11,
      },
    },

    // Y轴
    yAxis: {
      type: "value",
      axisLine: {
        show: false,
      },
      axisTick: {
        show: false,
      },
      splitLine: {
        lineStyle: {
          color: "#e8f0f5",
          type: "dashed",
        },
      },
      axisLabel: {
        color: "#7c95aa",
        fontSize: 11,
      },
    },

    // 系列配置 - 极致性能优化
    series: series.map((s, index) => {
      const color = s.color || colors[index % colors.length];

      return {
        name: s.name,
        type: "bar",
        data: s.data || [],
        barWidth: 20,
        itemStyle: {
          color: color,
        },
      };
    }),

    color: colors,
  };
});

// 移除不需要的辅助函数

const onChartReady = (chart) => {
  console.log("柱状图已就绪", chart);
};
</script>

<style lang="scss" scoped>
.echarts-bar-chart {
  width: 100%;
  min-height: 400rpx;
  display: flex;
  justify-content: center;
  align-items: center;
}
</style>
