# OpenClaw Relay Client

OpenClaw 实例连接到中继服务器的客户端。

## 功能

- ✅ WebSocket 连接到中继服务器
- ✅ 自动注册为 OpenClaw 实例
- ✅ 接收来自 Android App 的消息
- ✅ 调用本地 OpenClaw API（`openclaw sessions send`）
- ✅ 将回复发送回中继服务器
- ✅ 心跳保活（30 秒）
- ✅ 自动重连（指数退避）
- ✅ 离线消息队列
- ✅ 完善的错误处理和日志

## 安装

```bash
cd ~/.openclaw/workspace/projects/linjiang-command-center/src/openclaw-relay-client
npm install
```

## 配置

编辑 `config.json`：

```json
{
  "relayUrl": "ws://118.25.195.154:8080",
  "instanceId": "openclaw-vm-main",
  "instanceName": "虚拟机主实例",
  "openclawApiUrl": "http://127.0.0.1:18789",
  "openclawToken": "your-token-here",
  "heartbeatInterval": 30000,
  "reconnectDelay": 5000,
  "maxReconnectDelay": 60000,
  "reconnectBackoff": 1.5
}
```

**配置说明**：

- `relayUrl`: 中继服务器地址
- `instanceId`: 实例唯一标识符
- `instanceName`: 实例显示名称
- `openclawApiUrl`: OpenClaw API 地址（通常是本地）
- `openclawToken`: OpenClaw API 认证 token（从 `~/.openclaw/openclaw.json` 获取）
- `heartbeatInterval`: 心跳间隔（毫秒）
- `reconnectDelay`: 初始重连延迟（毫秒）
- `maxReconnectDelay`: 最大重连延迟（毫秒）
- `reconnectBackoff`: 重连退避系数

## 运行

### 前台运行（测试）

```bash
npm start
```

### 后台运行（systemd）

1. 复制服务文件：

```bash
sudo cp openclaw-relay-client.service /etc/systemd/system/
```

2. 重新加载 systemd：

```bash
sudo systemctl daemon-reload
```

3. 启动服务：

```bash
sudo systemctl start openclaw-relay-client
```

4. 设置开机自启：

```bash
sudo systemctl enable openclaw-relay-client
```

5. 查看状态：

```bash
sudo systemctl status openclaw-relay-client
```

6. 查看日志：

```bash
sudo journalctl -u openclaw-relay-client -f
```

## 协议

### 注册消息

客户端连接后发送：

```json
{
  "type": "register",
  "payload": {
    "type": "openclaw",
    "instanceId": "openclaw-vm-main",
    "instanceName": "虚拟机主实例",
    "metadata": {
      "version": "1.0.0",
      "platform": "linux",
      "nodeVersion": "v18.0.0"
    }
  }
}
```

服务器响应：

```json
{
  "type": "registered",
  "payload": {
    "clientId": "client-uuid",
    "instanceId": "openclaw-vm-main"
  }
}
```

### 接收消息

从 Android App 接收：

```json
{
  "type": "message",
  "from": "android-client-id",
  "payload": {
    "sessionKey": "session-key",
    "message": "用户消息内容"
  }
}
```

### 发送回复

发送到 Android App：

```json
{
  "type": "message",
  "target": "android-client-id",
  "payload": {
    "text": "OpenClaw 的回复",
    "sessionKey": "session-key",
    "timestamp": 1234567890
  }
}
```

### 心跳

客户端定期发送：

```json
{
  "type": "ping"
}
```

服务器响应：

```json
{
  "type": "pong"
}
```

## 日志

日志格式：

```
📘 [2026-03-08T16:30:00.000Z] Connecting to relay server: ws://118.25.195.154:8080
✅ [2026-03-08T16:30:00.100Z] Connected to relay server
📘 [2026-03-08T16:30:00.101Z] Registering as OpenClaw instance
✅ [2026-03-08T16:30:00.150Z] Registration confirmed
🔍 [2026-03-08T16:30:30.000Z] Sending heartbeat
```

日志级别：

- 📘 `info`: 一般信息
- ✅ `success`: 成功操作
- ❌ `error`: 错误
- ⚠️ `warn`: 警告
- 🔍 `debug`: 调试信息

## 故障排查

### 连接失败

1. 检查中继服务器是否运行：

```bash
curl -I http://118.25.195.154:8080
```

2. 检查防火墙：

```bash
sudo ufw status
```

3. 检查网络连接：

```bash
ping 118.25.195.154
```

### OpenClaw CLI 调用失败

1. 检查 OpenClaw 是否运行：

```bash
openclaw gateway status
```

2. 检查 token 是否正确：

```bash
cat ~/.openclaw/openclaw.json | grep token
```

3. 手动测试 CLI：

```bash
openclaw sessions send --session test "hello"
```

### 服务无法启动

1. 检查服务状态：

```bash
sudo systemctl status openclaw-relay-client
```

2. 查看详细日志：

```bash
sudo journalctl -u openclaw-relay-client -n 50
```

3. 检查文件权限：

```bash
ls -la ~/.openclaw/workspace/projects/linjiang-command-center/src/openclaw-relay-client/
```

## 架构

```
┌─────────────────┐
│  Android App    │
└────────┬────────┘
         │ WebSocket
         ↓
┌─────────────────┐
│  Relay Server   │
│  (翎云 VPS)     │
└────────┬────────┘
         │ WebSocket
         ↓
┌─────────────────┐
│  Relay Client   │ ← 本程序
└────────┬────────┘
         │ CLI
         ↓
┌─────────────────┐
│  OpenClaw       │
│  (本地实例)     │
└─────────────────┘
```

## 安全注意事项

1. **Token 保护**：不要将 `config.json` 提交到版本控制
2. **传输加密**：生产环境应使用 WSS（WebSocket Secure）
3. **认证机制**：中继服务器应添加认证
4. **输入验证**：对接收的消息进行验证
5. **命令注入**：已对 CLI 参数进行转义

## 性能

- **内存占用**：~30MB
- **CPU 占用**：空闲时 <1%
- **网络流量**：心跳 ~1KB/30s
- **延迟**：通常 <100ms（取决于网络）

## 开发

### 测试

```bash
npm test
```

### 调试

设置环境变量启用详细日志：

```bash
DEBUG=* npm start
```

## 许可证

MIT

---

**维护者**: 翎绛 🪶  
**版本**: 1.0.0  
**更新时间**: 2026-03-08
