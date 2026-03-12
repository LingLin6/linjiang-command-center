package com.linjiang.command.data.model

/**
 * OpenClaw 实例数据模型
 * 
 * @property id 实例唯一标识符
 * @property name 实例名称
 * @property url WebSocket 连接地址
 * @property clientId 服务器分配的客户端 ID
 * @property status 连接状态
 * @property lastSeen 最后连接时间戳
 */
data class OpenClawInstance(
    val id: String,
    val name: String,
    val url: String,
    val emoji: String = "🤖",
    val clientId: String? = null,
    val status: ConnectionStatus = ConnectionStatus.DISCONNECTED,
    val lastSeen: Long = System.currentTimeMillis()
)

/**
 * 连接状态枚举
 */
enum class ConnectionStatus {
    CONNECTED,      // 已连接
    DISCONNECTED,   // 已断开
    CONNECTING      // 连接中
}
