package com.aichat.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "agents")
data class Agent(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val systemPrompt: String,
    val avatar: String? = null,
    val isPreset: Boolean = false,
    val isSelected: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
