package com.linjiang.command.data.model

/**
 * 消息数据模型
 * 
 * @property id 消息唯一标识符
 * @property from 发送者标识
 * @property content 消息内容
 * @property timestamp 消息时间戳
 * @property type 消息类型
 * @property lifeState 消息生命周期状态（拟人化）
 * @property retryable 是否可重试
 */
data class Message(
    val id: String,
    val from: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val type: MessageType = MessageType.ASSISTANT,
    val lifeState: MessageLifeState = MessageLifeState.COMPLETE,
    val retryable: Boolean = false,
    val isStreaming: Boolean = false,
    val streamMessageId: String? = null
)

/**
 * 消息类型枚举
 */
enum class MessageType {
    USER,       // 用户消息
    ASSISTANT,  // 助手消息
    SYSTEM      // 系统消息
}

/**
 * 消息生命周期状态（拟人化）
 * 
 * 用户消息状态流：BORN → FLYING → RECEIVED → COMPLETE
 * 失败时：BORN → FLYING → LOST
 */
enum class MessageLifeState {
    BORN,           // 消息诞生（用户刚点击发送）
    FLYING,         // 消息正在飞向翎绛（WebSocket 传输中）
    RECEIVED,       // 翎绛收到了（服务器确认）
    COMPLETE,       // 完成（正常消息、助手回复）
    LOST            // 消息丢失（发送失败，可重试）
}

/**
 * AI 思考状态
 * 用于在对话界面显示 AI 当前在做什么
 */
enum class AiThinkingState {
    IDLE,           // 空闲
    RECEIVED,       // 收到了你的消息
    THINKING,       // 在整理思路
    SEARCHING,      // 在翻记忆 / 查资料
    WRITING,        // 在组织语言
    STREAMING,      // 正在说...
}

/**
 * AI 思考状态的拟人化描述
 */
fun AiThinkingState.toDisplayText(instanceName: String, instanceEmoji: String): String {
    return when (this) {
        AiThinkingState.IDLE -> ""
        AiThinkingState.RECEIVED -> "$instanceEmoji $instanceName 收到了"
        AiThinkingState.THINKING -> "$instanceEmoji $instanceName 在整理思路..."
        AiThinkingState.SEARCHING -> "$instanceEmoji $instanceName 在翻记忆..."
        AiThinkingState.WRITING -> "$instanceEmoji $instanceName 在组织语言..."
        AiThinkingState.STREAMING -> "$instanceEmoji $instanceName 正在说..."
    }
}

/**
 * 消息生命周期状态的拟人化描述
 */
fun MessageLifeState.toDisplayText(): String {
    return when (this) {
        MessageLifeState.BORN -> "刚说出口"
        MessageLifeState.FLYING -> "飘走了..."
        MessageLifeState.RECEIVED -> "收到了"
        MessageLifeState.COMPLETE -> ""
        MessageLifeState.LOST -> "迷路了"
    }
}

/**
 * 消息生命周期状态的图标
 */
fun MessageLifeState.toIcon(): String {
    return when (this) {
        MessageLifeState.BORN -> "💬"
        MessageLifeState.FLYING -> "💨"
        MessageLifeState.RECEIVED -> "👀"
        MessageLifeState.COMPLETE -> "✓"
        MessageLifeState.LOST -> "⚠️"
    }
}
