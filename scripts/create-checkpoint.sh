#!/bin/bash
# 创建项目检查点

set -e

if [ -z "$1" ]; then
    echo "用法: $0 <milestone-name>"
    echo "示例: $0 vpn-integration"
    exit 1
fi

MILESTONE=$1
TIMESTAMP=$(date +%Y%m%d-%H%M%S)
CHECKPOINT_NAME="${MILESTONE}-${TIMESTAMP}"
PROJECT_ROOT="$HOME/.openclaw/workspace/projects/linjiang-command-center"
CHECKPOINT_DIR="${PROJECT_ROOT}/.checkpoints"

mkdir -p "$CHECKPOINT_DIR"

echo "📦 创建检查点: $CHECKPOINT_NAME"

# 1. 打包源代码
echo "  → 打包源代码..."
cd "$PROJECT_ROOT"
tar -czf "${CHECKPOINT_DIR}/${CHECKPOINT_NAME}.tar.gz" \
    --exclude='.checkpoints' \
    --exclude='node_modules' \
    --exclude='.git' \
    --exclude='build' \
    --exclude='.gradle' \
    .

# 2. 保存状态快照
echo "  → 保存状态快照..."
cp STATUS.md "${CHECKPOINT_DIR}/${CHECKPOINT_NAME}-STATUS.md"
cp TESTING.md "${CHECKPOINT_DIR}/${CHECKPOINT_NAME}-TESTING.md"
cp CHANGELOG.md "${CHECKPOINT_DIR}/${CHECKPOINT_NAME}-CHANGELOG.md"

# 3. 记录 Git 信息
if [ -d .git ]; then
    echo "  → 记录 Git 信息..."
    git rev-parse HEAD > "${CHECKPOINT_DIR}/${CHECKPOINT_NAME}-commit.txt"
fi

# 4. 生成检查点清单
echo "  → 生成检查点清单..."
cat > "${CHECKPOINT_DIR}/${CHECKPOINT_NAME}-manifest.txt" <<EOF
检查点: $CHECKPOINT_NAME
里程碑: $MILESTONE
时间: $(date '+%Y-%m-%d %H:%M:%S')
Git Commit: $(git rev-parse HEAD 2>/dev/null || echo "N/A")
文件大小: $(du -h "${CHECKPOINT_DIR}/${CHECKPOINT_NAME}.tar.gz" | cut -f1)
EOF

echo "✅ 检查点创建完成: ${CHECKPOINT_DIR}/${CHECKPOINT_NAME}.tar.gz"
echo ""
cat "${CHECKPOINT_DIR}/${CHECKPOINT_NAME}-manifest.txt"
