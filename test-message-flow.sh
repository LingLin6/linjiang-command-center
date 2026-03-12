#!/bin/bash

# M3 消息流程测试脚本

echo "🧪 M3 消息流程测试"
echo "=================="
echo ""

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 检查中继服务器
echo "1️⃣ 检查中继服务器..."
if curl -s --max-time 2 http://118.25.195.154:8080 > /dev/null 2>&1; then
    echo -e "${GREEN}✅ 中继服务器可访问${NC}"
else
    echo -e "${RED}❌ 中继服务器不可访问${NC}"
    echo "   请检查服务器是否运行"
fi
echo ""

# 检查 OpenClaw 客户端
echo "2️⃣ 检查 OpenClaw 客户端..."
if ps aux | grep -v grep | grep "openclaw-relay-client" > /dev/null; then
    echo -e "${GREEN}✅ OpenClaw 客户端正在运行${NC}"
    
    # 显示最后几行日志
    echo "   最近日志："
    tail -5 /tmp/openclaw-relay-client.log | sed 's/^/   /'
else
    echo -e "${RED}❌ OpenClaw 客户端未运行${NC}"
    echo "   启动命令："
    echo "   cd ./projects/linjiang-command-center/src/openclaw-relay-client"
    echo "   node index.js"
fi
echo ""

# 检查 APK
echo "3️⃣ 检查 APK..."
APK_PATH="./projects/linjiang-command-center/src/android/app/build/outputs/apk/debug/app-debug.apk"
if [ -f "$APK_PATH" ]; then
    echo -e "${GREEN}✅ APK 已生成${NC}"
    echo "   路径: $APK_PATH"
    echo "   大小: $(du -h "$APK_PATH" | cut -f1)"
    echo "   修改时间: $(stat -c %y "$APK_PATH" | cut -d. -f1)"
else
    echo -e "${RED}❌ APK 未找到${NC}"
    echo "   编译命令："
    echo "   cd ./projects/linjiang-command-center/src/android"
    echo "   ./gradlew assembleDebug"
fi
echo ""

# 检查 ADB
echo "4️⃣ 检查 ADB 连接..."
if command -v adb > /dev/null 2>&1; then
    DEVICES=$(adb devices | grep -v "List" | grep "device$" | wc -l)
    if [ "$DEVICES" -gt 0 ]; then
        echo -e "${GREEN}✅ 已连接 $DEVICES 个设备${NC}"
        adb devices | grep "device$" | sed 's/^/   /'
        
        echo ""
        echo "   安装命令："
        echo "   adb install -r $APK_PATH"
    else
        echo -e "${YELLOW}⚠️  未检测到设备${NC}"
        echo "   请连接 Android 设备或启动模拟器"
    fi
else
    echo -e "${YELLOW}⚠️  ADB 未安装${NC}"
    echo "   请安装 Android SDK Platform Tools"
fi
echo ""

# 测试步骤
echo "5️⃣ 测试步骤"
echo "============"
echo ""
echo "📱 在 Android App 上："
echo "   1. 打开 App"
echo "   2. 点击「翎云」实例"
echo "   3. 点击「连接」按钮"
echo "   4. 等待显示「注册成功」"
echo "   5. 切换到「对话」标签"
echo "   6. 输入消息：测试消息"
echo "   7. 点击发送"
echo ""
echo "🔍 观察日志："
echo "   - App Logcat: adb logcat | grep -E 'RelayClient|WebSocketManager|ChatViewModel'"
echo "   - OpenClaw 客户端: tail -f /tmp/openclaw-relay-client.log"
echo ""
echo "✅ 预期结果："
echo "   - App 显示用户消息"
echo "   - OpenClaw 客户端日志显示接收消息"
echo "   - OpenClaw 客户端日志显示发送回复"
echo "   - App 显示助手回复"
echo ""

# 快速诊断
echo "6️⃣ 快速诊断命令"
echo "================"
echo ""
echo "# 查看 OpenClaw 客户端实时日志"
echo "tail -f /tmp/openclaw-relay-client.log"
echo ""
echo "# 查看 App 实时日志"
echo "adb logcat -c && adb logcat | grep -E 'RelayClient|WebSocketManager|ChatViewModel'"
echo ""
echo "# 重启 OpenClaw 客户端"
echo "pkill -f openclaw-relay-client"
echo "cd ./projects/linjiang-command-center/src/openclaw-relay-client && node index.js"
echo ""
echo "# 测试 OpenClaw CLI"
echo "openclaw sessions send --session agent:main:main '测试消息'"
echo ""

# 总结
echo "📋 修复内容"
echo "==========="
echo "1. MainActivity: 添加 RelayClient 传递逻辑"
echo "2. WebSocketManager: 添加应用层心跳（30秒）"
echo "3. OpenClaw 客户端: 已正确实现消息路由"
echo ""
echo "详细报告: ./projects/linjiang-command-center/M3-MESSAGE-FLOW-FIX.md"
echo ""
