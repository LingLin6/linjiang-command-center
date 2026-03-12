package com.linjiang.command.viewmodel

import androidx.lifecycle.ViewModel
import com.linjiang.command.data.model.Alert
import com.linjiang.command.data.model.AlertLevel
import com.linjiang.command.data.model.InstanceHealth
import com.linjiang.command.data.model.OverallHealth
import com.linjiang.command.data.model.SubAgentTask
import com.linjiang.command.data.model.TaskStatus
import com.linjiang.command.data.model.TimelineEvent
import com.linjiang.command.network.RelayClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * 态势大盘 ViewModel
 * 
 * 聚合所有实例的健康数据、告警、任务、时间线，
 * 提供态势页需要的所有计算属性。
 */
class DashboardViewModel : ViewModel() {
    
    // 数据源（由 RelayClient collect 填充）
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
    
    private val _onlineInstanceCount = MutableStateFlow(0)
    val onlineInstanceCount: StateFlow<Int> = _onlineInstanceCount
    
    // 计算属性
    val activeTaskCount: Int
        get() = _subAgentTasks.value.count { it.status == TaskStatus.RUNNING }
    
    val unreadAlertCount: Int
        get() = _alerts.value.count { !it.isRead }
    
    val overallHealth: OverallHealth
        get() {
            val alerts = _alerts.value.filter { !it.isRead }
            return when {
                alerts.any { it.level == AlertLevel.CRITICAL } -> OverallHealth.CRITICAL
                alerts.any { it.level == AlertLevel.WARNING } -> OverallHealth.WARNING
                else -> OverallHealth.HEALTHY
            }
        }
    
    val latestAlert: Alert?
        get() = _alerts.value.filter { !it.isRead }.maxByOrNull { it.timestamp }
    
    /**
     * 从 RelayClient 同步数据
     * 在 MainActivity 的 LaunchedEffect 中 collect RelayClient 的 StateFlow，
     * 调用 updateXxx 方法更新本 ViewModel。
     */
    
    fun updateHealth(health: InstanceHealth) {
        _instanceHealthMap.value = _instanceHealthMap.value + (health.instanceId to health)
    }
    
    fun updateHealthMap(map: Map<String, InstanceHealth>) {
        _instanceHealthMap.value = map
    }
    
    fun updateAlerts(alerts: List<Alert>) {
        _alerts.value = alerts
    }
    
    fun addAlert(alert: Alert) {
        _alerts.value = _alerts.value + alert
    }
    
    fun updateTimelineEvents(events: List<TimelineEvent>) {
        _timelineEvents.value = events
    }
    
    fun updateSubAgentTasks(tasks: List<SubAgentTask>) {
        _subAgentTasks.value = tasks
    }
    
    fun updateTaskLogs(logs: Map<String, List<String>>) {
        _taskLogs.value = logs
    }
    
    fun updateOnlineCount(count: Int) {
        _onlineInstanceCount.value = count
    }
    
    fun markAlertRead(alertId: String) {
        _alerts.value = _alerts.value.map {
            if (it.id == alertId) it.copy(isRead = true) else it
        }
    }
    
    fun markAllAlertsRead() {
        _alerts.value = _alerts.value.map { it.copy(isRead = true) }
    }
}
