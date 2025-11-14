# 解放小脑瓜 - 微信小程序

基于 uniapp + Vue3 开发的微信小程序，用于计算水电燃气费用的均摊。

## 功能特性

- 📅 选择月份，自动计算月份天数
- 💧⚡🔥 输入水费、电费、燃气费
- 🏠 根据房主居住天数计算费用分摊
- 💰 自动计算房主、谢林珠、张锦豪的费用分摊
- 📱 完美适配微信小程序
- 🎨 精美的 UI 设计

## 技术栈

- uniapp
- Vue 3 (Composition API)
- 微信小程序

## 项目结构

```
cost-sharing-calculator/
├── pages/                    # 页面目录
│   └── index/                # 首页
│       └── index.vue         # 主页面
├── utils/                    # 工具函数目录
│   ├── config.js            # 配置文件
│   ├── helpers.js           # 工具函数
│   ├── calculator.js        # 计算逻辑
│   └── validator.js         # 验证逻辑
├── App.vue                   # 应用入口
├── main.js                   # 主入口文件
├── pages.json                # 页面配置
├── manifest.json             # 应用配置
├── vite.config.js           # Vite 配置
└── package.json              # 项目配置
```

## 安装和运行

### 安装依赖

```bash
npm install
```

### 开发模式

```bash
# 微信小程序
npm run dev:mp-weixin
```

### 构建

```bash
# 构建微信小程序
npm run build:mp-weixin
```

构建完成后，使用微信开发者工具打开 `dist/dev/mp-weixin` 目录。

## 代码架构

### 模块化设计

- **utils/config.js**: 集中管理配置信息
- **utils/helpers.js**: 通用工具函数（日期处理、金额格式化等）
- **utils/validator.js**: 表单验证逻辑
- **utils/calculator.js**: 费用计算逻辑
- **pages/index/index.vue**: 主页面，使用 Vue3 Composition API

### 可扩展性

1. **配置扩展**: 在 `utils/config.js` 中添加新配置
2. **功能扩展**: 在相应的工具文件中添加新方法
3. **页面扩展**: 在 `pages/` 目录添加新页面
4. **计算规则扩展**: 修改 `utils/calculator.js` 中的计算逻辑

## 费用计算规则

- **总费用** = 水费 + 电费 + 燃气费
- **房主应承担** = (总费用 × 房主居住天数 / 总天数) / 3
- **剩余费用** = 总费用 - 房主应承担
- **谢林珠应承担** = 剩余费用 / 2
- **张锦豪应承担** = 剩余费用 / 2

## 开发说明

### 开发工具

1. 安装 HBuilderX 或使用 VS Code + uniapp 插件
2. 安装微信开发者工具
3. 运行 `npm run dev:mp-weixin`
4. 在微信开发者工具中打开生成的 `dist/dev/mp-weixin` 目录

### 注意事项

1. 首次运行需要配置 `manifest.json` 中的微信小程序 appid
2. 小程序需要配置合法域名（开发阶段可关闭域名校验）
3. 使用 Vue3 Composition API 编写组件
4. 样式使用 rpx 单位适配不同屏幕

## 许可证

本项目仅供学习和个人使用。
