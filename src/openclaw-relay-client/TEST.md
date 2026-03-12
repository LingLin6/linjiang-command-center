# OpenClaw Relay Client - 测试指南

## 当前状态

✅ **已完成**：
- OpenClaw 客户端已修改，可以调用本地翎绛（OpenClaw Gateway）
- 使用 `openclaw agent` CLI 命令发送消息
- 服务已重启并运行中

## 测试方法

### 1. 从 App 发送消息

翎麟在 App 上发送任何消息，例如：
```
你好翎绛，这是测试消息
```

### 2. 观察日志

服务日志会显示：
```
📘 Processing message from app-xxx
📘 Calling OpenClaw via CLI
✅ Got reply from OpenClaw
```

### 3. 在 App 上收到回复

翎绛的回复会通过中继服务器发回 App

## 完整流程

```
App (翎麟)
  ↓ 发送消息
中继服务器 (118.25.195.154:8080)
  ↓ WebSocket
OpenClaw 客户端 (虚拟机)
  ↓ CLI 调用
OpenClaw Gateway (127.0.0.1:18789)
  ↓ 处理
翎绛 (agent:main:main)
  ↓ 生成回复
OpenClaw Gateway
  ↓ 返回
OpenClaw 客户端
  ↓ WebSocket
中继服务器
  ↓ 发送
App (翎麟收到回复)
```

## 查看日志

实时日志：
```bash
journalctl -u openclaw-relay-client -f
```

最近 50 条：
```bash
journalctl -u openclaw-relay-client -n 50
```

## 重启服务

```bash
sudo systemctl restart openclaw-relay-client
```

## 技术细节

### 修改内容

1. **handleIncomingMessage()**: 改为调用 `callOpenClawGatewayCLI()`
2. **callOpenClawGatewayCLI()**: 使用 `openclaw agent --agent main --message "..."` 命令
3. 环境变量：传递 `OPENCLAW_GATEWAY_TOKEN` 用于认证

### 为什么使用 CLI 而不是 WebSocket？

- WebSocket 协议需要复杂的设备认证和签名
- CLI 方式更简单可靠，直接复用 OpenClaw 的认证机制
- 性能足够（每次消息约 1-2 秒延迟）

### 未来优化

如果需要更低延迟，可以：
1. 实现完整的 WebSocket 协议（包括设备签名）
2. 保持长连接而不是每次重新连接
3. 使用 HTTP API（如果 Gateway 支持）

## 故障排查

### 服务无法启动
```bash
sudo systemctl status openclaw-relay-client
journalctl -u openclaw-relay-client -n 50
```

### OpenClaw Gateway 未运行
```bash
openclaw gateway status
openclaw gateway start
```

### 认证失败
检查 `config.json` 中的 `openclawToken` 是否与 `~/.openclaw/openclaw.json` 中的 `gateway.auth.token` 一致

### 超时
增加 `--timeout` 参数（默认 60 秒）

---

**创建时间**: 2026-03-09 02:07
**版本**: 1.0.0
