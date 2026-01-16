# Docker 部署到腾讯云微信小程序云托管指南

## 📋 前置准备

### 1. 腾讯云准备

- **微信小程序账号**: 已注册并认证的微信小程序
- **云托管环境**: 在微信小程序后台开通云托管服务
- **容器镜像仓库**: 腾讯云容器镜像服务（CCR）

### 2. 本地环境要求

- **Docker Desktop**: 已安装并运行
- **Docker Buildx**: Docker Desktop 自带

---

## 🚀 部署步骤

### 步骤1: 修改 Dockerfile

确保 Dockerfile 不包含固定的 `--platform` 参数，让 Docker Buildx 在构建时指定平台：

```dockerfile
# 第一阶段：构建应用
FROM amazoncorretto:17 AS builder

# 设置工作目录
WORKDIR /app

# 安装必要的工具
RUN yum install -y wget tar gzip

# 下载并安装 Maven 3.9.5
RUN wget https://archive.apache.org/dist/maven/maven-3/3.9.5/binaries/apache-maven-3.9.5-bin.tar.gz && \
    tar -xzf apache-maven-3.9.5-bin.tar.gz -C /opt && \
    rm apache-maven-3.9.5-bin.tar.gz && \
    ln -s /opt/apache-maven-3.9.5/bin/mvn /usr/bin/mvn

# 复制 Maven 配置文件
COPY pom.xml .

# 下载依赖（利用 Docker 缓存）
RUN mvn dependency:go-offline -B || true

# 复制源代码
COPY src ./src

# 编译项目
RUN mvn clean package -DskipTests

# 第二阶段：运行应用
FROM amazoncorretto:17

# 设置工作目录
WORKDIR /app

# 从构建阶段复制编译好的 jar 文件
COPY --from=builder /app/target/cost-sharing-backend-1.0.0.jar ./app.jar

# 暴露端口
EXPOSE 8080

# 启动应用
CMD ["java", "-jar", "app.jar"]
```

### 步骤2: 登录腾讯云容器镜像仓库

```bash
# 登录腾讯云容器镜像仓库（需要先在腾讯云控制台获取登录凭证）
docker login ccr.ccs.tencentyun.com --username=<你的用户名>
# 输入密码（在腾讯云容器镜像服务控制台获取）
```

### 步骤3: 创建 Docker Buildx 构建器

```bash
# 创建新的 buildx 构建器（只需执行一次）
docker buildx create --name mybuilder --use

# 查看构建器信息
docker buildx inspect mybuilder --bootstrap
```

### 步骤4: 构建并推送镜像到腾讯云

#### 方式1: 构建 amd64 架构镜像（推荐）

适用于大多数云托管环境：

```bash
# 进入后端目录
cd backend

# 构建并推送 amd64 架构镜像
docker buildx build --platform linux/amd64 \
  -t ccr.ccs.tencentyun.com/tcb-100044998095-isls/ca-agxsodfx_cost-sharing:cost-sharing-v4 \
  --push \
  .
```

**说明**:
- `--platform linux/amd64`: 指定构建 amd64 架构的镜像
- `-t`: 指定镜像标签（替换为你的腾讯云镜像仓库地址）
- `--push`: 构建完成后自动推送到镜像仓库
- `.`: 当前目录作为构建上下文

#### 方式2: 构建多架构镜像（可选）

如果需要同时支持 amd64 和 arm64：

```bash
docker buildx build --platform linux/amd64,linux/arm64 \
  -t ccr.ccs.tencentyun.com/tcb-100044998095-isls/ca-agxsodfx_cost-sharing:cost-sharing-v4 \
  --push \
  .
```

#### 方式3: 构建不同版本标签

使用版本号管理镜像：

```bash
# 构建并推送带版本号的镜像
docker buildx build --platform linux/amd64 \
  -t ccr.ccs.tencentyun.com/tcb-100044998095-isls/ca-agxsodfx_cost-sharing:v1.0.0 \
  -t ccr.ccs.tencentyun.com/tcb-100044998095-isls/ca-agxsodfx_cost-sharing:latest \
  --push \
  .
```

### 步骤5: 验证镜像推送成功

```bash
# 查看本地镜像
docker images | grep cost-sharing

# 在腾讯云容器镜像服务控制台查看镜像是否已上传
# 地址: https://console.cloud.tencent.com/tcr
```

### 步骤6: 在微信小程序云托管中部署

1. **登录微信小程序后台**
   - 访问: https://mp.weixin.qq.com
   - 进入"云开发" → "云托管"

2. **创建服务或更新版本**
   - 选择现有服务或创建新服务
   - 点击"新建版本"

3. **配置镜像信息**
   - **镜像地址**: `ccr.ccs.tencentyun.com/tcb-100044998095-isls/ca-agxsodfx_cost-sharing`
   - **镜像标签**: `cost-sharing-v4`（或你的版本号）
   - **端口**: `8080`

4. **配置服务参数**
   - **CPU**: 0.5 核或以上
   - **内存**: 1GB 或以上
   - **实例数**: 1-5 个（根据需求调整）
   - **环境变量**（如需要）:
     ```
     SPRING_DATASOURCE_URL=jdbc:mysql://your-db:3306/cost_sharing_db
     SPRING_DATASOURCE_USERNAME=root
     SPRING_DATASOURCE_PASSWORD=your-password
     ```

5. **部署版本**
   - 点击"部署"按钮
   - 等待部署完成（通常需要 2-5 分钟）

6. **测试接口**
   - 复制云托管提供的访问域名
   - 测试接口: `https://your-cloud-domain/api/cards`

---

## 🔧 常见问题排查

### 问题1: ImagePullBackOff 错误

**错误信息**:
```
Error: ImagePullBackOff
no match for platform in manifest: not found
```

**原因**: 镜像平台架构不匹配

**解决方法**:
1. 确保使用 `docker buildx build --platform linux/amd64` 构建
2. 重新构建并推送镜像
3. 在云托管控制台重新部署

```bash
# 删除旧的构建器
docker buildx rm mybuilder

# 重新创建构建器
docker buildx create --name mybuilder --use

# 重新构建并推送
cd backend
docker buildx build --platform linux/amd64 \
  -t ccr.ccs.tencentyun.com/tcb-100044998095-isls/ca-agxsodfx_cost-sharing:cost-sharing-v4 \
  --push \
  .
```

### 问题2: 镜像推送失败

**错误信息**: `unauthorized: authentication required`

**解决方法**:
```bash
# 重新登录腾讯云容器镜像仓库
docker logout ccr.ccs.tencentyun.com
docker login ccr.ccs.tencentyun.com --username=<用户名>
```

### 问题3: 构建过程中下载依赖失败

**错误信息**: Maven 依赖下载超时

**解决方法**:

在 `pom.xml` 中配置国内镜像源：

```xml
<repositories>
    <repository>
        <id>aliyun</id>
        <url>https://maven.aliyun.com/repository/public</url>
    </repository>
</repositories>
```

或者增加超时时间重试：

```bash
docker buildx build --platform linux/amd64 \
  -t ccr.ccs.tencentyun.com/tcb-100044998095-isls/ca-agxsodfx_cost-sharing:cost-sharing-v4 \
  --push \
  --progress=plain \
  .
```

### 问题4: 服务启动失败

**检查步骤**:

1. 在云托管控制台查看日志
2. 检查环境变量配置
3. 确认数据库连接是否正确
4. 检查端口配置（必须是 8080）

---

## 💡 最佳实践

### 1. 使用版本标签管理镜像

```bash
# 使用语义化版本号
docker buildx build --platform linux/amd64 \
  -t ccr.ccs.tencentyun.com/tcb-100044998095-isls/ca-agxsodfx_cost-sharing:v1.0.0 \
  -t ccr.ccs.tencentyun.com/tcb-100044998095-isls/ca-agxsodfx_cost-sharing:latest \
  --push \
  .
```

### 2. 优化镜像构建速度

```dockerfile
# 在 Dockerfile 中使用多阶段构建
# 将依赖下载和代码编译分开，利用 Docker 缓存
```

### 3. 配置健康检查

在 Dockerfile 中添加健康检查：

```dockerfile
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1
```

### 4. 环境变量管理

不要在 Dockerfile 中硬编码敏感信息，使用环境变量：

```yaml
# 在云托管控制台配置环境变量
MYSQL_HOST=your-db-host
MYSQL_PORT=3306
MYSQL_DATABASE=cost_sharing_db
MYSQL_USERNAME=root
MYSQL_PASSWORD=your-secure-password
```

---

## 📝 快速命令参考

### Docker Buildx 常用命令

```bash
# 查看所有构建器
docker buildx ls

# 创建新构建器
docker buildx create --name mybuilder --use

# 删除构建器
docker buildx rm mybuilder

# 查看构建器详情
docker buildx inspect mybuilder

# 清理构建缓存
docker buildx prune
```

### 镜像管理命令

```bash
# 登录腾讯云镜像仓库
docker login ccr.ccs.tencentyun.com

# 查看本地镜像
docker images | grep cost-sharing

# 删除本地镜像
docker rmi <镜像ID>

# 拉取镜像（测试）
docker pull ccr.ccs.tencentyun.com/tcb-100044998095-isls/ca-agxsodfx_cost-sharing:cost-sharing-v4
```

---

## 🔄 更新应用流程

### 代码更新后重新部署

```bash
# 1. 进入后端目录
cd backend

# 2. 确保代码已更新
git pull  # 如果使用 Git

# 3. 构建新版本镜像（使用新的版本号）
docker buildx build --platform linux/amd64 \
  -t ccr.ccs.tencentyun.com/tcb-100044998095-isls/ca-agxsodfx_cost-sharing:cost-sharing-v5 \
  --push \
  .

# 4. 在微信小程序云托管控制台创建新版本并部署
```

### 回滚到旧版本

如果新版本出现问题，在云托管控制台切换到旧版本即可：

1. 进入"版本管理"
2. 选择旧版本
3. 点击"切换版本"

---

## 📊 监控和日志

### 查看应用日志

在微信小程序云托管控制台：

1. 进入"服务详情"
2. 点击"日志"
3. 查看实时日志或历史日志

### 监控指标

关注以下指标：

- **CPU 使用率**: 建议 < 80%
- **内存使用率**: 建议 < 80%
- **请求响应时间**: 建议 < 1s
- **错误率**: 建议 < 1%

---

## ✅ 部署完成检查清单

- [ ] Dockerfile 已正确配置（无 `--platform` 参数）
- [ ] Docker Buildx 构建器已创建
- [ ] 镜像已成功推送到腾讯云 CCR
- [ ] 在云托管控制台创建了新版本
- [ ] 服务已成功启动并运行
- [ ] API 接口可以正常访问
- [ ] 环境变量已正确配置
- [ ] 日志显示正常

---

## 📚 相关文档

- [微信小程序云托管文档](https://developers.weixin.qq.com/miniprogram/dev/wxcloud/guide/container/)
- [腾讯云容器镜像服务文档](https://cloud.tencent.com/document/product/1141)
- [Docker Buildx 文档](https://docs.docker.com/buildx/working-with-buildx/)

---

## 🆘 需要帮助？

遇到问题时的检查步骤：

1. 查看云托管部署日志
2. 检查镜像是否正确推送
3. 验证 Dockerfile 配置
4. 确认构建平台是否为 `linux/amd64`
5. 检查环境变量配置

---

**文档版本**: v1.0
**最后更新**: 2026-01-16
**适用平台**: 微信小程序云托管
**镜像架构**: linux/amd64
