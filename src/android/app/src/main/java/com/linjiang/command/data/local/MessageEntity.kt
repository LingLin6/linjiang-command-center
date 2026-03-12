package com.linjiang.command.data.local

import androidx.room.*
import com.linjiang.command.data.model.Message
import com.linjiang.command.data.model.MessageLifeState
import com.linjiang.command.data.model.MessageType

@Entity(
    tableName = "conversations",
    indices = [
        Index(value = ["instance_id", "timestamp"]),
        Index(value = ["instance_id", "message_id"], unique = true)
    ]
)
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "instance_id") val instanceId: String,
    @ColumnInfo(name = "message_id") val messageId: String,
    @ColumnInfo(name = "role") val role: String,  // "user" / "assistant" / "system"
    @ColumnInfo(name = "content") val content: String,
    @ColumnInfo(name = "content_type") val contentType: String = "text",
    @ColumnInfo(name = "timestamp") val timestamp: Long,
    @ColumnInfo(name = "is_read") val isRead: Boolean = true
)

@Entity(tableName = "conversation_meta")
data class ConversationMeta(
    @PrimaryKey @ColumnInfo(name = "instance_id") val instanceId: String,
    @ColumnInfo(name = "last_scroll_position") val lastScrollPosition: Int = 0,
    @ColumnInfo(name = "last_active_time") val lastActiveTime: Long = 0,
    @ColumnInfo(name = "draft_text") val draftText: String = ""
)

/**
 * MessageEntity → Message 转换
 */
fun MessageEntity.toMessage(): Message = Message(
    id = messageId,
    from = role,
    content = content,
    timestamp = timestamp,
    type = when (role) {
        "user" -> MessageType.USER
        "assistant" -> MessageType.ASSISTANT
        else -> MessageType.SYSTEM
    },
    lifeState = MessageLifeState.COMPLETE
)

/**
 * Message → MessageEntity 转换
 */
fun Message.toEntity(instanceId: String): MessageEntity = MessageEntity(
    instanceId = instanceId,
    messageId = id,
    role = when (type) {
        MessageType.USER -> "user"
        MessageType.ASSISTANT -> "assistant"
        MessageType.SYSTEM -> "system"
    },
    content = content,
    timestamp = timestamp,
    isRead = true
)
