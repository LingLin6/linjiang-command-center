package com.linjiang.command.network

import android.util.Log
import com.linjiang.command.data.HealthAlert
import com.linjiang.command.data.model.Alert
import com.linjiang.command.data.model.AlertLevel
import com.linjiang.command.data.model.InstanceHealth
import com.linjiang.command.data.model.Message
import com.linjiang.command.data.model.MessageLifeState
import com.linjiang.command.data.model.MessageType
import com.linjiang.command.data.model.Notification
import com.linjiang.command.data.model.SubAgent
import com.linjiang.command.data.model.SubAgentTask
import com.linjiang.command.data.model.TaskStatus
import com.linjiang.command.data.model.TimelineEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * 中继客户端
 * 封装 WebSocket 管理器，提供高层 API
 */
class RelayClient(
    private val relayUrl: String,
    private val authToken: String = "84c348bea7be634216ef5277cf84e4b2bfbbbf2df3d6d2e3",
    private val onToast: (String) -> Unit = {}
) {
    private val TAG = "RelayClient"
    
    private var wsManager: WebSocketManager? = null
    private var clientId: String? = null
    
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages
    
    private val _subAgents = MutableStateFlow<List<SubAgent>>(emptyList())
    val subAgents: StateFlow<List<SubAgent>> = _subAgents
    
    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    val notifications: StateFlow<List<Notification>> = _notifications
    
    private val _healthAlerts = MutableStateFlow<List<HealthAlert>>(emptyList())
    val healthAlerts: StateFlow<List<HealthAlert>> = _healthAlerts
    
    private val _instanceHealthMap = MutableStateFlow<Map<String, InstanceHealth>>(emptyMap())
    val instanceHealthMap: StateFlow<Map<String, InstanceHealth>> = _instanceHealthMap
    
    private val _alerts = MutableStateFlow<List<Alert>>(emptyList())
    val alerts: StateFlow<List<Alert>> = _alerts
    
    private val _timelineEvents = MutableStateFlow<List<TimelineEvent>>(emptyList())
    val timelineEvents: StateFlow<List<TimelineEvent>> = _timelineEvents
    
    private val _subAgentTasks = MutableStateFlow<List<SubAgentTask>>(emptyList())
    val subAgentTasks: StateFlow<List<SubAgentTask>> = _subAgentTasks
    
    private val _taskLogs = MutableStateFlow<Map<String, List<String>>>(emptyMap())
    val taskLogs: StateFlow<Map<String, List<String>>> = _taskLogs
    
    private val _instances = MutableStateFlow<List<com.linjiang.command.data.Instance>>(emptyList())
    val instances: StateFlow<List<com.linjiang.command.data.Instance>> = _instances
    
    private var selectedInstanceId: String? = null
    
    private val _processUpdates = MutableStateFlow<JSONObject?>(null)
    val processUpdates: StateFlow<JSONObject?> = _processUpdates
    
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected
    
    /** 重连事件（通知 ViewModel 补拉数据） */
    private val _reconnectEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val reconnectEvent = _reconnectEvent.asSharedFlow()
    
    /** 跨实例消息事件：Triple(instanceId, instanceName, preview) */
    data class CrossInstanceMessage(
        val instanceId: String,
        val instanceName: String,
        val preview: String
    )
    private val _crossInstanceMessages = MutableSharedFlow<CrossInstanceMessage>(extraBufferCapacity = 8)
    val crossInstanceMessages = _crossInstanceMessages.asSharedFlow()
    
    /**
     * 连接到中继服务器
     */
    fun connect() {
        if (wsManager != null) {
            Log.w(TAG, "Already connected or connecting")
            return
        }
        
        wsManager = WebSocketManager(
            url = relayUrl,
            onMessage = ::handleMessage,
            onConnectionChange = { connected ->
                val wasDisconnected = !_isConnected.value
                _isConnected.value = connected
                if (connected) {
                    onToast("正在注册实例...")
                    // 连接成功后注册
                    wsManager?.register("android", mapOf(
                        "version" to "1.0.0",
                        "deviceId" to getDeviceId()
                    ), token = authToken)
                    
                    // 重连事件（非首次连接时触发补拉）
                    if (wasDisconnected) {
                        _reconnectEvent.tryEmit(Unit)
                    }
                }
            },
            onToast = onToast
        )
        
        wsManager?.connect()
    }
    
    /**
     * 断开连接
     */
    fun disconnect() {
        wsManager?.disconnect()
        wsManager?.cleanup()
        wsManager = null
        clientId = null
        _isConnected.value = false
    }
    
    /**
     * 选择 OpenClaw 实例
     */
    fun selectInstance(instanceId: String) {
        selectedInstanceId = instanceId
        Log.d(TAG, "Selected instance: $instanceId")
        val instance = _instances.value.find { it.id == instanceId }
        onToast("已选择: ${instance?.getDisplayName() ?: instanceId}")
    }
    
    /**
     * 获取当前选择的实例
     */
    fun getSelectedInstance(): com.linjiang.command.data.Instance? {
        return _instances.value.find { it.id == selectedInstanceId }
    }
    
    /**
     * 发送消息到 OpenClaw 实例
     */
    fun sendMessage(content: String, targetClientId: String? = null) {
        Log.d(TAG, "sendMessage called: content=$content, targetClientId=$targetClientId")
        
        val target = targetClientId ?: selectedInstanceId
        
        if (target == null) {
            onToast("请先选择 OpenClaw 实例")
            Log.w(TAG, "No instance selected")
            return
        }
        
        val json = JSONObject().apply {
            put("type", "message")
            put("target", target)
            put("payload", JSONObject().apply {
                put("action", "send_to_session")
                put("sessionKey", "agent:main:main")
                put("message", content)
            })
        }
        
        Log.d(TAG, "Sending JSON: $json")
        val result = wsManager?.sendJson(json)
        Log.d(TAG, "Send result: $result")
        
        // 注意：不在这里添加用户消息了，由 ChatViewModel 管理消息状态
    }
    
    /**
     * 请求刷新 Sub-agent 列表（折中方案：手动触发）
     */
    fun requestSubAgentUpdate() {
        val target = selectedInstanceId ?: return
        
        val json = JSONObject().apply {
            put("type", "command")
            put("target", target)
            put("payload", JSONObject().apply {
                put("command", "request_subagent_update")
            })
        }
        
        wsManager?.sendJson(json)
        Log.d(TAG, "Requested sub-agent update")
    }
    
    /**
     * 派发任务到 OpenClaw 实例
     * 发送 task_dispatch 消息，由中继路由到目标实例执行
     */
    fun dispatchTask(
        targetInstanceId: String?,
        taskId: String,
        template: String,
        prompt: String,
        timeout: Int = 600,
        priority: String = "normal"
    ) {
        val target = targetInstanceId ?: selectedInstanceId
        
        if (target == null) {
            onToast("请先选择 OpenClaw 实例")
            return
        }
        
        val json = JSONObject().apply {
            put("type", "task_dispatch")
            put("target", target)
            put("payload", JSONObject().apply {
                put("taskId", taskId)
                put("template", template)
                put("prompt", prompt)
                put("timeout", timeout)
                put("priority", priority)
            })
        }
        
        wsManager?.sendJson(json)
        Log.d(TAG, "Task dispatched: $taskId ($template)")
        
        // 立即在本地创建任务（pending 状态）
        val newTask = SubAgentTask(
            id = taskId,
            instanceId = target,
            label = when (template) {
                "code_review" -> "代码审查"
                "test" -> "测试"
                "doc" -> "文档"
                "self_check" -> "自检"
                "deploy" -> "部署"
                "custom" -> prompt.take(20) + if (prompt.length > 20) "..." else ""
                else -> template
            },
            status = TaskStatus.PENDING,
            progress = null,
            startTime = System.currentTimeMillis() / 1000,
            duration = 0
        )
        _subAgentTasks.value = _subAgentTasks.value + newTask
    }
    
    /**
     * 取消/终止正在执行的任务
     */
    fun cancelTask(taskId: String, targetInstanceId: String? = null) {
        val target = targetInstanceId ?: selectedInstanceId
        
        val json = JSONObject().apply {
            put("type", "task_cancel")
            if (target != null) put("target", target)
            put("payload", JSONObject().apply {
                put("taskId", taskId)
            })
        }
        
        wsManager?.sendJson(json)
        Log.d(TAG, "Task cancel sent: $taskId")
        onToast("正在终止任务...")
    }
    
    // ═══ 记忆搜索 ═══
    
    private val _memoryResult = MutableStateFlow<String?>(null)
    val memoryResult: StateFlow<String?> = _memoryResult
    
    /**
     * 搜索记忆
     * 发送 memory_search 消息到 OpenClaw 实例
     */
    fun searchMemory(query: String, targetInstanceId: String? = null) {
        val target = targetInstanceId ?: selectedInstanceId
        
        if (target == null) {
            Log.w(TAG, "No instance selected for memory search")
            return
        }
        
        val json = JSONObject().apply {
            put("type", "memory_search")
            put("target", target)
            put("payload", JSONObject().apply {
                put("query", query)
                put("timestamp", System.currentTimeMillis())
            })
        }
        
        _memoryResult.value = null // 清空上次结果
        wsManager?.sendJson(json)
        Log.d(TAG, "Memory search sent: query=$query")
    }
    
    /**
     * 处理接收到的消息
     */
    private fun handleMessage(text: String) {
        try {
            val json = JSONObject(text)
            val type = json.optString("type")
            
            when (type) {
                "welcome" -> {
                    clientId = json.optString("clientId")
                    Log.d(TAG, "Received clientId: $clientId")
                }
                
                "registered" -> {
                    clientId = json.optString("clientId")
                    Log.d(TAG, "Registered with clientId: $clientId")
                    onToast("注册成功")
                }
                
                "instance_list" -> {
                    val payload = json.optJSONObject("payload")
                    val instancesArray = payload?.optJSONArray("instances")
                    
                    if (instancesArray != null) {
                        val instanceList = mutableListOf<com.linjiang.command.data.Instance>()
                        for (i in 0 until instancesArray.length()) {
                            val inst = instancesArray.getJSONObject(i)
                            instanceList.add(com.linjiang.command.data.Instance(
                                id = inst.optString("instanceId"),
                                name = inst.optString("instanceName", "翎绛"),
                                online = inst.optBoolean("online", true),
                                nickname = inst.opt("nickname") as? String,
                                emoji = inst.optString("emoji", "🪶"),
                                role = inst.opt("role") as? String
                            ))
                        }
                        _instances.value = instanceList
                        
                        // 如果没有选择实例，自动选择第一个
                        if (selectedInstanceId == null && instanceList.isNotEmpty()) {
                            selectedInstanceId = instanceList[0].id
                            Log.d(TAG, "Auto-selected first instance: ${instanceList[0].getDisplayName()}")
                        }
                        
                        Log.d(TAG, "Received instance list: ${instanceList.size} instances")
                    }
                }
                
                "message" -> {
                    val payload = json.optJSONObject("payload")
                    val from = json.optString("from", "assistant")
                    
                    if (payload != null) {
                        val content = payload.optString("text") ?: payload.toString()
                        
                        // 获取发送实例的信息
                        val fromInstance = _instances.value.find { it.id == from }
                        val senderName = fromInstance?.getBubbleName() ?: "🪶 翎绛"
                        
                        addMessage(Message(
                            id = UUID.randomUUID().toString(),
                            from = senderName,
                            content = content,
                            timestamp = json.optLong("timestamp", System.currentTimeMillis()),
                            type = MessageType.ASSISTANT
                        ))
                        
                        // 跨实例通知：如果消息来自非当前选中的实例
                        if (from != selectedInstanceId && fromInstance != null) {
                            _crossInstanceMessages.tryEmit(
                                CrossInstanceMessage(
                                    instanceId = from,
                                    instanceName = fromInstance.getShortName(),
                                    preview = content.take(60)
                                )
                            )
                        }
                        
                        // 检查是否有 sub-agent 信息
                        val subAgentsArray = payload.optJSONArray("subAgents")
                        if (subAgentsArray != null) {
                            val agents = mutableListOf<SubAgent>()
                            for (i in 0 until subAgentsArray.length()) {
                                val agent = subAgentsArray.getJSONObject(i)
                                agents.add(SubAgent(
                                    sessionKey = agent.optString("sessionKey"),
                                    label = agent.optString("label"),
                                    updatedAt = agent.optLong("updatedAt"),
                                    status = agent.optString("status")
                                ))
                            }
                            _subAgents.value = agents
                        }
                    }
                }
                
                "pong" -> {
                    Log.d(TAG, "Received pong")
                }
                
                "subagent_update" -> {
                    val payload = json.optJSONObject("payload")
                    val subAgentsArray = payload?.optJSONArray("subagents")
                    
                    if (subAgentsArray != null) {
                        val agents = mutableListOf<SubAgent>()
                        for (i in 0 until subAgentsArray.length()) {
                            val agent = subAgentsArray.getJSONObject(i)
                            agents.add(SubAgent(
                                sessionKey = agent.optString("sessionKey"),
                                label = agent.optString("label"),
                                updatedAt = agent.optLong("updatedAt"),
                                status = agent.optString("status")
                            ))
                        }
                        _subAgents.value = agents
                        Log.d(TAG, "Updated sub-agents: ${agents.size}")
                    }
                }
                
                "queued" -> {
                    Log.d(TAG, "Message queued for offline client")
                }
                
                "notification" -> {
                    val payload = json.optJSONObject("payload")
                    
                    if (payload != null) {
                        val notification = Notification(
                            title = payload.optString("title", "通知"),
                            message = payload.optString("message", ""),
                            severity = payload.optString("severity", "info"),
                            timestamp = payload.optLong("timestamp", System.currentTimeMillis()),
                            sessionKey = payload.optString("sessionKey")
                        )
                        
                        addNotification(notification)
                        onToast("${notification.title}: ${notification.message}")
                    }
                }
                
                "health_alert" -> {
                    val payload = json.optJSONObject("payload")
                    
                    if (payload != null) {
                        val alert = HealthAlert(
                            severity = payload.optString("severity", "warning"),
                            metric = payload.optString("metric", "unknown"),
                            value = payload.optString("value", "N/A"),
                            message = payload.optString("message", "健康警告"),
                            timestamp = payload.optLong("timestamp", System.currentTimeMillis())
                        )
                        _healthAlerts.value = _healthAlerts.value + alert
                        Log.d(TAG, "Health alert received: ${alert.message}")
                    }
                }
                
                "stream_start" -> {
                    val payload = json.optJSONObject("payload")
                    val messageId = payload?.optString("messageId") ?: return
                    
                    Log.d(TAG, "Stream started: $messageId")
                    
                    // 创建一个空的流式消息占位
                    val fromInstance = _instances.value.firstOrNull()
                    val senderName = fromInstance?.getBubbleName() ?: "🪶 翎绛"
                    
                    addMessage(Message(
                        id = messageId,
                        from = senderName,
                        content = "",
                        timestamp = System.currentTimeMillis(),
                        type = MessageType.ASSISTANT,
                        lifeState = MessageLifeState.COMPLETE,
                        isStreaming = true,
                        streamMessageId = messageId
                    ))
                }
                
                "stream_chunk" -> {
                    val payload = json.optJSONObject("payload")
                    val messageId = payload?.optString("messageId") ?: return
                    val chunk = payload.optString("chunk", "")
                    
                    Log.d(TAG, "Stream chunk: $messageId, length=${chunk.length}")
                    
                    // 追加内容到对应的流式消息
                    _messages.value = _messages.value.map { msg ->
                        if (msg.streamMessageId == messageId && msg.isStreaming) {
                            val separator = if (msg.content.isNotEmpty() && chunk.isNotEmpty()) "\n\n" else ""
                            msg.copy(content = msg.content + separator + chunk)
                        } else {
                            msg
                        }
                    }
                }
                
                "stream_end" -> {
                    val payload = json.optJSONObject("payload")
                    val messageId = payload?.optString("messageId") ?: return
                    val finalText = payload.optString("text", "")
                    val isError = payload.optBoolean("error", false)
                    
                    Log.d(TAG, "Stream ended: $messageId, error=$isError")
                    
                    // 标记流式消息结束，用完整文本替换（确保一致性）
                    _messages.value = _messages.value.map { msg ->
                        if (msg.streamMessageId == messageId) {
                            msg.copy(
                                content = finalText,
                                isStreaming = false
                            )
                        } else {
                            msg
                        }
                    }
                }
                
                "health_report" -> {
                    val payload = json.optJSONObject("payload") ?: return
                    val instanceId = payload.optString("instanceId", "")
                    if (instanceId.isEmpty()) return
                    
                    val health = InstanceHealth(
                        instanceId = instanceId,
                        cpuPercent = payload.optDouble("cpu", 0.0).toFloat(),
                        memoryUsedGb = payload.optJSONObject("memory")?.optDouble("used", 0.0)?.toFloat() ?: 0f,
                        memoryTotalGb = payload.optJSONObject("memory")?.optDouble("total", 0.0)?.toFloat() ?: 0f,
                        diskPercent = payload.optJSONObject("disk")?.optDouble("percent", 0.0)?.toFloat() ?: 0f,
                        uptime = payload.optLong("uptime", 0),
                        activeSubAgents = payload.optInt("activeSubAgents", 0),
                        lastUpdate = System.currentTimeMillis()
                    )
                    
                    _instanceHealthMap.value = _instanceHealthMap.value + (instanceId to health)
                    Log.d(TAG, "Health report: $instanceId cpu=${health.cpuPercent}% disk=${health.diskPercent}%")
                }
                
                "subagent_status" -> {
                    val payload = json.optJSONObject("payload") ?: return
                    val instanceId = payload.optString("instanceId", "")
                    val arr = payload.optJSONArray("subagents") ?: return
                    
                    val tasks = mutableListOf<SubAgentTask>()
                    for (i in 0 until arr.length()) {
                        val s = arr.getJSONObject(i)
                        tasks.add(SubAgentTask(
                            id = s.optString("id", "sub-$i"),
                            instanceId = instanceId,
                            label = s.optString("label", ""),
                            status = when (s.optString("status", "")) {
                                "running" -> TaskStatus.RUNNING
                                "completed" -> TaskStatus.COMPLETED
                                "failed" -> TaskStatus.FAILED
                                else -> TaskStatus.PENDING
                            },
                            progress = if (s.has("progress") && !s.isNull("progress")) s.optInt("progress") else null,
                            startTime = s.optLong("startTime", 0),
                            duration = s.optLong("duration", 0)
                        ))
                    }
                    
                    // 替换该实例的任务，保留其他实例的
                    _subAgentTasks.value = _subAgentTasks.value.filter { it.instanceId != instanceId } + tasks
                    Log.d(TAG, "SubAgent status: $instanceId, ${tasks.size} tasks")
                }
                
                "alert" -> {
                    val payload = json.optJSONObject("payload") ?: return
                    val alert = Alert(
                        instanceId = payload.optString("instanceId", ""),
                        level = when (payload.optString("level", "info")) {
                            "warning" -> AlertLevel.WARNING
                            "critical" -> AlertLevel.CRITICAL
                            else -> AlertLevel.INFO
                        },
                        title = payload.optString("title", ""),
                        message = payload.optString("message", ""),
                        timestamp = payload.optLong("timestamp", System.currentTimeMillis())
                    )
                    
                    _alerts.value = _alerts.value + alert
                    Log.d(TAG, "Alert: [${alert.level}] ${alert.title}")
                }
                
                "timeline_event" -> {
                    val payload = json.optJSONObject("payload") ?: return
                    val event = TimelineEvent(
                        instanceId = payload.optString("instanceId", ""),
                        instanceName = payload.optString("instanceName", ""),
                        eventType = payload.optString("event", ""),
                        title = payload.optString("title", ""),
                        timestamp = payload.optLong("timestamp", System.currentTimeMillis())
                    )
                    
                    _timelineEvents.value = listOf(event) + _timelineEvents.value
                    Log.d(TAG, "Timeline: ${event.eventType} - ${event.title}")
                }
                
                "memory_result" -> {
                    val payload = json.optJSONObject("payload") ?: return
                    val resultText = payload.optString("result", "")
                    val error = payload.optString("error", "")
                    
                    if (error.isNotEmpty()) {
                        _memoryResult.value = "ERROR:$error"
                        Log.e(TAG, "Memory search error: $error")
                    } else {
                        _memoryResult.value = resultText
                        Log.d(TAG, "Memory result received: ${resultText.take(100)}")
                    }
                }
                
                "process_update" -> {
                    val payload = json.optJSONObject("payload")
                    if (payload != null) {
                        _processUpdates.value = payload
                        Log.d(TAG, "Process update: phase=${payload.optString("phase")}")
                    }
                }
                
                "sync_task_result_response" -> {
                    val payload = json.optJSONObject("payload") ?: return
                    val results = payload.optJSONArray("results")
                    Log.d(TAG, "Received sync_task_result_response: ${results?.length() ?: 0} results")
                    // Handled by ChatViewModel via processUpdates or directly
                    _processUpdates.value = JSONObject().apply {
                        put("phase", "sync_task_result_response")
                        put("results", results)
                    }
                }
                
                "sync_messages_response" -> {
                    val payload = json.optJSONObject("payload") ?: return
                    val messagesArray = payload.optJSONArray("messages")
                    Log.d(TAG, "Received sync_messages_response: ${messagesArray?.length() ?: 0} messages")
                    _processUpdates.value = JSONObject().apply {
                        put("phase", "sync_messages_response")
                        put("messages", messagesArray)
                    }
                }
                
                "task_update" -> {
                    val payload = json.optJSONObject("payload") ?: return
                    val taskId = payload.optString("taskId", "")
                    if (taskId.isEmpty()) return
                    
                    val status = when (payload.optString("status", "")) {
                        "pending" -> TaskStatus.PENDING
                        "running" -> TaskStatus.RUNNING
                        "completed" -> TaskStatus.COMPLETED
                        "failed" -> TaskStatus.FAILED
                        else -> TaskStatus.PENDING
                    }
                    val progress = if (payload.has("progress") && !payload.isNull("progress")) payload.optInt("progress") else null
                    val output = payload.optString("output", "")
                    val instanceId = payload.optString("instanceId", "")
                    
                    // 更新已有任务或创建新任务
                    val existingTask = _subAgentTasks.value.find { it.id == taskId }
                    if (existingTask != null) {
                        _subAgentTasks.value = _subAgentTasks.value.map { task ->
                            if (task.id == taskId) {
                                task.copy(
                                    status = status,
                                    progress = progress,
                                    duration = if (task.startTime > 0) (System.currentTimeMillis() / 1000 - task.startTime) else 0
                                )
                            } else {
                                task
                            }
                        }
                    } else {
                        _subAgentTasks.value = _subAgentTasks.value + SubAgentTask(
                            id = taskId,
                            instanceId = instanceId,
                            label = taskId,
                            status = status,
                            progress = progress,
                            startTime = System.currentTimeMillis() / 1000,
                            duration = 0
                        )
                    }
                    
                    // 存储任务日志
                    if (output.isNotEmpty()) {
                        val currentLogs = _taskLogs.value[taskId] ?: emptyList()
                        // 按行分割输出，追加到日志
                        val newLines = output.split("\n").filter { it.isNotBlank() }
                        _taskLogs.value = _taskLogs.value + (taskId to (currentLogs + newLines).takeLast(500))
                    }
                    
                    Log.d(TAG, "Task update: $taskId status=$status progress=$progress")
                    
                    // 任务完成/失败时发通知
                    if (status == TaskStatus.COMPLETED || status == TaskStatus.FAILED) {
                        val statusText = if (status == TaskStatus.COMPLETED) "已完成" else "失败"
                        onToast("任务 $statusText")
                    }
                }
                
                else -> {
                    Log.d(TAG, "Unknown message type: $type")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing message: ${e.message}", e)
        }
    }
    
    // ═══ 断线恢复方法（R003） ═══
    
    /**
     * 重连后补拉待确认请求的结果
     */
    fun sendSyncTaskResult(requestIds: List<String>) {
        if (requestIds.isEmpty()) return
        val json = JSONObject().apply {
            put("type", "sync_task_result")
            put("payload", JSONObject().apply {
                put("requestIds", JSONArray(requestIds))
            })
        }
        wsManager?.sendJson(json)
        Log.d(TAG, "Sent sync_task_result for ${requestIds.size} pending requests")
    }
    
    /**
     * 重连后增量拉取离线消息
     */
    fun requestOfflineMessages(lastMessageId: String?) {
        val json = JSONObject().apply {
            put("type", "sync_messages")
            put("payload", JSONObject().apply {
                put("lastMessageId", lastMessageId ?: "")
                put("limit", 50)
            })
        }
        wsManager?.sendJson(json)
        Log.d(TAG, "Requested offline messages since: $lastMessageId")
    }
    
    /**
     * 添加消息到列表
     */
    private fun addMessage(message: Message) {
        _messages.value = _messages.value + message
    }
    
    /**
     * 添加通知到列表
     */
    private fun addNotification(notification: Notification) {
        _notifications.value = _notifications.value + notification
    }
    
    /**
     * 清空消息
     */
    fun clearMessages() {
        _messages.value = emptyList()
    }
    
    /**
     * 清空通知
     */
    fun clearNotifications() {
        _notifications.value = emptyList()
    }
    
    /**
     * 获取设备 ID
     */
    private fun getDeviceId(): String {
        return UUID.randomUUID().toString()
    }
    
    /**
     * 获取客户端 ID
     */
    fun getClientId(): String? = clientId
}
