package com.linjiang.command.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.linjiang.command.data.model.ConnectionStatus
import com.linjiang.command.data.model.OpenClawInstance
import com.linjiang.command.network.RelayClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * 实例管理 ViewModel
 */
class InstanceViewModel(application: Application) : AndroidViewModel(application) {
    
    private val _instances = MutableStateFlow<List<OpenClawInstance>>(emptyList())
    val instances: StateFlow<List<OpenClawInstance>> = _instances
    
    private val _currentInstance = MutableStateFlow<OpenClawInstance?>(null)
    val currentInstance: StateFlow<OpenClawInstance?> = _currentInstance
    
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage
    
    private val relayClients = mutableMapOf<String, RelayClient>()
    
    private val prefs = application.getSharedPreferences("linjiang_instances", Context.MODE_PRIVATE)
    
    init {
        loadInstances()
        autoConnect()
    }
    
    /** 中继连接状态（供设置页使用） */
    private val _relayConnected = MutableStateFlow(false)
    val relayConnected: StateFlow<Boolean> = _relayConnected
    
    /**
     * 启动后自动连接所有实例到中继服务器
     * 零操作：App 启动后自动建立连接
     */
    private fun autoConnect() {
        viewModelScope.launch {
            // 给 loadInstances 一帧时间完成
            delay(100)
            
            val currentInstances = _instances.value
            if (currentInstances.isEmpty()) {
                // 没有实例，创建默认实例
                loadDefaultInstances()
            }
            
            // 自动连接所有未连接的实例
            _instances.value.forEach { instance ->
                if (instance.status == ConnectionStatus.DISCONNECTED) {
                    connectInstance(instance.id)
                }
            }
        }
    }
    
    /**
     * 从 SharedPreferences 加载实例列表
     */
    private fun loadInstances() {
        val json = prefs.getString("instances", null)
        if (json != null) {
            try {
                val arr = JSONArray(json)
                val list = mutableListOf<OpenClawInstance>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(OpenClawInstance(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        url = obj.getString("url"),
                        status = ConnectionStatus.DISCONNECTED
                    ))
                }
                _instances.value = list
            } catch (e: Exception) {
                loadDefaultInstances()
            }
        } else {
            loadDefaultInstances()
        }
    }
    
    /**
     * 保存实例列表到 SharedPreferences
     */
    private fun saveInstances() {
        val arr = JSONArray()
        _instances.value.forEach { inst ->
            arr.put(JSONObject().apply {
                put("id", inst.id)
                put("name", inst.name)
                put("url", inst.url)
            })
        }
        prefs.edit().putString("instances", arr.toString()).apply()
    }
    
    /**
     * 加载默认实例（首次启动）
     */
    private fun loadDefaultInstances() {
        val defaultInstance = OpenClawInstance(
            id = UUID.randomUUID().toString(),
            name = "翎云",
            url = "wss://api.lingjiangapp.online/relay",
            status = ConnectionStatus.DISCONNECTED
        )
        
        _instances.value = listOf(defaultInstance)
        saveInstances()
    }
    
    /**
     * 添加实例
     */
    fun addInstance(name: String, url: String) {
        val newInstance = OpenClawInstance(
            id = UUID.randomUUID().toString(),
            name = name,
            url = url,
            status = ConnectionStatus.DISCONNECTED
        )
        
        _instances.value = _instances.value + newInstance
        saveInstances()
    }
    
    /**
     * 编辑实例
     */
    fun updateInstance(instanceId: String, name: String, url: String) {
        val oldInstance = _instances.value.find { it.id == instanceId } ?: return
        val urlChanged = oldInstance.url != url
        
        // 如果 URL 变了，先断开旧连接
        if (urlChanged) {
            disconnectInstance(instanceId)
        }
        
        _instances.value = _instances.value.map { instance ->
            if (instance.id == instanceId) {
                instance.copy(name = name, url = url)
            } else {
                instance
            }
        }
        saveInstances()
    }
    
    /**
     * 删除实例
     */
    fun removeInstance(instanceId: String) {
        disconnectInstance(instanceId)
        _instances.value = _instances.value.filter { it.id != instanceId }
        saveInstances()
        
        if (_currentInstance.value?.id == instanceId) {
            _currentInstance.value = null
        }
    }
    
    /**
     * 连接实例
     */
    fun connectInstance(instanceId: String) {
        viewModelScope.launch {
            val instance = _instances.value.find { it.id == instanceId } ?: return@launch
            
            // 显示开始连接提示
            _toastMessage.value = "正在连接到中继服务器..."
            
            // 更新状态为连接中
            updateInstanceStatus(instanceId, ConnectionStatus.CONNECTING)
            
            try {
                // 创建 RelayClient
                val client = RelayClient(instance.url, onToast = ::showToast)
                relayClients[instanceId] = client
                
                // 监听连接状态
                launch {
                    client.isConnected.collect { connected ->
                        if (connected) {
                            updateInstanceStatus(instanceId, ConnectionStatus.CONNECTED)
                            updateInstanceClientId(instanceId, client.getClientId())
                            _relayConnected.value = true
                        } else {
                            updateInstanceStatus(instanceId, ConnectionStatus.DISCONNECTED)
                            // 检查是否还有其他连接中的实例
                            _relayConnected.value = relayClients.any { (id, c) ->
                                id != instanceId && c.isConnected.value
                            }
                        }
                    }
                }
                
                // 连接
                client.connect()
                
            } catch (e: Exception) {
                updateInstanceStatus(instanceId, ConnectionStatus.DISCONNECTED)
                _toastMessage.value = "连接失败：${e.message}"
            }
        }
    }
    
    /**
     * 断开实例
     */
    fun disconnectInstance(instanceId: String) {
        relayClients[instanceId]?.disconnect()
        relayClients.remove(instanceId)
        updateInstanceStatus(instanceId, ConnectionStatus.DISCONNECTED)
    }
    
    /**
     * 切换当前实例
     */
    fun selectInstance(instanceId: String) {
        val instance = _instances.value.find { it.id == instanceId }
        _currentInstance.value = instance
    }
    
    /**
     * 获取实例的 RelayClient
     */
    fun getRelayClient(instanceId: String): RelayClient? {
        return relayClients[instanceId]
    }
    
    /**
     * 更新实例状态
     */
    private fun updateInstanceStatus(instanceId: String, status: ConnectionStatus) {
        _instances.value = _instances.value.map { instance ->
            if (instance.id == instanceId) {
                instance.copy(
                    status = status,
                    lastSeen = if (status == ConnectionStatus.CONNECTED) {
                        System.currentTimeMillis()
                    } else {
                        instance.lastSeen
                    }
                )
            } else {
                instance
            }
        }
    }
    
    /**
     * 更新实例 clientId
     */
    private fun updateInstanceClientId(instanceId: String, clientId: String?) {
        _instances.value = _instances.value.map { instance ->
            if (instance.id == instanceId) {
                instance.copy(clientId = clientId)
            } else {
                instance
            }
        }
    }
    
    /**
     * 显示 Toast 消息
     */
    private fun showToast(message: String) {
        _toastMessage.value = message
    }
    
    /**
     * 清除 Toast 消息
     */
    fun clearToast() {
        _toastMessage.value = null
    }
    
    override fun onCleared() {
        super.onCleared()
        // 清理所有连接
        relayClients.values.forEach { it.disconnect() }
        relayClients.clear()
    }
}
