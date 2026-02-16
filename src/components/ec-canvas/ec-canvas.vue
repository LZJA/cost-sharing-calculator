<template>
  <canvas
    v-if="isUseNewCanvas"
    type="2d"
    class="ec-canvas"
    :canvas-id="canvasId"
    :id="canvasId"
    :style="{ width: width + 'px', height: height + 'px' }"
    @touchstart="touchStart"
    @touchmove="touchMove"
    @touchend="touchEnd"
    @touchcancel="touchEnd"
  ></canvas>
  <canvas
    v-else
    class="ec-canvas"
    :canvas-id="canvasId"
    :id="canvasId"
    :style="{ width: width + 'px', height: height + 'px' }"
    @touchstart="touchStart"
    @touchmove="touchMove"
    @touchend="touchEnd"
    @touchcancel="touchEnd"
  ></canvas>
</template>

<script setup>
import { ref, onMounted, getCurrentInstance } from 'vue'
import WxCanvas from './wx-canvas.js'

const props = defineProps({
  canvasId: {
    type: String,
    default: 'ec-canvas'
  },
  width: {
    type: Number,
    default: 375
  },
  height: {
    type: Number,
    default: 300
  },
  ec: {
    type: Object,
    default: () => ({})
  },
  forceUseOldCanvas: {
    type: Boolean,
    default: false
  }
})

// 响应式数据
const isUseNewCanvas = ref(false)
const canvasNode = ref(null)
const chart = ref(null)

// 全局变量
let ctx = null
const instance = getCurrentInstance()

// 初始化检查是否使用新Canvas
const checkIsUseNewCanvas = () => {
  if (props.forceUseOldCanvas) return false

  const appBaseInfo = wx.getAppBaseInfo()
  const SDKVersion = appBaseInfo.SDKVersion
  const [major, minor, patch] = SDKVersion.split('.').map((n) => parseInt(n, 10))

  try {
    if (major > 2) return true
    if (major === 2 && minor > 9) return true
    if (major === 2 && minor === 9 && patch >= 0) return true
  } catch (e) {
    return false
  }
  return false
}

// 初始化
const init = (callback) => {
  console.log('[ec-canvas] init 开始, canvasId:', props.canvasId, 'isUseNewCanvas:', isUseNewCanvas.value, 'width:', props.width, 'height:', props.height)

  const appBaseInfo = wx.getAppBaseInfo()
  const SDKVersion = appBaseInfo.SDKVersion
  const [major, minor, patch] = SDKVersion.split('.').map((n) => parseInt(n, 10))
  const isValid = major > 2 || (major === 2 && minor >= 20) || (major === 2 && minor === 20 && patch >= 1)

  console.log('[ec-canvas] 系统版本:', SDKVersion, 'isValid:', isValid)

  if (!isValid) {
    console.error('微信基础库版本过低，需大于等于 2.9.0 版本。参见：https://github.com/ecomfe/echarts-for-weixin')
    return
  }

  const query = uni.createSelectorQuery().in(instance)

  if (isUseNewCanvas.value) {
    // 新版本
    query
      .select('.ec-canvas')
      .fields({ node: true, size: true })
      .exec((res) => {
        console.log('[ec-canvas] 新版本 Canvas 查询结果:', res)

        if (!res || !res[0] || !res[0].node) {
          console.error('[ec-canvas] 未获取到 canvas 节点')
          return
        }

        const node = res[0].node
        canvasNode.value = node
        const canvasContext = node.getContext('2d')
        const canvas = new WxCanvas(canvasContext, props.canvasId, true, node)

        ctx = canvas

        console.log('[ec-canvas] 开始初始化 ECharts, width:', props.width, 'height:', props.height, 'dpr:', wx.getWindowInfo().pixelRatio)

        if (typeof callback === 'function') {
          chart.value = callback(canvas, props.width, props.height, wx.getWindowInfo().pixelRatio)
        } else if (props.ec && props.ec.onInit) {
          chart.value = props.ec.onInit(canvas, props.width, props.height, wx.getWindowInfo().pixelRatio)
        }

        console.log('[ec-canvas] ECharts 初始化完成, chart:', chart.value)
      })
  } else {
    // 旧版本
    query
      .select('.ec-canvas')
      .boundingClientRect((res) => {
        console.log('[ec-canvas] 旧版本 Canvas 查询结果:', res)

        if (!res) {
          console.error('未获取到 canvas 信息')
          return
        }
        const canvasContext = uni.createCanvasContext(props.canvasId, instance)
        const canvas = new WxCanvas(canvasContext, props.canvasId, false)

        ctx = canvas

        console.log('[ec-canvas] 开始初始化 ECharts (旧版本), width:', res.width, 'height:', res.height, 'dpr:', wx.getWindowInfo().pixelRatio)

        if (typeof callback === 'function') {
          chart.value = callback(canvas, res.width, res.height, wx.getWindowInfo().pixelRatio)
        } else if (props.ec && props.ec.onInit) {
          chart.value = props.ec.onInit(canvas, res.width, res.height, wx.getWindowInfo().pixelRatio)
        }

        console.log('[ec-canvas] ECharts 初始化完成 (旧版本), chart:', chart.value)
      })
      .exec()
  }
}

// Canvas转临时文件路径
const canvasToTempFilePath = (opt) => {
  if (isUseNewCanvas.value) {
    // 新版本
    const query = uni.createSelectorQuery().in(instance)
    query
      .select('.ec-canvas')
      .fields({ node: true, size: true })
      .exec((res) => {
        const node = res[0].node
        opt.canvas = node
        uni.canvasToTempFilePath(opt)
      })
  } else {
    // 旧版本
    if (!opt.canvasId) {
      opt.canvasId = props.canvasId
    }
    ctx.draw(true, () => {
      uni.canvasToTempFilePath(opt, instance)
    })
  }
}

// 触摸开始
const touchStart = (e) => {
  console.log('touchStart', e, chart.value)
  if (chart.value && e.touches && e.touches.length > 0) {
    const touch = e.touches[0]
    const handler = chart.value.getZr().handler
    const eventData = {
      zrX: touch.x || touch.clientX || 0,
      zrY: touch.y || touch.clientY || 0,
      preventDefault: () => {},
      stopPropagation: () => {},
      stopImmediatePropagation: () => {}
    }
    handler.dispatch('mousedown', eventData)
    handler.dispatch('mousemove', eventData)

    // 处理手势事件
    const wrappedEvent = wrapTouch(e)
    handler.processGesture && handler.processGesture(wrappedEvent, 'start')
  }
}

// 触摸移动
const touchMove = (e) => {
  if (chart.value && e.touches && e.touches.length > 0) {
    const touch = e.touches[0]
    const handler = chart.value.getZr().handler
    const eventData = {
      zrX: touch.x || touch.clientX || 0,
      zrY: touch.y || touch.clientY || 0,
      preventDefault: () => {},
      stopPropagation: () => {},
      stopImmediatePropagation: () => {}
    }
    handler.dispatch('mousemove', eventData)

    // 处理手势事件
    const wrappedEvent = wrapTouch(e)
    handler.processGesture && handler.processGesture(wrappedEvent, 'change')
  }
}

// 触摸结束
const touchEnd = (e) => {
  if (chart.value) {
    const touch = e.changedTouches && e.changedTouches[0] ? e.changedTouches[0] : {}
    const handler = chart.value.getZr().handler
    const eventData = {
      zrX: touch.x || touch.clientX || 0,
      zrY: touch.y || touch.clientY || 0,
      preventDefault: () => {},
      stopPropagation: () => {},
      stopImmediatePropagation: () => {}
    }
    //handler.dispatch('mouseup', eventData)
    handler.dispatch('click', eventData)

    // 处理手势事件
    const wrappedEvent = wrapTouch(e)
    handler.processGesture && handler.processGesture(wrappedEvent, 'end')
  }
}

// 包装触摸事件
const wrapTouch = (event) => {
  // 处理 touches 数组
  if (event.touches && event.touches.length > 0) {
    for (let i = 0; i < event.touches.length; ++i) {
      const touch = event.touches[i]
      touch.offsetX = touch.x || touch.clientX || 0
      touch.offsetY = touch.y || touch.clientY || 0
    }
  }

  // 处理 changedTouches 数组
  if (event.changedTouches && event.changedTouches.length > 0) {
    for (let i = 0; i < event.changedTouches.length; ++i) {
      const touch = event.changedTouches[i]
      touch.offsetX = touch.x || touch.clientX || 0
      touch.offsetY = touch.y || touch.clientY || 0
    }
  }

  // 确保事件对象有必要的方法
  if (!event.preventDefault) {
    event.preventDefault = () => {}
  }
  if (!event.stopPropagation) {
    event.stopPropagation = () => {}
  }
  if (!event.stopImmediatePropagation) {
    event.stopImmediatePropagation = () => {}
  }

  return event
}

// 生命周期
onMounted(() => {
  if (!props.ec) {
    console.warn('组件需绑定 ec 变量，例：<ec-canvas id="mychart-dom-bar" ' + 'canvas-id="mychart-bar" :ec="ec"></ec-canvas>')
    return
  }

  if (!props.ec.lazyLoad) {
    init()
  }
})

// 初始化检查
isUseNewCanvas.value = checkIsUseNewCanvas()

// 暴露方法给外部使用
defineExpose({
  init,
  canvasToTempFilePath
})
</script>

<style scoped>
.ec-canvas {
  width: 100%;
  height: 100%;
  background: transparent;
}
</style>
