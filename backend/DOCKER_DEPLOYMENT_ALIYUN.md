# Docker 部署到阿里云服务器指南

## 📋 前置准备

### 1. 阿里云服务器要求

- **操作系统**: Ubuntu 20.04/22.04 或 CentOS 7/8
- **配置建议**:
  - CPU: 2核及以上
  - 内存: 4GB及以上
  - 硬盘: 40GB及以上
- **网络**: 开放端口 80, 443, 8080, 3306（可选）

### 2. 域名准备（可选）

- 在阿里云购买域名
- 完成域名备案（中国大陆服务器必须）
- 配置域名解析指向服务器IP

---

## 🚀 部署步骤

### 步骤1: 连接到阿里云服务器

```bash
# 使用SSH连接到服务器
ssh root@your-server-ip

# 或使用密钥登录
ssh -i /path/to/your-key.pem root@your-server-ip
```

### 步骤2: 安装Docker和Docker Compose

#### Ubuntu系统

```bash
# 更新软件包
sudo apt update
sudo apt upgrade -y

# 安装必要的依赖
sudo apt install -y apt-transport-https ca-certificates curl software-properties-common

# 添加Docker官方GPG密钥
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg

# 添加Docker仓库
echo "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

# 安装Docker
sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io

# 安装Docker Compose
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# 验证安装
docker --version
docker-compose --version
```

#### CentOS系统

```bash
# 更新系统
sudo yum update -y

# 安装必要的依赖
sudo yum install -y yum-utils device-mapper-persistent-data lvm2

# 添加Docker仓库
sudo yum-config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo

# 安装Docker
sudo yum install -y docker-ce docker-ce-cli containerd.io

# 启动Docker
sudo systemctl start docker
sudo systemctl enable docker

# 安装Docker Compose
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# 验证安装
docker --version
docker-compose --version
```

### 步骤3: 配置阿里云安全组

在阿里云控制台配置安全组规则，开放以下端口：

| 端口 | 协议 | 说明 |
|------|------|------|
| 22 | TCP | SSH连接 |
| 80 | TCP | HTTP访问 |
| 443 | TCP | HTTPS访问 |
| 8080 | TCP | 后端API（可选，建议通过Nginx代理） |

### 步骤4: 上传项目文件到服务器

#### 方式1: 使用Git（推荐）

```bash
# 在服务器上安装Git
sudo apt install -y git  # Ubuntu
# 或
sudo yum install -y git  # CentOS

# 创建项目目录
mkdir -p /opt/cost-sharing-calculator
cd /opt/cost-sharing-calculator

# 克隆项目（如果项目在Git仓库）
git clone https://github.com/your-username/cost-sharing-calculator.git .

# 或者只上传backend目录
```

#### 方式2: 使用SCP上传

```bash
# 在本地电脑执行（打包backend目录）
cd /Users/lizijian/SDT/cost-sharing-calculator
tar -czf backend.tar.gz backend/

# 上传到服务器
scp backend.tar.gz root@your-server-ip:/opt/

# 在服务器上解压
ssh root@your-server-ip
cd /opt
tar -xzf backend.tar.gz
cd backend
```

#### 方式3: 使用rsync同步

```bash
# 在本地电脑执行
rsync -avz --progress backend/ root@your-server-ip:/opt/cost-sharing-calculator/backend/
```

### 步骤5: 配置生产环境

```bash
# 进入项目目录
cd /opt/cost-sharing-calculator/backend

# 修改docker-compose.yml配置生产环境密码
vi docker-compose.yml
```

修改以下配置（重要！）：

```yaml
services:
  mysql:
    environment:
      MYSQL_ROOT_PASSWORD: your-strong-password-here  # 修改为强密码
      MYSQL_DATABASE: cost_sharing_db

  backend:
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/cost_sharing_db?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
      SPRING_DATASOURCE_USERNAME: root
      SPRING_DATASOURCE_PASSWORD: your-strong-password-here  # 与上面的密码一致
      JAVA_OPTS: -Xmx1024m -Xms512m  # 根据服务器内存调整
```

### 步骤6: 启动Docker服务

```bash
# 确保在backend目录
cd /opt/cost-sharing-calculator/backend

# 构建并启动服务（首次启动需要5-10分钟）
docker-compose up -d

# 查看启动日志
docker-compose logs -f

# 等待服务启动完成后，按 Ctrl+C 退出日志查看

# 检查服务状态
docker-compose ps

# 测试API是否正常
curl http://localhost:8080/api/cards
```

如果看到JSON响应，说明服务启动成功！

### 步骤7: 配置Nginx反向代理（推荐）

#### 安装Nginx

```bash
# Ubuntu
sudo apt install -y nginx

# CentOS
sudo yum install -y nginx
```

#### 配置Nginx

创建配置文件：

```bash
sudo vi /etc/nginx/sites-available/cost-sharing
# CentOS用户使用: sudo vi /etc/nginx/conf.d/cost-sharing.conf
```

添加以下配置：

```nginx
server {
    listen 80;
    server_name your-domain.com;  # 替换为你的域名或服务器IP

    # 日志配置
    access_log /var/log/nginx/cost-sharing-access.log;
    error_log /var/log/nginx/cost-sharing-error.log;

    # API代理
    location /api/ {
        proxy_pass http://localhost:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # 超时设置
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }

    # 前端静态文件（如果需要）
    location / {
        root /var/www/cost-sharing;
        index index.html;
        try_files $uri $uri/ /index.html;
    }
}
```

#### 启用配置并重启Nginx

```bash
# Ubuntu - 创建软链接
sudo ln -s /etc/nginx/sites-available/cost-sharing /etc/nginx/sites-enabled/

# 测试配置
sudo nginx -t

# 重启Nginx
sudo systemctl restart nginx
sudo systemctl enable nginx

# 检查状态
sudo systemctl status nginx
```

现在可以通过 `http://your-domain.com/api/cards` 访问API了！

### 步骤8: 配置SSL证书（推荐）

使用Let's Encrypt免费SSL证书：

```bash
# 安装Certbot
# Ubuntu
sudo apt install -y certbot python3-certbot-nginx

# CentOS
sudo yum install -y certbot python3-certbot-nginx

# 自动配置SSL证书
sudo certbot --nginx -d your-domain.com

# 按照提示输入邮箱并同意服务条款

# 测试自动续期
sudo certbot renew --dry-run
```

Certbot会自动修改Nginx配置，添加SSL支持。现在可以通过 `https://your-domain.com/api/cards` 访问了！

### 步骤9: 设置自动启动和监控

#### 配置Docker自动重启

```bash
# 修改docker-compose.yml，确保包含restart策略
cd /opt/cost-sharing-calculator/backend
vi docker-compose.yml
```

确保每个服务都有 `restart: always`：

```yaml
services:
  mysql:
    restart: always
    # ...其他配置

  backend:
    restart: always
    # ...其他配置
```

重新启动服务：

```bash
docker-compose down
docker-compose up -d
```

#### 监控和日志管理

```bash
# 查看所有容器状态
docker-compose ps

# 查看后端日志
docker-compose logs -f backend

# 查看MySQL日志
docker-compose logs -f mysql

# 查看最近100行日志
docker-compose logs --tail=100 backend

# 查看Nginx日志
sudo tail -f /var/log/nginx/cost-sharing-access.log
sudo tail -f /var/log/nginx/cost-sharing-error.log
```

---

## 🔧 常见问题排查

### 问题1: 容器启动失败

```bash
# 查看详细错误日志
docker-compose logs backend

# 检查端口占用
sudo netstat -tlnp | grep 8080
sudo netstat -tlnp | grep 3306

# 重新构建镜像
docker-compose down
docker-compose build --no-cache
docker-compose up -d
```

### 问题2: 数据库连接失败

```bash
# 检查MySQL容器状态
docker-compose ps mysql

# 进入MySQL容器检查
docker exec -it cost-sharing-mysql mysql -u root -p

# 检查网络连接
docker network ls
docker network inspect backend_cost-sharing-network
```

### 问题3: API无法访问

```bash
# 测试本地API
curl http://localhost:8080/api/cards

# 检查防火墙
sudo ufw status  # Ubuntu
sudo firewall-cmd --list-all  # CentOS

# 检查Nginx配置
sudo nginx -t
sudo systemctl status nginx
```

### 问题4: 内存不足

```bash
# 查看容器资源占用
docker stats

# 调整JVM内存（修改docker-compose.yml）
JAVA_OPTS: -Xmx512m -Xms256m  # 降低内存使用
```

---

## 💾 数据备份策略

### 自动备份脚本

创建备份脚本：

```bash
sudo mkdir -p /opt/backups
sudo vi /opt/backups/backup-db.sh
```

添加以下内容：

```bash
#!/bin/bash
# 数据库备份脚本

DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR="/opt/backups"
MYSQL_CONTAINER="cost-sharing-mysql"
MYSQL_PASSWORD="your-password"  # 替换为实际密码

# 创建备份
docker exec $MYSQL_CONTAINER mysqldump -u root -p$MYSQL_PASSWORD cost_sharing_db > $BACKUP_DIR/cost_sharing_db_$DATE.sql

# 压缩备份
gzip $BACKUP_DIR/cost_sharing_db_$DATE.sql

# 删除7天前的备份
find $BACKUP_DIR -name "cost_sharing_db_*.sql.gz" -mtime +7 -delete

echo "Backup completed: cost_sharing_db_$DATE.sql.gz"
```

设置权限并添加定时任务：

```bash
# 设置执行权限
sudo chmod +x /opt/backups/backup-db.sh

# 添加到crontab（每天凌晨2点备份）
sudo crontab -e
# 添加以下行：
0 2 * * * /opt/backups/backup-db.sh >> /var/log/backup.log 2>&1
```

### 手动备份

```bash
# 备份数据库
docker exec cost-sharing-mysql mysqldump -u root -p123456 cost_sharing_db > backup.sql

# 备份Docker数据卷
docker run --rm -v backend_mysql-data:/data -v $(pwd):/backup ubuntu tar czf /backup/mysql-data-backup.tar.gz /data
```

---

## 🔄 更新和维护

### 更新应用代码

```bash
# 进入项目目录
cd /opt/cost-sharing-calculator/backend

# 拉取最新代码（如果使用Git）
git pull

# 或重新上传文件（使用scp/rsync）

# 重新构建并启动
docker-compose down
docker-compose build --no-cache backend
docker-compose up -d

# 查看启动日志
docker-compose logs -f backend
```

### 清理Docker资源

```bash
# 清理未使用的镜像
docker image prune -a

# 清理未使用的容器
docker container prune

# 清理未使用的数据卷（谨慎使用！）
docker volume prune

# 查看磁盘占用
docker system df
```

---

## ⚡ 性能优化建议

### 1. JVM参数优化

根据服务器内存调整（修改docker-compose.yml）：

```yaml
# 4GB内存服务器
JAVA_OPTS: -Xmx2048m -Xms1024m -XX:+UseG1GC

# 2GB内存服务器
JAVA_OPTS: -Xmx1024m -Xms512m -XX:+UseG1GC

# 1GB内存服务器
JAVA_OPTS: -Xmx512m -Xms256m
```

### 2. MySQL优化

创建自定义MySQL配置：

```bash
vi /opt/cost-sharing-calculator/backend/mysql.cnf
```

添加配置：

```ini
[mysqld]
max_connections = 200
innodb_buffer_pool_size = 512M
innodb_log_file_size = 128M
query_cache_size = 32M
```

修改docker-compose.yml挂载配置文件：

```yaml
mysql:
  volumes:
    - ./mysql.cnf:/etc/mysql/conf.d/custom.cnf
```

### 3. Nginx缓存优化

在Nginx配置中添加缓存：

```nginx
# 在http块中添加
proxy_cache_path /var/cache/nginx levels=1:2 keys_zone=api_cache:10m max_size=100m inactive=60m;

# 在location块中添加
location /api/ {
    proxy_cache api_cache;
    proxy_cache_valid 200 5m;
    proxy_cache_key "$scheme$request_method$host$request_uri";
    # ...其他配置
}
```

---

## 📝 快速命令参考

### 常用Docker命令

```bash
# 启动服务
docker-compose up -d

# 停止服务
docker-compose down

# 重启服务
docker-compose restart

# 查看状态
docker-compose ps

# 查看日志
docker-compose logs -f

# 重新构建
docker-compose build --no-cache

# 进入容器
docker exec -it cost-sharing-backend sh
docker exec -it cost-sharing-mysql bash
```

### 常用维护命令

```bash
# 查看服务状态
systemctl status nginx
systemctl status docker

# 重启服务
systemctl restart nginx
systemctl restart docker

# 查看端口占用
netstat -tlnp | grep 8080

# 查看磁盘使用
df -h
docker system df

# 查看内存使用
free -h
docker stats
```

---

## ✅ 部署完成检查清单

部署完成后，请确认以下项目：

- [ ] Docker和Docker Compose已安装
- [ ] 阿里云安全组已配置（开放80、443端口）
- [ ] 项目文件已上传到服务器
- [ ] docker-compose.yml中的密码已修改
- [ ] Docker服务已启动并运行正常
- [ ] API可以正常访问（curl测试）
- [ ] Nginx已配置并运行
- [ ] SSL证书已配置（如果使用域名）
- [ ] 自动备份脚本已设置
- [ ] 日志监控已配置

---

## 📚 总结

恭喜！你已经成功将项目部署到阿里云服务器。

### 关键访问地址

- **API地址**: `https://your-domain.com/api`
- **测试接口**: `https://your-domain.com/api/cards`

### 下一步

1. **前端配置**: 修改前端项目中的API地址指向你的服务器
2. **监控告警**: 配置服务器监控和告警通知
3. **性能测试**: 进行压力测试，确保服务稳定
4. **文档更新**: 记录你的部署配置和自定义修改

### 需要帮助？

- 查看日志: `docker-compose logs -f`
- 检查状态: `docker-compose ps`
- 重启服务: `docker-compose restart`

---

**文档版本**: v1.0
**最后更新**: 2026-01-14
**适用系统**: Ubuntu 20.04+, CentOS 7+
