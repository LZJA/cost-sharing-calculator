# ECharts 图表库说明

此目录包含 ECharts 图表组件。由于微信小程序环境限制，需要使用专门构建的 echarts 库。

## 使用方法

### 1. 下载 echarts.min.js

访问 ECharts 在线定制工具：https://echarts.apache.org/zh/builder.html

#### 选择以下选项：
- ✅ 折线图 (line)
- ✅ 柱状图 (bar)
- ✅ 饼图 (pie)
- ✅ 提示框 (tooltip)
- ✅ 图例 (legend)
- ✅ 标题 (title)
- ✅ 网格 (grid)
- ✅ 直角坐标系 (grid)
- ❌ 取消其他不需要的组件以减小体积

#### 点击"下载"按钮，将下载的文件重命名为 `echarts.min.js`

### 2. 放置文件

将 `echarts.min.js` 文件放到此目录：

```
src/subpackages/statistics/components/echarts.min.js
```

### 3. 文件大小

按上述配置定制后，文件大小约为 300-400 KB

## 组件说明

- `ECharts.vue` - 主封装组件
- `EChartsLine.vue` - 折线图
- `EChartsBar.vue` - 柱状图
- `EChartsPie.vue` - 饼图
- `EChartsCombo.vue` - 组合图
