const http = require('http');
const WebSocket = require('ws');

const PORT = process.env.PORT || 8080;

// 客户端管理
const clients = new Map(); // clientId -> { ws, type, metadata, lastPing }
const instances = new Map(); // instanceId -> clientId
const offlineMessages = new Map(); // clientId -> [messages]

// 日志
function log(level, message, data = null) {
  const timestamp = new Date().toISOString();
  const prefix = {
    info: '📘',
    success: '✅',
    error: '❌',
    warn: '⚠️',
    debug: '🔍'
  }[level] || '📝';
  
  console.log(`${prefix} [${timestamp}] ${message}`);
  if (data) {
    console.log(JSON.stringify(data, null, 2));
  }
}

// 生成客户端 ID
function generateClientId() {
  return `${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
}

// 发送消息
function sendMessage(ws, data) {
  if (ws.readyState === WebSocket.OPEN) {
    try {
      ws.send(JSON.stringify(data));
      return true;
    } catch (error) {
      log('error', 'Failed to send message', { error: error.message });
      return false;
    }
  }
  return false;
}

// 广播消息
function broadcast(data, excludeClientId = null) {
  let sent = 0;
  clients.forEach((client, clientId) => {
    if (clientId !== excludeClientId) {
      if (sendMessage(client.ws, data)) {
        sent++;
      }
    }
  });
  return sent;
}

// 广播实例列表给所有 Android 客户端
function broadcastInstanceList() {
  const instanceList = [];
  instances.forEach((clientId, instanceId) => {
    const client = clients.get(clientId);
    if (client && client.type === 'openclaw') {
      instanceList.push({
        instanceId,
        instanceName: client.instanceName || instanceId,
        online: true
      });
    }
  });
  
  // 发送给所有 Android 客户端
  clients.forEach((client, clientId) => {
    if (client.type === 'android') {
      sendMessage(client.ws, {
        type: 'instance_list',
        payload: { instances: instanceList }
      });
    }
  });
  
  log('debug', `Broadcasted instance list to Android clients`, {
    instanceCount: instanceList.length
  });
}

// 处理注册
function handleRegister(clientId, client, payload) {
  const { type, instanceId, instanceName, metadata } = payload;
  
  client.type = type || 'unknown';
  client.metadata = metadata || {};
  
  if (type === 'openclaw' && instanceId) {
    // OpenClaw 实例注册
    client.instanceId = instanceId;
    client.instanceName = instanceName || instanceId;
    instances.set(instanceId, clientId);
    
    log('success', `OpenClaw instance registered: ${instanceId}`, {
      clientId,
      instanceName,
      metadata
    });
    
    // 确认注册
    sendMessage(client.ws, {
      type: 'registered',
      payload: {
        clientId,
        instanceId,
        instanceName
      }
    });
    
    // 广播实例上线
    broadcast({
      type: 'instance_online',
      payload: {
        instanceId,
        instanceName,
        timestamp: Date.now()
      }
    }, clientId);
    
    // 广播更新后的实例列表
    broadcastInstanceList();
    
    // 发送离线消息
    if (offlineMessages.has(clientId)) {
      const messages = offlineMessages.get(clientId);
      log('info', `Delivering ${messages.length} offline messages to ${clientId}`);
      messages.forEach(msg => sendMessage(client.ws, msg));
      offlineMessages.delete(clientId);
    }
    
  } else if (type === 'android') {
    // Android 客户端注册
    log('success', `Android client registered: ${clientId}`, { metadata });
    
    sendMessage(client.ws, {
      type: 'registered',
      payload: {
        clientId,
        type: 'android'
      }
    });
    
    // 发送实例列表
    const instanceList = [];
    instances.forEach((cid, iid) => {
      const c = clients.get(cid);
      if (c) {
        instanceList.push({
          instanceId: iid,
          instanceName: c.instanceName || iid,
          online: true
        });
      }
    });
    
    sendMessage(client.ws, {
      type: 'instance_list',
      payload: { instances: instanceList }
    });
    
  } else {
    // 其他类型客户端
    log('info', `Client registered: ${clientId}`, { type, metadata });
    
    sendMessage(client.ws, {
      type: 'registered',
      payload: { clientId, type }
    });
  }
}

// 处理消息
function handleMessage(clientId, client, msg) {
  let { target, payload } = msg;
  
  // 如果没有指定 target，且发送者是 Android 客户端，自动路由到 OpenClaw 实例
  if (!target && client.type === 'android') {
    // 查找第一个 OpenClaw 实例
    const openclawInstances = Array.from(clients.entries())
      .filter(([_, c]) => c.type === 'openclaw');
    
    if (openclawInstances.length > 0) {
      target = openclawInstances[0][0]; // 使用第一个 OpenClaw 实例的 clientId
      log('info', `Auto-routing Android message to OpenClaw: ${target}`);
    }
  }
  
  if (!target) {
    log('error', 'Message missing target', { clientId, msg });
    sendMessage(client.ws, {
      type: 'error',
      payload: { message: 'Missing target and no OpenClaw instance available' }
    });
    return;
  }
  
  // 查找目标客户端
  let targetClientId = target;
  
  // 如果 target 是 instanceId，转换为 clientId
  if (instances.has(target)) {
    targetClientId = instances.get(target);
  }
  
  const targetClient = clients.get(targetClientId);
  
  if (targetClient) {
    // 在线，直接发送
    log('info', `Routing message: ${clientId} -> ${targetClientId}`);
    
    sendMessage(targetClient.ws, {
      type: 'message',
      from: clientId,
      payload: payload
    });
    
  } else {
    // 离线，存储消息
    log('warn', `Target offline, queueing message: ${targetClientId}`);
    
    if (!offlineMessages.has(targetClientId)) {
      offlineMessages.set(targetClientId, []);
    }
    
    offlineMessages.get(targetClientId).push({
      type: 'message',
      from: clientId,
      payload: payload,
      timestamp: Date.now()
    });
    
    // 通知发送者
    sendMessage(client.ws, {
      type: 'message_queued',
      payload: {
        target: targetClientId,
        queueSize: offlineMessages.get(targetClientId).length
      }
    });
  }
}

// 处理命令
function handleCommand(clientId, client, msg) {
  const { target, payload } = msg;
  
  // 查找目标客户端
  let targetClientId = target;
  
  if (instances.has(target)) {
    targetClientId = instances.get(target);
  }
  
  const targetClient = clients.get(targetClientId);
  
  if (targetClient) {
    log('info', `Routing command: ${clientId} -> ${targetClientId}`);
    
    sendMessage(targetClient.ws, {
      type: 'command',
      from: clientId,
      payload: payload
    });
  } else {
    log('warn', `Command target not found: ${targetClientId}`);
    sendMessage(client.ws, {
      type: 'error',
      payload: { message: 'Target not found' }
    });
  }
}

// 处理 sub-agent 更新
function handleSubAgentUpdate(clientId, client, msg) {
  const { payload } = msg;
  
  log('info', `Sub-agent update from ${clientId}`, {
    subagentCount: payload?.subagents?.length || 0
  });
  
  // 广播给所有 Android 客户端
  clients.forEach((c, cid) => {
    if (c.type === 'android' && cid !== clientId) {
      sendMessage(c.ws, {
        type: 'subagent_update',
        from: clientId,
        payload: payload
      });
    }
  });
}

// 处理心跳
function handlePing(clientId, client) {
  client.lastPing = Date.now();
  sendMessage(client.ws, { type: 'pong' });
}

// 清理客户端
function cleanupClient(clientId) {
  const client = clients.get(clientId);
  
  if (client) {
    // 如果是 OpenClaw 实例，从实例列表移除
    if (client.instanceId) {
      instances.delete(client.instanceId);
      
      log('info', `OpenClaw instance offline: ${client.instanceId}`);
      
      // 广播实例下线
      broadcast({
        type: 'instance_offline',
        payload: {
          instanceId: client.instanceId,
          instanceName: client.instanceName,
          timestamp: Date.now()
        }
      });
      
      // 广播更新后的实例列表
      broadcastInstanceList();
    }
    
    clients.delete(clientId);
    log('info', `Client disconnected: ${clientId}`, {
      type: client.type,
      totalClients: clients.size
    });
  }
}

// 创建 HTTP 服务器
const server = http.createServer((req, res) => {
  if (req.url === '/health' && req.method === 'GET') {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({
      status: 'ok',
      uptime: process.uptime(),
      clients: clients.size,
      instances: instances.size,
      timestamp: Date.now()
    }));
  } else {
    res.writeHead(404);
    res.end('Not Found');
  }
});

// 创建 WebSocket 服务器
const wss = new WebSocket.Server({ server });

server.listen(PORT, () => {
  log('success', `🦞 OpenClaw Relay Server started on port ${PORT}`);
});

wss.on('connection', (ws) => {
  const clientId = generateClientId();
  
  const client = {
    ws,
    type: 'unknown',
    metadata: {},
    lastPing: Date.now()
  };
  
  clients.set(clientId, client);
  
  log('info', `New connection: ${clientId}`, {
    totalClients: clients.size
  });
  
  // 发送欢迎消息
  sendMessage(ws, {
    type: 'welcome',
    clientId,
    timestamp: Date.now()
  });
  
  // 处理消息
  ws.on('message', (data) => {
    try {
      const msg = JSON.parse(data.toString());
      
      log('debug', `Message from ${clientId}: ${msg.type}`);
      
      switch (msg.type) {
        case 'register':
          handleRegister(clientId, client, msg.payload || {});
          break;
          
        case 'message':
          handleMessage(clientId, client, msg);
          break;
          
        case 'command':
          handleCommand(clientId, client, msg);
          break;
          
        case 'subagent_update':
          handleSubAgentUpdate(clientId, client, msg);
          break;
          
        case 'ping':
          handlePing(clientId, client);
          break;
          
        case 'broadcast':
          const sent = broadcast(msg.payload, clientId);
          log('info', `Broadcast from ${clientId}, sent to ${sent} clients`);
          break;
          
        default:
          log('warn', `Unknown message type: ${msg.type}`, { clientId });
      }
      
    } catch (error) {
      log('error', 'Failed to parse message', {
        clientId,
        error: error.message
      });
    }
  });
  
  // 处理错误
  ws.on('error', (error) => {
    log('error', `WebSocket error: ${clientId}`, {
      error: error.message
    });
  });
  
  // 处理断开
  ws.on('close', () => {
    cleanupClient(clientId);
  });
});

// 心跳检测（每 60 秒）
setInterval(() => {
  const now = Date.now();
  const timeout = 90000; // 90 秒超时
  
  clients.forEach((client, clientId) => {
    if (now - client.lastPing > timeout) {
      log('warn', `Client timeout: ${clientId}`);
      client.ws.close();
      cleanupClient(clientId);
    }
  });
}, 60000);

// 状态报告（每 5 分钟）
setInterval(() => {
  log('info', 'Server status', {
    totalClients: clients.size,
    openclawInstances: instances.size,
    offlineQueues: offlineMessages.size
  });
}, 300000);

// 优雅退出
process.on('SIGINT', () => {
  log('info', 'Shutting down...');
  
  // 通知所有客户端
  broadcast({
    type: 'server_shutdown',
    payload: { message: 'Server is shutting down' }
  });
  
  // 关闭所有连接
  clients.forEach((client) => {
    client.ws.close();
  });
  
  wss.close(() => {
    log('success', 'Server stopped');
    process.exit(0);
  });
});

process.on('SIGTERM', () => {
  log('info', 'Received SIGTERM, shutting down...');
  process.emit('SIGINT');
});
