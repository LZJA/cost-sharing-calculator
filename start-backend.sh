#!/bin/bash
set -euo pipefail

cd "$(dirname "$0")/backend"

if ! command -v docker >/dev/null 2>&1 || ! docker info >/dev/null 2>&1; then
    echo "请先安装并启动 Docker Desktop。"
    exit 1
fi
if docker compose version >/dev/null 2>&1; then
    compose=(docker compose)
elif command -v docker-compose >/dev/null 2>&1; then
    compose=(docker-compose)
else
    echo "未找到 Docker Compose，请安装 Docker Compose 插件。"
    exit 1
fi

if [ ! -f .env ]; then
    (umask 077; cp .env.example .env)
    echo "已生成 backend/.env，正在检查微信登录参数…"
fi

# 只检查解析后的字段是否为空，不输出配置内容或密钥。
"${compose[@]}" config --format json | python3 -c '
import json, sys
values = json.load(sys.stdin)["services"]["backend"].get("environment", {})
missing = [key for key in ("WECHAT_APP_ID", "WECHAT_APP_SECRET") if not str(values.get(key) or "").strip()]
if missing:
    print("缺少微信登录参数：" + "、".join(missing))
    print("请在 backend/.env 中填写，或通过当前终端环境变量提供，再重新运行脚本。")
    sys.exit(1)
'

echo "正在构建并启动后端服务…"
"${compose[@]}" up -d --build

echo "正在等待后端就绪…"
for ((attempt = 0; attempt < 60; attempt++)); do
    # 未带会话访问受保护接口，应返回 401；旧 cards 接口已停用。
    status=$(curl -s -o /dev/null -w '%{http_code}' --max-time 2 http://localhost:8080/api/auth/me || true)
    if [ "$status" = "401" ]; then
        echo "后端已启动：http://localhost:8080/api"
        echo "请在微信开发者工具中重新编译，完成真实微信登录。"
        echo "查看日志：cd backend && ${compose[*]} logs -f backend"
        exit 0
    fi
    sleep 1
done

echo "后端启动超时，请检查：cd backend && ${compose[*]} logs backend"
exit 1
