#!/bin/bash
# 更新项目状态

set -e

PROJECT_ROOT="$HOME/.openclaw/workspace/projects/linjiang-command-center"
STATUS_FILE="${PROJECT_ROOT}/STATUS.md"

# 显示帮助
if [ "$1" = "-h" ] || [ "$1" = "--help" ]; then
    cat <<EOF
用法: $0 <action> [args]

Actions:
  complete <task>     标记任务完成
  start <task>        开始新任务
  block <reason>      添加阻塞点
  unblock             清除阻塞点
  deviation <plan> <actual> <reason>  记录偏差

示例:
  $0 complete "VPN 服务器配置"
  $0 start "Android WireGuard 集成"
  $0 block "等待翎麟确认 API 设计"
  $0 deviation "30min" "45min" "防火墙调试"
EOF
    exit 0
fi

ACTION=$1
shift

case $ACTION in
    complete)
        TASK=$1
        TIMESTAMP=$(date '+%Y-%m-%d %H:%M')
        echo "✅ 标记完成: $TASK ($TIMESTAMP)"
        # 这里可以添加自动更新 STATUS.md 的逻辑
        ;;
    start)
        TASK=$1
        TIMESTAMP=$(date '+%Y-%m-%d %H:%M')
        echo "🟡 开始任务: $TASK ($TIMESTAMP)"
        ;;
    block)
        REASON=$1
        TIMESTAMP=$(date '+%Y-%m-%d %H:%M')
        echo "⚠️ 阻塞: $REASON ($TIMESTAMP)"
        ;;
    unblock)
        TIMESTAMP=$(date '+%Y-%m-%d %H:%M')
        echo "✅ 解除阻塞 ($TIMESTAMP)"
        ;;
    deviation)
        PLAN=$1
        ACTUAL=$2
        REASON=$3
        echo "📊 偏差记录: 计划 $PLAN, 实际 $ACTUAL, 原因: $REASON"
        ;;
    *)
        echo "错误: 未知操作 '$ACTION'"
        echo "运行 '$0 --help' 查看帮助"
        exit 1
        ;;
esac
