const WebSocket = require('ws');

const RELAY_URL = 'ws://118.25.195.154:8080';

console.log('🦞 OpenClaw Relay Server 测试');
console.log('目标:', RELAY_URL);
console.log('');

console.log('正在连接...');
const ws = new WebSocket(RELAY_URL);

let startTime;

ws.on('open', () => {
  console.log('✅ 连接成功');
  
  // 注册
  console.log('发送注册消息...');
  ws.send(JSON.stringify({
    type: 'register',
    payload: { 
      type: 'test-client', 
      metadata: { 
        version: '1.0.0',
        location: 'virtual-machine'
      } 
    }
  }));
  
  // 等待注册确认后发送 ping
  setTimeout(() => {
    console.log('发送心跳测试...');
    startTime = Date.now();
    ws.send(JSON.stringify({ type: 'ping' }));
  }, 500);
});

ws.on('message', (data) => {
  const msg = JSON.parse(data.toString());
  console.log('收到消息:', msg.type);
  
  if (msg.type === 'pong') {
    const latency = Date.now() - startTime;
    console.log(`✅ 延迟: ${latency}ms`);
    console.log('');
    console.log('测试完成！');
    ws.close();
  }
});

ws.on('error', (err) => {
  console.error('❌ 连接错误:', err.message);
  process.exit(1);
});

ws.on('close', () => {
  console.log('连接已关闭');
});

// 超时保护
setTimeout(() => {
  console.error('❌ 测试超时（30秒）');
  ws.close();
  process.exit(1);
}, 30000);
