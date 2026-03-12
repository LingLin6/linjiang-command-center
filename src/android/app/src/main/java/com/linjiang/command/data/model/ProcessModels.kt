package com.linjiang.command.data.model

/**
 * 消息处理阶段
 * 用于实时展示 AI 处理用户消息的过程
 */
enum class ProcessPhase {
    IDLE, THINKING, TOOL_CALLING, STREAMING, COMPLETE, ERROR, TIMEOUT
}

/**
 * 工具调用状态
 */
enum class ToolStatus {
    RUNNING, SUCCESS, FAILED
}

/**
 * 工具调用记录
 */
data class ToolCallRecord(
    val callId: String,
    val tool: String,
    val args: String,
    val status: ToolStatus = ToolStatus.RUNNING,
    val summary: String? = null,
    val startTime: Long = System.currentTimeMillis()
)

/**
 * 消息处理上下文（绑定到一条用户消息的 AI 回复过程）
 * 
 * 追踪从用户发送消息到 AI 回复完成的整个过程，
 * 包括思考、工具调用、流式文字输出等阶段。
 */
data class MessageProcessContext(
    val requestId: String,
    val phase: ProcessPhase = ProcessPhase.IDLE,
    val thinkingText: String = "",
    val toolCalls: List<ToolCallRecord> = emptyList(),
    val streamedText: String = "",
    val elapsedSeconds: Int = 0,
    val timeoutLimit: Int = 180
)
