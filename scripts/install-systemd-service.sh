#!/bin/bash
# 安装 OpenClaw Relay Client systemd 服务

set -e

echo "🔧 安装 OpenClaw Relay Client 服务"
echo ""

# 配置
SERVICE_FILE="openclaw-relay-client.service"
SERVICE_PATH="/etc/systemd/system/$SERVICE_FILE"
CLIENT_DIR="$HOME/.openclaw/workspace/projects/linjiang-command-center/src/openclaw-relay-client"

# 检查是否在正确的目录
if [ ! -f "$CLIENT_DIR/$SERVICE_FILE" ]; then
  echo "❌ 错误: 找不到服务文件 $CLIENT_DIR/$SERVICE_FILE"
  exit 1
fi

# 检查是否已安装依赖
if [ ! -d "$CLIENT_DIR/node_modules" ]; then
  echo "📥 安装依赖..."
  cd "$CLIENT_DIR"
  npm install
  cd - > /dev/null
fi

# 复制服务文件
echo "📋 复制服务文件..."
sudo cp "$CLIENT_DIR/$SERVICE_FILE" "$SERVICE_PATH"

# 重新加载 systemd
echo "🔄 重新加载 systemd..."
sudo systemctl daemon-reload

# 启动服务
echo "▶️  启动服务..."
sudo systemctl start openclaw-relay-client

# 检查状态
echo ""
echo "📊 服务状态:"
sudo systemctl status openclaw-relay-client --no-pager

# 询问是否设置开机自启
echo ""
read -p "是否设置开机自启？(y/n) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
  echo "✅ 设置开机自启..."
  sudo systemctl enable openclaw-relay-client
  echo "✅ 已设置开机自启"
fi

echo ""
echo "✅ 安装完成！"
echo ""
echo "常用命令:"
echo "  查看状态: sudo systemctl status openclaw-relay-client"
echo "  查看日志: sudo journalctl -u openclaw-relay-client -f"
echo "  停止服务: sudo systemctl stop openclaw-relay-client"
echo "  重启服务: sudo systemctl restart openclaw-relay-client"
echo "  禁用自启: sudo systemctl disable openclaw-relay-client"
