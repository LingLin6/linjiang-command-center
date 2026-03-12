#!/bin/bash
# 记录经验教训

set -e

PROJECT_ROOT="$HOME/.openclaw/workspace/projects/linjiang-command-center"
LESSONS_FILE="${PROJECT_ROOT}/LESSONS.md"

# 显示帮助
if [ "$1" = "-h" ] || [ "$1" = "--help" ]; then
    cat <<EOF
用法: $0 <problem> <cause> <solution> [prevention]

参数:
  problem     问题描述
  cause       根本原因
  solution    解决方案
  prevention  预防措施（可选）

示例:
  $0 "WebSocket 内存泄漏" "连接未正确关闭" "在 onDestroy() 中显式关闭" "所有网络连接必须在生命周期结束时关闭"
EOF
    exit 0
fi

PROBLEM=$1
CAUSE=$2
SOLUTION=$3
PREVENTION=${4:-"待补充"}

TIMESTAMP=$(date '+%Y-%m-%d %H:%M')
LESSON_NUM=$(grep -c "^## 教训 #" "$LESSONS_FILE" 2>/dev/null || echo "0")
LESSON_NUM=$((LESSON_NUM + 1))

# 格式化编号
LESSON_ID=$(printf "%03d" $LESSON_NUM)

echo "📝 记录教训 #$LESSON_ID: $PROBLEM"

# 追加到 LESSONS.md
cat >> "$LESSONS_FILE" <<EOF

## 教训 #$LESSON_ID：$PROBLEM
- 时间：$TIMESTAMP
- 问题：$PROBLEM
- 原因：$CAUSE
- 解决：$SOLUTION
- 预防：$PREVENTION

EOF

echo "✅ 已记录到 LESSONS.md"
