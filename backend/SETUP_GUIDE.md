# 后端环境配置指南（针对前端工程师）

## 环境要求

作为前端工程师，你需要安装以下工具：
1. ☕ Java JDK 17（类似于 Node.js）
2. 🗄️ MySQL 数据库（类似于 MongoDB）
3. 📦 Maven（类似于 npm）

---

## 第一步：安装 Java JDK

### Mac 系统

#### 方式1：使用 Homebrew（推荐）

```bash
# 1. 如果没有 Homebrew，先安装它（类似于 npm）
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

# 2. 安装 Java 17
brew install openjdk@17

# 3. 配置环境变量
echo 'export PATH="/opt/homebrew/opt/openjdk@17/bin:$PATH"' >> ~/.zshrc
source ~/.zshrc

# 4. 验证安装
java -version
```

你应该看到类似这样的输出：
```
openjdk version "17.0.x"
```

#### 方式2：手动下载安装

1. 访问 https://adoptium.net/
2. 下载 JDK 17 for macOS
3. 安装下载的 .pkg 文件
4. 在终端运行 `java -version` 验证

---

## 第二步：安装 MySQL 数据库

### Mac 系统

#### 方式1：使用 Homebrew（推荐）

```bash
# 1. 安装 MySQL
brew install mysql

# 2. 启动 MySQL 服务
brew services start mysql

# 3. 设置 root 密码（记住这个密码！）
mysql_secure_installation

# 按照提示操作：
# - 是否设置密码：Y（输入新密码，例如：123456）
# - 移除匿名用户：Y
# - 禁止root远程登录：Y
# - 删除测试数据库：Y
# - 重新加载权限表：Y
```

#### 方式2：手动下载安装

1. 访问 https://dev.mysql.com/downloads/mysql/
2. 下载 MySQL Community Server for macOS
3. 安装 .dmg 文件
4. 在系统偏好设置中启动 MySQL

---

## 第三步：初始化数据库

```bash
# 1. 登录 MySQL（输入你刚才设置的密码）
mysql -u root -p

# 2. 在 MySQL 命令行中执行（复制粘贴）：
CREATE DATABASE IF NOT EXISTS cost_sharing_db
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

# 3. 验证数据库创建成功
SHOW DATABASES;

# 你应该能看到 cost_sharing_db 在列表中

# 4. 退出 MySQL
exit;
```

或者直接执行 SQL 脚本：

```bash
# 在项目根目录执行
mysql -u root -p < backend/src/main/resources/schema.sql
# 输入密码后回车
```

---

## 第四步：配置后端项目

```bash
# 1. 进入后端目录
cd /Users/lizijian/SDT/cost-sharing-calculator/backend

# 2. 修改数据库密码配置
# 打开 src/main/resources/application.yml 文件
# 找到这一行：
#   password: your_password_here
# 改为你的 MySQL 密码，例如：
#   password: 123456
```

使用编辑器或命令修改：

```bash
# 使用 VS Code 打开配置文件
code src/main/resources/application.yml

# 或使用 vim
vim src/main/resources/application.yml
```

修改这部分：
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/cost_sharing_db?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: root
    password: 123456  # ← 改成你的 MySQL 密码
```

---

## 第五步：安装 Maven（可选）

Maven 已经内置在项目中（mvnw），通常不需要单独安装。

如果需要全局安装：

```bash
# Mac 使用 Homebrew
brew install maven

# 验证
mvn -version
```

---

## 第六步：启动后端服务

### 方式1：使用 Maven 命令（推荐）

```bash
# 在 backend 目录下执行
cd /Users/lizijian/SDT/cost-sharing-calculator/backend

# 首次启动（会自动下载依赖，类似于 npm install）
./mvnw spring-boot:run

# 或使用全局 Maven
mvn spring-boot:run
```

### 方式2：使用 IDE（IntelliJ IDEA 或 VS Code）

**使用 IntelliJ IDEA：**
1. 打开 IntelliJ IDEA
2. File → Open → 选择 backend 目录
3. 等待依赖下载完成
4. 找到 `CostSharingApplication.java`
5. 右键 → Run 'CostSharingApplication'

**使用 VS Code：**
1. 安装插件：Extension Pack for Java
2. 打开 backend 目录
3. 找到 `CostSharingApplication.java`
4. 点击编辑器右上角的运行按钮

---

## 第七步：验证后端服务

### 1. 检查启动日志

看到这样的输出说明启动成功：

```
========================================
成本分摊计算器后端服务启动成功！
访问地址: http://localhost:8080/api
========================================
```

### 2. 测试 API 接口

在新的终端窗口执行：

```bash
# 测试获取卡片接口
curl http://localhost:8080/api/cards

# 你应该看到类似这样的 JSON 响应：
# {"code":200,"message":"success","data":[...]}
```

或在浏览器中打开：
```
http://localhost:8080/api/cards
```

### 3. 使用 Postman 测试（推荐）

1. 下载 Postman: https://www.postman.com/downloads/
2. 创建新请求：GET `http://localhost:8080/api/cards`
3. 点击 Send
4. 查看响应数据

---

## 常见问题解决

### ❌ 问题1：端口被占用

**错误信息：** `Port 8080 is already in use`

**解决方法：**
```bash
# 查找占用端口的进程
lsof -i :8080

# 杀死进程（替换 PID 为实际的进程ID）
kill -9 <PID>

# 或修改端口
# 在 application.yml 中改为：
server:
  port: 8081  # 改成其他端口
```

### ❌ 问题2：数据库连接失败

**错误信息：** `Access denied for user 'root'@'localhost'`

**解决方法：**
1. 检查 MySQL 是否启动：`brew services list`
2. 检查密码是否正确：`mysql -u root -p`
3. 修改 `application.yml` 中的密码配置

### ❌ 问题3：Maven 下载依赖慢

**解决方法：** 配置国内镜像

```bash
# 编辑 Maven 配置文件
mkdir -p ~/.m2
vim ~/.m2/settings.xml
```

添加以下内容：
```xml
<settings>
  <mirrors>
    <mirror>
      <id>aliyun</id>
      <mirrorOf>central</mirrorOf>
      <name>Aliyun Maven</name>
      <url>https://maven.aliyun.com/repository/public</url>
    </mirror>
  </mirrors>
</settings>
```

### ❌ 问题4：Java 版本不对

**错误信息：** `Unsupported class file major version`

**解决方法：**
```bash
# 检查 Java 版本
java -version

# 如果不是 17，需要安装 JDK 17
brew install openjdk@17
```

---

## 快速启动脚本

创建一个启动脚本方便使用：

```bash
# 创建启动脚本
cat > start-backend.sh << 'EOF'
#!/bin/bash
echo "🚀 启动成本分摊计算器后端服务..."

# 检查 MySQL 是否启动
if ! pgrep -x "mysqld" > /dev/null; then
    echo "📦 启动 MySQL 服务..."
    brew services start mysql
    sleep 3
fi

# 进入后端目录
cd "$(dirname "$0")/backend"

# 启动后端服务
echo "⚡ 启动 Spring Boot 应用..."
./mvnw spring-boot:run
EOF

# 赋予执行权限
chmod +x start-backend.sh

# 使用：
./start-backend.sh
```

---

## 完整启动流程总结

```bash
# 1. 启动 MySQL（如果没有自动启动）
brew services start mysql

# 2. 进入后端目录
cd /Users/lizijian/SDT/cost-sharing-calculator/backend

# 3. 启动后端服务
./mvnw spring-boot:run

# 4. 在浏览器访问测试
# http://localhost:8080/api/cards
```

---

## 下一步：前端集成

后端启动成功后，在前端项目中：

1. 确保前端可以访问 `http://localhost:8080/api`
2. 使用提供的 API 封装文件：`src/api/costSharingApi.js`
3. 开始调用后端接口

```javascript
// 在前端页面中使用
import api from '@/api/costSharingApi.js'

// 保存账单
const bill = await api.liziBill.save({
  month: 1,
  year: 2024,
  totalDays: 31,
  waterBill: 100.50,
  electricBill: 200.75,
  gasBill: 80.25,
  ownerDays: 15
})
```

---

## 如果遇到其他问题

1. 查看后端控制台的错误日志
2. 检查 `application.yml` 配置是否正确
3. 确认 MySQL 服务正在运行
4. 确认数据库已创建且密码正确

需要帮助？把错误信息截图或复制给我！
