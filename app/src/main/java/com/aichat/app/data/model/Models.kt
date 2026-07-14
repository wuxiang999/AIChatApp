package com.aichat.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "conversations")
data class Conversation(
    @PrimaryKey val id: String,
    val title: String,
    val createdAt: Date,
    val updatedAt: Date,
    val model: String = "gpt-3.5-turbo",
    val apiEndpoint: String? = null
)

@Entity(tableName = "messages", primaryKeys = ["conversationId", "index"])
data class Message(
    val conversationId: String,
    val index: Int,
    val role: String,
    val content: String,
    val timestamp: Date,
    val isStreaming: Boolean = false,
    val imageUris: String? = null,
    val reasoningContent: String? = null,
    val isRevoked: Boolean = false
)

@Entity(tableName = "api_endpoints")
data class ApiEndpoint(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val url: String,
    val apiKey: String,
    val isSelected: Boolean = false
)

data class Announcement(
    val content: String,
    val enabled: Boolean,
    val updatedAt: String
)

data class ModelInfo(
    val id: String,
    val name: String
)

data class ImageGenerationResult(
    val success: Boolean,
    val imageUrls: List<String>?,
    val reply: String?,
    val error: String?
)

data class UserMessage(
    val text: String,
    val images: List<String> = emptyList()
)
