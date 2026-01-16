<script>
export default {
  onLaunch: function() {
    console.log('App Launch')

    // 初始化云开发
    if (wx.cloud) {
      wx.cloud.init({
        env: 'prod-0gko48kec6190500', // 替换为您的云托管环境ID
        traceUser: true
      })
      console.log('云开发初始化成功')
    } else {
      console.error('请使用 2.2.3 或以上的基础库以使用云能力')
    }
  },
  onShow: function() {
    console.log('App Show')
  },
  onHide: function() {
    console.log('App Hide')
  }
}
</script>

<style>
/* 全局样式 */
page {
  background: linear-gradient(135deg, #a8edea 0%, #fed6e3 50%, #ffecd2 100%);
  min-height: 100vh;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Microsoft YaHei', 'SimHei', sans-serif;
}

/* 重置样式 - 微信小程序兼容 */
view, text, button, input, textarea, image, picker {
  box-sizing: border-box;
}
</style>

