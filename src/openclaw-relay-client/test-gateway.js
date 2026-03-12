const WebSocket = require('ws');

const gatewayUrl = 'ws://127.0.0.1:18789';
const gatewayToken = 'c967680d0c039aa402fc3017ed6f2f0c88ea1e0338aeff3c';

console.log('🧪 Testing OpenClaw Gateway connection...');
console.log(`Gateway URL: ${gatewayUrl}`);

const ws = new WebSocket(gatewayUrl);
let requestId = `req_${Date.now()}`;

ws.on('open', () => {
  console.log('✅ WebSocket connected');
  console.log('📤 Sending connect request...');
  
  ws.send(JSON.stringify({
    type: 'req',
    id: requestId,
    method: 'connect',
    params: {
      minProtocol: 3,
      maxProtocol: 3,
      client: {
        id: 'cli',
        version: '1.0.0',
        platform: process.platform,
        mode: 'operator'
      },
      role: 'operator',
      scopes: ['operator.read', 'operator.write'],
      caps: [],
      commands: [],
      permissions: {},
      auth: { token: gatewayToken },
      locale: 'zh-CN',
      userAgent: 'openclaw-relay-client/1.0.0'
    }
  }));
});

ws.on('message', (data) => {
  const msg = JSON.parse(data.toString());
  console.log('📥 Received:', JSON.stringify(msg, null, 2));
  
  if (msg.type === 'res' && msg.id === requestId && msg.ok) {
    console.log('✅ Connected successfully!');
    console.log('📤 Sending test message to agent:main:main...');
    
    requestId = `req_${Date.now()}`;
    ws.send(JSON.stringify({
      type: 'req',
      id: requestId,
      method: 'sessions_send',
      params: {
        sessionKey: 'agent:main:main',
        message: '测试消息：你好翎绛！',
        metadata: {
          source: 'test-script'
        }
      }
    }));
  }
});

ws.on('error', (error) => {
  console.error('❌ WebSocket error:', error.message);
});

ws.on('close', (code, reason) => {
  console.log(`🔌 Connection closed: ${code} ${reason || ''}`);
  process.exit(0);
});

setTimeout(() => {
  console.log('⏱️  Timeout, closing...');
  ws.close();
}, 10000);
