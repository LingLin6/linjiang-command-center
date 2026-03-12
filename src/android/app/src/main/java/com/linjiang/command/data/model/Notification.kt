package com.linjiang.command.data.model

/**
 * 通知数据模型
 * 
 * @property id 通知唯一标识符
 * @property title 通知标题
 * @property message 通知内容
 * @property severity 严重程度（success/error/info/warning）
 * @property timestamp 通知时间戳
 * @property sessionKey 关联的 sub-agent sessionKey
 */
data class Notification(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val message: String,
    val severity: String = "info",
    val timestamp: Long = System.currentTimeMillis(),
    val sessionKey: String? = null
)
