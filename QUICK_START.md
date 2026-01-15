# 🚀 Docker 快速启动指南（3 分钟搞定）

> 为前端工程师准备，无需安装 Java 和 MySQL！

## 📦 只需要安装一个软件：Docker Desktop

### 安装 Docker Desktop（5 分钟）

#### Mac 系统

1. 下载 Docker Desktop

   ```
   https://www.docker.com/products/docker-desktop
   ```

   根据你的芯片选择：

   - Intel 芯片：选择 "Mac with Intel chip"
   - M1/M2/M3 芯片：选择 "Mac with Apple silicon"

2. 安装

   - 打开下载的 `.dmg` 文件
   - 将 Docker 图标拖到 Applications 文件夹
   - 启动 Docker Desktop（第一次启动需要授权）

3. 验证安装

   ```bash
   docker --version
   ```

   看到版本号就成功了！

---

## 🎯 启动后端服务（1 分钟）

### 方式 1：一键启动（超级简单！）

在项目根目录执行：

```bash
./start-backend.sh
```

就这样！脚本会自动完成所有事情 ✨

**首次启动需要 5-10 分钟**（下载镜像和编译代码），之后再启动只需要几秒钟。

看到这个提示就成功了：

```
========================================
🎉 所有服务已启动！
========================================

📍 后端 API 地址: http://localhost:8080/api

🧪 测试接口:
   curl http://localhost:8080/api/cards
   或在浏览器打开: http://localhost:8080/api/cards
```

### 方式 2：手动执行

```bash
# 进入后端目录
cd backend

# 启动服务
docker-compose up -d

# 查看日志
docker-compose logs -f
```

---

## 🧪 测试后端是否启动成功

### 方法 1：浏览器测试

在浏览器打开：

```
http://localhost:8080/api/cards
```

### 方法 2：命令行测试

```bash
curl http://localhost:8080/api/cards
```

看到 JSON 数据就成功了！

```json
{
  "code": 200,
  "message": "success",
  "data": [...]
}
```

---

## 🛑 停止服务

```bash
./stop-backend.sh
```

或者：

```bash
cd backend
docker-compose down
```

**注意**：数据会保留，下次启动数据还在！

---

## 💻 前端集成

后端启动后，前端就可以调用 API 了！

### 1. 确保已创建 API 文件

文件路径：`src/api/costSharingApi.js`（已创建）

### 2. 在页面中使用

```javascript
// 导入 API
import api from "@/api/costSharingApi.js";

// 保存李子账单
const bill = await api.liziBill.save({
  month: 1,
  year: 2024,
  totalDays: 31,
  waterBill: 100.5,
  electricBill: 200.75,
  gasBill: 80.25,
  ownerDays: 15,
});

// 保存卡片配置
const card = await api.card.save({
  type: "lizi",
  name: "李子的分账计算器",
  description: "分账让生活更简单",
  avatar: "🍐",
  background: "",
  enableBackground: true,
});
```

### 3. 微信开发者工具配置

设置 → 项目设置 → 勾选 **"不校验合法域名"**

---

## 📋 常用命令

```bash
# 查看服务状态
cd backend && docker-compose ps

# 查看日志
cd backend && docker-compose logs -f

# 重启服务
cd backend && docker-compose restart

# 完全重启（包括重新构建）
cd backend && docker-compose down && docker-compose up -d --build
```

---

## ❓ 常见问题

### Q1: Docker Desktop 打不开或闪退

**解决**：重启电脑，确保系统是最新版本

### Q2: 端口被占用

**错误信息**：`port is already allocated`

**解决**：

```bash
# 查看占用端口的进程
lsof -i :8080

# 杀死进程
kill -9 <PID>
```

### Q3: 首次启动很慢

**原因**：需要下载镜像（MySQL、Java）和编译代码

**解决**：耐心等待 5-10 分钟，查看日志：

```bash
cd backend && docker-compose logs -f backend
```

### Q4: 修改代码后没有生效

**解决**：重新构建

```bash
cd backend
docker-compose down
docker-compose build --no-cache backend
docker-compose up -d
```

---

## 🎯 完整开发流程

```bash
# 1. 启动 Docker Desktop（只需确保运行中）

# 2. 启动后端服务
./start-backend.sh

# 3. 启动前端开发服务器
npm run dev:mp-weixin

# 4. 在微信开发者工具中预览

# 开发完成后停止后端
./stop-backend.sh
```

---

## 📚 更多文档

- 详细 Docker 指南：`backend/DOCKER_GUIDE.md`
- 完整 API 文档：`backend/README.md`
- 生产环境部署：`backend/DEPLOYMENT.md`
- 环境配置（非 Docker）：`backend/SETUP_GUIDE.md`

---

## 🆘 需要帮助？

1. 查看日志：`cd backend && docker-compose logs -f`
2. 确认 Docker Desktop 正在运行
3. 检查端口是否被占用：`lsof -i :8080`
4. 重启 Docker Desktop

还有问题？把错误信息发给我！

---

## ✅ 总结：超级简单的三步

```bash
# ① 安装 Docker Desktop（只需一次）
# 下载: https://www.docker.com/products/docker-desktop

# ② 启动后端
./start-backend.sh

# ③ 测试
curl http://localhost:8080/api/cards
```

**就这么简单！** 🎉

现在你可以专注于前端开发，后端已经搞定了！
