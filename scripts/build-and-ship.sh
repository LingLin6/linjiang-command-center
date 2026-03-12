#!/bin/bash
# 翎绛指挥中心 — 构建并发送 APK
# 规则：必须 git commit + push 后才能发 APK，没有例外

set -e

PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$PROJECT_DIR"

echo "🪶 翎绛指挥中心 — 构建流程"
echo "=========================="

# Step 1: 检查 git 状态
echo ""
echo "📋 Step 1: 检查 Git 状态..."

if [ -n "$(git status --porcelain)" ]; then
    echo "❌ 工作区有未提交的改动："
    git status --short
    echo ""
    echo "🚫 规则：发 APK 前必须 git commit + push"
    echo "请先执行: git add -A && git commit -m '...' && git push origin master"
    exit 1
fi

echo "✅ 工作区干净"

# Step 2: 检查是否已 push
AHEAD=$(git rev-list --count origin/master..HEAD 2>/dev/null || echo "unknown")
if [ "$AHEAD" != "0" ] && [ "$AHEAD" != "unknown" ]; then
    echo "❌ 本地有 $AHEAD 个 commit 未 push"
    echo "🚫 规则：发 APK 前必须 push 到 GitHub"
    echo "请先执行: git push origin master"
    exit 1
fi

echo "✅ 已同步到 GitHub"

# Step 2: 检查技术文档是否更新
echo ""
echo "📝 Step 2: 检查技术文档..."

# 检查最近 3 次 commit 中是否包含文档更新（允许分开提交）
RECENT_FILES=$(git diff-tree --no-commit-id --name-only -r HEAD~3..HEAD 2>/dev/null || git diff-tree --no-commit-id --name-only -r HEAD)

if ! echo "$RECENT_FILES" | grep -q "docs/CHANGELOG.md"; then
    echo "⚠️  警告：最近 3 次 commit 未更新 docs/CHANGELOG.md"
    echo "🚫 规则：每次发版必须更新 CHANGELOG"
    echo "请更新 CHANGELOG.md 后重新 commit + push"
    exit 1
fi

if ! echo "$RECENT_FILES" | grep -q "PROJECT.md"; then
    echo "⚠️  警告：最近 3 次 commit 未更新 PROJECT.md"
    echo "🚫 规则：里程碑状态变更必须更新 PROJECT.md"
    echo "请更新 PROJECT.md 后重新 commit + push"
    exit 1
fi

echo "✅ CHANGELOG.md 已更新"
echo "✅ PROJECT.md 已更新"

# Step 3: 构建 APK
echo ""
echo "🔨 Step 2: 构建 APK..."
cd "$PROJECT_DIR/src/android"
./gradlew assembleDebug --quiet

APK_PATH="$PROJECT_DIR/src/android/app/build/outputs/apk/debug/app-debug.apk"
if [ ! -f "$APK_PATH" ]; then
    echo "❌ APK 构建失败，文件不存在"
    exit 1
fi

APK_SIZE=$(du -h "$APK_PATH" | cut -f1)
GIT_HASH=$(cd "$PROJECT_DIR" && git rev-parse --short HEAD)
echo "✅ APK 构建成功: $APK_SIZE (commit: $GIT_HASH)"
echo ""
echo "📦 APK 路径: $APK_PATH"
echo "🚀 可以发送给翎麟了！"
