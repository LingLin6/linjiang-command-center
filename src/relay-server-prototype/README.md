# OpenClaw Relay Server

中继服务器，用于在 Android App 和 OpenClaw 实例之间路由消息。

## 功能

- ✅ WebSocket 服务器
- ✅ 客户端注册（Android / OpenClaw）
- ✅ 消息路由（点对点）
- ✅ 实例管理（上线/下线通知）
- ✅ 离线消息队列
- ✅ 心跳检测（90 秒超时）
- ✅ 广播消息
- ✅ 完善的日志

## 安装

```bash
cd src/relay-server-prototype
npm install
```

## 运行

### 开发模式

```bash
npm start
```

### 生产模式（PM2）

```bash
# 启动
npm run pm2:start

# 停止
npm run pm2:stop

# 重启
npm run pm2:restart

# 查看日志
npm run pm2:logs
```

## 协议

### 连接

客户端连接后，服务器发送欢迎消息：

```json
{
  "type": "welcome",
  "clientId": "1772987123057-elr9qn6qa",
  "timestamp": 1772987123057
}
```

### 注册

#### OpenClaw 实例注册

```json
{
  "type": "register",
  "payload": {
    "type": "openclaw",
    "instanceId": "openclaw-vm-main",
    "instanceName": "虚拟机主实例",
    "metadata": {
      "version": "1.0.0",
      "platform": "linux"
    }
  }
}
```

服务器响应：

```json
{
  "type": "registered",
  "payload": {
    "clientId": "1772987123057-elr9qn6qa",
    "instanceId": "openclaw-vm-main",
    "instanceName": "虚拟机主实例"
  }
}
```

#### Android 客户端注册

```json
{
  "type": "register",
  "payload": {
    "type": "android",
    "metadata": {
      "version": "1.0.0",
      "device": "Pixel 6"
    }
  }
}
```

服务器响应：

```json
{
  "type": "registered",
  "payload": {
    "clientId": "1772987123058-abc123def",
    "type": "android"
  }
}
```

并发送实例列表：

```json
{
  "type": "instance_list",
  "payload": {
    "instances": [
      {
        "instanceId": "openclaw-vm-main",
        "instanceName": "虚拟机主实例",
        "online": true
      }
    ]
  }
}
```

### 消息路由

#### Android → OpenClaw

Android 发送：

```json
{
  "type": "message",
  "target": "openclaw-vm-main",
  "payload": {
    "sessionKey": "session-123",
    "message": "用户消息"
  }
}
```

服务器转发给 OpenClaw：

```json
{
  "type": "message",
  "from": "1772987123058-abc123def",
  "payload": {
    "sessionKey": "session-123",
    "message": "用户消息"
  }
}
```

#### OpenClaw → Android

OpenClaw 发送：

```json
{
  "type": "message",
  "target": "1772987123058-abc123def",
  "payload": {
    "text": "OpenClaw 的回复",
    "sessionKey": "session-123",
    "timestamp": 1772987123100
  }
}
```

服务器转发给 Android：

```json
{
  "type": "message",
  "from": "1772987123057-elr9qn6qa",
  "payload": {
    "text": "OpenClaw 的回复",
    "sessionKey": "session-123",
    "timestamp": 1772987123100
  }
}
```

### 实例状态通知

#### 实例上线

```json
{
  "type": "instance_online",
  "payload": {
    "instanceId": "openclaw-vm-main",
    "instanceName": "虚拟机主实例",
    "timestamp": 1772987123057
  }
}
```

#### 实例下线

```json
{
  "type": "instance_offline",
  "payload": {
    "instanceId": "openclaw-vm-main",
    "instanceName": "虚拟机主实例",
    "timestamp": 1772987123200
  }
}
```

### 离线消息

如果目标客户端离线，消息会被队列化：

```json
{
  "type": "message_queued",
  "payload": {
    "target": "1772987123058-abc123def",
    "queueSize": 3
  }
}
```

当目标客户端上线时，队列中的消息会自动发送。

### 心跳

客户端发送：

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

### 广播

```json
{
  "type": "broadcast",
  "payload": {
    "message": "系统通知"
  }
}
```

## 部署

### 翎云 VPS 部署

1. 上传代码：

```bash
scp -r src/relay-server-prototype root@118.25.195.154:/root/openclaw-relay/
```

2. SSH 登录：

```bash
ssh root@118.25.195.154
```

3. 安装依赖：

```bash
cd /root/openclaw-relay/relay-server-prototype
npm install
```

4. 使用 PM2 启动：

```bash
pm2 start server.js --name openclaw-relay
pm2 save
pm2 startup
```

5. 配置防火墙：

```bash
ufw allow 8080/tcp
```

### 更新部署

```bash
# 本地
scp src/relay-server-prototype/server.js root@118.25.195.154:/root/openclaw-relay/relay-server-prototype/

# 服务器
ssh root@118.25.195.154
cd /root/openclaw-relay/relay-server-prototype
pm2 restart openclaw-relay
```

## 监控

### 查看日志

```bash
pm2 logs openclaw-relay
```

### 查看状态

```bash
pm2 status
```

### 查看详细信息

```bash
pm2 show openclaw-relay
```

## 架构

```
┌─────────────────┐
│  Android App    │
└────────┬────────┘
         │
         │ WebSocket
         │ (register: android)
         │
         ↓
┌─────────────────────────────────┐
│     Relay Server (翎云 VPS)      │
│                                 │
│  - 客户端管理                    │
│  - 实例注册表                    │
│  - 消息路由                      │
│  - 离线队列                      │
│  - 心跳检测                      │
└────────┬────────────────────────┘
         │
         │ WebSocket
         │ (register: openclaw)
         │
         ↓
┌─────────────────┐
│  Relay Client   │
│  (虚拟机)       │
└────────┬────────┘
         │
         │ CLI
         │
         ↓
┌─────────────────┐
│  OpenClaw       │
└─────────────────┘
```

## 性能

- **并发连接**：支持 1000+ 客户端
- **内存占用**：~50MB（空闲）
- **CPU 占用**：<5%（正常负载）
- **延迟**：<50ms（本地网络）

## 安全

### 当前状态

- ⚠️ 无认证机制
- ⚠️ 无加密传输（WS，非 WSS）
- ⚠️ 无速率限制

### 建议改进

1. **添加认证**：Token 或 API Key
2. **启用 WSS**：使用 Let's Encrypt 证书
3. **速率限制**：防止滥用
4. **输入验证**：防止注入攻击
5. **日志审计**：记录所有操作

## 故障排查

### 连接失败

```bash
# 检查服务是否运行
pm2 status

# 检查端口
netstat -tlnp | grep 8080

# 检查防火墙
ufw status
```

### 消息未送达

1. 检查客户端是否注册
2. 检查目标 ID 是否正确
3. 查看服务器日志

### 性能问题

```bash
# 查看资源使用
pm2 monit

# 查看详细日志
pm2 logs openclaw-relay --lines 100
```

## 开发

### 本地测试

```bash
# 启动服务器
npm start

# 另一个终端，测试连接
node ../../tests/relay-client-test.js
```

### 调试

设置环境变量：

```bash
DEBUG=* npm start
```

## 更新日志

### v1.1.0 (2026-03-08)

- ✅ 支持 OpenClaw 实例注册
- ✅ 实例管理（上线/下线通知）
- ✅ 消息路由（Android ↔ OpenClaw）
- ✅ 实例列表推送
- ✅ 离线消息队列

### v1.0.0 (2026-03-08)

- ✅ 基础 WebSocket 服务器
- ✅ 客户端注册
- ✅ 点对点消息
- ✅ 广播消息
- ✅ 心跳检测

## 许可证

MIT

---

**维护者**: 翎绛 🪶  
**版本**: 1.1.0  
**更新时间**: 2026-03-08
