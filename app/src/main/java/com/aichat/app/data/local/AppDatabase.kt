package com.aichat.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.aichat.app.data.model.Agent
import com.aichat.app.data.model.ApiEndpoint
import com.aichat.app.data.model.Conversation
import com.aichat.app.data.model.McpServer
import com.aichat.app.data.model.Memory
import com.aichat.app.data.model.Message
import com.aichat.app.data.model.Skill
import java.util.Date

class Converters {
    @TypeConverter
    fun fromDate(date: Date): Long = date.time

    @TypeConverter
    fun toDate(timestamp: Long): Date = Date(timestamp)
}

@Database(
    entities = [Conversation::class, Message::class, ApiEndpoint::class, Agent::class,
        Skill::class, McpServer::class, Memory::class],
    version = 5,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun apiEndpointDao(): ApiEndpointDao
    abstract fun agentDao(): AgentDao
    abstract fun skillDao(): SkillDao
    abstract fun mcpServerDao(): McpServerDao
    abstract fun memoryDao(): MemoryDao
}
