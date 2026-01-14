# 项目部署指南

## 生产环境部署步骤

### 1. 服务器准备

确保服务器已安装：
- Java 17 或更高版本
- MySQL 8.0 或更高版本
- Maven 3.6+ （如果需要在服务器上编译）

### 2. 数据库初始化

```bash
# 登录 MySQL
mysql -u root -p

# 执行初始化脚本
source /path/to/backend/src/main/resources/schema.sql
```

### 3. 修改生产环境配置

创建 `application-prod.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://your-db-host:3306/cost_sharing_db?useUnicode=true&characterEncoding=utf8&useSSL=true&serverTimezone=Asia/Shanghai
    username: your_username
    password: your_password

  jpa:
    hibernate:
      ddl-auto: validate  # 生产环境使用 validate
    show-sql: false  # 生产环境关闭SQL日志

server:
  port: 8080

logging:
  level:
    com.costsharing.calculator: INFO
```

### 4. 打包项目

```bash
cd backend
mvn clean package -DskipTests
```

### 5. 部署运行

方式一：直接运行jar包
```bash
java -jar -Dspring.profiles.active=prod target/cost-sharing-backend-1.0.0.jar
```

方式二：使用系统服务（推荐）

创建 systemd 服务文件 `/etc/systemd/system/cost-sharing.service`：

```ini
[Unit]
Description=Cost Sharing Calculator Backend
After=syslog.target network.target

[Service]
Type=simple
User=your-user
WorkingDirectory=/path/to/backend
ExecStart=/usr/bin/java -jar -Dspring.profiles.active=prod /path/to/backend/target/cost-sharing-backend-1.0.0.jar
SuccessExitStatus=143
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
```

启动服务：
```bash
sudo systemctl daemon-reload
sudo systemctl enable cost-sharing
sudo systemctl start cost-sharing
sudo systemctl status cost-sharing
```

### 6. Nginx 反向代理（可选）

```nginx
server {
    listen 80;
    server_name your-domain.com;

    location /api/ {
        proxy_pass http://localhost:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

### 7. 配置SSL证书（推荐）

使用 Let's Encrypt 免费证书：

```bash
sudo certbot --nginx -d your-domain.com
```

## 前端配置

修改前端 API 地址：

```javascript
// src/api/api.js
const API_BASE_URL = 'https://your-domain.com/api'
```

## 监控和日志

查看应用日志：
```bash
sudo journalctl -u cost-sharing -f
```

或查看文件日志（如果配置了文件输出）：
```bash
tail -f /var/log/cost-sharing/app.log
```

## 性能优化建议

1. 配置数据库连接池大小
2. 启用 JVM 性能调优参数
3. 配置应用缓存
4. 使用 CDN 加速静态资源

## 安全建议

1. 使用强密码
2. 定期更新依赖包
3. 配置防火墙规则
4. 限制API访问频率
5. 启用HTTPS
6. 配置CORS白名单

## 备份策略

定期备份数据库：
```bash
# 创建备份脚本 /opt/backup/backup-db.sh
#!/bin/bash
DATE=$(date +%Y%m%d_%H%M%S)
mysqldump -u username -p'password' cost_sharing_db > /opt/backup/cost_sharing_db_$DATE.sql
# 保留最近7天的备份
find /opt/backup -name "cost_sharing_db_*.sql" -mtime +7 -delete
```

添加到 crontab：
```bash
# 每天凌晨2点备份
0 2 * * * /opt/backup/backup-db.sh
```

## 故障排查

1. 检查服务状态：`systemctl status cost-sharing`
2. 查看日志：`journalctl -u cost-sharing -n 100`
3. 测试数据库连接：`mysql -h host -u user -p`
4. 检查端口占用：`netstat -tlnp | grep 8080`
5. 测试API接口：`curl http://localhost:8080/api/cards`
