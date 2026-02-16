<template>
  <view class="echarts-wrap">
    <ec-canvas
      :canvas-id="canvasId"
      :width="width"
      :height="height"
      :ec="ec"
    />
  </view>
</template>

<script setup>
import { ref, reactive, watch } from 'vue'
import * as echarts from './echarts.min.js'
import ecCanvas from '@/components/ec-canvas/ec-canvas.vue'

const props = defineProps({
  // 图表配置选项
  option: {
    type: Object,
    default: () => ({})
  },
  // 画布标识符
  canvasId: {
    type: String,
    default: () => 'echarts-' + Math.random().toString(36).substring(2, 9)
  },
  // 画布宽度
  width: {
    type: Number,
    default: 375
  },
  // 画布高度
  height: {
    type: Number,
    default: 300
  }
})

const emit = defineEmits(['ready', 'click', 'legendselectchanged', 'dataZoom', 'restore', 'finished'])

const echartsInstance = ref(null)
const isReady = ref(false)

// ECharts 初始化配置
const ec = reactive({
  onInit: initChart
})

// 初始化图表
function initChart(canvas, width, height, dpr) {
  console.log('初始化 ECharts, canvasId:', props.canvasId, { width, height, dpr })

  const chart = echarts.init(canvas, null, {
    width: width,
    height: height,
    devicePixelRatio: dpr
  })

  // 保存实例
  echartsInstance.value = chart
  canvas.setChart(chart)

  // 设置初始配置
  if (props.option) {
    chart.setOption(props.option, true)
  }

  // 绑定点击事件
  chart.on('click', (params) => {
    emit('click', params)
  })

  // 绑定图例选择事件
  chart.on('legendselectchanged', (params) => {
    emit('legendselectchanged', params)
  })

  // 绑定数据区域缩放事件
  chart.on('dataZoom', (params) => {
    emit('dataZoom', params)
  })

  // 绑定还原事件
  chart.on('restore', (params) => {
    emit('restore', params)
  })

  // 绑定渲染完成事件
  chart.on('finished', () => {
    isReady.value = true
    emit('finished')
  })

  isReady.value = true
  emit('ready', chart)

  console.log('ECharts 初始化成功')
  return chart
}

// 设置图表配置
const setOption = (option, notMerge = false, lazyUpdate = false) => {
  if (echartsInstance.value && option) {
    echartsInstance.value.setOption(option, notMerge, lazyUpdate)
  }
}

// 清除图表
const clear = () => {
  if (echartsInstance.value) {
    echartsInstance.value.clear()
  }
}

// 释放图表实例
const dispose = () => {
  if (echartsInstance.value) {
    echartsInstance.value.dispose()
    echartsInstance.value = null
    isReady.value = false
  }
}

// 监听配置变化
watch(() => props.option, (newOption) => {
  if (isReady.value && newOption) {
    setOption(newOption)
  }
}, { deep: true })

defineExpose({
  echarts: echartsInstance,
  setOption,
  clear,
  dispose
})
</script>

<style lang="scss" scoped>
.echarts-wrap {
  width: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
}
</style>
