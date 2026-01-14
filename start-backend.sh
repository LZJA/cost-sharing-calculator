#!/bin/bash

echo "🐳 成本分摊计算器 - Docker 启动脚本"
echo "========================================"

# 检查 Docker 是否安装
if ! command -v docker &> /dev/null; then
    echo "❌ 错误: Docker 未安装"
    echo "请先安装 Docker Desktop: https://www.docker.com/products/docker-desktop"
    exit 1
fi

# 检查 Docker 是否运行
if ! docker info &> /dev/null; then
    echo "❌ 错误: Docker 未运行"
    echo "请启动 Docker Desktop 后再试"
    exit 1
fi

echo "✅ Docker 已就绪"
echo ""

# 进入后端目录
cd "$(dirname "$0")/backend" || exit 1

# 停止并删除旧容器
echo "🧹 清理旧容器..."
docker-compose down

# 启动服务
echo ""
echo "🚀 启动服务（首次启动需要下载镜像和编译代码，可能需要5-10分钟）..."
echo ""

docker-compose up -d

# 等待服务启动
echo ""
echo "⏳ 等待服务启动..."
sleep 5

# 检查服务状态
echo ""
echo "📊 服务状态："
docker-compose ps

# 等待后端服务就绪
echo ""
echo "⏳ 等待后端服务就绪（最多等待60秒）..."
for i in {1..60}; do
    if curl -s http://localhost:8080/api/cards > /dev/null 2>&1; then
        echo ""
        echo "✅ 后端服务启动成功！"
        echo ""
        echo "=========================================="
        echo "🎉 所有服务已启动！"
        echo "=========================================="
        echo ""
        echo "📍 后端 API 地址: http://localhost:8080/api"
        echo "📍 MySQL 地址: localhost:3306"
        echo "   - 用户名: root"
        echo "   - 密码: 123456"
        echo "   - 数据库: cost_sharing_db"
        echo ""
        echo "🧪 测试接口:"
        echo "   curl http://localhost:8080/api/cards"
        echo "   或在浏览器打开: http://localhost:8080/api/cards"
        echo ""
        echo "📋 查看日志:"
        echo "   docker-compose logs -f backend"
        echo ""
        echo "🛑 停止服务:"
        echo "   ./stop-backend.sh"
        echo "   或执行: docker-compose down"
        echo ""
        exit 0
    fi
    echo -n "."
    sleep 1
done

echo ""
echo "⚠️  后端服务启动超时，请检查日志："
echo "   docker-compose logs backend"
echo ""
echo "可能的原因："
echo "1. 首次启动需要下载依赖，时间较长"
echo "2. 端口被占用（8080 或 3306）"
echo "3. Docker 资源不足"
echo ""
echo "查看实时日志："
echo "   docker-compose logs -f"
