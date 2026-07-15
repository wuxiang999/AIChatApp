package com.aichat.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 8,
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

    companion object {
        val MIGRATION_5_6 = Migration(5, 6) { db ->
            db.execSQL("ALTER TABLE memories ADD COLUMN source TEXT NOT NULL DEFAULT 'manual'")
            db.execSQL("ALTER TABLE memories ADD COLUMN conversationId TEXT DEFAULT NULL")
            db.execSQL("ALTER TABLE memories ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
            db.execSQL("UPDATE memories SET updatedAt = createdAt")
        }

        val MIGRATION_6_7 = Migration(6, 7) { db ->
            db.execSQL("ALTER TABLE skills ADD COLUMN category TEXT NOT NULL DEFAULT 'general'")
            db.execSQL("ALTER TABLE skills ADD COLUMN tags TEXT NOT NULL DEFAULT ''")
        }

        val MIGRATION_7_8 = Migration(7, 8) { db ->
            db.execSQL("ALTER TABLE memories ADD COLUMN importance INTEGER NOT NULL DEFAULT 5")
            db.execSQL("ALTER TABLE memories ADD COLUMN tags TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE memories ADD COLUMN embedding TEXT DEFAULT NULL")
            db.execSQL("ALTER TABLE memories ADD COLUMN accessCount INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE memories ADD COLUMN lastAccessAt INTEGER NOT NULL DEFAULT 0")
        }
    }
}
