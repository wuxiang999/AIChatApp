package com.aichat.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 记忆实体：跨会话持久化的长期记忆，注入到每轮对话的系统提示词中。
 * category: general / fact / preference 等
 */
@Entity(tableName = "memories")
data class Memory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val content: String,
    val category: String = "general",
    val createdAt: Long = System.currentTimeMillis()
)
