# 翎绛指挥中心 - 开发进度

## 关键文档
- **UI 设计方案**: `docs/UI-DESIGN-v2.md`（15KB，含布局/色彩/数据流/实现计划）
- **UI 原型**: `ui-prototype/index.html`（浏览器打开预览）
- **里程碑状态**: `STATUS.md`（M1-M10 定义与进度）

## 当前状态（2026-03-10 16:04）

### 已完成
- ✅ M1-M5 基础功能（中继服务器 + Android App + OpenClaw 客户端）
- ✅ 消息双向流转验证通过
- ✅ Sub-agent 监控面板
- ✅ 对话功能（Markdown 渲染）
- ✅ 消息发送状态提示（BORN → FLYING → RECEIVED → COMPLETE / LOST）

### v0.5.0（2026-03-10 ~14:00）
- ✅ **流式输出**（三端改造）
  - OpenClaw 客户端：独立会话（--session-id）+ 分段推送（splitIntoChunks）
  - 中继服务器：stream_start / stream_chunk / stream_end 透传
  - Android App：流式消息增量渲染 + 闪烁光标动画 ▌
  - AI 思考状态：收到 → 思考中 → 翻记忆 → 组织语言 → 正在说
- ✅ **认证机制**（三端）
  - 预共享 token 认证（48 字符 hex）
  - 服务器：注册时验证 token，失败关闭连接（4001）

### v0.6.0（2026-03-10 ~14:45）
- ✅ **独立会话**（openclaw agent --session-id "app-relay-${clientId}"）
- ✅ **实例持久化**（SharedPreferences，App 重启后实例不丢失）
- ✅ **快捷指令**（状态/记忆/待办/诊断 四个 SuggestionChip）
- ✅ **Bug 修复**（重连注册、注册时序、发送按钮、自动滚动）

### v0.7.0（2026-03-10 ~15:10）
- ✅ **消息历史持久化**（SharedPreferences，最近 200 条，重启不丢）
- ✅ **实例编辑**（EditInstanceDialog，改名/改 URL，URL 变更自动断开重连）
- ✅ **系统通知**（NotificationChannel × 2，App 后台时推送 AI 回复）
- ✅ **编译警告清零**

### v0.7.1（2026-03-10 ~15:35）
- ✅ **Bug 修复：编辑/删除按钮图标消失**（OutlinedButton → IconButton）
- ✅ **Bug 修复：用户消息不显示**（RelayClient collect 改为合并而非覆盖）

### 服务端更新（2026-03-10）
- ✅ **WSS 加密**（中继服务器同时监听 WS:8080 + WSS:8443）
  - 证书：Let's Encrypt ECDSA（api.lingjiangapp.online，有效至 2026-06-03）
  - nginx 反代 /relay 路径（备用方案，外部 443 连通性不稳定）
  - Node.js https.createServer 直接启动 WSS:8443 ✅ 已验证可用

### 待做
- 🔴 **P0：OpenClaw 客户端切 WSS**（当前明文传 token）
- 🔴 **P0：App 默认地址改 WSS**
- 🟡 **P1：消息按实例分离**（当前所有实例共用一个消息列表）
- 🟡 **P1：连接断开重连 UI 提示**（当前断了用户不知道）
- 🟡 **P1：对话界面显示实例名称/状态**
- 🟢 **P2：消息长按复制**
- 🟢 **P2：Sub-agent 手动刷新按钮**
- 🟢 **P2：深色主题适配**
- 🔵 **P3：多实例同时连接**
- 🔵 **P3：语音输入**
- 🟡 进度通知（sub-agent 完成/失败推送）
- 🟡 健康检查展示（CPU、内存、磁盘、API 额度）
- 🟡 记忆搜索（搜索 MEMORY.md）
- 🟡 每日总结（自动生成工作摘要）

### 已知问题
- 磁盘使用率 90%（health_alert）

### 部署状态
- ✅ 中继服务器：118.25.195.154（WS:8080 + WSS:8443，PM2 运行中）
- ✅ OpenClaw 客户端：本地 VM（认证注册成功）
- ✅ Android APK：v0.7.1 已发送到飞书
