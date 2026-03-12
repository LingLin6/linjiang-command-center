package com.linjiang.command.network

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.*
import okio.ByteString
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * WebSocket 管理器
 * 负责连接管理、消息收发、心跳、自动重连
 */
class WebSocketManager(
    private val url: String,
    private val onMessage: (String) -> Unit,
    private val onConnectionChange: (Boolean) -> Unit,
    private val onToast: (String) -> Unit = {}
) {
    private val TAG = "WebSocketManager"
    
    private var webSocket: WebSocket? = null
    private val client = OkHttpClient.Builder()
        .pingInterval(30, TimeUnit.SECONDS)
        .build()
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected
    
    private var reconnectAttempts = 0
    private val maxReconnectAttempts = 5
    private var reconnectJob: Job? = null
    
    private var heartbeatJob: Job? = null
    
    private val listener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            Log.d(TAG, "WebSocket connected")
            _isConnected.value = true
            reconnectAttempts = 0
            onConnectionChange(true)
            onToast("连接成功")
            startHeartbeat()
        }
        
        override fun onMessage(webSocket: WebSocket, text: String) {
            Log.d(TAG, "Received: $text")
            onMessage(text)
        }
        
        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
            Log.d(TAG, "Received bytes: ${bytes.hex()}")
        }
        
        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            Log.d(TAG, "WebSocket closing: $code $reason")
            webSocket.close(1000, null)
        }
        
        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            Log.d(TAG, "WebSocket closed: $code $reason")
            _isConnected.value = false
            onConnectionChange(false)
            onToast("连接已断开")
            stopHeartbeat()
        }
        
        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Log.e(TAG, "WebSocket error: ${t.message}", t)
            _isConnected.value = false
            onConnectionChange(false)
            onToast("连接失败：${t.message ?: "未知错误"}")
            stopHeartbeat()
            scheduleReconnect()
        }
    }
    
    /**
     * 连接到 WebSocket 服务器
     */
    fun connect() {
        if (_isConnected.value) {
            Log.w(TAG, "Already connected")
            return
        }
        
        Log.d(TAG, "Connecting to $url")
        val request = Request.Builder()
            .url(url)
            .build()
        
        webSocket = client.newWebSocket(request, listener)
    }
    
    /**
     * 断开连接
     */
    fun disconnect() {
        Log.d(TAG, "Disconnecting")
        stopHeartbeat()
        reconnectJob?.cancel()
        reconnectJob = null
        reconnectAttempts = 0
        webSocket?.close(1000, "User disconnect")
        webSocket = null
        _isConnected.value = false
        onConnectionChange(false)
    }
    
    /**
     * 发送消息
     */
    fun send(message: String): Boolean {
        return if (_isConnected.value) {
            Log.d(TAG, "Sending: $message")
            webSocket?.send(message) ?: false
        } else {
            Log.w(TAG, "Cannot send, not connected")
            false
        }
    }
    
    /**
     * 发送 JSON 消息
     */
    fun sendJson(json: JSONObject): Boolean {
        return send(json.toString())
    }
    
    /**
     * 注册客户端
     */
    fun register(type: String = "android", metadata: Map<String, String> = emptyMap(), token: String? = null) {
        val json = JSONObject().apply {
            put("type", "register")
            put("payload", JSONObject().apply {
                put("type", type)
                if (token != null) put("token", token)
                put("metadata", JSONObject(metadata))
            })
        }
        sendJson(json)
    }
    
    /**
     * 发送心跳
     */
    fun ping() {
        val json = JSONObject().apply {
            put("type", "ping")
        }
        sendJson(json)
    }
    
    /**
     * 启动心跳
     */
    private fun startHeartbeat() {
        stopHeartbeat()
        
        heartbeatJob = scope.launch {
            while (isActive && _isConnected.value) {
                delay(30000) // 30秒
                if (_isConnected.value) {
                    Log.d(TAG, "Sending application-level heartbeat")
                    ping()
                }
            }
        }
    }
    
    /**
     * 停止心跳
     */
    private fun stopHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = null
    }
    
    /**
     * 调度重连（指数退避）
     */
    private fun scheduleReconnect() {
        if (reconnectAttempts >= maxReconnectAttempts) {
            Log.e(TAG, "Max reconnect attempts reached")
            return
        }
        
        reconnectJob?.cancel()
        
        val delay = (1000L * (1 shl reconnectAttempts)).coerceAtMost(30000L)
        reconnectAttempts++
        
        Log.d(TAG, "Scheduling reconnect in ${delay}ms (attempt $reconnectAttempts)")
        
        reconnectJob = scope.launch {
            delay(delay)
            connect()
        }
    }
    
    /**
     * 清理资源
     */
    fun cleanup() {
        stopHeartbeat()
        disconnect()
        scope.cancel()
        client.dispatcher.executorService.shutdown()
    }
}
