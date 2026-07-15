package com.aichat.app.agent

/**
 * Context passed to every tool execution.
 * Provides session, permission, and cancellation information.
 */
data class ToolContext(
    val sessionId: String,
    val conversationId: String,
    val userId: String = "default",
    val workingDirectory: String = "/storage/emulated/0/.Download/Python/APP/AIchat",
    val isCancelled: () -> Boolean = { false }
)
