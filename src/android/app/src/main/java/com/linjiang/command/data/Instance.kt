package com.linjiang.command.data

/**
 * OpenClaw 实例数据模型（中继服务器返回的实例信息）
 */
data class Instance(
    val id: String,
    val name: String,
    val online: Boolean,
    val nickname: String? = null,
    val emoji: String = "🪶",
    val role: String? = null
) {
    /**
     * 获取显示名称（完整）
     * 例如：🪶 翎绛·虚拟机主实例
     */
    fun getDisplayName(): String {
        return when {
            nickname != null -> "$emoji $name·$nickname"
            role != null -> "$emoji $name（$role）"
            else -> "$emoji $name"
        }
    }
    
    /**
     * 获取简短名称
     * 例如：🪶 翎绛
     */
    fun getShortName(): String {
        return "$emoji $name"
    }
    
    /**
     * 获取消息气泡中显示的名称
     * 例如：翎绛·虚拟机
     */
    fun getBubbleName(): String {
        return when {
            nickname != null -> "$emoji $name·$nickname"
            else -> "$emoji $name"
        }
    }
}
