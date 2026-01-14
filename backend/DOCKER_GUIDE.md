# Docker 快速启动指南 🐳

## 前提条件

只需要安装 **Docker Desktop**，就这一个！

### 安装 Docker Desktop

#### Mac 系统

1. **下载 Docker Desktop**
   - 访问: https://www.docker.com/products/docker-desktop
   - 下载 Mac 版本（根据芯片选择 Intel 或 Apple Silicon）

2. **安装**
   - 打开下载的 .dmg 文件
   - 将 Docker 拖到 Applications 文件夹
   - 启动 Docker Desktop

3. **验证安装**
   ```bash
   docker --version
   docker-compose --version
   ```

   应该看到类似输出：
   ```
   Docker version 24.x.x
   Docker Compose version v2.x.x
   ```

---

## 🚀 一键启动（超级简单）

### 方式1：使用启动脚本（推荐）

```bash
# 在项目根目录执行
./start-backend.sh
```

就这样！脚本会自动：
- ✅ 检查 Docker 是否运行
- ✅ 启动 MySQL 数据库
- ✅ 编译并启动后端服务
- ✅ 等待服务就绪
- ✅ 显示访问地址

### 方式2：手动执行命令

```bash
# 1. 进入后端目录
cd backend

# 2. 启动所有服务
docker-compose up -d

# 3. 查看启动状态
docker-compose ps

# 4. 查看日志
docker-compose logs -f
```

---

## 📍 服务访问地址

启动成功后：

- **后端 API**: http://localhost:8080/api
- **MySQL 数据库**: localhost:3306
  - 用户名: `root`
  - 密码: `123456`
  - 数据库: `cost_sharing_db`

### 测试接口

在浏览器打开：
```
http://localhost:8080/api/cards
```

或使用 curl：
```bash
curl http://localhost:8080/api/cards
```

看到 JSON 响应就成功了！

---

## 🛑 停止服务

### 方式1：使用停止脚本

```bash
./stop-backend.sh
```

### 方式2：手动执行

```bash
cd backend
docker-compose down
```

**注意**：数据会保留，下次启动时数据还在！

---

## 📋 常用命令

### 查看服务状态
```bash
cd backend
docker-compose ps
```

### 查看日志
```bash
# 查看所有日志
docker-compose logs

# 实时查看日志
docker-compose logs -f

# 只看后端日志
docker-compose logs -f backend

# 只看数据库日志
docker-compose logs -f mysql
```

### 重启服务
```bash
# 重启所有服务
docker-compose restart

# 只重启后端
docker-compose restart backend

# 只重启数据库
docker-compose restart mysql
```

### 完全重新构建
```bash
# 如果修改了代码，需要重新构建
docker-compose down
docker-compose build --no-cache
docker-compose up -d
```

### 清理所有数据（慎用！）
```bash
# 停止服务并删除数据卷
docker-compose down -v

# 这会删除数据库中的所有数据！
```

---

## ❓ 常见问题

### 问题1: 启动失败，提示端口被占用

**错误信息**: `port is already allocated`

**解决方法**:

方法A - 关闭占用端口的程序：
```bash
# 查看占用 8080 端口的进程
lsof -i :8080

# 杀死进程（替换 PID）
kill -9 <PID>
```

方法B - 修改端口：

编辑 `backend/docker-compose.yml`，修改端口映射：
```yaml
services:
  backend:
    ports:
      - "8081:8080"  # 改成 8081 或其他端口
```

### 问题2: 首次启动很慢

**原因**: 需要下载镜像和编译代码

**解决**: 耐心等待，通常需要 5-10 分钟

查看进度：
```bash
docker-compose logs -f backend
```

### 问题3: Docker Desktop 未运行

**错误信息**: `Cannot connect to the Docker daemon`

**解决**: 启动 Docker Desktop 应用

### 问题4: 数据库连接失败

**检查步骤**:

1. 确认 MySQL 容器运行中：
   ```bash
   docker-compose ps mysql
   ```

2. 查看 MySQL 日志：
   ```bash
   docker-compose logs mysql
   ```

3. 等待 MySQL 完全启动（首次需要初始化）

### 问题5: 修改代码后没有生效

**原因**: Docker 使用的是旧的编译结果

**解决**: 重新构建
```bash
docker-compose down
docker-compose build --no-cache backend
docker-compose up -d
```

---

## 🗄️ 数据持久化

数据存储在 Docker 数据卷中，即使容器删除，数据也不会丢失。

### 查看数据卷
```bash
docker volume ls | grep cost-sharing
```

### 备份数据库
```bash
# 导出数据库
docker exec cost-sharing-mysql mysqldump -u root -p123456 cost_sharing_db > backup.sql

# 导入数据库
docker exec -i cost-sharing-mysql mysql -u root -p123456 cost_sharing_db < backup.sql
```

---

## 🔧 进入容器调试

有时需要进入容器内部查看：

### 进入后端容器
```bash
docker exec -it cost-sharing-backend sh
```

### 进入 MySQL 容器
```bash
docker exec -it cost-sharing-mysql mysql -u root -p123456

# 进入后可以执行 SQL
USE cost_sharing_db;
SHOW TABLES;
SELECT * FROM cards;
```

---

## 🎯 与前端集成

后端启动后，前端就可以调用 API 了：

### 1. 确保后端运行
```bash
curl http://localhost:8080/api/cards
```

### 2. 在前端使用 API
```javascript
// src/api/costSharingApi.js
const API_BASE_URL = 'http://localhost:8080/api'

// 前端直接调用
import api from '@/api/costSharingApi.js'

const cards = await api.card.getAll()
```

### 3. 开发环境配置

如果前端在微信开发者工具中：
- 设置 → 项目设置 → 勾选 "不校验合法域名"

---

## 💡 开发工作流

### 开发时
```bash
# 1. 启动 Docker 服务
./start-backend.sh

# 2. 启动前端开发服务器
npm run dev:mp-weixin

# 3. 在微信开发者工具中预览
```

### 停止时
```bash
# 停止后端
./stop-backend.sh

# 停止前端（Ctrl+C）
```

### 修改后端代码后
```bash
# 1. 停止服务
./stop-backend.sh

# 2. 重新构建并启动
cd backend
docker-compose build --no-cache backend
docker-compose up -d

# 3. 查看日志确认启动
docker-compose logs -f backend
```

---

## 📊 资源监控

### 查看资源占用
```bash
docker stats
```

### 清理无用资源（释放空间）
```bash
# 清理停止的容器
docker container prune

# 清理无用镜像
docker image prune

# 清理所有无用资源（慎用）
docker system prune -a
```

---

## 🌐 生产环境部署

Docker 也可以用于生产环境：

1. 修改密码（`docker-compose.yml`）
2. 使用环境变量管理配置
3. 配置反向代理（Nginx）
4. 启用 HTTPS
5. 设置自动重启
6. 配置日志收集

详见 `DEPLOYMENT.md`

---

## 🆘 需要帮助？

1. 查看启动日志：`docker-compose logs -f`
2. 查看容器状态：`docker-compose ps`
3. 检查 Docker Desktop 是否运行
4. 确认端口未被占用：`lsof -i :8080`
5. 重启 Docker Desktop

还有问题？把错误信息发给我！

---

## ✅ 总结：超级简单的三步走

```bash
# 1. 安装 Docker Desktop（只需安装一次）
# 下载: https://www.docker.com/products/docker-desktop

# 2. 启动后端服务
./start-backend.sh

# 3. 测试接口
curl http://localhost:8080/api/cards
```

完成！现在前端就可以调用后端 API 了！ 🎉
