const WebSocket = require('ws');
const { exec } = require('child_process');
const { promisify } = require('util');
const fs = require('fs');
const path = require('path');
const os = require('os');

const execAsync = promisify(exec);

class OpenClawRelayClient {
  constructor(config) {
    this.config = config;
    this.ws = null;
    this.connected = false;
    this.reconnectAttempts = 0;
    this.heartbeatTimer = null;
    this.reconnectTimer = null;
    this.subagentPollTimer = null;
    this.pendingMessages = [];
    this.monitorSessionKey = null; // 监控会话
    this.monitorSessionReady = false;
    this.activeTasks = new Map(); // taskId -> { process, startTime, status }
    this.lastSubAgentStates = new Map(); // 记录上次状态，用于检测变化
    this.notifiedSubAgents = new Set(); // 已通知的 sessionKey，防止重复
    this.healthCheckTimer = null; // 健康检查定时器
    this.healthThresholds = {
      cpu: 80, // CPU 使用率阈值 (%)
      memory: 85, // 内存使用率阈值 (%)
      disk: 85 // 磁盘使用率阈值 (%)
    };
  }

  log(level, message, data = null) {
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

  connect() {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      this.log('warn', 'Already connected');
      return;
    }

    this.log('info', `Connecting to relay server: ${this.config.relayUrl}`);
    
    try {
      this.ws = new WebSocket(this.config.relayUrl);
      
      this.ws.on('open', () => this.onOpen());
      this.ws.on('message', (data) => this.onMessage(data));
      this.ws.on('error', (error) => this.onError(error));
      this.ws.on('close', (code, reason) => this.onClose(code, reason));
      
    } catch (error) {
      this.log('error', 'Failed to create WebSocket', { error: error.message });
      this.scheduleReconnect();
    }
  }

  onOpen() {
    this.log('success', 'Connected to relay server');
    this.connected = true;
    this.reconnectAttempts = 0;
    
    // 先注册，等收到 registered 确认后再启动其他功能
    // 避免 health_alert 等消息在注册完成前发出
    this.register();
  }

  onRegistered() {
    this.log('success', 'Registration confirmed, starting services');
    this.startHeartbeat();
    this.startHealthCheck();
    this.flushPendingMessages();
  }

  onMessage(data) {
    try {
      const msg = JSON.parse(data.toString());
      this.log('debug', `Received message: ${msg.type}`);
      
      switch (msg.type) {
        case 'registered':
          this.log('success', 'Registration confirmed', msg.payload);
          this.onRegistered();
          break;
          
        case 'message':
          this.handleIncomingMessage(msg);
          break;
        
        case 'command':
          this.handleCommand(msg);
          break;
          
        case 'task_dispatch':
          this.handleTaskDispatch(msg);
          break;
          
        case 'task_cancel':
          this.handleTaskCancel(msg);
          break;
          
        case 'memory_search':
          this.handleMemorySearch(msg);
          break;
          
        case 'pong':
          this.log('debug', 'Heartbeat acknowledged');
          break;
          
        case 'error':
          this.log('error', 'Server error', msg.payload);
          break;
          
        default:
          this.log('warn', `Unknown message type: ${msg.type}`, msg);
      }
    } catch (error) {
      this.log('error', 'Failed to parse message', { error: error.message, data: data.toString() });
    }
  }

  onError(error) {
    this.log('error', 'WebSocket error', { error: error.message });
  }

  onClose(code, reason) {
    this.log('warn', `Connection closed: ${code} ${reason || '(no reason)'}`);
    this.connected = false;
    this.ws = null; // 清空引用，确保重连时能创建新连接
    this.stopHeartbeat();
    this.stopSubAgentPolling();
    this.stopHealthCheck();
    this.scheduleReconnect();
  }

  register() {
    this.log('info', 'Registering as OpenClaw instance');
    this.send({
      type: 'register',
      payload: {
        type: 'openclaw',
        instanceId: this.config.instanceId,
        instanceName: this.config.instanceName,
        token: this.config.relayAuthToken || '84c348bea7be634216ef5277cf84e4b2bfbbbf2df3d6d2e3',
        metadata: {
          version: '1.0.0',
          platform: process.platform,
          nodeVersion: process.version
        }
      }
    });
  }

  async handleIncomingMessage(msg) {
    const { from, payload } = msg;
    const { sessionKey, message } = payload || {};
    
    if (!message) {
      this.log('error', 'Invalid message payload', msg);
      return;
    }
    
    this.log('info', `Processing message from ${from}`, { message: message.substring(0, 50) });
    
    // 生成消息 ID 用于流式推送
    const messageId = `msg-${Date.now()}-${Math.random().toString(36).substr(2, 6)}`;
    // 生成 requestId 用于 process_update 关联
    const requestId = `req-${Date.now()}-${Math.random().toString(36).substr(2, 6)}`;
    // 使用独立会话 ID，避免跟 webchat 主会话冲突
    const appSessionId = `app-relay-${from}`;
    const startTime = Date.now();
    
    // 通知 App 开始生成回复（保留原有 stream_start）
    this.send({
      type: 'stream_start',
      target: from,
      payload: { messageId, requestId, timestamp: Date.now() }
    });
    
    // 发送 process_update: thinking_start
    this.sendProcessUpdate(requestId, 'thinking_start', { messageId });
    
    // 定期发心跳，让 App 知道还在处理
    const heartbeatInterval = setInterval(() => {
      const elapsed = Math.floor((Date.now() - startTime) / 1000);
      this.sendProcessUpdate(requestId, 'thinking_content', {
        text: `处理中... (${elapsed}s)`,
        elapsed
      });
    }, 5000);
    
    try {
      const replyText = await this.callOpenClawWithStreaming(message, appSessionId, from, messageId);
      
      clearInterval(heartbeatInterval);
      
      this.log('success', 'Got reply from OpenClaw', { length: replyText.length });
      
      // 发送 process_update: complete
      this.sendProcessUpdate(requestId, 'complete', {
        messageId,
        finalText: replyText.substring(0, 500)
      });
      
      // 通知 App 生成完成（保留原有 stream_end，带上 requestId）
      this.send({
        type: 'stream_end',
        target: from,
        payload: { messageId, requestId, text: replyText, timestamp: Date.now() }
      });
      
    } catch (error) {
      clearInterval(heartbeatInterval);
      
      this.log('error', 'Failed to process message', { 
        error: error.message,
        code: error.code
      });
      
      // 发送 process_update: error
      this.sendProcessUpdate(requestId, 'error', {
        messageId,
        error: 'execution_error',
        message: error.message
      });
      
      // 通知 App 生成失败（保留原有 stream_end）
      this.send({
        type: 'stream_end',
        target: from,
        payload: {
          messageId,
          requestId,
          text: `❌ Error: ${error.message}`,
          error: true,
          timestamp: Date.now()
        }
      });
    }
  }

  /**
   * 使用独立会话调用 OpenClaw，并分段推送结果到 App
   * 
   * 改进点：
   * 1. 使用 --session-id 避免跟 webchat 主会话冲突
   * 2. 使用 --json 获取结构化输出
   * 3. 将长回复按段落分割，逐段推送给 App（模拟流式输出）
   */
  async callOpenClawWithStreaming(message, sessionId, targetClientId, messageId) {
    this.log('info', 'Calling OpenClaw via independent session', { 
      message: message.substring(0, 50),
      sessionId 
    });
    
    try {
      const { stdout, stderr } = await execAsync(
        `openclaw agent --session-id ${JSON.stringify(sessionId)} --message ${JSON.stringify(message)} --json --timeout 120`,
        {
          env: {
            ...process.env,
            OPENCLAW_GATEWAY_TOKEN: this.config.openclawToken
          },
          maxBuffer: 10 * 1024 * 1024 // 10MB
        }
      );
      
      if (stderr) {
        this.log('warn', 'OpenClaw CLI stderr', { stderr: stderr.substring(0, 200) });
      }
      
      // 解析 JSON 输出
      let replyText = '';
      try {
        const result = JSON.parse(stdout);
        if (result.result && result.result.payloads) {
          replyText = result.result.payloads
            .map(p => p.text)
            .filter(Boolean)
            .join('\n');
        }
      } catch (e) {
        // JSON 解析失败，用原始输出
        replyText = stdout.trim();
      }
      
      if (!replyText) {
        throw new Error('Empty reply from OpenClaw');
      }
      
      // 分段推送：按段落或每 100 字符切分，模拟流式输出
      const chunks = this.splitIntoChunks(replyText);
      
      for (let i = 0; i < chunks.length; i++) {
        this.send({
          type: 'stream_chunk',
          target: targetClientId,
          payload: {
            messageId,
            chunk: chunks[i],
            index: i,
            total: chunks.length,
            timestamp: Date.now()
          }
        });
        
        // 每段之间加小延迟，模拟打字效果
        if (i < chunks.length - 1) {
          await new Promise(resolve => setTimeout(resolve, 80));
        }
      }
      
      return replyText;
      
    } catch (error) {
      this.log('error', 'OpenClaw call error', { 
        error: error.message,
        stdout: error.stdout?.substring(0, 200),
        stderr: error.stderr?.substring(0, 200)
      });
      throw new Error(`OpenClaw call failed: ${error.message}`);
    }
  }

  /**
   * 将文本按段落分割为流式推送的块
   * 优先按段落分，段落太长则按句子分，再长按字符数分
   */
  splitIntoChunks(text) {
    const MAX_CHUNK_SIZE = 150;
    const chunks = [];
    
    // 先按段落分
    const paragraphs = text.split(/\n\n+/);
    
    for (const para of paragraphs) {
      if (para.length <= MAX_CHUNK_SIZE) {
        chunks.push(para);
      } else {
        // 段落太长，按句子分
        const sentences = para.split(/(?<=[。！？.!?\n])\s*/);
        let current = '';
        
        for (const sentence of sentences) {
          if (current.length + sentence.length > MAX_CHUNK_SIZE && current.length > 0) {
            chunks.push(current);
            current = sentence;
          } else {
            current += (current ? '' : '') + sentence;
          }
        }
        
        if (current) {
          chunks.push(current);
        }
      }
    }
    
    // 如果整个文本就一句话且很短，直接作为一个 chunk
    if (chunks.length === 0) {
      chunks.push(text);
    }
    
    return chunks;
  }

  send(data) {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      try {
        this.ws.send(JSON.stringify(data));
        return true;
      } catch (error) {
        this.log('error', 'Failed to send message', { error: error.message });
        return false;
      }
    } else {
      this.log('warn', 'Not connected, queueing message');
      this.pendingMessages.push(data);
      return false;
    }
  }

  flushPendingMessages() {
    if (this.pendingMessages.length === 0) return;
    
    this.log('info', `Flushing ${this.pendingMessages.length} pending messages`);
    
    while (this.pendingMessages.length > 0) {
      const msg = this.pendingMessages.shift();
      this.send(msg);
    }
  }

  startHeartbeat() {
    this.stopHeartbeat();
    
    this.heartbeatTimer = setInterval(() => {
      if (this.connected) {
        this.log('debug', 'Sending heartbeat');
        this.send({ type: 'ping' });
      }
    }, this.config.heartbeatInterval);
  }

  stopHeartbeat() {
    if (this.heartbeatTimer) {
      clearInterval(this.heartbeatTimer);
      this.heartbeatTimer = null;
    }
  }

  async pollSubAgents() {
    if (!this.monitorSessionReady) {
      this.log('warn', 'Monitor session not ready, skipping poll');
      return;
    }
    
    try {
      this.log('debug', 'Polling sub-agents via monitor session');
      
      // 使用 sessions_send 发送到监控会话
      const { stdout } = await execAsync(
        `openclaw sessions send --label linjiang-monitor --message "/subagents list" --timeout 10`,
        {
          env: {
            ...process.env,
            OPENCLAW_GATEWAY_TOKEN: this.config.openclawToken
          },
          maxBuffer: 1024 * 1024 // 1MB
        }
      );
      
      // 解析输出，提取 sub-agent 信息
      const subagents = this.parseSubAgentList(stdout);
      
      // 检测状态变化并推送通知
      this.detectStateChangesAndNotify(subagents);
      
      if (subagents.length > 0 || this.lastSubAgentCount !== 0) {
        this.log('info', `Found ${subagents.length} sub-agents`);
        
        // 发送到中继服务器
        this.send({
          type: 'subagent_update',
          payload: { subagents }
        });
        
        this.lastSubAgentCount = subagents.length;
      }
      
    } catch (error) {
      this.log('error', 'Failed to poll sub-agents', { error: error.message });
    }
  }

  detectStateChangesAndNotify(currentSubAgents) {
    for (const subagent of currentSubAgents) {
      const { sessionKey, status, label } = subagent;
      const lastState = this.lastSubAgentStates.get(sessionKey);
      
      // 新 sub-agent 或状态变化
      if (!lastState || lastState !== status) {
        // 检测完成或失败
        if ((status === 'completed' || status === 'failed') && !this.notifiedSubAgents.has(sessionKey)) {
          this.log('info', `Sub-agent state changed: ${sessionKey} → ${status}`);
          
          // 推送通知
          this.sendNotification({
            title: status === 'completed' ? 'Sub-agent 完成' : 'Sub-agent 失败',
            message: `任务「${label}」已${status === 'completed' ? '完成' : '失败'}`,
            severity: status === 'completed' ? 'success' : 'error',
            timestamp: Date.now(),
            sessionKey
          });
          
          // 标记已通知
          this.notifiedSubAgents.add(sessionKey);
        }
        
        // 更新状态记录
        this.lastSubAgentStates.set(sessionKey, status);
      }
    }
    
    // 清理已消失的 sub-agent（避免内存泄漏）
    const currentKeys = new Set(currentSubAgents.map(s => s.sessionKey));
    for (const key of this.lastSubAgentStates.keys()) {
      if (!currentKeys.has(key)) {
        this.lastSubAgentStates.delete(key);
        // 保留通知记录，避免重复通知（如果 sub-agent 重启）
      }
    }
  }

  sendNotification(notification) {
    this.log('success', `Sending notification: ${notification.title}`, notification);
    
    this.send({
      type: 'notification',
      payload: notification
    });
  }

  parseSubAgentList(output) {
    const subagents = [];
    
    try {
      // 尝试匹配 sub-agent 列表输出
      // 格式示例：
      // agent:main:subagent:xxx | label | running | 2024-03-09T02:00:00.000Z
      const lines = output.split('\n');
      
      for (const line of lines) {
        // 匹配包含 "subagent:" 的行
        if (line.includes('subagent:')) {
          const parts = line.split('|').map(p => p.trim());
          
          if (parts.length >= 3) {
            const sessionKey = parts[0];
            const label = parts[1] || sessionKey.split(':').pop();
            const status = parts[2] || 'unknown';
            const timestamp = parts[3] ? new Date(parts[3]).getTime() : Date.now();
            
            subagents.push({
              sessionKey,
              label,
              status,
              updatedAt: timestamp
            });
          }
        }
      }
    } catch (error) {
      this.log('warn', 'Failed to parse sub-agent list', { error: error.message });
    }
    
    return subagents;
  }

  startSubAgentPolling() {
    this.stopSubAgentPolling();
    this.lastSubAgentCount = 0;
    
    // 等待监控会话就绪后再开始轮询
    const waitForMonitor = setInterval(() => {
      if (this.monitorSessionReady) {
        clearInterval(waitForMonitor);
        
        // 立即执行一次
        this.pollSubAgents();
        
        // 每 5 秒轮询一次
        this.subagentPollTimer = setInterval(() => {
          if (this.connected) {
            this.pollSubAgents();
          }
        }, 5000);
        
        this.log('info', 'Started sub-agent polling (5s interval)');
      }
    }, 500);
  }

  stopSubAgentPolling() {
    if (this.subagentPollTimer) {
      clearInterval(this.subagentPollTimer);
      this.subagentPollTimer = null;
      this.log('info', 'Stopped sub-agent polling');
    }
  }

  /**
   * 处理任务派发消息
   * App → 中继 → 本客户端
   * 调用 openclaw agent 执行任务，定期回传状态
   */
  async handleTaskDispatch(msg) {
    const { from, payload } = msg;
    const { taskId, template, prompt, timeout = 600, priority } = payload || {};
    
    if (!taskId || !prompt) {
      this.log('error', 'Invalid task_dispatch payload', payload);
      return;
    }
    
    this.log('info', `Task dispatch received: ${taskId}`, { template, prompt: prompt.substring(0, 80), timeout });
    
    // 生成 requestId 用于 process_update 关联
    const requestId = `req-task-${taskId}`;
    
    // 立即发送 pending 状态
    this.sendTaskUpdate(taskId, 'pending', null, '任务已接收，准备执行...');
    
    // 检查是否已有同 ID 任务在执行
    if (this.activeTasks.has(taskId)) {
      this.log('warn', `Task ${taskId} already running, ignoring duplicate dispatch`);
      return;
    }
    
    const sessionId = `task-${taskId}`;
    const startTime = Date.now();
    let outputBuffer = '';
    
    // 发送 process_update: thinking_start
    this.sendProcessUpdate(requestId, 'thinking_start', { taskId });
    
    try {
      // 标记为运行中
      this.sendTaskUpdate(taskId, 'running', null, '正在启动 OpenClaw Agent...');
      
      // 使用 spawn 而不是 exec，以便获取实时输出和取消能力
      const { spawn } = require('child_process');
      
      const child = spawn('openclaw', [
        'agent',
        '--session-id', sessionId,
        '--message', prompt,
        '--json',
        '--timeout', String(timeout)
      ], {
        env: {
          ...process.env,
          OPENCLAW_GATEWAY_TOKEN: this.config.openclawToken
        },
        stdio: ['pipe', 'pipe', 'pipe']
      });
      
      // 存储活跃任务
      this.activeTasks.set(taskId, {
        process: child,
        startTime,
        status: 'running',
        from,
        requestId
      });
      
      // 定期发送状态更新（每 10 秒）+ process_update 心跳（每 5 秒）
      const updateInterval = setInterval(() => {
        if (this.activeTasks.has(taskId)) {
          const elapsed = Math.floor((Date.now() - startTime) / 1000);
          const lastOutput = outputBuffer.length > 500 
            ? '...' + outputBuffer.substring(outputBuffer.length - 500) 
            : outputBuffer;
          this.sendTaskUpdate(taskId, 'running', null, lastOutput || `执行中... (${elapsed}s)`);
        }
      }, 10000);
      
      const heartbeatInterval = setInterval(() => {
        const elapsed = Math.floor((Date.now() - startTime) / 1000);
        this.sendProcessUpdate(requestId, 'thinking_content', {
          taskId,
          text: `任务执行中... (${elapsed}s)`,
          elapsed
        });
      }, 5000);
      
      // 超时前 60 秒发警告
      let warningTimeout = null;
      if (timeout > 120) {
        warningTimeout = setTimeout(() => {
          const elapsed = Math.floor((Date.now() - startTime) / 1000);
          this.sendProcessUpdate(requestId, 'timeout_warning', {
            taskId,
            elapsed,
            limit: timeout,
            message: `任务较大，已运行 ${Math.floor(elapsed / 60)} 分钟`
          });
        }, (timeout - 60) * 1000);
      }
      
      // 收集 stdout，尝试解析工具调用
      child.stdout.on('data', (data) => {
        const text = data.toString();
        outputBuffer += text;
        this.log('debug', `Task ${taskId} stdout: ${text.substring(0, 100)}`);
        
        // 尝试从 JSON 行中解析工具调用事件
        for (const line of text.split('\n')) {
          if (!line.trim()) continue;
          try {
            const event = JSON.parse(line);
            if (event.type === 'tool_use') {
              this.sendProcessUpdate(requestId, 'tool_call', {
                taskId,
                tool: event.name,
                args: JSON.stringify(event.input || {}).substring(0, 200),
                callId: event.id
              });
            }
          } catch (e) {
            // 非 JSON 行，忽略
          }
        }
      });
      
      // 收集 stderr
      child.stderr.on('data', (data) => {
        const text = data.toString();
        outputBuffer += text;
        this.log('debug', `Task ${taskId} stderr: ${text.substring(0, 100)}`);
      });
      
      // 等待完成
      await new Promise((resolve, reject) => {
        child.on('close', (code) => {
          clearInterval(updateInterval);
          clearInterval(heartbeatInterval);
          if (warningTimeout) clearTimeout(warningTimeout);
          
          if (code === 0) {
            resolve();
          } else {
            reject(new Error(`Process exited with code ${code}`));
          }
        });
        
        child.on('error', (err) => {
          clearInterval(updateInterval);
          clearInterval(heartbeatInterval);
          if (warningTimeout) clearTimeout(warningTimeout);
          reject(err);
        });
      });
      
      // 解析结果
      let resultText = '';
      try {
        const result = JSON.parse(outputBuffer);
        if (result.result && result.result.payloads) {
          resultText = result.result.payloads
            .map(p => p.text)
            .filter(Boolean)
            .join('\n');
        }
      } catch (e) {
        resultText = outputBuffer.trim();
      }
      
      if (!resultText) {
        resultText = '任务已完成（无输出）';
      }
      
      this.log('success', `Task ${taskId} completed`, { outputLength: resultText.length });
      
      // 发送 process_update: complete
      this.sendProcessUpdate(requestId, 'complete', {
        taskId,
        finalText: resultText.substring(0, 500)
      });
      
      this.sendTaskUpdate(taskId, 'completed', 100, resultText);
      
    } catch (error) {
      this.log('error', `Task ${taskId} failed`, { error: error.message });
      
      // 发送 process_update: error
      this.sendProcessUpdate(requestId, 'error', {
        taskId,
        error: 'execution_error',
        message: error.message
      });
      
      const errorOutput = outputBuffer 
        ? outputBuffer + '\n\n❌ ' + error.message 
        : '❌ ' + error.message;
      
      this.sendTaskUpdate(taskId, 'failed', null, errorOutput);
      
    } finally {
      this.activeTasks.delete(taskId);
    }
  }
  
  /**
   * 处理任务取消消息
   */
  handleTaskCancel(msg) {
    const { payload } = msg;
    const { taskId } = payload || {};
    
    if (!taskId) {
      this.log('error', 'Invalid task_cancel payload', payload);
      return;
    }
    
    this.log('info', `Task cancel received: ${taskId}`);
    
    const task = this.activeTasks.get(taskId);
    if (task && task.process) {
      try {
        task.process.kill('SIGTERM');
        // 给 3 秒优雅退出，否则 SIGKILL
        setTimeout(() => {
          try {
            task.process.kill('SIGKILL');
          } catch (e) {
            // 可能已经退出了
          }
        }, 3000);
        
        this.sendTaskUpdate(taskId, 'failed', null, '任务已被用户终止');
        this.activeTasks.delete(taskId);
        this.log('success', `Task ${taskId} cancelled`);
      } catch (error) {
        this.log('error', `Failed to cancel task ${taskId}`, { error: error.message });
      }
    } else {
      this.log('warn', `Task ${taskId} not found in active tasks`);
      this.sendTaskUpdate(taskId, 'failed', null, '任务未找到或已结束');
    }
  }
  
  /**
   * 发送任务状态更新到中继服务器
   */
  sendTaskUpdate(taskId, status, progress, output) {
    this.send({
      type: 'task_update',
      payload: {
        taskId,
        instanceId: this.config.instanceId,
        status,
        progress,
        output: output || '',
        timestamp: Date.now()
      }
    });
  }

  /**
   * 发送 process_update 到中继服务器
   * 让 App 实时了解 AI 处理进度（思考中、调用工具、完成等）
   * 
   * phases: thinking_start, thinking_content, tool_call, 
   *         timeout_warning, complete, error
   */
  sendProcessUpdate(requestId, phase, data) {
    this.send({
      type: 'process_update',
      payload: {
        instanceId: this.config.instanceId,
        requestId,
        phase,
        data,
        timestamp: Date.now()
      }
    });
  }

  /**
   * 处理记忆搜索消息
   * App → 中继 → 本客户端
   * 调用 openclaw agent 搜索记忆，返回 memory_result
   */
  async handleMemorySearch(msg) {
    const { from, payload } = msg;
    const { query } = payload || {};
    
    this.log('info', `Memory search received from ${from}`, { query });
    
    const searchQuery = query 
      ? `搜索记忆: ${query}` 
      : '列出最近的记忆条目，包括项目进度、教训、重要事项。按分类整理，每条包含标题和简短摘要。';
    
    const sessionId = 'memory-search';
    
    try {
      const { stdout, stderr } = await execAsync(
        `openclaw agent --session-id ${JSON.stringify(sessionId)} --message ${JSON.stringify(searchQuery)} --json --timeout 60`,
        {
          env: {
            ...process.env,
            OPENCLAW_GATEWAY_TOKEN: this.config.openclawToken
          },
          maxBuffer: 10 * 1024 * 1024
        }
      );
      
      if (stderr) {
        this.log('warn', 'Memory search stderr', { stderr: stderr.substring(0, 200) });
      }
      
      // 解析 JSON 输出
      let resultText = '';
      try {
        const result = JSON.parse(stdout);
        if (result.result && result.result.payloads) {
          resultText = result.result.payloads
            .map(p => p.text)
            .filter(Boolean)
            .join('\n');
        }
      } catch (e) {
        resultText = stdout.trim();
      }
      
      if (!resultText) {
        resultText = '没有找到相关记忆';
      }
      
      this.log('success', `Memory search completed`, { resultLength: resultText.length });
      
      // 发送结果回 App
      this.send({
        type: 'memory_result',
        target: from,
        payload: {
          query: query || '',
          result: resultText,
          timestamp: Date.now()
        }
      });
      
    } catch (error) {
      this.log('error', 'Memory search failed', { error: error.message });
      
      this.send({
        type: 'memory_result',
        target: from,
        payload: {
          query: query || '',
          result: '',
          error: `搜索失败: ${error.message}`,
          timestamp: Date.now()
        }
      });
    }
  }

  handleCommand(msg) {
    const { payload } = msg;
    const { command } = payload || {};
    
    this.log('info', `Received command: ${command}`);
    
    switch (command) {
      case 'request_subagent_update':
        this.pollSubAgents();
        break;
        
      case 'kill_subagent':
        this.killSubAgent(payload.sessionKey);
        break;
        
      default:
        this.log('warn', `Unknown command: ${command}`);
    }
  }

  async killSubAgent(sessionKey) {
    try {
      this.log('info', `Killing sub-agent: ${sessionKey}`);
      
      const { stdout } = await execAsync(
        `openclaw agent --agent main --message "/subagents kill ${sessionKey}" --timeout 10`,
        {
          env: {
            ...process.env,
            OPENCLAW_GATEWAY_TOKEN: this.config.openclawToken
          }
        }
      );
      
      this.log('success', `Sub-agent killed: ${sessionKey}`);
      
      // 立即刷新列表
      setTimeout(() => this.pollSubAgents(), 500);
      
    } catch (error) {
      this.log('error', `Failed to kill sub-agent: ${sessionKey}`, { error: error.message });
    }
  }

  scheduleReconnect() {
    if (this.reconnectTimer) {
      return; // Already scheduled
    }
    
    this.reconnectAttempts++;
    
    // 指数退避
    const baseDelay = this.config.reconnectDelay;
    const maxDelay = this.config.maxReconnectDelay;
    const backoff = this.config.reconnectBackoff;
    
    const delay = Math.min(
      baseDelay * Math.pow(backoff, this.reconnectAttempts - 1),
      maxDelay
    );
    
    this.log('info', `Reconnecting in ${(delay / 1000).toFixed(1)}s (attempt ${this.reconnectAttempts})`);
    
    this.reconnectTimer = setTimeout(() => {
      this.reconnectTimer = null;
      this.connect();
    }, delay);
  }

  startHealthCheck() {
    this.stopHealthCheck();
    
    // 立即执行一次
    this.checkHealth();
    this.reportHealth();
    
    // 每 60 秒上报健康数据（态势大盘用）
    this.healthReportTimer = setInterval(() => {
      if (this.connected) {
        this.reportHealth();
      }
    }, 60 * 1000);
    
    // 每 5 分钟检查阈值（告警用）
    this.healthCheckTimer = setInterval(() => {
      if (this.connected) {
        this.checkHealth();
      }
    }, 5 * 60 * 1000);
    
    this.log('info', 'Started health reporting (60s) + health check (5min)');
  }

  stopHealthCheck() {
    if (this.healthCheckTimer) {
      clearInterval(this.healthCheckTimer);
      this.healthCheckTimer = null;
    }
    if (this.healthReportTimer) {
      clearInterval(this.healthReportTimer);
      this.healthReportTimer = null;
    }
    this.log('info', 'Stopped health check');
  }

  /**
   * 上报完整健康数据（态势大盘）
   * 消息类型：health_report（按 DESIGN.md 协议）
   */
  async reportHealth() {
    try {
      const cpuPercent = parseFloat(((os.loadavg()[0] / os.cpus().length) * 100).toFixed(1));
      const memUsed = parseFloat(((os.totalmem() - os.freemem()) / (1024 * 1024 * 1024)).toFixed(1));
      const memTotal = parseFloat((os.totalmem() / (1024 * 1024 * 1024)).toFixed(1));
      
      let diskPercent = 0;
      let diskUsedGb = 0;
      let diskTotalGb = 0;
      try {
        const { stdout } = await execAsync("df -BG / | tail -1 | awk '{print $2, $3, $5}'");
        const parts = stdout.trim().split(/\s+/);
        if (parts.length >= 3) {
          diskTotalGb = parseInt(parts[0]);
          diskUsedGb = parseInt(parts[1]);
          diskPercent = parseInt(parts[2]);
        }
      } catch (e) {
        // fallback
      }
      
      this.send({
        type: 'health_report',
        payload: {
          instanceId: this.config.instanceId,
          cpu: cpuPercent,
          memory: {
            used: memUsed,
            total: memTotal
          },
          disk: {
            used: diskUsedGb,
            total: diskTotalGb,
            percent: diskPercent
          },
          uptime: os.uptime(),
          activeSubAgents: this.lastSubAgentCount || 0,
          timestamp: Date.now()
        }
      });
      
      this.log('debug', `Health report: CPU=${cpuPercent}% MEM=${memUsed}/${memTotal}G DISK=${diskPercent}%`);
    } catch (error) {
      this.log('error', 'Failed to report health', { error: error.message });
    }
  }

  async checkHealth() {
    try {
      this.log('debug', 'Checking system health');
      
      // CPU 使用率
      const cpuUsage = (os.loadavg()[0] / os.cpus().length) * 100;
      if (cpuUsage > this.healthThresholds.cpu) {
        this.sendHealthAlert('cpu', cpuUsage.toFixed(1), cpuUsage > 90 ? 'critical' : 'warning');
      }
      
      // 内存使用率
      const memoryUsage = (1 - os.freemem() / os.totalmem()) * 100;
      if (memoryUsage > this.healthThresholds.memory) {
        this.sendHealthAlert('memory', memoryUsage.toFixed(1), memoryUsage > 95 ? 'critical' : 'warning');
      }
      
      // 磁盘使用率
      try {
        const { stdout } = await execAsync('df -h / | tail -1');
        const match = stdout.match(/(\d+)%/);
        if (match) {
          const diskUsage = parseInt(match[1]);
          if (diskUsage > this.healthThresholds.disk) {
            this.sendHealthAlert('disk', `${diskUsage}`, diskUsage > 95 ? 'critical' : 'warning');
          }
        }
      } catch (error) {
        this.log('warn', 'Failed to check disk usage', { error: error.message });
      }
      
    } catch (error) {
      this.log('error', 'Health check failed', { error: error.message });
    }
  }

  sendHealthAlert(metric, value, severity) {
    const messages = {
      cpu: `CPU 使用率过高: ${value}%`,
      memory: `内存使用率过高: ${value}%`,
      disk: `磁盘使用率过高: ${value}%`
    };
    
    this.log('warn', `Health alert: ${metric} = ${value}%`, { severity });
    
    this.send({
      type: 'health_alert',
      payload: {
        severity,
        metric,
        value: `${value}%`,
        message: messages[metric] || `${metric} 异常`,
        timestamp: Date.now()
      }
    });
  }

  disconnect() {
    this.log('info', 'Disconnecting...');
    
    this.stopHeartbeat();
    this.stopSubAgentPolling();
    this.stopHealthCheck(); // 停止健康检查
    
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer);
      this.reconnectTimer = null;
    }
    
    if (this.ws) {
      this.ws.close();
      this.ws = null;
    }
    
    this.connected = false;
    this.monitorSessionReady = false;
  }
  
  async ensureMonitorSession() {
    try {
      this.log('info', 'Checking for monitor session');
      
      // 检查是否已存在 linjiang-monitor 会话
      const { stdout: listOutput } = await execAsync(
        `openclaw sessions list`,
        {
          env: {
            ...process.env,
            OPENCLAW_GATEWAY_TOKEN: this.config.openclawToken
          }
        }
      );
      
      // 查找 linjiang-monitor 会话
      if (listOutput.includes('linjiang-monitor')) {
        this.log('success', 'Monitor session already exists');
        this.monitorSessionReady = true;
        this.startSubAgentPolling();
        return;
      }
      
      // 创建新的监控会话
      this.log('info', 'Creating monitor session');
      
      const { stdout: spawnOutput } = await execAsync(
        `openclaw sessions spawn --label linjiang-monitor --mode session --task "你是一个专门处理状态查询的监控会话。只回答 /subagents list 等查询命令，不需要额外解释。" --runtime subagent`,
        {
          env: {
            ...process.env,
            OPENCLAW_GATEWAY_TOKEN: this.config.openclawToken
          },
          maxBuffer: 1024 * 1024
        }
      );
      
      this.log('success', 'Monitor session created', { output: spawnOutput.substring(0, 200) });
      
      // 等待会话初始化（2秒）
      await new Promise(resolve => setTimeout(resolve, 2000));
      
      this.monitorSessionReady = true;
      this.startSubAgentPolling();
      
    } catch (error) {
      this.log('error', 'Failed to ensure monitor session', { error: error.message });
      
      // 5秒后重试
      setTimeout(() => {
        if (this.connected) {
          this.ensureMonitorSession();
        }
      }, 5000);
    }
  }
}

// 启动
function main() {
  console.log('🦞 OpenClaw Relay Client v1.0.0');
  console.log('');
  
  // 读取配置
  const configPath = path.join(__dirname, 'config.json');
  
  if (!fs.existsSync(configPath)) {
    console.error('❌ Config file not found:', configPath);
    process.exit(1);
  }
  
  const config = JSON.parse(fs.readFileSync(configPath, 'utf8'));
  
  console.log('📋 Configuration:');
  console.log(`  Relay URL: ${config.relayUrl}`);
  console.log(`  Instance ID: ${config.instanceId}`);
  console.log(`  Instance Name: ${config.instanceName}`);
  console.log('');
  
  // 创建客户端
  const client = new OpenClawRelayClient(config);
  
  // 启动连接
  client.connect();
  
  // 优雅退出
  process.on('SIGINT', () => {
    console.log('');
    console.log('📴 Shutting down...');
    client.disconnect();
    setTimeout(() => process.exit(0), 1000);
  });
  
  process.on('SIGTERM', () => {
    console.log('');
    console.log('📴 Shutting down...');
    client.disconnect();
    setTimeout(() => process.exit(0), 1000);
  });
}

if (require.main === module) {
  main();
}

module.exports = OpenClawRelayClient;
