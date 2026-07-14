package com.aichat.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * MCP(Model Context Protocol) 服务器配置实体。
 * url 为 HTTP/SSE 端点，enabled 控制是否注入到对话上下文。
 */
@Entity(tableName = "mcp_servers")
data class McpServer(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val url: String,
    val enabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
