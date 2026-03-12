package com.linjiang.command.data.model

/**
 * 实例健康数据
 */
data class InstanceHealth(
    val instanceId: String,
    val cpuPercent: Float = 0f,
    val memoryUsedGb: Float = 0f,
    val memoryTotalGb: Float = 0f,
    val diskPercent: Float = 0f,
    val uptime: Long = 0,
    val activeSubAgents: Int = 0,
    val lastUpdate: Long = System.currentTimeMillis()
)

/**
 * 告警级别
 */
enum class AlertLevel {
    INFO, WARNING, CRITICAL
}

/**
 * 告警
 */
data class Alert(
    val id: String = "${System.currentTimeMillis()}-${(Math.random() * 1000).toInt()}",
    val instanceId: String,
    val level: AlertLevel = AlertLevel.INFO,
    val title: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)

/**
 * 时间线事件
 */
data class TimelineEvent(
    val id: String = "${System.currentTimeMillis()}-${(Math.random() * 1000).toInt()}",
    val instanceId: String,
    val instanceName: String = "",
    val eventType: String,  // task_completed, task_failed, deploy, alert, idle
    val title: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Sub-agent 任务状态
 */
enum class TaskStatus {
    PENDING, RUNNING, COMPLETED, FAILED
}

/**
 * Sub-agent 任务
 */
data class SubAgentTask(
    val id: String,
    val instanceId: String,
    val label: String,
    val status: TaskStatus = TaskStatus.PENDING,
    val progress: Int? = null,  // null = indeterminate
    val startTime: Long = System.currentTimeMillis(),
    val duration: Long = 0      // 秒
)

/**
 * 全局健康状态
 */
enum class OverallHealth {
    HEALTHY, WARNING, CRITICAL
}
