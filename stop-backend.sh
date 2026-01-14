#!/bin/bash

echo "🛑 停止成本分摊计算器后端服务..."
echo ""

cd "$(dirname "$0")/backend" || exit 1

# 停止服务
docker-compose down

echo ""
echo "✅ 服务已停止"
echo ""
echo "💡 提示："
echo "   - 数据已保存，下次启动会保留"
echo "   - 如需完全删除（包括数据），执行："
echo "     docker-compose down -v"
