# M3 部署指南

## 快速部署

### 1. 部署中继服务器更新（5 分钟）

```bash
cd ~/.openclaw/workspace/projects/linjiang-command-center
./scripts/deploy-relay-server.sh
```

这个脚本会：
- ✅ 打包服务器代码
- ✅ 上传到翎云 VPS
- ✅ 备份旧版本
- ✅ 安装依赖
- ✅ 重启 PM2 服务
- ✅ 显示服务状态和日志

### 2. 安装 OpenClaw 客户端服务（3 分钟）

```bash
cd ~/.openclaw/workspace/projects/linjiang-command-center
./scripts/install-systemd-service.sh
```

这个脚本会：
- ✅ 安装 npm 依赖
- ✅ 复制 systemd 服务文件
- ✅ 启动服务
- ✅ 询问是否设置开机自启

### 3. 验证部署（2 分钟）

#### 检查客户端状态

```bash
sudo systemctl status openclaw-relay-client
```

#### 查看客户端日志

```bash
sudo journalctl -u openclaw-relay-client -f
```

应该看到：
```
✅ Connected to relay server
✅ Registration confirmed
```

#### 检查服务器状态

```bash
ssh root@118.25.195.154 'pm2 status'
```

#### 查看服务器日志

```bash
ssh root@118.25.195.154 'pm2 logs openclaw-relay --lines 50'
```

应该看到：
```
✅ OpenClaw instance registered: openclaw-vm-main
```

## 手动部署

### 方案 A: 部署中继服务器

```bash
# 1. 打包
cd ~/.openclaw/workspace/projects/linjiang-command-center/src/relay-server-prototype
tar -czf /tmp/relay-server.tar.gz .

# 2. 上传
scp /tmp/relay-server.tar.gz root@118.25.195.154:/tmp/

# 3. SSH 登录
ssh root@118.25.195.154

# 4. 备份
cd /root/openclaw-relay
tar -czf backup-$(date +%Y%m%d-%H%M%S).tar.gz relay-server-prototype

# 5. 解压
mkdir -p relay-server-prototype
cd relay-server-prototype
tar -xzf /tmp/relay-server.tar.gz

# 6. 安装依赖
npm install

# 7. 重启服务
pm2 restart openclaw-relay

# 8. 查看状态
pm2 status
pm2 logs openclaw-relay --lines 20
```

### 方案 B: 安装客户端服务

```bash
# 1. 进入客户端目录
cd ~/.openclaw/workspace/projects/linjiang-command-center/src/openclaw-relay-client

# 2. 安装依赖
npm install

# 3. 复制服务文件
sudo cp openclaw-relay-client.service /etc/systemd/system/

# 4. 重新加载 systemd
sudo systemctl daemon-reload

# 5. 启动服务
sudo systemctl start openclaw-relay-client

# 6. 查看状态
sudo systemctl status openclaw-relay-client

# 7. 设置开机自启（可选）
sudo systemctl enable openclaw-relay-client

# 8. 查看日志
sudo journalctl -u openclaw-relay-client -f
```

## 故障排查

### 问题 1: 客户端无法连接

**症状**: 日志显示连接失败

**检查**:
```bash
# 检查中继服务器是否运行
ssh root@118.25.195.154 'pm2 status'

# 检查端口是否开放
curl -I http://118.25.195.154:8080

# 检查网络连接
ping 118.25.195.154
```

**解决**:
```bash
# 重启中继服务器
ssh root@118.25.195.154 'pm2 restart openclaw-relay'
```

### 问题 2: 客户端注册失败

**症状**: 连接成功但未收到注册确认

**检查**:
```bash
# 查看客户端日志
sudo journalctl -u openclaw-relay-client -n 50

# 查看服务器日志
ssh root@118.25.195.154 'pm2 logs openclaw-relay --lines 50'
```

**解决**:
```bash
# 重启客户端
sudo systemctl restart openclaw-relay-client
```

### 问题 3: OpenClaw CLI 调用失败

**症状**: 收到消息但无法调用 CLI

**检查**:
```bash
# 检查 OpenClaw 是否运行
openclaw gateway status

# 检查 token 是否正确
cat ~/.openclaw/openclaw.json | grep token

# 手动测试 CLI
openclaw sessions send --session test "hello"
```

**解决**:
```bash
# 更新 config.json 中的 token
cd ~/.openclaw/workspace/projects/linjiang-command-center/src/openclaw-relay-client
nano config.json

# 重启客户端
sudo systemctl restart openclaw-relay-client
```

### 问题 4: systemd 服务无法启动

**症状**: `systemctl start` 失败

**检查**:
```bash
# 查看详细错误
sudo systemctl status openclaw-relay-client -l

# 查看 journal 日志
sudo journalctl -u openclaw-relay-client -n 50
```

**解决**:
```bash
# 检查服务文件语法
sudo systemd-analyze verify /etc/systemd/system/openclaw-relay-client.service

# 检查文件权限
ls -la ~/.openclaw/workspace/projects/linjiang-command-center/src/openclaw-relay-client/

# 手动测试
cd ~/.openclaw/workspace/projects/linjiang-command-center/src/openclaw-relay-client
node index.js
```

## 验证清单

部署完成后，逐项检查：

- [ ] 中继服务器运行正常（`pm2 status`）
- [ ] 客户端服务运行正常（`systemctl status`）
- [ ] 客户端成功连接到服务器
- [ ] 客户端成功注册为 OpenClaw 实例
- [ ] 心跳正常（查看日志）
- [ ] 服务器日志显示实例上线
- [ ] 开机自启已配置（可选）

## 回滚

如果部署出现问题，可以回滚：

### 回滚中继服务器

```bash
ssh root@118.25.195.154

# 查看备份
cd /root/openclaw-relay
ls -lh backup-*.tar.gz

# 恢复备份（选择最新的）
tar -xzf backup-20260308-160000.tar.gz

# 重启服务
pm2 restart openclaw-relay
```

### 停止客户端服务

```bash
# 停止服务
sudo systemctl stop openclaw-relay-client

# 禁用自启
sudo systemctl disable openclaw-relay-client

# 删除服务文件（可选）
sudo rm /etc/systemd/system/openclaw-relay-client.service
sudo systemctl daemon-reload
```

## 下一步

部署完成后：

1. **测试端到端流程**（需要 Android App）
   - Android App 发送消息
   - 验证消息到达 OpenClaw
   - 验证回复返回 Android App

2. **性能测试**
   - 测试消息延迟
   - 测试并发连接
   - 测试长时间运行稳定性

3. **安全加固**
   - 添加认证机制
   - 配置 SSL 证书（WSS）
   - 添加速率限制

---

**维护者**: 翎绛 🪶  
**更新时间**: 2026-03-09 00:30
