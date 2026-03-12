#!/bin/bash
# 部署中继服务器更新到翎云 VPS

set -e

echo "🚀 部署中继服务器更新"
echo ""

# 配置
VPS_HOST="118.25.195.154"
VPS_USER="root"
VPS_PATH="/root/openclaw-relay"
LOCAL_PATH="src/relay-server-prototype"

# 检查本地文件
if [ ! -d "$LOCAL_PATH" ]; then
  echo "❌ 错误: 找不到 $LOCAL_PATH"
  exit 1
fi

echo "📦 打包文件..."
cd "$LOCAL_PATH"
tar -czf /tmp/relay-server-update.tar.gz .
cd - > /dev/null

echo "📤 上传到 VPS..."
scp /tmp/relay-server-update.tar.gz ${VPS_USER}@${VPS_HOST}:/tmp/

echo "🔧 在 VPS 上部署..."
ssh ${VPS_USER}@${VPS_HOST} << 'EOF'
  set -e
  
  echo "📂 创建备份..."
  cd /root/openclaw-relay
  if [ -d "relay-server-prototype" ]; then
    tar -czf backup-$(date +%Y%m%d-%H%M%S).tar.gz relay-server-prototype
  fi
  
  echo "📦 解压新版本..."
  mkdir -p relay-server-prototype
  cd relay-server-prototype
  tar -xzf /tmp/relay-server-update.tar.gz
  
  echo "📥 安装依赖..."
  npm install
  
  echo "🔄 重启服务..."
  pm2 restart openclaw-relay || pm2 start server.js --name openclaw-relay
  
  echo "✅ 部署完成"
  
  echo ""
  echo "📊 服务状态:"
  pm2 status openclaw-relay
  
  echo ""
  echo "📝 最近日志:"
  pm2 logs openclaw-relay --lines 20 --nostream
EOF

echo ""
echo "🧹 清理临时文件..."
rm /tmp/relay-server-update.tar.gz

echo ""
echo "✅ 部署完成！"
echo ""
echo "查看日志: ssh ${VPS_USER}@${VPS_HOST} 'pm2 logs openclaw-relay'"
echo "查看状态: ssh ${VPS_USER}@${VPS_HOST} 'pm2 status'"
