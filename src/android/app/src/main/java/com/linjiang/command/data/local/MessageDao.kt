package com.linjiang.command.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM conversations WHERE instance_id = :instanceId ORDER BY timestamp ASC")
    fun getMessagesByInstance(instanceId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM conversations WHERE instance_id = :instanceId ORDER BY timestamp ASC")
    suspend fun getMessagesByInstanceOnce(instanceId: String): List<MessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)

    @Query("DELETE FROM conversations WHERE instance_id = :instanceId")
    suspend fun deleteByInstance(instanceId: String)

    @Query("SELECT COUNT(*) FROM conversations WHERE instance_id = :instanceId AND is_read = 0")
    fun getUnreadCount(instanceId: String): Flow<Int>

    @Query("UPDATE conversations SET is_read = 1 WHERE instance_id = :instanceId")
    suspend fun markAllAsRead(instanceId: String)

    // 更新流式消息内容
    @Query("UPDATE conversations SET content = :content WHERE instance_id = :instanceId AND message_id = :messageId")
    suspend fun updateContent(instanceId: String, messageId: String, content: String)

    // 会话元数据
    @Query("SELECT * FROM conversation_meta WHERE instance_id = :instanceId")
    suspend fun getMeta(instanceId: String): ConversationMeta?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveMeta(meta: ConversationMeta)
}
