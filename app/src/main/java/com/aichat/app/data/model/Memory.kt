package com.aichat.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "memories")
data class Memory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val content: String,
    val category: String = "general",
    val source: String = "manual",
    val conversationId: String? = null,
    val importance: Int = 5,
    val tags: String = "",
    val embedding: String? = null,
    val accessCount: Int = 0,
    val lastAccessAt: Long = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
