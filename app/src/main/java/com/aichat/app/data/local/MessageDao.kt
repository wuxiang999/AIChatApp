package com.aichat.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aichat.app.data.model.Message
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY `index` ASC")
    fun getMessagesForConversation(conversationId: String): Flow<List<Message>>

    @Query("SELECT MAX(`index`) FROM messages WHERE conversationId = :conversationId")
    suspend fun getMaxIndexForConversation(conversationId: String): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: Message)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<Message>)

    @Query("DELETE FROM messages WHERE conversationId = :conversationId")
    suspend fun deleteMessagesForConversation(conversationId: String)

    @Query("UPDATE messages SET content = :content, isStreaming = :isStreaming WHERE conversationId = :conversationId AND `index` = :index")
    suspend fun updateMessageContent(conversationId: String, index: Int, content: String, isStreaming: Boolean)

    @Query("UPDATE messages SET imageUris = :imageUris WHERE conversationId = :conversationId AND `index` = :index")
    suspend fun updateMessageImages(conversationId: String, index: Int, imageUris: String)
}
