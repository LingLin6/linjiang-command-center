#!/bin/bash
# 记录测试结果

set -e

PROJECT_ROOT="$HOME/.openclaw/workspace/projects/linjiang-command-center"
TESTING_FILE="${PROJECT_ROOT}/TESTING.md"

# 显示帮助
if [ "$1" = "-h" ] || [ "$1" = "--help" ]; then
    cat <<EOF
用法: $0 <test-name> <result> <duration> [notes]

参数:
  test-name   测试名称
  result      pass/fail
  duration    用时（如 "45min"）
  notes       备注（可选）

示例:
  $0 "VPN 连接稳定性" pass "35min" "丢包率 0%"
  $0 "多实例切换" fail "10min" "第 15 次切换时崩溃"
EOF
    exit 0
fi

TEST_NAME=$1
RESULT=$2
DURATION=$3
NOTES=${4:-"无"}

TIMESTAMP=$(date '+%Y-%m-%d %H:%M')
TEST_NUM=$(grep -c "^## 测试 #" "$TESTING_FILE" 2>/dev/null || echo "0")
TEST_NUM=$((TEST_NUM + 1))

# 格式化测试编号
TEST_ID=$(printf "%03d" $TEST_NUM)

# 结果图标
if [ "$RESULT" = "pass" ]; then
    ICON="✅"
    STATUS="通过"
elif [ "$RESULT" = "fail" ]; then
    ICON="❌"
    STATUS="失败"
else
    ICON="⚠️"
    STATUS="未知"
fi

echo "$ICON 测试 #$TEST_ID: $TEST_NAME - $STATUS (用时: $DURATION)"

# 追加到 TESTING.md
cat >> "$TESTING_FILE" <<EOF

## 测试 #$TEST_ID：$TEST_NAME
- 时间：$TIMESTAMP
- 结果：$ICON $STATUS
- 用时：$DURATION
- 备注：$NOTES

EOF

echo "✅ 已记录到 TESTING.md"
