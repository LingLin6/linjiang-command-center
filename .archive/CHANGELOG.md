# 变更日志

## [Unreleased]

## [0.3.1] - 2026-03-09

### 新增
- ✅ Toast 提示功能
  - 连接状态提示（正在连接、连接成功、连接失败、连接已断开）
  - 注册状态提示（正在注册、注册成功、注册失败）
  - 错误信息显示（包含具体错误原因）
  
### 改进
- InstanceViewModel 添加 `toastMessage` StateFlow
- WebSocketManager 添加 `onToast` 回调
- RelayClient 添加 `onToast` 回调
- InstanceListScreen 监听并显示 Toast 消息
- 优化用户体验，实时反馈连接状态

### 技术细节
- 使用 StateFlow 在 ViewModel 和 UI 层之间传递 Toast 消息
- LaunchedEffect 监听消息变化并显示 Toast
- 自动清除已显示的消息，避免重复显示

### 文件修改
- `InstanceViewModel.kt` - 添加 Toast 消息管理
- `WebSocketManager.kt` - 添加连接状态提示
- `RelayClient.kt` - 添加注册状态提示
- `InstanceListScreen.kt` - 添加 Toast 显示逻辑
- `ChatViewModel.kt` - 修复 RelayClient 消息监听

### APK
- 文件: `app-debug.apk`
- 大小: 8.4 MB
- 位置: `./projects/linjiang-command-center/app-debug.apk`

## [0.3.0] - 2026-03-09

### 新增
- ✅ OpenClaw 客户端（M3）
  - WebSocket 连接到中继服务器
  - 自动注册为 OpenClaw 实例
  - 接收来自 Android App 的消息
  - 调用本地 OpenClaw CLI（`openclaw sessions send`）
  - 将回复发送回中继服务器
  - 心跳保活（30 秒）
  - 自动重连（指数退避）
  - 离线消息队列
  - 完善的错误处理和日志
  
- ✅ systemd 服务配置
  - 开机自启配置
  - 自动重启机制
  - 日志输出到 journal
  - 安全加固选项
  
- ✅ 中继服务器增强
  - 客户端类型区分（android / openclaw）
  - 实例注册表（instanceId → clientId）
  - 消息路由（Android ↔ OpenClaw）
  - 实例状态广播（上线/下线）
  - 实例列表推送（给 Android）
  - 离线消息队列
  
- ✅ 文档
  - OpenClaw 客户端使用文档
  - 中继服务器文档更新
  - M3 测试报告

### 改进
- 中继服务器协议完善
  - 新增 `registered` 消息（区分客户端类型）
  - 新增 `instance_list` 消息
  - 新增 `instance_online` / `instance_offline` 通知
  - 新增 `message_queued` 确认

### 技术栈
- Node.js
- WebSocket (ws)
- systemd

### 代码统计
- 客户端文件数: 4
- 客户端代码行数: ~300 行
- 服务器代码行数: ~350 行

### 已知问题
- ⚠️ systemd 服务未测试
- ⚠️ 中继服务器未部署到 VPS
- ⚠️ 端到端流程未验证
- ⚠️ 无持久化（离线消息）
- ⚠️ 无认证机制
- ⚠️ 无加密传输（WS 非 WSS）

## [0.2.0] - 2026-03-08

### 新增
- ✅ Android App 基础框架（M2）
  - 完整的 MVVM 架构
  - 3 个主要界面（实例列表、对话、设置）
  - WebSocket 客户端（自动重连、心跳）
  - 数据模型（Instance, Message, SubAgent）
  - Material 3 主题（绛红色）
  
- ✅ 实例管理功能
  - 添加/删除实例
  - 连接/断开实例
  - 状态指示器（🟢/🟡/🔴）
  - 实例切换
  
- ✅ 对话功能
  - 消息收发
  - 消息气泡（用户/助手/系统）
  - 自动滚动
  - Sub-agent 监控面板
  
- ✅ 网络层
  - WebSocketManager（底层连接管理）
  - RelayClient（高层 API 封装）
  - 自动重连（指数退避）
  - 心跳机制（30 秒）
  - JSON 消息解析
  
- ✅ 文档
  - 开发文档（架构、核心类、使用示例）
  - 构建指南（环境、步骤、运行）
  - 测试报告（测试用例、结果、指南）

### 技术栈
- Kotlin
- Jetpack Compose
- Material 3
- OkHttp WebSocket
- Coroutines + Flow
- MVVM 架构

### 代码统计
- 总文件数: 20+
- 代码行数: ~2000 行
- Kotlin 文件: 15 个

### 已知问题
- ⚠️ 未在 Android Studio 中编译验证
- ⚠️ 未在真机上测试
- ⚠️ 无持久化（重启后丢失实例列表）
- ⚠️ 无认证机制
- ⚠️ 无加密传输

## [0.1.0] - 2026-03-08

### 新增
- ✅ 中继服务器 MVP（M1）
  - WebSocket 服务器（Node.js + ws）
  - 客户端注册
  - 消息转发（点对点 + 广播）
  - 离线消息队列
  - 心跳检测
  
- ✅ 部署到翎云
  - 服务器地址: ws://118.25.195.154:8080
  - 使用 PM2 管理进程
  - 防火墙配置
  
- ✅ 文档
  - README（功能、协议、部署）
  - 测试指南

### 用时
- 计划: 5-7 天
- 实际: 58 分钟
- 偏差: 提前 6.9 天

---

## 版本说明

### 版本号规则
- 主版本号: 重大架构变更
- 次版本号: 新功能
- 修订号: Bug 修复

### 里程碑对应
- M1 → v0.1.0
- M2 → v0.2.0
- M3-M5 → v0.3.0 - v0.5.0
- M6-M10 → v1.0.0

---

**维护者**: 翎绛 🪶
