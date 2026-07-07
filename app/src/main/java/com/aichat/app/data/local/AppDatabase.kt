package com.aichat.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.aichat.app.data.model.ApiEndpoint
import com.aichat.app.data.model.Conversation
import com.aichat.app.data.model.Message
import java.util.Date

class Converters {
    @TypeConverter
    fun fromDate(date: Date): Long = date.time

    @TypeConverter
    fun toDate(timestamp: Long): Date = Date(timestamp)
}

@Database(
    entities = [Conversation::class, Message::class, ApiEndpoint::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun apiEndpointDao(): ApiEndpointDao
}
