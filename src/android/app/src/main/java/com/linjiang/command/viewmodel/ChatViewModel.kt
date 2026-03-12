package com.linjiang.command.viewmodel

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.linjiang.command.CommandApp
import com.linjiang.command.R
import com.linjiang.command.data.HealthAlert
import com.linjiang.command.data.model.AiThinkingState
import com.linjiang.command.data.model.Message
import com.linjiang.command.data.model.MessageLifeState
import com.linjiang.command.data.model.MessageProcessContext
import com.linjiang.command.data.model.MessageType
import com.linjiang.command.data.model.Notification
import com.linjiang.command.data.model.OpenClawInstance
import com.linjiang.command.data.model.ProcessPhase
import com.linjiang.command.data.model.SubAgent
import com.linjiang.command.data.model.ToolCallRecord
import com.linjiang.command.data.model.ToolStatus
import com.linjiang.command.data.local.ConversationMeta
import com.linjiang.command.data.local.MessageDao
import com.linjiang.command.data.local.toEntity
import com.linjiang.command.data.local.toMessage
import com.linjiang.command.network.RelayClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * 对话管理 ViewModel
 * 
 * 负责管理消息列表、AI 思考状态和 Sub-agent 状态
 */
class ChatViewModel(application: Application) : AndroidViewModel(application) {
    
    private val prefs = application.getSharedPreferences("linjiang_messages", Context.MODE_PRIVATE)
    private val notificationManager = application.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val appContext = application.applicationContext
    private val MAX_PERSISTED_MESSAGES = 200
    
    /** Room DAO for SQLite persistence */
    private val messageDao: MessageDao = CommandApp.instance.database.messageDao()
    
    /** App 是否在前台（由 Activity 设置） */
    var isAppInForeground = true
    
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages
    
    private val _subAgents = MutableStateFlow<List<SubAgent>>(emptyList())
    val subAgents: StateFlow<List<SubAgent>> = _subAgents
    
    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    val notifications: StateFlow<List<Notification>> = _notifications
    
    private val _healthAlerts = MutableStateFlow<List<HealthAlert>>(emptyList())
    val healthAlerts: StateFlow<List<HealthAlert>> = _healthAlerts
    
    private val _currentInstance = MutableStateFlow<OpenClawInstance?>(null)
    val currentInstance: StateFlow<OpenClawInstance?> = _currentInstance
    
    private val _instances = MutableStateFlow<List<com.linjiang.command.data.Instance>>(emptyList())
    val instances: StateFlow<List<com.linjiang.command.data.Instance>> = _instances
    
    /** AI 思考状态 */
    private val _aiThinkingState = MutableStateFlow(AiThinkingState.IDLE)
    val aiThinkingState: StateFlow<AiThinkingState> = _aiThinkingState
    
    /** 当前连接的实例名称和 emoji（用于拟人化显示） */
    private val _currentInstanceName = MutableStateFlow("翎绛")
    val currentInstanceName: StateFlow<String> = _currentInstanceName
    
    private val _currentInstanceEmoji = MutableStateFlow("🪶")
    val currentInstanceEmoji: StateFlow<String> = _currentInstanceEmoji
    
    /** 当前活跃的处理上下文（流式过程 UI） */
    private val _activeProcess = MutableStateFlow<MessageProcessContext?>(null)
    val activeProcess: StateFlow<MessageProcessContext?> = _activeProcess
    
    /** 当前选中的实例 ID（用于实例切换） */
    private val _currentInstanceId = MutableStateFlow<String?>(null)
    val currentInstanceId: StateFlow<String?> = _currentInstanceId.asStateFlow()
    
    // ═══ 跨实例未读计数（R002） ═══
    
    private val _unreadCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val unreadCounts: StateFlow<Map<String, Int>> = _unreadCounts.asStateFlow()
    val totalUnread: StateFlow<Int> = _unreadCounts
        .map { it.values.sum() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)
    
    /** 跨实例通知事件（前台横幅用） */
    data class CrossInstanceNotification(
        val instanceId: String,
        val instanceName: String,
        val preview: String
    )
    
    private val _crossInstanceNotification = MutableSharedFlow<CrossInstanceNotification>(extraBufferCapacity = 8)
    val crossInstanceNotification = _crossInstanceNotification.asSharedFlow()
    
    // ═══ 断线恢复：待确认请求追踪（R003） ═══
    
    private val pendingPrefs = application.getSharedPreferences("linjiang_pending_requests", Context.MODE_PRIVATE)
    private val pendingRequests = mutableSetOf<String>()
    
    private var relayClient: RelayClient? = null
    private var thinkingTimeoutJob: Job? = null
    
    init {
        loadMessages()
        loadPendingRequests()
    }
    
    /**
     * 从 SharedPreferences 加载消息历史
     */
    private fun loadMessages() {
        val json = prefs.getString("messages", null) ?: return
        try {
            val arr = JSONArray(json)
            val list = mutableListOf<Message>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(Message(
                    id = obj.getString("id"),
                    from = obj.getString("from"),
                    content = obj.getString("content"),
                    timestamp = obj.getLong("timestamp"),
                    type = MessageType.valueOf(obj.getString("type")),
                    lifeState = MessageLifeState.COMPLETE
                ))
            }
            _messages.value = list
        } catch (_: Exception) {}
    }
    
    /**
     * 保存消息历史到 SharedPreferences（只保留最近 N 条已完成消息）
     */
    private fun saveMessages() {
        val toSave = _messages.value
            .filter { !it.isStreaming && it.lifeState != MessageLifeState.FLYING && it.lifeState != MessageLifeState.BORN }
            .takeLast(MAX_PERSISTED_MESSAGES)
        val arr = JSONArray()
        toSave.forEach { msg ->
            arr.put(JSONObject().apply {
                put("id", msg.id)
                put("from", msg.from)
                put("content", msg.content)
                put("timestamp", msg.timestamp)
                put("type", msg.type.name)
            })
        }
        prefs.edit().putString("messages", arr.toString()).apply()
    }
    
    /**
     * 推送系统通知（仅 App 在后台时）
     */
    private fun pushNotification(title: String, content: String) {
        if (isAppInForeground) return
        
        val notification = NotificationCompat.Builder(appContext, CommandApp.CHANNEL_MESSAGES)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(content.take(100))
            .setAutoCancel(true)
            .build()
        
        try {
            notificationManager.notify(System.currentTimeMillis().toInt(), notification)
        } catch (_: SecurityException) {
            // 通知权限未授予，静默忽略
        }
    }
    
    // ═══ 跨实例通知方法（R002） ═══
    
    /**
     * 收到非当前实例的消息时：增加未读计数 + 发横幅 + 后台系统通知
     */
    fun onCrossInstanceMessage(instanceId: String, instanceName: String, preview: String) {
        if (instanceId == _currentInstanceId.value) return
        
        // 1. 未读计数 +1
        _unreadCounts.update { map ->
            map + (instanceId to (map[instanceId] ?: 0) + 1)
        }
        
        // 2. 前台横幅通知
        viewModelScope.launch {
            _crossInstanceNotification.emit(
                CrossInstanceNotification(instanceId, instanceName, preview.take(60))
            )
        }
        
        // 3. 后台系统通知
        pushNotification(instanceName, preview)
    }
    
    /**
     * 切换到某实例时清零未读
     */
    fun markInstanceAsRead(instanceId: String) {
        _unreadCounts.update { it - instanceId }
    }
    
    // ═══ 断线恢复方法（R003） ═══
    
    fun trackRequest(requestId: String) {
        pendingRequests.add(requestId)
        savePendingRequests()
    }
    
    fun completeRequest(requestId: String) {
        pendingRequests.remove(requestId)
        savePendingRequests()
    }
    
    fun getPendingRequestIds(): Set<String> = pendingRequests.toSet()
    
    private fun savePendingRequests() {
        pendingPrefs.edit()
            .putStringSet("pending_ids", pendingRequests.toSet())
            .apply()
    }
    
    private fun loadPendingRequests() {
        val saved = pendingPrefs.getStringSet("pending_ids", emptySet()) ?: emptySet()
        pendingRequests.clear()
        pendingRequests.addAll(saved)
    }
    
    /**
     * 处理断线恢复的 sync_task_result_response
     */
    fun handleSyncTaskResultResponse(results: JSONArray?) {
        results ?: return
        for (i in 0 until results.length()) {
            val result = results.getJSONObject(i)
            val requestId = result.optString("requestId", "")
            val finalText = result.optString("finalText", "")
            if (finalText.isNotEmpty()) {
                addMessage(Message(
                    id = UUID.randomUUID().toString(),
                    from = _currentInstanceName.value,
                    content = finalText,
                    type = MessageType.ASSISTANT
                ))
            }
            if (requestId.isNotEmpty()) {
                completeRequest(requestId)
            }
        }
    }
    
    /**
     * 处理离线消息补拉 sync_messages_response
     */
    fun handleSyncMessagesResponse(messagesArray: JSONArray?) {
        messagesArray ?: return
        for (i in 0 until messagesArray.length()) {
            val msg = messagesArray.getJSONObject(i)
            val content = msg.optString("text", "")
            val from = msg.optString("from", "assistant")
            val isUser = from == "user"
            if (content.isNotEmpty()) {
                addMessage(Message(
                    id = msg.optString("id", UUID.randomUUID().toString()),
                    from = if (isUser) "user" else _currentInstanceName.value,
                    content = content,
                    timestamp = msg.optLong("timestamp", System.currentTimeMillis()),
                    type = if (isUser) MessageType.USER else MessageType.ASSISTANT
                ))
            }
        }
    }
    
    /**
     * 选择实例（含 SQLite 持久化切换）
     *
     * 切换时：保存旧实例元数据 → 更新当前 ID → 从 SQLite 加载历史 → 标记已读
     */
    fun selectInstance(instanceId: String) {
        relayClient?.selectInstance(instanceId)
        // 更新当前实例的名称和 emoji
        val instance = _instances.value.find { it.id == instanceId }
        if (instance != null) {
            _currentInstanceName.value = instance.name
            _currentInstanceEmoji.value = instance.emoji
        }
        
        // 清零该实例的未读计数
        markInstanceAsRead(instanceId)
        
        viewModelScope.launch {
            // 保存旧实例的元数据
            _currentInstanceId.value?.let { oldId ->
                try {
                    messageDao.saveMeta(ConversationMeta(
                        instanceId = oldId,
                        lastActiveTime = System.currentTimeMillis()
                    ))
                } catch (_: Exception) {}
            }
            
            _currentInstanceId.value = instanceId
            
            // 从 SQLite 加载该实例的历史消息
            try {
                val entities = messageDao.getMessagesByInstanceOnce(instanceId)
                if (entities.isNotEmpty()) {
                    _messages.value = entities.map { it.toMessage() }
                }
                messageDao.markAllAsRead(instanceId)
            } catch (_: Exception) {}
        }
    }
    
    /**
     * 获取当前选择的实例
     */
    fun getSelectedInstance(): com.linjiang.command.data.Instance? {
        return relayClient?.getSelectedInstance()
    }
    
    /**
     * 设置当前实例
     */
    fun setCurrentInstance(instance: OpenClawInstance) {
        _currentInstance.value = instance
        _currentInstanceId.value = instance.id
        _messages.value = emptyList()
        _subAgents.value = emptyList()
        _aiThinkingState.value = AiThinkingState.IDLE
    }
    
    /**
     * 切换实例（从对话页实例选择器调用）
     * 清空当前消息列表，后续改为从 SQLite 加载
     */
    fun switchInstance(instanceId: String) {
        _currentInstanceId.value = instanceId
        // 清空当前消息列表（后续改为从 SQLite 加载）
        _messages.value = emptyList()
        _aiThinkingState.value = AiThinkingState.IDLE
        // 同步到 RelayClient 的实例选择
        selectInstance(instanceId)
    }
    
    /**
     * 设置 RelayClient
     */
    fun setRelayClient(client: RelayClient?) {
        relayClient = client
        
        if (client == null) return
        
        // 监听实例列表
        viewModelScope.launch {
            client.instances.collect { instanceList ->
                _instances.value = instanceList
            }
        }
        
        // 监听接收到的消息
        viewModelScope.launch {
            client.messages.collect { messageList ->
                // 合并：保留 ViewModel 中的用户消息，加入 RelayClient 的新消息
                val existingIds = _messages.value.map { it.id }.toSet()
                val newMessages = messageList.filter { it.id !in existingIds }
                if (newMessages.isNotEmpty()) {
                    _messages.value = _messages.value + newMessages
                }
                
                // 更新流式消息内容（已存在的消息可能内容变了）
                val relayMap = messageList.associateBy { it.id }
                _messages.value = _messages.value.map { msg ->
                    val updated = relayMap[msg.id]
                    if (updated != null && (updated.content != msg.content || updated.isStreaming != msg.isStreaming)) {
                        updated
                    } else {
                        msg
                    }
                }
                
                val lastMsg = _messages.value.lastOrNull()
                if (lastMsg != null && lastMsg.type == MessageType.ASSISTANT) {
                    if (lastMsg.isStreaming) {
                        // 流式输出中，切换到 STREAMING 状态
                        _aiThinkingState.value = AiThinkingState.STREAMING
                        thinkingTimeoutJob?.cancel()
                    } else {
                        // 完整消息到达，回到 IDLE
                        _aiThinkingState.value = AiThinkingState.IDLE
                        thinkingTimeoutJob?.cancel()
                        saveMessages()
                        pushNotification("🪶 翎绛", lastMsg.content)
                    }
                    
                    // 更新之前发送中的用户消息状态为 COMPLETE
                    _messages.value = _messages.value.map { msg ->
                        if (msg.type == MessageType.USER && msg.lifeState == MessageLifeState.RECEIVED) {
                            msg.copy(lifeState = MessageLifeState.COMPLETE)
                        } else {
                            msg
                        }
                    }
                }
            }
        }
        
        // 监听 sub-agents
        viewModelScope.launch {
            client.subAgents.collect { agents ->
                _subAgents.value = agents
            }
        }
        
        // 监听通知
        viewModelScope.launch {
            client.notifications.collect { notificationList ->
                _notifications.value = notificationList
            }
        }
        
        // 监听健康警告
        viewModelScope.launch {
            client.healthAlerts.collect { alerts ->
                _healthAlerts.value = alerts
            }
        }
        
        // 监听跨实例消息通知（R002）
        viewModelScope.launch {
            client.crossInstanceMessages.collect { msg ->
                onCrossInstanceMessage(msg.instanceId, msg.instanceName, msg.preview)
            }
        }
        
        // 监听重连事件，触发断线恢复（R003）
        viewModelScope.launch {
            client.reconnectEvent.collect {
                // 补拉待确认请求的结果
                val pending = getPendingRequestIds()
                if (pending.isNotEmpty()) {
                    client.sendSyncTaskResult(pending.toList())
                }
                // 补拉离线消息
                val lastMsgId = _messages.value.lastOrNull()?.id
                client.requestOfflineMessages(lastMsgId)
            }
        }
        
        // 监听 sync_task_result_response 和 sync_messages_response
        viewModelScope.launch {
            client.processUpdates.collect { payload ->
                if (payload == null) return@collect
                when (payload.optString("phase")) {
                    "sync_task_result_response" -> {
                        handleSyncTaskResultResponse(payload.optJSONArray("results"))
                    }
                    "sync_messages_response" -> {
                        handleSyncMessagesResponse(payload.optJSONArray("messages"))
                    }
                    else -> {
                        // 正常的 process_update，交给现有处理
                        handleProcessUpdate(payload)
                    }
                }
            }
        }
    }
    
    /**
     * 发送消息
     * 
     * 状态流：BORN → FLYING → RECEIVED → COMPLETE
     * 失败时：BORN → FLYING → LOST
     */
    fun sendMessage(content: String) {
        if (content.isBlank()) return
        
        val messageId = UUID.randomUUID().toString()
        
        // 1. 消息诞生（BORN）
        val userMessage = Message(
            id = messageId,
            from = "user",
            content = content,
            type = MessageType.USER,
            lifeState = MessageLifeState.BORN
        )
        addMessage(userMessage)
        
        // 2. 消息起飞（FLYING）- 50ms 后
        viewModelScope.launch {
            delay(50)
            updateMessageLifeState(messageId, MessageLifeState.FLYING)
            
            // 3. 发送消息
            try {
                relayClient?.sendMessage(content, null)
                
                // 4. 消息到达（RECEIVED）- 发送成功
                delay(100)
                updateMessageLifeState(messageId, MessageLifeState.RECEIVED)
                
                // 5. 更新 AI 思考状态
                startAiThinkingSequence()
                
            } catch (e: Exception) {
                // 消息丢失（LOST）
                updateMessageLifeState(messageId, MessageLifeState.LOST, retryable = true)
                _aiThinkingState.value = AiThinkingState.IDLE
            }
        }
    }
    
    /**
     * 重新发送失败的消息
     */
    fun retryMessage(messageId: String) {
        val message = _messages.value.find { it.id == messageId } ?: return
        if (message.lifeState != MessageLifeState.LOST) return
        
        // 重置状态为 FLYING
        updateMessageLifeState(messageId, MessageLifeState.FLYING)
        
        viewModelScope.launch {
            try {
                relayClient?.sendMessage(message.content, null)
                delay(100)
                updateMessageLifeState(messageId, MessageLifeState.RECEIVED)
                startAiThinkingSequence()
            } catch (e: Exception) {
                updateMessageLifeState(messageId, MessageLifeState.LOST, retryable = true)
            }
        }
    }
    
    /**
     * 启动 AI 思考状态序列
     * 
     * 时间线：
     * 0ms: RECEIVED（收到了）
     * 500ms: THINKING（在整理思路）
     * 5s: SEARCHING（在翻记忆）
     * 10s: WRITING（在组织语言）
     * 
     * 收到回复后自动重置为 IDLE
     */
    private fun startAiThinkingSequence() {
        thinkingTimeoutJob?.cancel()
        _aiThinkingState.value = AiThinkingState.RECEIVED
        
        thinkingTimeoutJob = viewModelScope.launch {
            delay(500)
            if (_aiThinkingState.value == AiThinkingState.RECEIVED) {
                _aiThinkingState.value = AiThinkingState.THINKING
            }
            
            delay(4500)
            if (_aiThinkingState.value == AiThinkingState.THINKING) {
                _aiThinkingState.value = AiThinkingState.SEARCHING
            }
            
            delay(5000)
            if (_aiThinkingState.value == AiThinkingState.SEARCHING) {
                _aiThinkingState.value = AiThinkingState.WRITING
            }
        }
    }
    
    /**
     * 处理 process_update 消息
     * 
     * 根据不同的 phase 更新 activeProcess 状态，
     * UI 层通过 collectAsState 自动响应变化。
     */
    fun handleProcessUpdate(payload: JSONObject) {
        val requestId = payload.optString("requestId", "")
        val phase = payload.optString("phase", "")
        val data = payload.optJSONObject("data") ?: JSONObject()

        val current = _activeProcess.value?.takeIf { it.requestId == requestId }
            ?: MessageProcessContext(requestId = requestId)

        val updated = when (phase) {
            "thinking_start" -> current.copy(phase = ProcessPhase.THINKING)
            "thinking_content" -> current.copy(
                phase = ProcessPhase.THINKING,
                thinkingText = data.optString("text", current.thinkingText)
            )
            "tool_call" -> current.copy(
                phase = ProcessPhase.TOOL_CALLING,
                toolCalls = current.toolCalls + ToolCallRecord(
                    callId = data.optString("callId", ""),
                    tool = data.optString("tool", ""),
                    args = data.optString("args", "")
                )
            )
            "tool_result" -> {
                val callId = data.optString("callId", "")
                current.copy(
                    toolCalls = current.toolCalls.map {
                        if (it.callId == callId) it.copy(
                            status = if (data.optBoolean("success", true)) ToolStatus.SUCCESS else ToolStatus.FAILED,
                            summary = if (data.has("summary") && !data.isNull("summary")) data.optString("summary") else null
                        ) else it
                    }
                )
            }
            "text_chunk" -> current.copy(
                phase = ProcessPhase.STREAMING,
                streamedText = current.streamedText + data.optString("text", "")
            )
            "timeout_warning" -> current.copy(
                elapsedSeconds = data.optInt("elapsed", current.elapsedSeconds),
                timeoutLimit = data.optInt("limit", current.timeoutLimit)
            )
            "complete" -> {
                // 完成时清除 activeProcess，最终文字通过正常消息流显示
                _activeProcess.value = null
                return
            }
            "error" -> current.copy(phase = ProcessPhase.ERROR)
            else -> current
        }
        _activeProcess.value = updated
    }

    /**
     * 手动刷新 Sub-agent 列表
     */
    fun refreshSubAgents() {
        viewModelScope.launch {
            relayClient?.requestSubAgentUpdate()
        }
    }
    
    /**
     * 更新消息生命周期状态
     */
    private fun updateMessageLifeState(messageId: String, state: MessageLifeState, retryable: Boolean = false) {
        _messages.value = _messages.value.map { message ->
            if (message.id == messageId) {
                message.copy(lifeState = state, retryable = retryable)
            } else {
                message
            }
        }
    }
    
    /**
     * 添加消息到列表，同时持久化到 SQLite
     */
    private fun addMessage(message: Message) {
        _messages.value = _messages.value + message
        saveMessages()
        persistMessage(message)
    }
    
    /**
     * 将消息持久化到 SQLite（异步，不阻塞 UI）
     */
    private fun persistMessage(message: Message) {
        val instanceId = _currentInstanceId.value ?: return
        // 只持久化已完成或用户发送的消息，不持久化流式中间态
        if (message.isStreaming) return
        viewModelScope.launch {
            try {
                messageDao.insertMessage(message.toEntity(instanceId))
            } catch (_: Exception) {}
        }
    }
    
    /**
     * 批量持久化完成态消息到 SQLite
     */
    private fun persistCompletedMessages() {
        val instanceId = _currentInstanceId.value ?: return
        val toSave = _messages.value.filter {
            !it.isStreaming && it.lifeState == MessageLifeState.COMPLETE
        }
        if (toSave.isEmpty()) return
        viewModelScope.launch {
            try {
                messageDao.insertMessages(toSave.map { it.toEntity(instanceId) })
            } catch (_: Exception) {}
        }
    }
    
    /**
     * 清空消息列表（含 SQLite 清理）
     */
    fun clearMessages() {
        _messages.value = emptyList()
        _aiThinkingState.value = AiThinkingState.IDLE
        saveMessages()
        val instanceId = _currentInstanceId.value ?: return
        viewModelScope.launch {
            try {
                messageDao.deleteByInstance(instanceId)
            } catch (_: Exception) {}
        }
    }
}
