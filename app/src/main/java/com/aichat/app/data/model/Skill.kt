package com.aichat.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 技能实体：可被用户调用/注入系统提示词的预设能力模板。
 * promptTemplate 中可用 {{input}} 占位符表示用户输入。
 */
@Entity(tableName = "skills")
data class Skill(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String,
    val promptTemplate: String,
    val enabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
