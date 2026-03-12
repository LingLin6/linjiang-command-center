#!/bin/bash
# 生成每日进度报告

set -e

PROJECT_ROOT="$HOME/.openclaw/workspace/projects/linjiang-command-center"
STATUS_FILE="${PROJECT_ROOT}/STATUS.md"
TESTING_FILE="${PROJECT_ROOT}/TESTING.md"
CHANGELOG_FILE="${PROJECT_ROOT}/CHANGELOG.md"

TODAY=$(date '+%Y-%m-%d')
REPORT_FILE="${PROJECT_ROOT}/.checkpoints/daily-report-${TODAY}.md"

echo "📊 生成每日报告: $TODAY"

# 生成报告
cat > "$REPORT_FILE" <<EOF
# 翎绛日报 - $TODAY

## ✅ 今日完成
$(grep "^###.*$TODAY" "$CHANGELOG_FILE" 2>/dev/null | sed 's/### /- /' || echo "- 无")

## 📋 明日计划
（从 STATUS.md 提取下一步）

## ⚠️ 阻塞点
（从 STATUS.md 提取）

## 📊 偏差记录
（从 STATUS.md 提取）

## 🧪 测试情况
今日测试: $(grep -c "$TODAY" "$TESTING_FILE" 2>/dev/null || echo "0") 个
通过: $(grep "$TODAY" "$TESTING_FILE" 2>/dev/null | grep -c "✅" || echo "0")
失败: $(grep "$TODAY" "$TESTING_FILE" 2>/dev/null | grep -c "❌" || echo "0")

## 🎯 总体进度
（从 STATUS.md 提取）

---
生成时间: $(date '+%Y-%m-%d %H:%M:%S')
EOF

echo "✅ 报告已生成: $REPORT_FILE"
cat "$REPORT_FILE"

# 可选：发送通知
# curl -X POST http://127.0.0.1:18790/api/internal/notify \
#   -H "Content-Type: application/json" \
#   -d "{\"title\":\"翎绛日报\",\"message\":\"$(cat $REPORT_FILE)\",\"type\":\"agent\",\"severity\":\"info\"}"
